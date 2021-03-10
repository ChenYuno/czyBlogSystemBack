package net.sjw.blog.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import net.sjw.blog.entity.Category;
import net.sjw.blog.entity.User;
import net.sjw.blog.mapper.CategoriesMapper;
import net.sjw.blog.service.CategoryService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import net.sjw.blog.service.UserService;
import net.sjw.blog.utils.Constants;
import net.sjw.blog.utils.R;
import net.sjw.blog.utils.TextUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author czy
 * @since 2020-10-13
 */
@Service
@Transactional
public class CategoryServiceImpl extends ServiceImpl<CategoriesMapper, Category> implements CategoryService,BaseService {


    @Autowired
    private UserService userService;
    @Override
    public R addCategory(Category category) {
        //检查数据
        //必须有的数据
        //分类名称、分类的拼音、pinyin、顺序、描述
        if (TextUtils.isEmpty(category.getName())) {
            return R.FAILED("分类名称不可以为空。");
        }
        if (TextUtils.isEmpty(category.getPinyin())) {
            return R.FAILED("分类拼音不可以为空。");
        }
        if (TextUtils.isEmpty(category.getDescription())) {
            return R.FAILED("分类描述不可以为空。");
        }
        Integer countFromDb = baseMapper.selectCount(Wrappers.<Category>lambdaQuery()
                .eq(Category::getName, category.getName()));
        if (countFromDb > 0) {
            return R.FAILED("分类已经存在，请不要重复添加");
        }
        //补全数据
        category.setStatus("1");
        category.setCreateTime(new Date());
        category.setUpdateTime(new Date());

        //保存数据
        baseMapper.insert(category);
        //返回结果
        return R.SUCCESS("添加分类成功");
    }

    @Override
    public R getCategory(String categoryId) {
        Category category = baseMapper.selectById(categoryId);
        if (category == null) {
            return R.FAILED("分类不存在");
        }

        return R.SUCCESS("获取分类成功").data("data", category);
    }

    @Override
    public R listCategories() {

        //创建条件
        //查询
        //判断用户角色，普通用户和未登录用户 ，只能获取到状态正常的category
        //管理员可以拿到所有分类
        User user = userService.checkUser();
        List<Category> all;
        if (user == null || !Constants.User.ROLE_ADMIN.equals(user.getRoles())) {
            all = baseMapper.selectList(Wrappers.<Category>lambdaQuery()
                    .eq(Category::getStatus, "1")
                    .orderByDesc(Category::getUpdateTime).orderByDesc(Category::getOrder));
        } else {
             all = baseMapper.selectList(Wrappers.<Category>lambdaQuery()
                    .orderByDesc(Category::getUpdateTime).orderByDesc(Category::getOrder));
        }
        //返回结果
        return R.SUCCESS("获取分类列表成功").data("data", all);
    }

    @Override
    public R updateCategory(String categoryId, Category category) {
        //1、找出来
        Category categoryFromDb = baseMapper.selectById(categoryId);
        if (categoryFromDb == null) {
            return R.FAILED("分类不存在");
        }
        //2、 内容判断
        String name = category.getName();
        if (!TextUtils.isEmpty(name) && !categoryFromDb.getName().equals(name) ){
            Integer countFromDb = baseMapper.selectCount(Wrappers.<Category>lambdaQuery()
                    .eq(Category::getName, name));
            if (countFromDb > 0) {
                return R.FAILED("该分类已经存在，请换个名称");
            }
            categoryFromDb.setName(name);
        }
        String pinyin = category.getPinyin();
        if (!TextUtils.isEmpty(pinyin)) {
            categoryFromDb.setPinyin(pinyin);
        }
        String description = category.getDescription();
        if (!TextUtils.isEmpty(description)) {
            categoryFromDb.setDescription(description);
        }
        categoryFromDb.setStatus(category.getStatus());
        categoryFromDb.setOrder(category.getOrder());
        categoryFromDb.setUpdateTime(new Date());
        //3、保存数据
        baseMapper.updateById(categoryFromDb);
        //4、返回结果
        return R.SUCCESS("分类更新成功");
    }

    @Override
    public R deleteCategory(String categoryId) {
        int result = baseMapper.update(null, Wrappers.<Category>lambdaUpdate()
                .set(Category::getStatus, "0")
                .set(Category::getUpdateTime, new Date())
                .eq(Category::getId, categoryId));
        if (result == 0) {
            return R.FAILED("该分类不存在");
        }
        return R.SUCCESS("删除分类成功");
    }
}
