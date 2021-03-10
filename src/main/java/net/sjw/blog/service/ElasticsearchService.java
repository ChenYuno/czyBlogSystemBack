package net.sjw.blog.service;

import net.sjw.blog.entity.Article;
import net.sjw.blog.utils.R;

public interface ElasticsearchService {

    void addArticle(Article article);


    R doSearch(String keyword, int page, int size, String categoryId, Integer sort);


    void deleteArticle(String articleId);

    void updateArticle(String articleId,Article article);
}
