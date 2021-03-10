package net.sjw.blog.controller.admin;

import net.sjw.blog.entity.Looper;
import net.sjw.blog.service.LooperService;
import net.sjw.blog.utils.R;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/loop")
public class LooperAdminApi {

    @Autowired
    private LooperService looperService;

    @PreAuthorize("@permission.admin()")
    @PostMapping
    public R addLopp(@RequestBody Looper looper) {
        return looperService.addLoop(looper);
    }

    @PreAuthorize("@permission.admin()")
    @DeleteMapping("/{loopId}")
    public R deleteLoop(@PathVariable("loopId") String loopId) {
        return looperService.deleteLoop(loopId);
    }

    @PreAuthorize("@permission.admin()")
    @PutMapping("/{loopId}")
    public R updateLoop(@PathVariable("loopId") String loopId,
                        @RequestBody Looper looper) {
        return looperService.updateLoop(loopId,looper);
    }

    @PreAuthorize("@permission.admin()")
    @GetMapping("/{loopId}")
    public R getLoop(@PathVariable("loopId") String loopId) {
        return looperService.getLoop(loopId);
    }


    @GetMapping("/list")
    public R listLoop() {
        return looperService.listLoops();
    }
}
