package net.sjw.blog.config;

import lombok.extern.slf4j.Slf4j;
import net.sjw.blog.interceptor.ApiInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Slf4j
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Autowired
    private ApiInterceptor apiInterceptor;
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(apiInterceptor);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/dic/*.txt")
                .addResourceLocations("classpath:/static/dic/");
        registry.addResourceHandler("/qr/*.jpg")
                .addResourceLocations("classpath:/static/qr/");
    }
}
