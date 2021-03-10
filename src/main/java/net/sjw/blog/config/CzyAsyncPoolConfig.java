package net.sjw.blog.config;

import net.sjw.blog.hanlder.CzyBlogAsyncUncaughtExceptionHandler;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

@Configuration
@EnableAsync
public class CzyAsyncPoolConfig implements AsyncConfigurer {

    @Override
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(3);//线程池维护线程的最小数量.
        executor.setMaxPoolSize(10);//线程池维护线程的最大数量.
        executor.setQueueCapacity(25);//队列最大长度
        executor.setKeepAliveSeconds(200);//线程池维护线程所允许的空闲时间
        executor.setThreadNamePrefix("|>>>CzyBlogSystemAsync--->ThreadPoolTaskExecutor<<<|");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60 * 10);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();//如果不初始化，导致找到不到执行器
        return executor;
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return new CzyBlogAsyncUncaughtExceptionHandler();
    }
}
/*
*  <!-- 异步线程池 -->
    <bean id="threadPool"
        class="org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor">
        <!-- 核心线程数 -->
        <property name="corePoolSize" value="3" />
        <!-- 最大线程数 -->
        <property name="maxPoolSize" value="10" />
        <!-- 队列最大长度 >=mainExecutor.maxSize -->
        <property name="queueCapacity" value="25" />
        <!-- 线程池维护线程所允许的空闲时间 -->
        <property name="keepAliveSeconds" value="300" />
        <!-- 线程池对拒绝任务(无线程可用)的处理策略 ThreadPoolExecutor.CallerRunsPolicy策略 ,调用者的线程会执行该任务,如果执行器已关闭,则丢弃.  -->
        <property name="rejectedExecutionHandler">
            <bean class="java.util.concurrent.ThreadPoolExecutorAsyncConfiguration" />
        </property>
    </bean>
Reject策略预定义有四种：
(1)ThreadPoolExecutor.AbortPolicy策略，是默认的策略,处理程序遭到拒绝将抛出运行时 RejectedExecutionException。
(2)ThreadPoolExecutor.CallerRunsPolicy策略 ,调用者的线程会执行该任务,如果执行器已关闭,则丢弃.
(3)ThreadPoolExecutor.DiscardPolicy策略，不能执行的任务将被丢弃.
(4)ThreadPoolExecutor.DiscardOldestPolicy策略，如果执行程序尚未关闭，则位于工作队列头部的任务将被删除，然后重试执行程序（如果再次失败，则重复此过程）.
* */
