package net.sjw.blog.quartz;

import org.quartz.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class QuartzConfig {


    @Bean
    public JobDetail myJobDetail() {
        JobDetail myJobDetail =
                JobBuilder.newJob(UpdateCommentUserInfoJob.class)
                        .withIdentity("UpdateCommentUserInfoJobTask","UpdateCommentUserInfoJobGroup").storeDurably().build();
        return myJobDetail;
    }

    @Bean
    public CronTrigger myCronTrigger() {
        CronScheduleBuilder cron = CronScheduleBuilder.cronSchedule("0 0 3 * * ?");
        CronTrigger cronTrigger = TriggerBuilder.newTrigger()
                .forJob("UpdateCommentUserInfoJobTask","UpdateCommentUserInfoJobGroup")
                .withIdentity("CronTrigger--UpdateCommentUserInfoJob", "TriggerGtoup--UpdateCommentUserInfoJob")
                .withSchedule(cron).build();
        return cronTrigger;
    }
}
