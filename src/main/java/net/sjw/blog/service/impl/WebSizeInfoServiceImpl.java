package net.sjw.blog.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.extern.slf4j.Slf4j;
import net.sjw.blog.entity.Setting;
import net.sjw.blog.mapper.SettingMapper;
import net.sjw.blog.service.WebSizeInfoService;
import net.sjw.blog.utils.Constants;
import net.sjw.blog.utils.R;
import net.sjw.blog.utils.RedisUtils;
import net.sjw.blog.utils.TextUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

@Slf4j
@Transactional
@Service
public class WebSizeInfoServiceImpl implements WebSizeInfoService, BaseService {

    @Autowired
    private SettingMapper settingMapper;
    @Override
    public R getWebSizeTitle() {
        Setting title = settingMapper.selectOne(Wrappers.<Setting>lambdaQuery()
                .eq(Setting::getKey, Constants.Settings.WEB_SIZE_TITLE));
        return R.SUCCESS("获取网站title成功").data(title.getKey(),title.getValue());
    }

    @Override
    public R putWebSizeTitle(String title) {
        if (TextUtils.isEmpty(title)) {
            return R.FAILED("网站标题不可以为空");
        }
        boolean flag = false;
        Setting titleFromDb = settingMapper.selectOne(Wrappers.<Setting>lambdaQuery()
                .eq(Setting::getKey, Constants.Settings.WEB_SIZE_TITLE));
        if (titleFromDb == null) {
            flag = true;
            titleFromDb = new Setting();
            titleFromDb.setCreateTime(new Date());
            titleFromDb.setKey(Constants.Settings.WEB_SIZE_TITLE);
        }
        titleFromDb.setValue(title);
        titleFromDb.setUpdateTime(new Date());
        if (flag) {
            settingMapper.insert(titleFromDb);
        } else {
            settingMapper.updateById(titleFromDb);
        }
        return R.SUCCESS("网站Title更新成功");
    }

    @Override
    public R getSeoInfo() {
        Setting keywords = settingMapper.selectOne(Wrappers.<Setting>lambdaQuery().eq(Setting::getKey, Constants.Settings.WEB_SIZE_KEYWORDS));
        Setting description = settingMapper.selectOne(Wrappers.<Setting>lambdaQuery().eq(Setting::getKey, Constants.Settings.WEB_SIZE_DESCRIPTION));
        return R.SUCCESS("获取网站SEO信息成功").data((keywords!=null ? keywords.getKey() : ""), keywords!=null ? keywords.getValue() : "")
                .data(description!=null ? description.getKey() : "", description!=null ? description.getValue() : "");
    }

    @Override
    public R putSeoInfo(String keywords, String description) {
        //判断
        if (TextUtils.isEmpty(keywords)) {
            return R.FAILED("关键字不可以为空");
        }
        if (TextUtils.isEmpty(description)) {
            return R.FAILED("网站描述不可以为空");
        }
        Setting keywordsFromDb = settingMapper.selectOne(Wrappers.<Setting>lambdaQuery().eq(Setting::getKey, Constants.Settings.WEB_SIZE_KEYWORDS));
        boolean flagKeyWords = false;
        if (keywordsFromDb == null) {
            flagKeyWords = true;
            keywordsFromDb = new Setting();
            keywordsFromDb.setCreateTime(new Date());
            keywordsFromDb.setKey(Constants.Settings.WEB_SIZE_KEYWORDS);
        }
        keywordsFromDb.setUpdateTime(new Date());
        keywordsFromDb.setValue(keywords);
        if (flagKeyWords) {
            settingMapper.insert(keywordsFromDb);
        } else {
            settingMapper.updateById(keywordsFromDb);
        }
        Setting descriptionFromDb = settingMapper.selectOne(Wrappers.<Setting>lambdaQuery().eq(Setting::getKey, Constants.Settings.WEB_SIZE_DESCRIPTION));
        boolean flagDescription = false;
        if (descriptionFromDb == null) {
            flagDescription = true;
            descriptionFromDb = new Setting();
            descriptionFromDb.setCreateTime(new Date());
            descriptionFromDb.setKey(Constants.Settings.WEB_SIZE_DESCRIPTION);
        }
        descriptionFromDb.setUpdateTime(new Date());
        descriptionFromDb.setValue(description);
        if (flagDescription) {
            settingMapper.insert(descriptionFromDb);
        } else {
            settingMapper.updateById(descriptionFromDb);
        }
        return R.SUCCESS("更新SEO信息成功");
    }

