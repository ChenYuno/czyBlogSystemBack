package net.sjw.blog.controller.admin;

import net.sjw.blog.entity.Comment;
import net.sjw.blog.service.CommentService;
import net.sjw.blog.utils.R;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/comment")
public class CommentAdminApi {


    @Autowired
    private CommentService commentService;


    @PreAuthorize("@permission.admin()")
    @PutMapping("/{commentId}")
    public R deleteComment(@PathVariable("commentId") String commentId) {
        return commentService.deleteComment(commentId);
    }


    @PreAuthorize("@permission.admin()")
    @GetMapping("/list/{page}/{size}")
    public R listComment(@PathVariable("page") int page,
                         @PathVariable("size") int size,
                         @RequestParam(value = "commentId",required = false) String commentId) {
        return commentService.listComment(page, size,commentId);
    }

    @PreAuthorize("@permission.admin()")
    @PutMapping("/top/{commentId}")
    public R topComment(@PathVariable("commentId") String commentId) {
        return commentService.topComment(commentId);
    }

    @PreAuthorize("@permission.admin()")
    @PutMapping("/updateCommentUserInfo")
    public R updateCommentUserInfo() {
        return commentService.updateCommentUserInfo();
    }



    @PreAuthorize("@permission.admin()")
    @GetMapping("/count")
    public R getCommentCount() {
        return commentService.getCommentCount();
    }


}
