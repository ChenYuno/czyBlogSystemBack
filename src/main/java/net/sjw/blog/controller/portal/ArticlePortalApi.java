package net.sjw.blog.controller.portal;

import net.sjw.blog.service.ArticleService;
import net.sjw.blog.service.CategoryService;
import net.sjw.blog.utils.Constants;
import net.sjw.blog.utils.R;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/portal/article")
public class ArticlePortalApi {

    @Autowired
    private ArticleService articleService;

    @Autowired
    private CategoryService categoryService;

    /**
     * 获取文章列表
     * 权限：所有用户
     * 状态：必须已经发布的，置顶的由另外一个借口获取，其他的不可以从此借口获取
     *
     * @param page
     * @param size
     * @return
     */
    @GetMapping("/list/{page}/{size}")
    public R listArticle(@PathVariable("page") int page,
                         @PathVariable("size") int size,
                         @RequestParam(value = "categoryId", required = false) String categoryId) {
        return articleService.listArticles(page, size, null, categoryId, Constants.Article.STATE_PUBLIC);
    }

    @GetMapping("/publish/count")
    public R totalArticlePublishCount() {
        return articleService.totalArticlePublishCount();
    }

//    @GetMapping("/list/{categoryId}/{page}/{size}")
//    public R listArticleByCategoryId(@PathVariable("categoryId") String categoryId,
//                                     @PathVariable("page") int page,
//                                     @PathVariable("size") int size) {
//        return articleService.listArticles(page, size, null, categoryId, Constants.Article.STATE_PUBLIC);
//    }

    @GetMapping("/{articleId}")
    public R getArticleDetail(@PathVariable("articleId") String articleId) {
        return articleService.getArticleById(articleId);
    }

    /**
     * 通过标签来计算这个匹配度
     * 标签：有一个，或者多个（5个以内，包含5个）
     * 从里面随机拿一个标签出来 ---> 每一次获取的文章不要太雷同
     * 通过标签去查询类似的文章，所包含此标签的文章
     * 如果没有相关文章就从数据库中获取最新的文章
     *
     * @param articleId
     * @return
     */
    @GetMapping("/recommend/{articleId}/{size}")
    public R getRecommendArticle(@PathVariable("articleId") String articleId,
                                 @PathVariable("size") int size) {

        return articleService.listRecommendArticle(articleId, size);
    }

    @GetMapping("/top")
    public R getTopArticle() {
        return articleService.listTopArticles();
    }

    /**
     * 通过标签获取相关的文章列表
     *
     * @param label
     * @param page
     * @param size
     * @return
     */
    @GetMapping("/list/label/{label}/{page}/{size}")
    public R listArticleByLabel(@PathVariable("label") String label,
                                @PathVariable("page") int page,
                                @PathVariable("size") int size) {
        return articleService.listArticleByLabel(page, size, label);
    }

    /**
     * 获取标签云，用户点击标签，就会通过标签获取相关的文章列表
     * 任意用户
     *
     * @param size
     * @return
     */
    @GetMapping("/label/{size}")
    public R getLabels(@PathVariable("size") int size) {

        return articleService.listLabels(size);
    }

    @GetMapping("/categories")
    public R getCategories() {
        return categoryService.listCategories();
    }

    @GetMapping("/archive/articles")
    public R getArchiveArticles() {
        return articleService.getArchiveArticles();
    }
}
