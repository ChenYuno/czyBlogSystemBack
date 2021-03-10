package net.sjw.blog.controller.portal;

import net.sjw.blog.entity.Comment;
import net.sjw.blog.interceptor.CheckTooFrequentCommit;
import net.sjw.blog.service.CommentService;
import net.sjw.blog.utils.R;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/portal/comment")
public class CommentPortalApi {

    @Autowired
    private CommentService commentService;


    @CheckTooFrequentCommit
    @PostMapping
    public R postComment(@RequestBody Comment comment) {
        return commentService.postComment(comment);
    }
    @DeleteMapping("/{commentId}")
    public R deleteComment(@PathVariable("commentId") String commentId) {
        return commentService.deleteCommentById(commentId);
    }

    @GetMapping("/list/{articleId}/{page}/{size}")
    public R listComment(@PathVariable("articleId") String articleId,
                         @PathVariable("page") int page,
                         @PathVariable("size") int size,
                         @RequestParam(value = "isMore", required = false) String isMore) {
        return commentService.listCommentByArticleId(articleId, page, size,isMore);
    }
}
