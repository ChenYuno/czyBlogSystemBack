package net.sjw.blog.service.impl;

import cn.hutool.core.lang.Snowflake;

import net.sjw.blog.entity.SearchPageList;
import net.sjw.blog.utils.*;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import net.sjw.blog.entity.Image;
import net.sjw.blog.entity.User;
import net.sjw.blog.mapper.ImagesMapper;
import net.sjw.blog.service.ImageService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import net.sjw.blog.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.locks.ReentrantLock;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author czy
 * @since 2020-10-13
 */
@Slf4j
@Transactional
@Service
public class ImageServiceImpl extends ServiceImpl<ImagesMapper, Image> implements ImageService,BaseService {

//    private static final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy_MM_dd");

    @Autowired
    private Snowflake snowflake;

    @Autowired
    private UserService userService;


    @Value("${czy.blog.images.save-path}")
    private String imagesPath;

    @Value("${spring.servlet.multipart.max-file-size}")
    private String maxSize;


    /**
     * 上传路径： 可以配置，在配置文件里配置
     * 上传内容：命名 --> 可以用id, --> 每天一个文件夹保存
     * 保存记录数据到数据库
     * ID / 存储路径 / url / 原名称 / 用户ID / 状态 / 创建日期 / 更新日期
     *
     *
     * (todo:使用md5加密上传去重)
     *
     * @param original
     * @param file
     * @return
     */
    @Override
    public R uploadImage(String original, MultipartFile file) {
        //判断是否有问件
        if (file == null) {
            return R.FAILED("图片不可以为空");
        }
        //判断文件类型，只支持png、jpg、jpeg、gif
        String contentType = file.getContentType();
        if (TextUtils.isEmpty(contentType)) {
            return R.FAILED("文件格式错误");
        }

        String originFileName = file.getOriginalFilename();
        //获取相关数据比如文件类型、文件名称
        log.info(file.getName());
        log.info(file.getContentType());
        log.info(originFileName);
        String type = getType(contentType, originFileName);
        if (TextUtils.isEmpty(type)) {
            return R.FAILED("不支持此图片类型");
        }


        //检查图片大小返回结果
        long size = file.getSize();
        log.info(" maxSize ==> " + maxSize + "  size == > " + size);
        String maxSizeString = maxSize.substring(0, 2);
        long maxSizeParseFromFile = Long.parseLong(maxSizeString);
        log.info(maxSizeParseFromFile + "");
        if ((size / (1024 * 1024)) > maxSizeParseFromFile) {
            return R.FAILED("图片最大仅仅支持" + maxSize);
        }


        //根据我们定的规则进行命名
        //创建文件保存目录
        //规则：配置文件/日期/类型/ID.类型
        long currentTimeMillis = System.currentTimeMillis();
        String currentDay = new SimpleDateFormat("yyyy_MM_dd").format(currentTimeMillis);

        String fileName = snowflake.nextIdStr();

        log.info("fileName ==> snow:"+fileName);

        log.info(":  format: " + currentDay);
        //====================================
        //判断日期文件夹是否存在
        String dayPath = imagesPath + File.separator + currentDay;
        File dayPathFile = new File(dayPath);
        if (!dayPathFile.exists()) {
            dayPathFile.mkdirs();
        }
        String targetPath = dayPath +
                File.separator + type + File.separator;
        File targetFile = new File(targetPath);
        //判断类型文件夹是否存在
        if (!targetFile.exists()) {
            targetFile.mkdirs();
        }
        targetFile = new File(targetPath+fileName+ "." + type);
        log.info("targetFile ==> " + targetFile);
        //保存文件
        try {
            User user = userService.checkUser();
            if (user == null) {
                return R.ACCOUNT_NOT_LOGIN();
            }
            String roles = user.getRoles();
            if (!roles.equals(Constants.User.ROLE_ADMIN)) {
                return R.PERMISSION_DENIED();
            }
            if (!targetFile.exists()) {
                targetFile.createNewFile();
            }
            file.transferTo(targetFile);
            String resultPath = currentTimeMillis + "_" + fileName + "." + type;
            //保存数据到数据库

            Image image = new Image();
            image.setContentType(contentType);
            image.setId(fileName);
            image.setCreateTime(new Date());
            image.setUpdateTime(new Date());
            image.setPath(targetFile.getPath());
            image.setName(originFileName);
            image.setUrl(resultPath);
            image.setOriginal(original);
            image.setState("1");

            image.setUserId(user.getId());

            //返回结果：包扩图片的名称和访问的路径
            //第一个是访问的路劲 -- > 对应着解析来
            //第二个是名称 -- > 现在搜索引擎支持图片 == alt="图片描述"

            baseMapper.insert(image);
            return R.SUCCESS("图片上传成功").data("id", resultPath)
                    .data("name", originFileName)
                    .data("original",image.getOriginal());
        } catch (IOException e) {
            e.printStackTrace();
        }
        //记录文件
        //返回结果
        return R.FAILED("图片上传失败");
    }

