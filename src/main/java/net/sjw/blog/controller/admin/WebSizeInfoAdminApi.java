package net.sjw.blog.controller.admin;

import lombok.extern.slf4j.Slf4j;
import net.sjw.blog.interceptor.CheckTooFrequentCommit;
import net.sjw.blog.service.WebSizeInfoService;
import net.sjw.blog.utils.R;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/admin/web_size_info")
public class WebSizeInfoAdminApi {

    @Autowired
    private WebSizeInfoService webSizeInfoService;


    @GetMapping("/title")
    public R getWebSizeTitle() {
        return webSizeInfoService.getWebSizeTitle();
    }

    @CheckTooFrequentCommit
    @PreAuthorize("@permission.admin()")
    @PutMapping("/title")
    public R updateWebSizeTitle(@RequestParam("title") String title) {
        return webSizeInfoService.putWebSizeTitle(title);
    }


    @GetMapping("/seo")
    public R getSeoInfo() {
        return webSizeInfoService.getSeoInfo();
    }

    @CheckTooFrequentCommit
    @PreAuthorize("@permission.admin()")
    @PutMapping("/seo")
    public R putSeoInfo(@RequestParam("keywoeds") String keywords,
                        @RequestParam("description") String description) {
        return webSizeInfoService.putSeoInfo(keywords,description);
    }


    @GetMapping("/view_count")
    public R getWebSizeViewCount() {
        return webSizeInfoService.getSizeViewCount();
    }

    @PutMapping("/view_count")
    public R updateViewCount() {
        return webSizeInfoService.updateSizeViewCount();
    }

    @PreAuthorize("@permission.admin()")
    @PostMapping("/advice")
    public R getWebSizeAdvice(@RequestBody Map<String, Object> json) {
        if (json.containsKey("advice")) {
            String advice = json.get("advice").toString();
            return webSizeInfoService.setOrUpdateWebSizeAdvice(advice);
        } else {
            return R.FAILED("提交数据错误");
        }
    }
}
