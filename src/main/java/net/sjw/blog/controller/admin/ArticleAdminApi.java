package net.sjw.blog.controller.admin;

import net.sjw.blog.entity.Article;
import net.sjw.blog.interceptor.CheckTooFrequentCommit;
import net.sjw.blog.service.ArticleService;
import net.sjw.blog.utils.R;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/article")
public class ArticleAdminApi {

    @Autowired
    private ArticleService articleService;


    @CheckTooFrequentCommit
    @PreAuthorize("@permission.admin()")
    @PostMapping
    public R postArticle(@RequestBody Article article) {
        return articleService.postArticle(article);
    }

    /**
     * 如果是多用户的，用户不可以删，删除只是修改状态
     * 管理员可以删除
     *
     * 做成真的删除
     * <p></p>
     * 到时候该状态的可以交给前端来控制状态的更新
     *
     *
     * @param articleId
     * @return
     */
    @PreAuthorize("@permission.admin()")
    @DeleteMapping("/{articleId}")
    public R deleteArticle(@PathVariable("articleId") String articleId) {
        return articleService.deleteArticle(articleId);
    }


    @CheckTooFrequentCommit
    @PreAuthorize("@permission.admin()")
    @PutMapping("/{articleId}")
    public R updateArticle(@PathVariable("articleId") String articleId,
                           @RequestBody Article article) {
        return articleService.updateArticle(articleId,article);
    }

    @GetMapping("/{articleId}")
    public R getArticle(@PathVariable("articleId") String articleId) {
        return articleService.getArticleById(articleId);
    }

    @GetMapping("/list/{page}/{size}")
    public R listArticle(@PathVariable("page") int page,
                         @PathVariable("size") int size,
                         @RequestParam(value = "state",required = false)String state,
                         @RequestParam(value = "keyword", required = false) String keyword,
                         @RequestParam(value = "categoryId", required = false) String categoryId) {
        return articleService.listArticles(page, size, keyword, categoryId,state);
    }

    @PreAuthorize("@permission.admin()")
    @PutMapping("/state/{articleId}")
    public R deleteArticleByUpdateState(@PathVariable("articleId") String articleId) {
        return articleService.deleteArticleByUpdateState(articleId);
    }

    @PreAuthorize("@permission.admin()")
    @PutMapping("/top/{articleId}")
    public R updateArticleStateTop(@PathVariable("articleId") String articleId) {
        return articleService.topArticle(articleId);
    }


    @GetMapping("/count")
    public R getArticleCount() {
        return articleService.getArticleCount();
    }
}
