package net.sjw.blog.service.impl;

import net.sjw.blog.entity.User;
import net.sjw.blog.mapper.UserMapper;
import net.sjw.blog.service.UserService;
import net.sjw.blog.utils.Constants;
import net.sjw.blog.utils.CookieUtils;
import net.sjw.blog.utils.TextUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Service("permission")
public class PermissionService {

    @Autowired
    private UserService userService;
    /**
     * 判断是不是管理员
     * 这里的RequestAttributes是通过ThreadLocal设置进去的
     */
    public boolean admin() {
        //拿到request和response
        ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = requestAttributes.getRequest();
        String tokenKey = CookieUtils.getCookie(request, Constants.User.COOKIE_TOKEN_KEY);

        //没有令牌的key，没有登陆，不用继续执行
        if (TextUtils.isEmpty(tokenKey)) {
            return false;
        }
        User user = userService.checkUser();
        if (user == null) {
            return false;
        }

        if (Constants.User.ROLE_ADMIN.equals(user.getRoles())) {
            return true;
        }
        return false;

    }

}
