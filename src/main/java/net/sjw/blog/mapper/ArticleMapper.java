package net.sjw.blog.mapper;

import net.sjw.blog.entity.Article;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

/**
 * <p>
 *  Mapper 接口
 * </p>
 *
 * @author czy
 * @since 2020-10-13
 */
public interface ArticleMapper extends BaseMapper<Article> {
    Article getArticleWithUser(@Param("articleId") String articleId);
}
