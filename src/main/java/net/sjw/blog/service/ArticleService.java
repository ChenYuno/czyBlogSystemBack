package net.sjw.blog.service;

import net.sjw.blog.entity.Article;
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
public interface ArticleService extends IService<Article> {

    R postArticle(Article article);

    R listArticles(int page, int size, String keyword, String categoryId, String state);

    R getArticleById(String articleId);

    R updateArticle(String articleId, Article article);

    R deleteArticle(String articleId);

    R deleteArticleByUpdateState(String articleId);

    R topArticle(String articleId);

    R listTopArticles();

    R listRecommendArticle(String articleId, int size);

    R listArticleByLabel(int page, int size, String label);

    R listLabels(int size);

    R getArticleCount();

    R totalArticlePublishCount();

    R getArchiveArticles();

}