    /**
     * 这是全网站的访问量，做得细点可以分来源
     * 先只统计浏览量 只统计文章的浏览量，提供一个浏览器的统计接口（页面级别)
     *
     *
     * TODO：使用Quartz和redis实现全网的浏览统计，加上异步的方式实现异步更新每篇文章的统计量
     *
     * @return 浏览量
     */
    @Override
    public R getSizeViewCount() {
        Integer oldViewCount = (Integer) redisUtils.get(Constants.Settings.WEB_SIZE_VIEW_COUNT);

        if (oldViewCount != null) {
            settingMapper.update(null, Wrappers.<Setting>lambdaUpdate()
                    .set(Setting::getValue, String.valueOf(oldViewCount))
                    .eq(Setting::getKey, Constants.Settings.WEB_SIZE_VIEW_COUNT));
            return R.SUCCESS("获取浏览量").data("data", oldViewCount);
        }
        Setting descriptionFromDb = settingMapper.selectOne(Wrappers.<Setting>lambdaQuery()
                .eq(Setting::getKey, Constants.Settings.WEB_SIZE_VIEW_COUNT));
        if (descriptionFromDb == null) {
            descriptionFromDb = new Setting();
            descriptionFromDb.setKey(Constants.Settings.WEB_SIZE_VIEW_COUNT);
            descriptionFromDb.setCreateTime(new Date());
            descriptionFromDb.setUpdateTime(new Date());
            descriptionFromDb.setValue("1");
            settingMapper.insert(descriptionFromDb);
        }
        redisUtils.set(Constants.Settings.WEB_SIZE_VIEW_COUNT,
                Integer.parseInt(descriptionFromDb.getValue()));
        return R.SUCCESS("获取网站浏览量成功")
                .data(descriptionFromDb.getKey(),Long.parseLong(descriptionFromDb.getValue()));
    }

    @Autowired
    private RedisUtils redisUtils;
    @Override
    public R updateSizeViewCount() {
        Integer oldViewCount = (Integer) redisUtils.get(Constants.Settings.WEB_SIZE_VIEW_COUNT);
        if (oldViewCount != null) {
            redisUtils.incr(Constants.Settings.WEB_SIZE_VIEW_COUNT, 1);
            return R.SUCCESS("浏览量增加成功").data("data",oldViewCount+1);
        }
        Setting webSizeViewCount = settingMapper.selectOne(Wrappers.<Setting>lambdaQuery()
                .eq(Setting::getKey, Constants.Settings.WEB_SIZE_VIEW_COUNT));
        if (webSizeViewCount == null) {
            webSizeViewCount = new Setting();
            webSizeViewCount.setKey(Constants.Settings.WEB_SIZE_VIEW_COUNT);
            webSizeViewCount.setCreateTime(new Date());
            webSizeViewCount.setUpdateTime(new Date());
            webSizeViewCount.setValue("1");
            settingMapper.insert(webSizeViewCount);
            return R.SUCCESS("初始化浏览量成功，下一次请求开始更新");
        }
        int intval = Integer.parseInt(webSizeViewCount.getValue());
        webSizeViewCount.setValue(String.valueOf(intval + 1));
        settingMapper.updateById(webSizeViewCount);
        redisUtils.set(Constants.Settings.WEB_SIZE_VIEW_COUNT,
                intval);
        return R.SUCCESS("更新文章浏览量成功");
    }

    @Override
    public R setOrUpdateWebSizeAdvice(String advice) {
        Integer count = settingMapper.selectCount(Wrappers.<Setting>lambdaQuery()
                .eq(Setting::getKey, Constants.Settings.WEB_SIZE_ADVICE));
        if (count >= 1) {
            settingMapper.update(null, Wrappers.<Setting>lambdaUpdate()
                    .set(Setting::getValue, advice)
                    .set(Setting::getUpdateTime, new Date())
                    .eq(Setting::getKey, Constants.Settings.WEB_SIZE_ADVICE));
            return R.SUCCESS("更新文章的通知成功");
        } else {
            Setting setting = new Setting();
            setting.setKey(Constants.Settings.WEB_SIZE_ADVICE);
            setting.setValue(advice);
            setting.setCreateTime(new Date());
            setting.setUpdateTime(new Date());
            settingMapper.insert(setting);
            return R.SUCCESS("初始化文章通知成功！！");
        }
    }

    @Override
    public R getWebSizeAdvice() {
        Setting setting = settingMapper.selectOne(Wrappers.<Setting>lambdaQuery()
                .eq(Setting::getKey, Constants.Settings.WEB_SIZE_ADVICE));
        if (setting!=null && !setting.getValue().equals("")) {

            return R.SUCCESS("获取文章通知信息成功！").data("data", setting.getValue());
        } else {
            return R.SUCCESS("获取文章通知信息成功！").data("data", "网站主人很懒，啥子都某得留下");
        }
    }
}
