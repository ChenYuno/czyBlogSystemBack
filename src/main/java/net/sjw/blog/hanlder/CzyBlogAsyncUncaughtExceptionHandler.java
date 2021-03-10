package net.sjw.blog.hanlder;

import lombok.extern.slf4j.Slf4j;
import net.sjw.blog.exception.CzyBlogAsyncException;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;

import java.lang.reflect.Method;

@Slf4j
public class CzyBlogAsyncUncaughtExceptionHandler implements AsyncUncaughtExceptionHandler {
    @Override
    public void handleUncaughtException(Throwable ex, Method method, Object... params) {
        log.info("CyBlogAsync method has uncaught exception, params: " + params);
        if (ex instanceof CzyBlogAsyncException) {
            CzyBlogAsyncException asyncException = (CzyBlogAsyncException) ex;
            log.info("asyncException:" + asyncException.getMessage());
        }
        log.error("Exception :", ex);
    }
}
