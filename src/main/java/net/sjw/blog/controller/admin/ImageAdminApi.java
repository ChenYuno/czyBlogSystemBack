package net.sjw.blog.controller.admin;

import net.sjw.blog.entity.Image;
import net.sjw.blog.service.ImageService;
import net.sjw.blog.utils.R;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/admin/image")
public class ImageAdminApi {

    @Autowired
    private ImageService imageService;
    /**
     * 图片上传
     * 一般来说，比较常见的是对象存储 --> 看文档
     * 使用 Nginx + fastDFS --> 处理文件上传
     * fastDFS 处理文件上传   Nginx 负责处理文件访问
     *
     * @param file
     * @return
     */
    @PostMapping("/{original}")
    @CrossOrigin
    public R uploadImage(@PathVariable("original")String original,
            @RequestParam("file")MultipartFile file) {
        return imageService.uploadImage(original,file);
    }

    @PreAuthorize("@permission.admin()")
    @DeleteMapping("/{imageId}")
    public R deleteImage(@PathVariable("imageId") String imageId) {
        return imageService.deleteById(imageId);
    }


    @GetMapping("/{imageId}")
    public void getImage(@PathVariable("imageId") String imageId) {
        imageService.viewImage(imageId);
    }

    @PreAuthorize("@permission.admin()")
    @GetMapping("/list/{page}/{size}")
    public R listImage(@PathVariable("page") int page
            , @PathVariable("size") int size
    ,@RequestParam(value = "original",required = false) String original) {
        return imageService.listImages(page,size,original);
    }

    @PreAuthorize("@permission.admin()")
    @DeleteMapping("/absoluteRemoveForever")
    public R absoluteRemoveForever(@RequestBody Image image) {
        return imageService.absoluteRemoveForever(image);
    }
}
