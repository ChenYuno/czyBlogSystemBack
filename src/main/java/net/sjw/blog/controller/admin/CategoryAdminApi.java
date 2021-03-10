package net.sjw.blog.controller.admin;

import net.sjw.blog.entity.Category;
import net.sjw.blog.interceptor.CheckTooFrequentCommit;
import net.sjw.blog.service.CategoryService;
import net.sjw.blog.utils.R;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 管理中心分类的api
 */
@RestController
@RequestMapping("/admin/category")
public class CategoryAdminApi {


    @Autowired
    private CategoryService categoryService;

    /**
     * 添加分类
     * 需要管理员权限
     *
     * @param category
     * @return
     */
    @CheckTooFrequentCommit
    @PreAuthorize("@permission.admin()")
    @PostMapping
    public R addCategory(@RequestBody Category category) {

        return categoryService.addCategory(category);
    }

    /**
     * 删除分类
     *
     * @param categoryId
     * @return
     */
    @PreAuthorize("@permission.admin()")
    @DeleteMapping("/{categoryId}")
    public R deleteCategory(@PathVariable("categoryId") String categoryId) {
        return categoryService.deleteCategory(categoryId);
    }

    /**
     * 修改分类
     * @param categoryId
     * @param category
     * @return
     */
    @CheckTooFrequentCommit
    @PreAuthorize("@permission.admin()")
    @PutMapping("/{categoryId}")
    public R updateCategory(@PathVariable("categoryId")String categoryId,
                            @RequestBody Category category) {
        return categoryService.updateCategory(categoryId, category);
    }

    /**
     * 获取分类
     * <p></p>
     * 使用的case：修改的时候获取一下，填充弹窗
     * 不获取也可以，从列表里获取数据
     * <p></p>
     * 权限：管理员权限
     *
     * @param categoryId
     * @return
     */
    @PreAuthorize("@permission.admin()")
    @GetMapping("/{categoryId}")
    public R getCategory(@PathVariable("categoryId") String categoryId) {
        return categoryService.getCategory(categoryId);
    }

    /**
     * 获取分类列表
     * <p></p>
     * 权限：管理员权限
     * @return
     */
    @PreAuthorize("@permission.admin()")
    @GetMapping("/list")
    public R listCategory() {
        return categoryService.listCategories();
    }
}
