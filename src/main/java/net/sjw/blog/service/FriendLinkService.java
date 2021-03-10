package net.sjw.blog.service;

import net.sjw.blog.entity.FriendLink;
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
public interface FriendLinkService extends IService<FriendLink> {

    R addFriendLink(FriendLink friendLink);

    R getFriendLink(String firendLinkId);

    R listFriendLinks();

    R deleteFriendLink(String firendLinkId);

    R updateFriendLink(String firendLinkId, FriendLink friendLink);
}
