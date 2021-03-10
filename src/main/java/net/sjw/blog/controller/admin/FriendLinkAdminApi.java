package net.sjw.blog.controller.admin;

import net.sjw.blog.entity.FriendLink;
import net.sjw.blog.interceptor.CheckTooFrequentCommit;
import net.sjw.blog.service.FriendLinkService;
import net.sjw.blog.utils.R;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/friend_link")
public class FriendLinkAdminApi {

    @Autowired
    private FriendLinkService friendLinkService;

    @CheckTooFrequentCommit
    @PreAuthorize("@permission.admin()")
    @PostMapping
    public R addFriendLink(@RequestBody FriendLink friendLink) {
        return friendLinkService.addFriendLink(friendLink);
    }

    @PreAuthorize("@permission.admin()")
    @DeleteMapping("/{firendLinkId}")
    public R deleteFriendLink(@PathVariable("firendLinkId") String firendLinkId) {
        return friendLinkService.deleteFriendLink(firendLinkId);
    }

    @CheckTooFrequentCommit
    @PreAuthorize("@permission.admin()")
    @PutMapping("/{firendLinkId}")
    public R updateFriendLink(@PathVariable("firendLinkId") String firendLinkId,
                              @RequestBody FriendLink friendLink) {
        return friendLinkService.updateFriendLink(firendLinkId, friendLink);
    }

    @PreAuthorize("@permission.admin()")
    @GetMapping("/{firendLinkId}")
    public R getFriendLink(@PathVariable("firendLinkId") String firendLinkId) {
        return friendLinkService.getFriendLink(firendLinkId);
    }

    @GetMapping("/list")
    public R listFriendLink() {
        return friendLinkService.listFriendLinks();
    }
}
