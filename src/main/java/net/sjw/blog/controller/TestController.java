package net.sjw.blog.controller;

import com.wf.captcha.GifCaptcha;
import com.wf.captcha.SpecCaptcha;
import com.wf.captcha.base.Captcha;
import lombok.extern.slf4j.Slf4j;
import net.sjw.blog.entity.Comment;
import net.sjw.blog.entity.User;
import net.sjw.blog.mapper.CommentMapper;
import net.sjw.blog.service.UserService;
import net.sjw.blog.utils.Constants;
import net.sjw.blog.utils.CookieUtils;
import net.sjw.blog.utils.R;
import net.sjw.blog.utils.RedisUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.awt.*;
import java.util.Calendar;
import java.util.Date;
import java.util.Random;

@Slf4j
@Controller
public class TestController {

    @Autowired
    private RedisUtils redisUtils;

    @RequestMapping("/captcha")
    public void captcha(HttpServletRequest request, HttpServletResponse response) throws Exception {
        // 设置请求头为输出图片类型
        response.setContentType("image/gif");
        response.setHeader("Pragma", "No-cache");
        response.setHeader("Cache-Control", "no-cache");
        response.setDateHeader("Expires", 0);

        // 三个参数分别为宽、高、位数
//        SpecCaptcha specCaptcha = new SpecCaptcha(130, 48, 5);
        // gif类型
        GifCaptcha specCaptcha = new GifCaptcha(130, 48, 4);
        // 设置字体
//        specCaptcha.setFont(new Font("Verdana", Font.PLAIN, 32));  // 有默认字体，可以不用设置
        specCaptcha.setFont(Captcha.FONT_7);
        // 设置类型，纯数字、纯字母、字母数字混合
        specCaptcha.setCharType(Captcha.TYPE_ONLY_LOWER);

        // 验证码存入session
//        request.getSession().setAttribute("captcha", specCaptcha.text().toLowerCase());
        redisUtils.set(Constants.User.KEY_CAPTCHA_CONTENT + "1234"
                , specCaptcha.text().toLowerCase(), 60 * 2);
        // 输出图片流
        specCaptcha.out(response.getOutputStream());
    }

    @GetMapping("/redis")
    @ResponseBody
    public R getRedisCaptcha() {
        String captcha = (String) redisUtils.get(Constants.User.KEY_CAPTCHA_CONTENT + "1234");
        return R.SUCCESS().data("captcha", captcha);
    }

    @Autowired
    private UserService userService;

    @Autowired
    private CommentMapper commentMapper;
    @PostMapping("/comment")
    public R testComment(@RequestBody Comment comment,
                         HttpServletRequest request,
                         HttpServletResponse response) {
        String content = comment.getContent();
        log.info("comment ==> " + content);
        //还得知道是谁的评论，对这个评论进行身份的确定
        String tokenKey = CookieUtils.getCookie(request, Constants.User.COOKIE_TOKEN_KEY);
        if (tokenKey == null) {
            return R.ACCOUNT_NOT_LOGIN();
        }
        User user = userService.checkUser();
        if (user == null) {
            return R.ACCOUNT_NOT_LOGIN();
        }
        comment.setUserId(user.getId());
        comment.setUserAvatar(user.getAvatar());
        comment.setUserName(user.getUserName());
        comment.setCreateTime(new Date());
        comment.setUpdateTime(new Date());
        commentMapper.insert(comment);
        return R.SUCCESS("评论成功");
    }
}
