package net.sjw.blog.controller.user;

import com.wf.captcha.ArithmeticCaptcha;
import com.wf.captcha.GifCaptcha;
import com.wf.captcha.SpecCaptcha;
import com.wf.captcha.base.Captcha;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import lombok.extern.slf4j.Slf4j;
import net.sjw.blog.entity.User;
import net.sjw.blog.service.UserService;
import net.sjw.blog.utils.Constants;
import net.sjw.blog.utils.R;
import net.sjw.blog.utils.RedisUtils;
import net.sjw.blog.utils.TextUtils;
import org.checkerframework.checker.units.qual.A;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.awt.*;
import java.io.IOException;
import java.util.Random;

@Slf4j
@RestController
@RequestMapping("/user")
public class UserApi {


    @Autowired
    private UserService userService;

    /**
     * 初始化管理员账号init-admin
     */
    @PostMapping("admin_account")
    public R initManagerAccount(@RequestBody User user, HttpServletRequest request) {
        log.info(user.getUserName());
        log.info(user.getPassword());
        log.info(user.getEmail());

        return userService.initManagerAccount(user, request);
    }

    /**
     * 注册
     * 测试用户注册
     * 1、前端获取图灵验证码: localhost:9000/user/captcha?captcha_key=3815780579163
     * jzxmb
     * 2、输入邮箱获取邮箱验证码：localhost:9000/user/verify_code?email=xxxxxxxxxx@qq.com&type=register
     *
     * @param user
     * @return
     */
    @PostMapping("join_in")
    public R register(@RequestBody User user,
                      @RequestParam("email_code") String emailCode,
                      @RequestParam("captcha_code") String captchaCode,
                      @RequestParam("captcha_key") String captchaKey,
                      HttpServletRequest request) {
        return userService.register(user, emailCode, captchaCode, captchaKey, request);
    }


    /**
     * 登录
     * <p>
     * 需要提交的数据
     * 1、用户账号-可以昵称、邮箱、手机--->做唯一处理
     * 2、密码
     * 3、图灵验证码
     * 4、图灵验证码key
     * </p>
     *
     * @param captchaKey 图灵验证码key
     * @param captcha    图灵验证码
     * @param user       用户bean
     * @return
     */
    @PostMapping("/login/{captcha}/{captcha_key}")
    public R login(@PathVariable("captcha_key") String captchaKey,
                   @PathVariable("captcha") String captcha,
                   @RequestBody User user,
                   @RequestParam(value = "from", required = false) String from) {
        return userService.doLogin(captcha, captchaKey, user, from);
    }

    /**
     * 获取图灵验证码
     *
     * @return
     */
    @GetMapping("/captcha")
    public void getCaptcha(HttpServletResponse response,
                           @RequestParam("captcha_key") String key,
                           @RequestParam(value = "random",required = false) String random) {
        try {
            if (!TextUtils.isEmpty(random)) {
                key = random;
            }
            userService.createCaptcha(response, key);
        } catch (Exception e) {
            log.error(e.toString());
            e.printStackTrace();
        }
    }


    /**
     * 发送邮件email
     * <p>
     * 使用场景：注册、找回密码、修改邮箱（会输入新的邮箱）
     * 注册：如果已经注册过了，就提示说，该邮箱已经注册
     * 找回密码：如果没有注册过，提示说该邮箱没有注册
     * 修改邮箱（新的邮箱）：如果已经注册了，提示邮箱已经注册
     *
     * @param emailAddress
     * @return
     */
    @GetMapping("/verify_code")
    public R sendVerfiyCode(HttpServletRequest request,
                            @RequestParam("type") String type,
                            @RequestParam("email") String emailAddress,
                            @RequestParam("captcha_key") String captchaKey,
                            @RequestParam("captcha_code") String captchaCode) {
        log.info("email ==> " + emailAddress);
        return userService.sendEmail(type, request, emailAddress,captchaKey,captchaCode);
    }

    /**
     * 修改密码password
     * 修改密码
     * 普通做法： 通过比对来更新密码
     * <p>
     * 即可以找回密码，也可以修改密码
     * 发送验证码到邮箱/手机 --> 判断验证码是否正确来判断
     * 对应邮箱/手机号码所注册的账号是否属于你
     * <p>
     * 步骤：
     * 1、用户填写邮箱
     * 2、用户获取验证码 type = forget
     * 3、填写验证码
     * 4、填写新的密码
     * 5、提交数据
     * <p>
     * 数据包括
     * <p>
     * 1、邮箱和新的密码
     * 2、验证码
     * <p>
     * 如果验证码正确--> 所用邮箱注册的账号属于你的，就可以修改密码
     *
     * @param user
     * @return
     */
    @PutMapping("/password/{verifyCode}")
    public R updatePassword(@PathVariable("verifyCode") String verifyCode,
                            @RequestBody User user) {
        return userService.updateUserPassword(verifyCode, user);
    }

    /**
     * 获取作者信息
     *
     * @param userId
     * @return
     */
    @CrossOrigin
    @GetMapping("/user_info/{userId}")
    public R getUserInfo(@PathVariable("userId") String userId) {
        return userService.getUserInfo(userId);
    }

