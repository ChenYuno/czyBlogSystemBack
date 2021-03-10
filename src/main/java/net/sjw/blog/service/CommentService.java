package net.sjw.blog.service;

import net.sjw.blog.entity.Comment;
import com.baomidou.mybatisplus.extension.service.IService;
import net.sjw.blog.utils.R;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author czy
 * @since 2020-10-13
 */
public interface CommentService extends IService<Comment> {

    R postComment(Comment comment);

    R listCommentByArticleId(String articleId, int page, int size, String isMore);

    R deleteCommentById(String commentId);

    R deleteComment(String commentId);

    R listComment(int page, int size, String commentId);

    R topComment(String commentId);

    R updateCommentUserInfo();

    R getCommentCount();
}
