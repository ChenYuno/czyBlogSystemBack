package net.sjw.blog.controller.portal;

import net.sjw.blog.service.CategoryService;
import net.sjw.blog.service.FriendLinkService;
import net.sjw.blog.service.LooperService;
import net.sjw.blog.service.WebSizeInfoService;
import net.sjw.blog.utils.R;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/portal/web_size_info")
public class WebSizeInfoPortalApi {


    @Autowired
    private FriendLinkService friendLinkService;

    @Autowired
    private LooperService looperService;

    @Autowired
    private WebSizeInfoService webSizeInfoService;


    @GetMapping("/title")
    public R getWebSizeTitle() {
        return webSizeInfoService.getWebSizeTitle();
    }

    @GetMapping("view_count")
    public R getWebSizeViewCount() {
        return webSizeInfoService.getSizeViewCount();
    }

    @GetMapping("/seo")
    public R getWebSizeInfo() {
        return webSizeInfoService.getSeoInfo();
    }

    @GetMapping("/loop")
    public R getLoop() {
        return looperService.listLoops();
    }

    @GetMapping("friend_link")
    public R getLinks() {
        return friendLinkService.listFriendLinks();
    }

    @GetMapping("advice")
    public R getWebSizeAdvice() {
        return webSizeInfoService.getWebSizeAdvice();
    }
}