    /**
     * 修改用户信息User-Info
     *
     * <p>
     * 允许用户修改的内容
     * 1、头像
     * 2、用户名 （唯一的）
     * 2.5 签名
     * 3、密码（单独的接口）
     * 4、Email（唯一的，单独接口）
     * </p>
     *
     * @param user
     * @return
     */
    @PutMapping("/user_info/{userId}")
    public R updateUserInfo(@PathVariable("userId") String userId,
                            @RequestBody User user) {
        return userService.updateUserInfo(userId, user);
    }

    /**
     * 获取用户列表
     * 权限：管理员权限
     *
     * @param page
     * @param size
     * @return
     */
    @PreAuthorize("@permission.admin()")
    @GetMapping("/list")
    public R listUser(@RequestParam("page") int page,
                      @RequestParam("size") int size,
                      @RequestParam(value = "userName", required = false) String userName,
                      @RequestParam(value = "email", required = false) String email) {
        return userService.listUsers(page, size, userName, email);
    }

    /**
     * 删除用户，需要管理员权限
     *
     * @param userId
     * @return
     */
    @PreAuthorize("@permission.admin()")
    @DeleteMapping("/{userId}")
    public R deleteUser(@PathVariable("userId") String userId) {
        //判断当前操作用户是谁
        //根据当前角色是否可以删除
        return userService.deleteUserById(userId);
    }


    /**
     * 检查该Email是否已经注册
     *
     * @param email 邮箱地址
     * @return SUCCESS ==> 已经注册了,FAILED ==> 没有注册
     */
    @ApiResponses({
            @ApiResponse(code = 20000, message = "表示当前邮箱已经注册"),
            @ApiResponse(code = 40000, message = "表示当前邮箱未注册")
    })
    @GetMapping("/email")
    public R checkEmail(@RequestParam("email") String email) {
        return userService.checkEmail(email);
    }

    /**
     * 检查该userName是否已经注册
     *
     * @param userName 用户名
     * @return SUCCESS ==> 已经注册了,FAILED ==> 没有注册
     */
    @ApiResponses({
            @ApiResponse(code = 20000, message = "表示当前用户名已经注册"),
            @ApiResponse(code = 40000, message = "表示当前用户名未注册")
    })
    @GetMapping("/user_name")
    public R checkUserName(@RequestParam("userName") String userName) {
        return userService.checkUserName(userName);
    }


    /**
     * 1、必须已经登录了
     * 2、新的邮箱没有注册过
     * <p></p>
     * 用户的步骤：
     * 1、已经登录
     * 2、输入新的邮箱地址
     * 3、获取验证码 type = update
     * 4、输入验证码
     * 5、提交数据
     * <p></p>
     * 需要提交的数据
     * 1、新的邮箱地址
     * 2、验证码
     * 3、其他信息我们可以从token里获取
     *
     * @return
     */
    @PutMapping("/email")
    public R updateEmail(@RequestParam("email") String email,
                         @RequestParam("verify_code") String verifyCode) {
        return userService.updateEmail(email, verifyCode);
    }

    /**
     * 退出登录
     * <p></p>
     * 拿到token
     * ->删除redis里对应的token
     * ->删除mysql里面的refreshToken
     * ->删除cookie里的token_key
     */
    @GetMapping("/logout")
    public R logout() {
        return userService.doLogout();
    }


    /**
     * 获取登录二维码
     * 二维码的图片路劲
     * 二维码的内容字符串
     *
     * @return
     */
    @GetMapping("/pc-login-qr-code")
    public R getPcLoginQrCode() {
        //1、生成一个唯一id
        //2、保存到redis里，值为false，时间为5分钟（二维码有效期）
        //返回结果
        return userService.getPcLoginQrCodeInfo();
    }


    /**
     * 检查二维码的登录状态
     *
     * @return
     */
    @GetMapping("/qr-code-state/{loginId}")
    public R checkQrCodeLoginState(@PathVariable("loginId") String loginId) {
        return userService.checkQrCodeLoginState(loginId);
    }

    @PutMapping("/qr-code-state/{loginId}")
    public R updateQrCodeLoginState(@PathVariable("loginId") String loginId) {
        return userService.updateQrCodeLoginState(loginId);
    }


    @CrossOrigin
    @GetMapping("/check-token")
    public R parseToken() {
        return userService.parseToken();
    }

    @PutMapping("reset-password/{userId}")
    public R resetPassword(@PathVariable("userId") String userId,
                           @RequestParam("password") String password) {
        return userService.resetPassword(userId, password);
    }

    @PreAuthorize("@permission.admin()")
    @GetMapping("/register_count")
    public R getRegisterCount() {
        return userService.getRegisterCount();
    }

    @GetMapping("/check_email_code")
    public R checkEmailCode(@RequestParam("email") String email,
                            @RequestParam("verify_code") String verifyCode,
                            @RequestParam("captcha_code") String captchaCode,
                            @RequestParam("captcha_key") String captchaKey) {
        return userService.checkEmailCode(email, verifyCode, captchaCode, captchaKey);
    }

    @PreAuthorize("@permission.admin()")
    @GetMapping("/sysInfo")
    public R getSystemInfo() {
        return userService.getSystemInfo();
    }


}
