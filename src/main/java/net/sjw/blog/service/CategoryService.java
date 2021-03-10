package net.sjw.blog.service;

import net.sjw.blog.entity.Category;
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
public interface CategoryService extends IService<Category> {

    R addCategory(Category category);

    R getCategory(String categoryId);

    R listCategories();

    R updateCategory(String categoryId, Category category);

    R deleteCategory(String categoryId);
}
