package net.sjw.blog.service;

import net.sjw.blog.entity.Image;
import com.baomidou.mybatisplus.extension.service.IService;
import net.sjw.blog.utils.R;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author czy
 * @since 2020-10-13
 */
public interface ImageService extends IService<Image> {

    R uploadImage(String original, MultipartFile file);

    void viewImage(String imageId);

    R listImages(int page, int size, String original);

    R deleteById(String imageId);

    void createQrCode(String code,HttpServletResponse response);

    R absoluteRemoveForever(Image image);
}
