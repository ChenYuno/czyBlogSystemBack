package net.sjw.blog.service.impl;

import cn.hutool.core.lang.Snowflake;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.google.gson.Gson;
import com.wf.captcha.ArithmeticCaptcha;
import com.wf.captcha.GifCaptcha;
import com.wf.captcha.SpecCaptcha;
import com.wf.captcha.base.Captcha;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import net.sjw.blog.entity.*;
import net.sjw.blog.mapper.RefreshTokenMapper;
import net.sjw.blog.mapper.SettingMapper;
import net.sjw.blog.mapper.UserMapper;
import net.sjw.blog.service.UserService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import net.sjw.blog.utils.*;
import org.hyperic.sigar.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.awt.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author czy
 * @since 2020-10-13
 */
@Slf4j
@Service
@Transactional
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService, BaseService {

    @Autowired
    private Random random;
    public static final int[] captcha_front_types = {
            Captcha.FONT_1,
            Captcha.FONT_2,
//            Captcha.FONT_3,
            Captcha.FONT_4,
            Captcha.FONT_5,
            Captcha.FONT_6,
            Captcha.FONT_7,
//            Captcha.FONT_8,
            Captcha.FONT_9,
            Captcha.FONT_10,
    };

    @Autowired
    private RedisUtils redisUtils;
    @Autowired
    private UserMapper userMapper;

    @Autowired
    private SettingMapper settingMapper;

    @Autowired
    private RefreshTokenMapper refreshTokenMapper;

    @Autowired
    private BCryptPasswordEncoder bCryptPasswordEncoder;

    @Autowired
    private Gson gson;

    @Override
    public R initManagerAccount(User user, HttpServletRequest request) {
        //检查是否有初始化
        Setting managerAccountState = settingMapper.selectOne(Wrappers.<Setting>lambdaQuery().eq(Setting::getKey, Constants.Settings.MANAGER_ACCOUNT_INIT_STATE));
        if (managerAccountState != null) {
            return R.FAILED("管理员账号已经初始化了");
        }

        //检查参数
        if (TextUtils.isEmpty(user.getUserName())) {
            return R.FAILED("用户名不能为空");
        }
        if (TextUtils.isEmpty(user.getPassword())) {
            return R.FAILED("密码不能为空");
        }
        if (TextUtils.isEmpty(user.getEmail())) {
            return R.FAILED("邮箱不能为空");
        }
        //补充数据
        user.setRoles(Constants.User.ROLE_ADMIN);
        user.setAvatar(Constants.User.DEFAULT_AVATAR);
        user.setState(Constants.User.DEFALUT_STATE);
        log.info(request.getRemoteAddr());
        log.info(request.getLocalAddr());
        user.setLoginIp(request.getRemoteAddr());
        user.setRegIp(request.getRemoteAddr());

        //$2a$10$ckt8b.iB65xkojyyV04pp.oUTYNpuuQ1F41K.YRG8LL/4iK71aEB2
        user.setPassword(bCryptPasswordEncoder.encode(
                user.getPassword()
        ));

        user.setCreateTime(new Date());
        user.setUpdateTime(new Date());
        //保存到数据库
        userMapper.insert(user);
        //更新已经添加的标记
        Setting setting = new Setting();
        setting.setCreateTime(new Date());
        setting.setUpdateTime(new Date());
        setting.setKey(Constants.Settings.MANAGER_ACCOUNT_INIT_STATE);
        setting.setValue("1");
        settingMapper.insert(setting);
        return R.SUCCESS("初始化成功");
    }

    @Override
    public void createCaptcha(HttpServletResponse response, String key) throws Exception {
        if (TextUtils.isEmpty(key) || key.length() < 13) {
            return;
        }

        // 设置请求头为输出图片类型
        response.setContentType("image/*");
        response.setHeader("Pragma", "No-cache");
        response.setHeader("Cache-Control", "no-cache");
        response.setDateHeader("Expires", 0);
        int captcheType = random.nextInt(3);
        Captcha captcha;
        int width = 170;
        int height = 40;
        if (captcheType == 0) {
            captcha = new SpecCaptcha(width, height, 5);
            captcha.setCharType(Captcha.TYPE_ONLY_LOWER);
        } else if (captcheType == 1) {
            //gif
            captcha = new GifCaptcha(width, height, 5);
            captcha.setCharType(Captcha.TYPE_ONLY_LOWER);
        } else {
            captcha = new ArithmeticCaptcha(width, height);
            captcha.setLen(2);
        }

        int fontIndex = random.nextInt(captcha_front_types.length);
        log.info("captcha font type ==> " + fontIndex);
        captcha.setFont(captcha_front_types[fontIndex]);


        String content = captcha.text().toLowerCase();
        //保存到redis里面
        //删除时机
        //1、自然过期：就是5分钟之后直接过期
        //2、验证码用完之后删除
        //用完看get的地方
        redisUtils.set(Constants.User.KEY_CAPTCHA_CONTENT + key
                , content, 60 * 5);
        captcha.out(response.getOutputStream());
    }


    @Autowired
    private TaskService taskService;

    /**
     * 发送邮箱的验证码
     * 使用场景：注册、找回密码、修改邮箱（会输入新的邮箱）
     * 注册(register)：如果已经注册过了，就提示说，该邮箱已经注册
     * 找回密码(forget)：如果没有注册过，提示说该邮箱没有注册
     * 修改邮箱(update)（新的邮箱）：如果已经注册了，提示邮箱已经注册
     *
     * @param type
     * @param request
     * @param emailAddress
     * @return
     */
    @Override
    public R sendEmail(String type, HttpServletRequest request, String emailAddress,String captchaKey,String captchaCode) {
        String captchaCodeFromRedis = (String) redisUtils.get(Constants.User.KEY_CAPTCHA_CONTENT + captchaKey);
        if (TextUtils.isEmpty(captchaCode)) {
            return R.FAILED("请先输入验证码");
        }
        if (!TextUtils.isEmpty(captchaCodeFromRedis) && !captchaCodeFromRedis.equals(captchaCode)) {
            return R.FAILED("验证码错误");
        }
        if (TextUtils.isEmpty(emailAddress)) {
            return R.FAILED("邮箱不可以为空");
        }
        if ("register".equals(type) || "update".equals(type)) {
            User userByEmail = baseMapper.selectOne(Wrappers.<User>lambdaQuery()
                    .eq(User::getEmail, emailAddress));
            if (userByEmail != null) {
                return R.FAILED("该邮箱已经注册");
            }
        } else if ("forget".equals(type)) {
            User userByEmail = baseMapper.selectOne(Wrappers.<User>lambdaQuery()
                    .eq(User::getEmail, emailAddress));
            if (userByEmail == null) {
                return R.FAILED("该邮箱未注册");
            }
        }
        //1、防止暴力发送，就是不断发送同一个邮箱，间隔要超过30秒发送一次，同一个ip，
        //同一个ip1小时内最多只能发送10次（如果是短信最多只能发5次）
        String remoteAddr = request.getRemoteAddr();
        if (remoteAddr != null) {
            remoteAddr = remoteAddr.replaceAll(":", "_");
        }
        log.info("sendEmailIp ==> " + remoteAddr);

        Integer ipSendTime = (Integer) redisUtils.get(Constants.User.KEY_EMAIL_SEND_IP + remoteAddr);
        if (ipSendTime != null && ipSendTime > 10) {
            return R.FAILED("😔您发送验证码的太频繁了吧。。。");
        }
        Integer addressSendTime = (Integer) redisUtils.get(Constants.User.KEY_EMAIL_SEND_ADDRESS + emailAddress);
        if (addressSendTime != null) {
            return R.FAILED("您发送验证码的太频繁了吧。。。");
        }
        //检查邮箱地址是否正确
        boolean isEmailFormateOk = TextUtils.isEmailAddressOk(emailAddress);
        if (!isEmailFormateOk) {
            return R.FAILED("邮箱地址格式不正确");
        }
        //0~999999
        int code = random.nextInt(1000000);
        if (code < 100000) {
            code += 100000;
        }
        log.info("sendEmailCode ==> " + code);
        try {
            //异步发送邮件，增加响应速度
            taskService.sendEmailVerifyCode(String.valueOf(code), emailAddress);
        } catch (Exception e) {
            return R.FAILED("验证码发送失败，请稍后再试");
        }
        if (ipSendTime == null) {
            redisUtils.set(Constants.User.KEY_EMAIL_SEND_IP + remoteAddr, 1, 60 * 60);
        } else {
            redisUtils.incr(Constants.User.KEY_EMAIL_SEND_IP + remoteAddr, 1l);
        }
        redisUtils.set(Constants.User.KEY_EMAIL_SEND_ADDRESS + emailAddress, 1, 10);
        redisUtils.set(Constants.User.KEY_EMAIL_CODE_CONTENT + emailAddress, String.valueOf(code), 60 * 10);
        return R.SUCCESS("发送验证码成功!!!");
    }

    @Override
    public R register(User user, String emailCode, String captchaCode, String captchaKey, HttpServletRequest request) {
        //1、检查用户是否已经注册
        String username = user.getUserName();
        if (TextUtils.isEmpty(username)) {
            return R.FAILED("用户名不可以为空");
        }
        User userByName = baseMapper.selectOne(Wrappers.<User>lambdaQuery()
                .eq(User::getUserName, username));
        if (userByName != null) {
            return R.FAILED("该用户已经注册。。");
        }
        String email = user.getEmail();
        if (TextUtils.isEmpty(email)) {
            return R.FAILED("邮箱地址不能为空.");
        }
        if (!TextUtils.isEmailAddressOk(email)) {
            return R.FAILED("邮箱地址格式不正确");
        }
        //检查该邮箱是否已经注册
        User userByEmail = baseMapper.selectOne(Wrappers.<User>lambdaQuery()
                .eq(User::getEmail, email));
        if (userByEmail != null) {
            return R.FAILED("该邮箱地址已经注册");
        }
        //检查邮箱验证码是否正确
        String emailVerifyCode = (String) redisUtils.get(Constants.User.KEY_EMAIL_CODE_CONTENT + email);
        if (TextUtils.isEmpty(emailVerifyCode)) {
            return R.FAILED("邮箱验证码过期了，请重新发送");
        }
        if (!emailVerifyCode.equals(emailCode)) {
            return R.FAILED("邮箱验证码不正确");
        } else {
            //正确，干掉redis里面的内容
            redisUtils.del(Constants.User.KEY_EMAIL_CODE_CONTENT + email);
        }
        //检查图灵验证码是否正确
        String captchaVerifyCode = (String) redisUtils.get(Constants.User.KEY_CAPTCHA_CONTENT + captchaKey);
        if (TextUtils.isEmpty(captchaVerifyCode)) {
            return R.FAILED("人类验证码已经过期");
        }
        if (!captchaVerifyCode.equals(captchaCode)) {
            return R.FAILED("人类验证码不正确");
        } else {
            redisUtils.del(Constants.User.KEY_CAPTCHA_CONTENT + captchaKey);
        }
        //到这里就达到可以注册的条件
        String password = user.getPassword();
        if (TextUtils.isEmpty(password)) {
            return R.FAILED("密码不可以为空");
        }
        user.setPassword(bCryptPasswordEncoder.encode(user.getPassword()));
        //补全数据
        String ipAddress = request.getRemoteAddr();
        user.setRegIp(ipAddress);
        user.setLoginIp(ipAddress);
        user.setUpdateTime(new Date());
        user.setCreateTime(new Date());
        user.setAvatar(Constants.User.DEFAULT_AVATAR);
        user.setRoles(Constants.User.ROLE_NORMAL);
        user.setState("1");
        //保存到数据库中
        baseMapper.insert(user);
        return R.JOIN_IN_SUCCESS();
    }

    @Override
    public R doLogin(String captcha,
                     String captchaKey,
                     User user,
                     String from) {
        //from可能不存在值
        if (TextUtils.isEmpty(from) ||
                (Constants.FROM_MOBILE.equals(from) && !Constants.FROM_PC.equals(from))) {
            from = Constants.FROM_MOBILE;
        }
        HttpServletRequest request = getRequest();
        HttpServletResponse response = getResponse();
        String captchaValue = (String) redisUtils.get(Constants.User.KEY_CAPTCHA_CONTENT + captchaKey);
        if (!captcha.equals(captchaValue)) {
            return R.FAILED("人类验证码不正确");
        }
        //验证成功，删除redis里面的验证码
        redisUtils.del(Constants.User.KEY_CAPTCHA_CONTENT + captchaKey);
        //可能是账号也可能是邮箱
        String userName = user.getUserName();
        if (TextUtils.isEmpty(userName)) {
            return R.FAILED("账号不可以为空");
        }
        String password = user.getPassword();
        if (TextUtils.isEmpty(password)) {
            return R.FAILED("密码不可以为空");
        }
        User userFromDb = baseMapper.selectOne(Wrappers.<User>lambdaQuery()
                .eq(User::getUserName, userName));
        if (userFromDb == null) {
            //这里的user是email
            userFromDb = baseMapper.selectOne(Wrappers.<User>lambdaQuery()
                    .eq(User::getEmail, userName));
        }
        if (userFromDb == null) {
            return R.FAILED("用户名或密码不正确");
        }
        //用户存在
        //比对密码
        boolean matchs = bCryptPasswordEncoder.matches(password, userFromDb.getPassword());
        if (!matchs) {
            return R.FAILED("用户名或密码不正确");
        }
        //判断用户状态
        if (!"1".equals(userFromDb.getState())) {
            return R.ACCOUNT_DENIED();
        }
        //修改更新时间和登录ip
        userFromDb.setUpdateTime(new Date());
        userFromDb.setLoginIp(request.getRemoteAddr());
        createToken(response, userFromDb, from);
        return R.SUCCESS("登陆成功");
    }

    private String createToken(HttpServletResponse response, User userFromDb, String from) {
        String oldTokenKey = CookieUtils.getCookie(getRequest(), Constants.User.COOKIE_TOKEN_KEY);

        //确保单端登录，删除redis里的token
        RefreshToken oldRefreshToken = refreshTokenMapper.selectOne(Wrappers.<RefreshToken>lambdaQuery()
                .eq(RefreshToken::getUserId, userFromDb.getId()));

        if (oldRefreshToken != null)
            if (Constants.FROM_MOBILE.equals(from)) {
                redisUtils.del(Constants.User.KEY_TOKEN + oldRefreshToken.getMobileTokenKey());
                refreshTokenMapper.update(null, Wrappers.<RefreshToken>lambdaUpdate()
                        .set(RefreshToken::getMobileTokenKey, "")
                        .eq(RefreshToken::getMobileTokenKey, oldTokenKey));
            } else if (Constants.FROM_PC.equals(from)) {
                redisUtils.del(Constants.User.KEY_TOKEN + oldRefreshToken.getTokenKey());
                refreshTokenMapper.update(null, Wrappers.<RefreshToken>lambdaUpdate()
                        .set(RefreshToken::getTokenKey, "")
                        .eq(RefreshToken::getTokenKey, oldTokenKey));
            }
        //生成token  claims已经包含from
        Map<String, Object> claims = ClaimsUtils.userToClaims(userFromDb, from);
        //token默认为2个小时
        String token = JwtUtil.createToken(claims);
        //返回tokenMD5值，token会保存到redis里面
        //前端访问的时候携带token的MD5key，从redis中获取即可
        String tokenKey = from + DigestUtils.md5DigestAsHex(token.getBytes());
        //保存token到redis里，有效期为2小时，key是tokenKey
        redisUtils.set(Constants.User.KEY_TOKEN + tokenKey, token, Constants.TimeSecond.HOUR_2);
        //把tokenKey写到cookie里
        //这个要动态获取，可以从request里面获取
        CookieUtils.setUpCookie(response, Constants.User.COOKIE_TOKEN_KEY, tokenKey);
        //Double机制
        //生成refreshToken
        //先判断数据库里有没有refreshToken
        //如果有的话就更新
        //如果没有的话就创建
        RefreshToken refreshToken = refreshTokenMapper.selectOne(Wrappers.<RefreshToken>lambdaQuery()
                .eq(RefreshToken::getUserId, userFromDb.getId()));
        if (refreshToken == null) {
            refreshToken = new RefreshToken();
            refreshToken.setCreateTime(new Date());
            refreshToken.setUserId(userFromDb.getId());
        }


        //不管是过期了还是新登录都会重新生成
        String refreshTokenValue = JwtUtil.createRefreshToken(userFromDb.getId(), Constants.TimeMillions.MONTH);
        //保存到数据库refreshToken:tokenKey、用户Id
        refreshToken.setRefreshToken(refreshTokenValue);

        //要判断来源
        //如果是手机端的就设置到手机端，如果是pc端的就设置到pc端
        if (Constants.FROM_PC.equals(from)) {

            refreshToken.setTokenKey(tokenKey);
        } else {
            refreshToken.setMobileTokenKey(tokenKey);

        }

        refreshToken.setUpdateTime(new Date());
        String id = refreshToken.getId();
        if (TextUtils.isEmpty(id)) {
            refreshTokenMapper.insert(refreshToken);
        } else {
            refreshTokenMapper.updateById(refreshToken);
        }
        return tokenKey;
    }

    /**
     * 本质：
     * 通过携带的tokenKey检查用户是否登录，如果登录了，就返回用户信息
     *
     * @return
     */
    @Override
    public User checkUser() {
        //拿到token
        String tokenKey = CookieUtils.getCookie(getRequest(), Constants.User.COOKIE_TOKEN_KEY);

        if (TextUtils.isEmpty(tokenKey)) {
            log.info("checkUser token ==> " + tokenKey);
            return null;
        }

        User user = parseByTokenKey(tokenKey);
        //token中要解析此请求是什么端的
        String from = tokenKey.startsWith(Constants.FROM_PC) ?
                Constants.FROM_PC : Constants.FROM_MOBILE;

        if (user == null) {
            //说明解析出错或过期了
            //1、去mysql查询refreshToken
            //如果是从pc，就去查pc的tokenKey
            //如果是从mobile，就去查mobile的tokenKey
            RefreshToken refreshToken;
            if (Constants.FROM_PC.equals(from)) {
                refreshToken = refreshTokenMapper.selectOne(Wrappers.<RefreshToken>lambdaQuery()
                        .eq(RefreshToken::getTokenKey, tokenKey));
            } else {
                refreshToken = refreshTokenMapper.selectOne(Wrappers.<RefreshToken>lambdaQuery()
                        .eq(RefreshToken::getMobileTokenKey, tokenKey));
            }

            //2、如果不存在就是当前访问没有登陆，提示用户登录
            if (refreshToken == null) {
                log.info("refreshToken == null ...");
                return null;
            }
            //3、如果存在，就解析refreshToken
            try {
                log.info("存在refreshToken 解析refreshToken");
                JwtUtil.parseJWT(refreshToken.getRefreshToken());
                //5、如果refreshToken有效，创建新的token，和新的refreshToken
                String userId = refreshToken.getUserId();
                User userFromDb = userMapper.selectOne(Wrappers.<User>lambdaQuery()
                        .eq(User::getId, userId));

                String newTokenKey = createToken(getResponse(), userFromDb, from);
                log.info("409行  createTokenKey" + newTokenKey);
                return parseByTokenKey(newTokenKey);
            } catch (Exception exception) {
                //4、如果refreshToken过期了就是当前访问没有登陆，提示用户登录
                log.info("refreshToken过期了就是当前访问没有登陆，提示用户登录");
                return null;
            }
        }
        log.info("checkUser(不为空) ==> " + user.toString());
        return user;
    }

    /**
     * 得到用户信息
     *
     * @param userId
     * @return
     */
    @Override
    public R getUserInfo(String userId) {
        //从数据库中获取
        User user = baseMapper.selectById(userId);
        //判断结果
        if (user == null) {
            return R.FAILED("用户不存在");
        }
        //如果不存在，就返回不存在
        //如果存在，就复制对象，清空密码，Email，登录ip，注册ip
        user.setPassword("");
        user.setLoginIp("");
        user.setRegIp("");
        //返回结果
        return R.SUCCESS("获取成功").data("user", user);
    }

    @Override
    public R checkEmail(String email) {
        User user = userMapper.selectOne(Wrappers.<User>lambdaQuery()
                .eq(User::getEmail, email));
        return user == null ? R.FAILED("该邮箱未注册") : R.SUCCESS("该邮箱已经注册");
    }

    @Override
    public R checkUserName(String userName) {
        User user = userMapper.selectOne(Wrappers.<User>lambdaQuery()
                .eq(User::getUserName, userName));
        return user == null ? R.SUCCESS("该用户名可以使用") : R.FAILED("该用户名已存在");
    }

    /**
     * 更新用户信息
     *
     * @param userId
     * @param user
     * @return
     */
    @Override
    public R updateUserInfo(String userId, User user) {
        User userAccount = checkUser();

        if (userAccount == null) {
            return R.ACCOUNT_NOT_LOGIN();
        }
        //判断用户的ID是否一致，如果一致才可以修改
        if (!userAccount.getId().equals(userId)) {
            return R.PERMISSION_DENIED();
        }
        //可以进行修改
        //可以修改的有：
        //用户名
        String userName = user.getUserName();
        if (!TextUtils.isEmpty(userName) && !userName.equals(userAccount.getUserName())) {
            User userByUserName = userMapper.selectOne(Wrappers.<User>lambdaQuery()
                    .eq(User::getUserName, userName));
            if (userByUserName != null) {
                return R.FAILED("该用户名已经被注册");
            }
            userAccount.setUserName(userName);
        }
        //头像
        if (!TextUtils.isEmpty(user.getAvatar())) {
            userAccount.setAvatar(user.getAvatar());
        }
        userAccount.setUpdateTime(new Date());
        //签名 :可以为空
        userAccount.setSign(user.getSign());
        baseMapper.updateById(userAccount);
        String oldTokenKey = CookieUtils.getCookie(getRequest(), Constants.User.COOKIE_TOKEN_KEY);
        redisUtils.del(Constants.User.KEY_TOKEN + oldTokenKey);
        if (oldTokenKey.startsWith(Constants.FROM_PC)) {
            createToken(getResponse(), userAccount, Constants.FROM_PC);
        } else {
            createToken(getResponse(), userAccount, Constants.FROM_MOBILE);
        }

        return R.SUCCESS("用户信息更新成功");
    }

    public HttpServletRequest getRequest() {
        ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return requestAttributes.getRequest();
    }

    public HttpServletResponse getResponse() {
        ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return requestAttributes.getResponse();
    }

    /**
     * 删除用户，并不是真的删除
     * 而是修改状态
     * <p>
     * PS:需要管理员权限
     *
     * @param userId
     * @return
     */
    @Override
    public R deleteUserById(String userId) {


        //可以删除用户了
        int result = userMapper.update(null, Wrappers.<User>lambdaUpdate()
                .set(User::getState, "0").eq(User::getId, userId));
        if (result > 0) {
            return R.SUCCESS("删除成功");
        }
        return R.FAILED("用户不存在");
    }

    /**
     * 需要管理员权限
     *
     * @param page
     * @param size
     * @param userName
     * @param email
     * @return
     */
    @Override
    public R listUsers(int page, int size, String userName, String email) {


        //可以获取用户列表了
        //分页查询
        page = checkPage(page);
        size = checkSize(size);
        //根据注册日期来排序
        Page<User> all = baseMapper.selectPage(new Page<>(page, size),
                Wrappers.<User>lambdaQuery().select(
                        User::getId,
                        User::getUserName,
                        User::getRoles,
                        User::getAvatar,
                        User::getEmail,
                        User::getSign,
                        User::getState,
                        User::getRegIp,
                        User::getLoginIp,
                        User::getCreateTime,
                        User::getUpdateTime
                ).like(!TextUtils.isEmpty(userName), User::getUserName, userName)
                        .like(!TextUtils.isEmpty(email), User::getEmail, email)
                        .orderByDesc(User::getCreateTime));
        SearchPageList<User> results = new SearchPageList<>(all);
        return R.SUCCESS("获取用户列表成功").data("data", results);
    }

    /**
     * 更新密码
     *
     * @param verifyCode
     * @param user
     * @return
     */
    @Override
    public R updateUserPassword(String verifyCode, User user) {
        String email = user.getEmail();
        if (TextUtils.isEmpty(email)) {
            return R.FAILED("邮箱不可以为空");
        }
        //根据邮箱去redis里面拿验证码
        String redisVerifyCode = (String) redisUtils.get(Constants.User.KEY_EMAIL_CODE_CONTENT + email);
        if (redisVerifyCode == null || !redisVerifyCode.equals(verifyCode)) {
            return R.FAILED("验证码错误");
        }
        redisUtils.del(Constants.User.KEY_EMAIL_CODE_CONTENT + email);

        int result = baseMapper.update(null, Wrappers.<User>lambdaUpdate()
                .set(User::getPassword, bCryptPasswordEncoder.encode(user.getPassword()))
                .eq(User::getEmail, user.getEmail()));

        return result > 0 ? R.SUCCESS("修改密码成功") : R.FAILED("修改密码失败");
    }

    /**
     * 更新邮箱地址
     *
     * @param email
     * @param verifyCode
     * @return
     */
    @Override
    public R updateEmail(String email, String verifyCode) {
        //1、确保用户已经登录了
        User user = this.checkUser();
        //2、没有登陆
        if (user == null) {
            return R.ACCOUNT_NOT_LOGIN();
        }
        //2、对比验证码，确保新的邮箱是属于当前用户的
        String redisVerifyCode = (String) redisUtils.get(Constants.User.KEY_EMAIL_CODE_CONTENT + email);
        if (TextUtils.isEmpty(redisVerifyCode) || !redisVerifyCode.equals(verifyCode)) {
            return R.FAILED("验证码错误");
        }
        //校验验证码正确，删除验证码
        redisUtils.del(Constants.User.KEY_EMAIL_CODE_CONTENT + email);
        //可以修改邮箱
        int result = userMapper.update(null, Wrappers.<User>lambdaUpdate()
                .set(User::getEmail, email).eq(User::getId, user.getId()));
        return result > 0 ? R.SUCCESS("修改邮箱成功") : R.FAILED("修改邮箱失败");
    }


    @Override
    public R doLogout() {
        //拿到token_key
        String tokenKey = CookieUtils.getCookie(getRequest(), Constants.User.COOKIE_TOKEN_KEY);
        if (TextUtils.isEmpty(tokenKey)) {
            return R.ACCOUNT_NOT_LOGIN();
        }
        //删除redis里的token
        //因为各端是独立的，所以可以直接删除
        redisUtils.del(Constants.User.KEY_TOKEN + tokenKey);
        //删除mysql里面的refreshToken X  ==> 多端登录不做删除，只做更新
        if (tokenKey.startsWith(Constants.FROM_PC)) {
            refreshTokenMapper.update(null, Wrappers.<RefreshToken>lambdaUpdate()
                    .set(RefreshToken::getTokenKey, "").eq(RefreshToken::getTokenKey, tokenKey));
        } else {
            refreshTokenMapper.update(null, Wrappers.<RefreshToken>lambdaUpdate()
                    .set(RefreshToken::getMobileTokenKey, "").eq(RefreshToken::getMobileTokenKey, tokenKey));
        }
        CookieUtils.deleteCookie(getResponse(), Constants.User.COOKIE_TOKEN_KEY);
        return R.SUCCESS("退出登录成功");
    }


    @Autowired
    private Snowflake snowflake;

    @Autowired
    private CountDownLatchManager countDownLatchManager;

    /**
     * 获取pc登录二维码
     *
     * @return
     */
    @Override
    public R getPcLoginQrCodeInfo() {
        //尝试取出上一次的loginId
        String lastLoginId = CookieUtils.getCookie(getRequest(), Constants.User.LAST_REQUEST_LOGIN_ID);
        log.info(lastLoginId);

        redisUtils.del(Constants.User.KEY_PC_QR_LOGIN_ID + lastLoginId);
//        if (!TextUtils.isEmpty(lastLoginId)) {
//
//            Object lastGetTime = redisUtils.get(Constants.User.KEY_PC_QR_LOGIN_ID + lastLoginId);
//            if (lastGetTime != null) {
//                //先把redis里的删除
//
//                return R.FAILED("服务器繁忙，请稍后重试.");
//            }
//
//        }
        //1、生成一个唯一id
        long code;
        String codeStr;
        if (TextUtils.isEmpty(lastLoginId)) {
            code = snowflake.nextId();
            codeStr = String.valueOf(code);
        } else {
            codeStr = lastLoginId;
            code = Long.parseLong(lastLoginId);
        }

        //2、保存到redis里，值为false，时间为5分钟（二维码有效期）
        CookieUtils.setUpCookie(getResponse(), Constants.User.LAST_REQUEST_LOGIN_ID, codeStr);
        log.info(code + "|||");
        redisUtils.set(Constants.User.KEY_PC_QR_LOGIN_ID + codeStr,
                "false", Constants.TimeSecond.MIN_5);
        // 返回结果
//        redisUtils.set(Constants.User.KEY_PC_QR_LOGIN_ID+code,
//                false,
//                Constants.TimeSecond.MIN_5);
        //返回结果
        HttpServletRequest request = getRequest();
        StringBuffer url = request.getRequestURL();
        String servletPath = request.getServletPath();
        String originalHost = url.toString().replace(servletPath, "");
        log.info(originalHost + "/portal/image/qr-code/" + codeStr);
        return R.SUCCESS("获取成功").data("code", codeStr).data("url", originalHost + "/portal/image/qr-code/" + codeStr);
    }


    @Override
    public R parseToken() {
        User user = checkUser();
        if (user == null) {
            return R.FAILED("用户未登录");
        }
        return R.SUCCESS("获取用户成功").data("data", user);
    }

    /**
     * @param loginId snowflakeId
     * @return
     */
    private R checkLoginIdState(String loginId) {
        String loginState = (String) redisUtils.get(Constants.User.KEY_PC_QR_LOGIN_ID + loginId);
        if (loginState == null) {
            return R.QR_CODE_DEPRECATE();
        }
        //不为false，且不为null，那么就是用户id了，也就是登录成功了
        if (!TextUtils.isEmpty(loginState) && !loginState.equals("false")) {
            //创建token，就是走PC端登录
            User userFromDb = userMapper.selectById(loginState);
            createToken(getResponse(), userFromDb, Constants.FROM_PC);
            return R.LOGIN_SUCCESS();
        }
        return null;
    }

    /**
     * 检查二维码的登录状态
     * 结果有：
     * 1、登录成功（loginId对应的值为有ID内容）
     * 2、等待扫描（loginId对应的值为false）
     * 3、二维码已经过期了 loginId对应的值为null
     * <p>
     * 是被PC端轮询调用的
     *
     * @param loginId
     * @return
     */
    @Override
    public R checkQrCodeLoginState(String loginId) {
        //从redis里取值出来
        R result = checkLoginIdState(loginId);
        if (result != null) return result;
        //先等待一段时间，再去检查
        //如果超出了这个时间，我就们就返回等待扫码
        Callable<R> callable = new Callable<R>() {
            @Override
            public R call() throws Exception {

                log.info("start waiting for scan...");
                //先阻塞
                countDownLatchManager.getLatch(loginId).await(Constants.User.QR_CODE_STATE_CHECK_WAITING_TIME,
                        TimeUnit.SECONDS);
                //收到状态更新的通知，我们就检查loginId对应的状态
                log.info("start check login state...");
                R checkResult = checkLoginIdState(loginId);
                if (checkResult != null) return checkResult;
                //超时则返回等待扫描
                //完事后，删除对应的latch
                log.info("delete latch...");
                countDownLatchManager.deleteLatch(loginId);
                return R.WAITING_FOR_SCAN();
            }
        };
        try {
            return callable.call();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return R.WAITING_FOR_SCAN();
    }

    @Override
    public R updateQrCodeLoginState(String loginId) {
        //1、检查用户是否登录
        User User = checkUser();
        if (User == null) {
            return R.ACCOUNT_NOT_LOGIN();
        }
        //2、改变loginId对应的值=true
        redisUtils.set(Constants.User.KEY_PC_QR_LOGIN_ID + loginId, User.getId());
        //2.1、通知正在等待的扫描任务
        countDownLatchManager.onPhoneDoLogin(loginId);
        //3、返回结果
        return R.SUCCESS("登录成功.");
    }

    /**
     * 管理员重设所有人的密码
     *
     * @param userId
     * @param password
     * @return
     */
    @Override
    public R resetPassword(String userId, String password) {
        User user = baseMapper.selectById(userId);
        if (user == null) {
            return R.FAILED("用户不存在");
        }
        user.setPassword(bCryptPasswordEncoder.encode(password));
        baseMapper.updateById(user);
        return R.SUCCESS("密码重置成功！！");
    }

    @Override
    public R getRegisterCount() {
        int count = this.count();
        return R.SUCCESS("获取用户总数成功.").data("data", count);
    }

    /**
     * 找回密码验证邮箱的验证码接口业务
     *
     * @param email
     * @param verifyCode
     * @param captchaCode
     * @param captchaKey
     * @return
     */
    @Override
    public R checkEmailCode(String email, String verifyCode, String captchaCode, String captchaKey) {
        if (TextUtils.isEmpty(email)) {
            return R.FAILED("邮箱不能为空..");
        }
        if (TextUtils.isEmpty(captchaCode)) {
            return R.FAILED("验证码没有输入");
        }
        String captchaCodeFromRedis = (String) redisUtils.get(Constants.User.KEY_CAPTCHA_CONTENT + captchaKey);
        if (TextUtils.isEmpty(captchaCodeFromRedis)) {
            return R.FAILED("请重新获取验证码");
        }
        if (!captchaCodeFromRedis.equals(captchaCode)) {
            return R.FAILED("人类验证码匹配错误");
        }
        String verifyCodeFromRedis = (String) redisUtils.get(Constants.User.KEY_EMAIL_CODE_CONTENT + email);
        if (TextUtils.isEmpty(verifyCodeFromRedis)) {
            return R.FAILED("邮箱验证码已过期，请重新获取");
        }
        if (!verifyCode.equals(verifyCodeFromRedis)) {
            return R.FAILED("验证码错误");
        }
        return R.SUCCESS("通过验证(*^▽^*)");
    }




    /**
     * 解析此token是从PC端来的还是移动端来的
     *
     * @param tokenKey
     * @return
     */
    private String parseFrom(String tokenKey) {
        if (tokenKey == null) {
            return null;
        }
        String token = (String) redisUtils.get(Constants.User.KEY_TOKEN + tokenKey);
        if (token != null) {
            try {
                //说明有token，解析token
                Claims claims = JwtUtil.parseJWT(token);
                return ClaimsUtils.getFrom(claims);
            } catch (Exception e) {
                //说明解析出错或是redis的存储过期了
                return null;
            }
        }
        return null;
    }


    private User parseByTokenKey(String tokenKey) {
        if (tokenKey == null) {
            return null;
        }
        String token = (String) redisUtils.get(Constants.User.KEY_TOKEN + tokenKey);
        if (token != null) {
            try {
                //说明有token，解析token
                Claims claims = JwtUtil.parseJWT(token);
                return ClaimsUtils.claimsToUser(claims);
            } catch (Exception e) {
                //说明解析出错或是redis的存储过期了
                return null;
            }
        }
        return null;
    }


    /**
     * 服务器的信息
     * @return
     */
    @Override
    public R getSystemInfo() {
        SystemInfo systemInfo = new SystemInfo();
        try {
            Sigar sigar = new Sigar();
//            System.out.println(CpuPerc.format(sigar.getCpuPerc().getCombined()));

            String cpuDataString = CpuPerc.format(sigar.getCpuPerc().getCombined()).toString();
//            Double finalCpuData = Double.parseDouble(cpuDataString.substring(0, cpuDataString.indexOf("%")));


            log.info(cpuDataString);
            systemInfo.setCpu(Double.parseDouble(cpuDataString.substring(0, cpuDataString.indexOf("%"))));
            Mem mem = sigar.getMem();
            long memTotal = mem.getTotal();
            long memUsed = mem.getUsed();
            System.out.println("=memUsed======" + memUsed);
            System.out.println("=memTotal======" + memTotal);
            double meminfo = (memUsed / (double) memTotal) * 100;
            BigDecimal b = new BigDecimal(meminfo);
            double memVal = b.setScale(2, RoundingMode.HALF_UP).doubleValue();

            systemInfo.setMemory(memVal);
            System.out.println("使用了内存：" + memVal + "%");
            FileSystem[] fileSystemList = sigar.getFileSystemList();
            for (FileSystem pan : fileSystemList) {
                System.out.println("=======================================");
                String devName = pan.getDevName();
                FileSystemUsage fileSystemUsage = sigar.getFileSystemUsage(pan.getDirName());
                long fileSystemUsageTotal = fileSystemUsage.getTotal();
                long fileSystemUsageUsed = fileSystemUsage.getUsed();
                double panInfo = (fileSystemUsageUsed / (double) fileSystemUsageTotal) * 100;
                System.out.println("fileSystemUsageUsed :" + fileSystemUsageUsed);
                System.out.println("fileSystemUsageTotal :" + fileSystemUsageTotal);
                System.out.println(devName + " ==> " + panInfo + "%");

                Map<String, Object> disks = systemInfo.getDisks();
                disks.put(devName, new BigDecimal(panInfo).setScale(2, RoundingMode.HALF_UP).toString());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return R.SUCCESS().data("data", systemInfo);
    }

}
