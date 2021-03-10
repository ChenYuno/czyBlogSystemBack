package net.sjw.blog.quartz;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import net.sjw.blog.entity.Comment;
import net.sjw.blog.entity.User;
import net.sjw.blog.mapper.CommentMapper;
import net.sjw.blog.mapper.UserMapper;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class UpdateCommentUserInfoJob implements Job {

    @Autowired
    private CommentMapper commentMapper;

    @Autowired
    private UserMapper userMapper;
    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        List<Comment> comments = commentMapper.selectList(null);
        for (int i = 0; i < comments.size(); i++) {
            Comment comment = comments.get(i);
            User user = userMapper.selectOne(Wrappers.<User>lambdaQuery()
                    .select(User::getUserName, User::getAvatar)
            .eq(User::getId,comment.getUserId()));

            comment.setUserName(user.getUserName());
            comment.setUserAvatar(user.getAvatar());
            commentMapper.updateById(comment);
            System.out.println("updateUserInfoFromComment Finish!!!!!!!!!!!!");
        }
    }
}
