package net.sjw.blog.service;

import net.sjw.blog.utils.R;

public interface WebSizeInfoService {
    R getWebSizeTitle();

    R putWebSizeTitle(String title);

    R getSeoInfo();

    R putSeoInfo(String keywords, String description);

    R getSizeViewCount();

    R updateSizeViewCount();

    R setOrUpdateWebSizeAdvice(String advice);

    R getWebSizeAdvice();

}