    private String getType(String contentType, String originFileName) {
        String type = null;
        if (Constants.ImageType.TYPE_PNG_WITH_PREFIX.equals(contentType)
                && originFileName.endsWith(Constants.ImageType.TYPE_PNG)) {
            type = Constants.ImageType.TYPE_PNG;
        } else if (Constants.ImageType.TYPE_JPG_WITH_PREFIX.equals(contentType)
                && originFileName.endsWith(Constants.ImageType.TYPE_JPG)) {
            type = Constants.ImageType.TYPE_JPG;
        } else if (Constants.ImageType.TYPE_JPEG_WITH_PREFIX.equals(contentType)
                && (originFileName.endsWith(Constants.ImageType.TYPE_JPEG)
                || originFileName.endsWith(Constants.ImageType.TYPE_JPG))) {

            type = Constants.ImageType.TYPE_JPEG;
        } else if (Constants.ImageType.TYPE_GIF_WITH_PREFIX.equals(contentType)
                && originFileName.endsWith(Constants.ImageType.TYPE_GIF)) {
            type = Constants.ImageType.TYPE_GIF;
        }
        return type;
    }

    @Override
    public void viewImage(String imageId) {
        //配置的目录已知

        //根据尺寸来动态返回图片给前端
        //好处：减少带宽占用、传输速度快
        //缺点：消耗后台CPU资源
        //推荐做法：上传上来的时候把图片复制成(大、中、小)三个尺寸
        // 根据尺寸返回结果即可

        //需要日期
        //需要类型
        String[] paths = imageId.split("_");
        String datValue = paths[0];
        String format;
        format = new SimpleDateFormat("yyyy_MM_dd").format(Long.parseLong(datValue));
        String name = paths[1];
        //需要的类型
        String type = name.substring(name.indexOf(".")+1);
        log.info("type ==> " + type);
        String targetPath = imagesPath + File.separator + format + File.separator +
                type + File.separator + name;
        log.info("get image target path ==> "+targetPath);
        OutputStream out = null;
        InputStream in = null;
        try {
            HttpServletResponse response = getResponse();
            response.setContentType(Constants.ImageType.PREFIX + type);
            out = response.getOutputStream();
            File url = new File(targetPath);
            in = new FileInputStream(url);
            byte[] buff = new byte[3072];
            int len;
            while ((len = in.read(buff)) != -1) {
                out.write(buff, 0, len);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
//            if (out != null) {
//                try {
//                    out.close();
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }
//            }

        }

    }

    @Override
    public R listImages(int page, int size, String original) {
        page = checkPage(page);
        size = checkSize(size);
        User user = userService.checkUser();
        if (user == null) {
            return R.ACCOUNT_NOT_LOGIN();
        }
        final String userId = user.getId();
        Page<Image> all = baseMapper.selectPage(new Page<>(page, size), Wrappers.<Image>lambdaQuery()
                .eq(Image::getState, "1")
                .eq(Image::getUserId, userId)
                .eq(!TextUtils.isEmpty(original), Image::getOriginal, original)
                .orderByDesc(Image::getCreateTime));
        SearchPageList result = new SearchPageList(all);
        return R.SUCCESS("获取图片列表成功").data("data", result);
    }

    /**
     * 删除图片，
     * 只是改变状态
     *
     * @param imageId
     * @return
     */
    @Override
    public R deleteById(String imageId) {
        int result = baseMapper.update(null, Wrappers.<Image>lambdaUpdate()
                .set(Image::getState, "0").eq(Image::getId, imageId));
        if (result > 0) {
            return R.SUCCESS("删除成功");
        }
        return R.FAILED("图片不存在");
    }


    @Autowired
    private RedisUtils redisUtils;

    @Override
    public void createQrCode(String code, HttpServletResponse response) {
        //检查二维码是否过期
        Boolean loginState = (Boolean) redisUtils.get(Constants.User.KEY_PC_QR_LOGIN_ID + code);

        log.info("loginState ==>" + loginState);
        if (loginState == null) {
            //TODO:返回一张图片显示已经过期
            return;
        }
        //生成二维码
        //二维码内容
        //1、可以是简单的一个code，也就是传进来的这个
        //这种情况，如果是用我们自己写的App扫描，是识别并解析，请求对应的接口
        //如果是第三方的来扫描，可以识别，但是没有用，只能显示code
        //2、我们应该是一个App的下载地址+code，如果是我们自己app扫到，切割后面的内容拿到code进行解析
        //请求对应的接口，如果是第三方的app扫到，他是个网址，就会访问下载app的地址，去下载我们的app
        //APP_DOWNloAD_PATH/code
        HttpServletRequest request = getRequest();
        String servletPath = request.getServletPath();
        String originDomain = request.getRequestURL().toString().replace(servletPath, "");

        String content = originDomain + Constants.APP_DOWNLOAD_PATH + "===" + code;
        log.info("content ==> " + content);
        byte[] result = QrCodeUtils.encodeQrCode(content);

        response.setContentType(QrCodeUtils.RESPONSE_CONTENT_TYPE);
        try {
            ServletOutputStream outputStream = response.getOutputStream();
            outputStream.write(result);
            outputStream.flush();
            outputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Override
    public R absoluteRemoveForever(Image image) {
        String realPathStr = image.getPath();
        log.info("删除图片真实地址: ==> " + realPathStr);
        Path reaPath = Paths.get(realPathStr);
        File realFile = reaPath.toFile();
        if (!realFile.isFile() && !realFile.exists()) {
            return R.FAILED("图片不存在");
        }
        boolean deleteFlag = realFile.delete();


        if (!deleteFlag) {
            return R.FAILED("删除文件失败");
        }

        int deleteImageFromDb = baseMapper.deleteById(image);
        if (deleteImageFromDb > 0 && deleteFlag) {
            return R.SUCCESS("永久删除该图片成功o(╥﹏╥)o");
        }
        return R.FAILED("想要永久删除该图片失败");
    }


}
