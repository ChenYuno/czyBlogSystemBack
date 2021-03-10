package net.sjw.blog.quartz;

import org.junit.jupiter.api.Test;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.LinkedList;
import java.util.List;

@SpringBootTest
public class testQuartz {


    @Autowired
    private Scheduler scheduler;
    @Test
    void test01() {
        try {
            String schedulerName = scheduler.getSchedulerName();
            System.out.println(schedulerName);
            List<String> triggerGroupNames = scheduler.getTriggerGroupNames();
            for (String triggerGroupName : triggerGroupNames) {
                System.out.println(triggerGroupName);
            }
            List<String> jobGroupNames = scheduler.getJobGroupNames();
            for (String jobGroupName : jobGroupNames) {
                System.out.println(jobGroupName);
            }
        } catch (SchedulerException e) {
            e.printStackTrace();
        }
        LinkedList<Integer> linkedList = new LinkedList<>();
        linkedList.addFirst(1);
    }
}
