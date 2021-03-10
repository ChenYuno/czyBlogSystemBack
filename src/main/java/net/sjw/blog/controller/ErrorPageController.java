package net.sjw.blog.controller;

import net.sjw.blog.utils.R;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 错误吗统一返回的结果
 */
@RestController
public class ErrorPageController {

    @GetMapping("/404")
    public R page404() {
        return R.ERROR_404();
    }
    @GetMapping("/403")
    public R page403() {
        return R.ERROR_403();
    }
    @GetMapping("/504")
    public R page504() {
        return R.ERROR_504();
    }
    @GetMapping("/505")
    public R page505() {
        return R.ERROR_505();
    }
}
