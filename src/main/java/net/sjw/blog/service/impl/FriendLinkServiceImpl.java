package net.sjw.blog.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import net.sjw.blog.entity.Category;
import net.sjw.blog.entity.FriendLink;
import net.sjw.blog.entity.User;
import net.sjw.blog.mapper.FriendsMapper;
import net.sjw.blog.service.FriendLinkService;
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
@Transactional
@Service
public class FriendLinkServiceImpl extends ServiceImpl<FriendsMapper, FriendLink> implements FriendLinkService,BaseService {

    @Autowired
    private UserService userService;
    /**
     * 添加友情链接
     *
     * @param friendLink
     * @return
     */
    @Override
    public R addFriendLink(FriendLink friendLink) {
        //判断数据
        String url = friendLink.getUrl();
        if (TextUtils.isEmpty(url)) {
            return R.FAILED("链接url不可以为空");
        }
        String logo = friendLink.getLogo();
        if (TextUtils.isEmpty(logo)) {
            return R.FAILED("logo不可以为空");
        }
        String name = friendLink.getName();
        if (TextUtils.isEmpty(name)) {
            return R.FAILED("对方网站名不可以为空");
        }
        //补全数据
        friendLink.setCreateTime(new Date());
        friendLink.setUpdateTime(new Date());

        //保存数据
        baseMapper.insert(friendLink);
        //返回结果
        return R.SUCCESS("添加成功");
    }

    @Override
    public R getFriendLink(String firendLinkId) {
        FriendLink friendLink = baseMapper.selectById(firendLinkId);
        if (friendLink == null) {
            return R.FAILED("链接不存在");
        }
        return R.SUCCESS("获取成功").data("data", friendLink);
    }

    @Override
    public R listFriendLinks() {
        User user = userService.checkUser();
        List<FriendLink> all;
        if (user == null || !Constants.User.ROLE_ADMIN.equals(user.getRoles())) {
            all = baseMapper.selectList(Wrappers.<FriendLink>lambdaQuery()
                    .eq(FriendLink::getState, "1")
                    .orderByDesc(FriendLink::getOrder).orderByDesc(FriendLink::getUpdateTime));
        } else {
            all = baseMapper.selectList(Wrappers.<FriendLink>lambdaQuery()
                    .orderByDesc(FriendLink::getOrder).orderByDesc(FriendLink::getUpdateTime));
        }
        return R.SUCCESS("获取友情链接列表成功").data("data",all);
    }

    @Override
    public R deleteFriendLink(String firendLinkId) {
        int result = baseMapper.deleteById(firendLinkId);
        if (result == 0) {
            return R.FAILED("删除友情链接失败");
        }
        return R.SUCCESS("删除友情链接成功");
    }

    /**
     * 更新的内容列表：
     * logo
     * 对方网站的名称
     * url
     * order
     *
     * @param firendLinkId
     * @param friendLink
     * @return
     */
    @Override
    public R updateFriendLink(String firendLinkId, FriendLink friendLink) {
        FriendLink friendLinkFromDb = baseMapper.selectById(firendLinkId);
        if (friendLinkFromDb == null) {
            return R.FAILED("更新友情链接不存在");
        }
        String logo = friendLink.getLogo();
        if (!TextUtils.isEmpty(logo)) {
            friendLinkFromDb.setLogo(logo);
        }
        String name = friendLink.getName();
        if (!TextUtils.isEmpty(name)) {
            friendLinkFromDb.setName(name);
        }
        String url = friendLink.getUrl();
        if (!TextUtils.isEmpty(url)) {
            friendLinkFromDb.setUrl(url);
        }
        String description = friendLink.getDescription();
        if (!TextUtils.isEmpty(description)) {
            friendLinkFromDb.setDescription(description);

        }
        friendLinkFromDb.setState(friendLink.getState());
        friendLinkFromDb.setOrder(friendLink.getOrder());
        friendLinkFromDb.setUpdateTime(new Date());
        baseMapper.updateById(friendLinkFromDb);
        return R.SUCCESS("更新成功");
    }
}
