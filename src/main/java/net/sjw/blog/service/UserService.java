package net.sjw.blog.service;

import net.sjw.blog.entity.User;
import com.baomidou.mybatisplus.extension.service.IService;
import net.sjw.blog.utils.R;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author czy
 * @since 2020-10-13
 */
public interface UserService extends IService<User> {
    R initManagerAccount(User user, HttpServletRequest request);

    void createCaptcha(HttpServletResponse response, String key) throws Exception;

    R sendEmail(String type,HttpServletRequest request, String emailAddress,String captchaKey,String captchaCode);

    R register(User user, String emailCode, String captchaCode, String captchaKey, HttpServletRequest request);

    R doLogin(String captcha, String captchaKey, User user, String from);

    User checkUser();

    R getUserInfo(String userId);

    R checkEmail(String email);

    R checkUserName(String userName);

    R updateUserInfo(String userId, User user);

    R deleteUserById(String userId);

    R listUsers(int page, int size, String userName, String email);

    R updateUserPassword(String verifyCode, User user);

    R updateEmail(String email, String verifyCode);

    R doLogout();

    R getPcLoginQrCodeInfo();

    R parseToken();

    R checkQrCodeLoginState(String loginId);

    R updateQrCodeLoginState(String loginId);

    R resetPassword(String userId, String password);

    R getRegisterCount();

    R checkEmailCode(String email, String verifyCode, String captchaCode, String captchaKey);

    R getSystemInfo();

}
