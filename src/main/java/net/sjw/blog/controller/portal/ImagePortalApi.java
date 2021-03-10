package net.sjw.blog.controller.portal;


import net.sjw.blog.service.ImageService;
import net.sjw.blog.utils.R;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/portal/image")
public class ImagePortalApi {

    @Autowired
    private ImageService imageService;


    @GetMapping("{imageId}")
    public void getImage(@PathVariable("imageId") String imageId) {
        try {
            imageService.viewImage(imageId);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @GetMapping("/qr-code/{code}")
    public void getQrCodeImage(HttpServletResponse response,
                            @PathVariable("code") String code) {

        imageService.createQrCode(code,response);
    }
}
