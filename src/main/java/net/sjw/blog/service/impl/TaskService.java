package net.sjw.blog.service.impl;

import lombok.extern.slf4j.Slf4j;
import net.sjw.blog.entity.Article;
import net.sjw.blog.entity.Labels;
import net.sjw.blog.entity.User;
import net.sjw.blog.mapper.CommentMapper;
import net.sjw.blog.mapper.LabelsMapper;
import net.sjw.blog.utils.EmailSender;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

@Slf4j
@Service
public class TaskService {


    @Async
    public void sendEmailVerifyCode(String verifyCode,String emailAddress) throws Exception{
        EmailSender.sendRegisterVerifyCode(verifyCode, emailAddress);
    }

    @Autowired
    private LabelsMapper labelsMapper;

    @Async
    @Transactional
    public void setupLabels(String labels) {
        if (labels.equals("")) {
            return;
        }
        List<String> labelList = new ArrayList<>();
        if (labels.contains("-")) {
            labelList.addAll(Arrays.asList(labels.split("-")));
        } else {
            labelList.add(labels);
        }
        //入库统计
        for (String label : labelList) {

            int result = labelsMapper.updateCountByName(label);
            if (result == 0) {
                Labels newLabel = new Labels();
                newLabel.setName(label);
                newLabel.setCount(1);
                newLabel.setCreateTime(new Date());
                newLabel.setUpdateTime(new Date());
                labelsMapper.insert(newLabel);
            }

//            Labels labelsFromDb = labelsMapper.selectOne(Wrappers.<Labels>lambdaQuery()
//                    .eq(Labels::getName, label));
//            if (labelsFromDb == null) {
//                Labels newLabel = new Labels();
//                newLabel.setName(label);
//                newLabel.setCount(0);
//                newLabel.setCreateTime(new Date());
//                newLabel.setUpdateTime(new Date());
//                labelsMapper.insert(newLabel);
//            } else {
//                labelsFromDb.setUpdateTime(new Date());
//                Integer count = labelsFromDb.getCount();
//                labelsFromDb.setCount(count + 1);
//                labelsMapper.updateById(labelsFromDb);
//            }
        }
    }



    @Autowired
    private CommentMapper commentMapper;

    public void notifyCriticzedAuthor(String articleId, User user) {
        //评论人的信息
//        String email = user.getEmail();
        String userName = user.getUserName();
        //下面的文章里只有被评论人的文章标题和被评论人的email和名字
        Article article = commentMapper.queryCriticzedAuthorByArticle(articleId);
        try {
            EmailSender.sendCommentNotify(
                    "您好  亲爱的<" + article.getUser().getUserName() + ">," +
                            "您的文章：《" + article.getTitle() + " 》,被评论啦！~~  \n"
                            + "请好好交流，互相学习，大家共同进步！！！\n" +
                            "----By CzyBlog", article.getUser().getEmail()
            );
        } catch (Exception e) {
            log.info(e.getCause()+"");
        }
    }
}
