package net.sjw.blog.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import net.sjw.blog.entity.FriendLink;
import net.sjw.blog.entity.Looper;
import net.sjw.blog.entity.User;
import net.sjw.blog.mapper.LooperMapper;
import net.sjw.blog.service.LooperService;
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
public class LooperServiceImpl extends ServiceImpl<LooperMapper, Looper> implements LooperService,BaseService {

    @Override
    public R addLoop(Looper looper) {
        //检查数据
        String title = looper.getTitle();
        if (TextUtils.isEmpty(title)) {
            return R.FAILED("标题不可以为空");
        }
        String imageUrl = looper.getImageUrl();
        if (TextUtils.isEmpty(imageUrl)) {
            return R.FAILED("图片不可以为空");
        }
        String targetUrl = looper.getTargetUrl();
        if (TextUtils.isEmpty(targetUrl)) {
            return R.FAILED("跳转连接不可以为空");
        }
        //补充数据

        looper.setCreateTime(new Date());
        looper.setUpdateTime(new Date());

        //保存数据
        baseMapper.insert(looper);
        //返回结果
        return R.SUCCESS("轮播图添加成功");
    }

    @Override
    public R getLoop(String loopId) {
        Looper looper = baseMapper.selectById(loopId);
        if (looper == null) {
            return R.FAILED("轮播图不存在");
        }
        return R.SUCCESS("轮播图获取成功").data("data", looper);
    }

    @Autowired
    private UserService userService;
    @Override
    public R listLoops () {
        User user = userService.checkUser();
        List<Looper> all;
        if (user == null || !Constants.User.ROLE_ADMIN.equals(user.getRoles())) {
            all = baseMapper.selectList(Wrappers.<Looper>lambdaQuery()
                    .eq(Looper::getState, "1")
                    .orderByDesc(Looper::getOrder).orderByDesc(Looper::getUpdateTime));
        } else {
            all = baseMapper.selectList(Wrappers.<Looper>lambdaQuery()
                    .orderByDesc(Looper::getOrder).orderByDesc(Looper::getUpdateTime));
        }

        return R.SUCCESS("获取轮播图列表成功").data("data",all);
    }

    @Override
    public R updateLoop(String loopId, Looper looper) {
        Looper looperFromDb = baseMapper.selectById(loopId);
        if (looperFromDb == null) {
            return R.FAILED("轮播图不存在");
        }
        String title = looper.getTitle();
        if (!TextUtils.isEmpty(title)) {
            looperFromDb.setTitle(title);
        }
        String imageUrl = looper.getImageUrl();
        if (!TextUtils.isEmpty(imageUrl)) {
            looperFromDb.setImageUrl(imageUrl);
        }
        String targetUrl = looper.getTargetUrl();
        if (!TextUtils.isEmpty(targetUrl)) {
            looperFromDb.setTargetUrl(targetUrl);
        }
        if (!TextUtils.isEmpty(looper.getState())) {
            looperFromDb.setState(looper.getState());
        }
        looperFromDb.setOrder(looper.getOrder());

        looperFromDb.setUpdateTime(new Date());
        baseMapper.updateById(looperFromDb);
        return R.SUCCESS("轮播图更新成功");
    }

    @Override
    public R deleteLoop(String loopId) {
        baseMapper.deleteById(loopId);
        return R.SUCCESS("轮播图删除成功");
    }
}
