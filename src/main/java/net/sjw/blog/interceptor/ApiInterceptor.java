package net.sjw.blog.interceptor;

import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import net.sjw.blog.utils.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.lang.reflect.Method;

@Slf4j
@Component
public class ApiInterceptor extends HandlerInterceptorAdapter {


    @Autowired
    private RedisUtils redisUtils;

    @Autowired
    private Gson gson;
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (handler instanceof HandlerMethod) {
            HandlerMethod handlerMethod = (HandlerMethod) handler;

            CheckTooFrequentCommit methodAnnotation = handlerMethod.getMethodAnnotation(CheckTooFrequentCommit.class);
            if (methodAnnotation != null) {

                String methodName = handlerMethod.getMethod().getName();

                //所有提交内容的方法，必须是登录的用户，所以使用token作为key来记录请求的频率
                String tokenKey = CookieUtils.getCookie(request, Constants.User.COOKIE_TOKEN_KEY);
                log.info("tokenKey ==> " + tokenKey);
                if (!TextUtils.isEmpty(tokenKey)) {
                    String hasCommit = (String) redisUtils.get(Constants.User.KEY_COMMIT_TOKEN_RECORD + tokenKey+ methodName);
                    if (!TextUtils.isEmpty(hasCommit)) {
                        //从redis里获取，判断是否存在，如果存在，则返回提交太频繁
                        R failed = R.FAILED("提交过于频繁，请稍后再试。。。");
                        response.setCharacterEncoding("UTF-8");
                        response.setContentType("application/json");
                        PrintWriter writer = response.getWriter();
                        writer.write(gson.toJson(failed));
                        writer.flush();
                        return false;
                    } else {
                        //如果不存在，说明可以提交，并且记录此次提交
                        redisUtils.set(Constants.User.KEY_COMMIT_TOKEN_RECORD + tokenKey + methodName,
                                "true", Constants.TimeSecond.SECOND_TEN);
                    }
                }
                //去判断是不是真的提交太频繁了
            }
        }
        return true;
    }
}
