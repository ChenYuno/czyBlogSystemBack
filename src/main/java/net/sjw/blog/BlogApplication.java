package net.sjw.blog;

import cn.hutool.core.lang.Snowflake;
import com.google.gson.Gson;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

import java.text.SimpleDateFormat;
import java.util.Random;

@SpringBootApplication
@EnableSwagger2
public class BlogApplication {
    public static void main(String[] args) {
        SpringApplication.run(BlogApplication.class, args);
    }

    @Bean
    public Random createRandom() {
        return new Random();
    }

    @Bean
    public Gson createGson() {
        return new Gson();
    }

    @Bean
    public Snowflake snowflake() {
        return new Snowflake(0, 0);
    }


    @Bean("esDate")
    public SimpleDateFormat simpleDateFormat() {
        return  new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    }
}
