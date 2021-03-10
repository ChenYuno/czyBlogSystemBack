package net.sjw.blog.utils;

public interface Constants {

    String FROM_PC = "p_";

    String FROM_MOBILE = "m_";

    String APP_DOWNLOAD_PATH = "/portal/app/";

    interface User{
        String ROLE_ADMIN = "role_admin";
        String ROLE_NORMAL = "role_normal";
        String DEFAULT_AVATAR = "https://my-avatar-guli.oss-cn-shenzhen.aliyuncs.com/2020/06/14/24ed6b3fded541d787b0561fa2ccd71801.gif";
        String DEFALUT_STATE = "1";
        //redis的key
        String KEY_CAPTCHA_CONTENT = "key_captcha_content_";
        String KEY_EMAIL_CODE_CONTENT = "key_email_code_content_";
        String KEY_EMAIL_SEND_IP = "key_email_send_ip_";
        String KEY_EMAIL_SEND_ADDRESS = "key_email_send_address_";
        String KEY_TOKEN = "key_token_";
        String KEY_COMMIT_TOKEN_RECORD = "key_commit_token_record_";
        String COOKIE_TOKEN_KEY = "czy_blog_token";
        String KEY_PC_QR_LOGIN_ID = "key_pc_qr_login_id_";
        String LAST_REQUEST_LOGIN_ID = "l_r_l_i";
        int QR_CODE_STATE_CHECK_WAITING_TIME = 30;
    }

    interface ImageType{
        String PREFIX = "image/";
        String TYPE_JPG = "jpg";
        String TYPE_JPEG = "jpeg";
        String TYPE_PNG = "png";
        String TYPE_GIF = "gif";
        String TYPE_JPG_WITH_PREFIX = PREFIX+"jpg";
        String TYPE_JPEG_WITH_PREFIX= PREFIX+"jpeg";
        String TYPE_PNG_WITH_PREFIX = PREFIX+"png";
        String TYPE_GIF_WITH_PREFIX = PREFIX+"gif";
    }
    interface Settings{
        String MANAGER_ACCOUNT_INIT_STATE = "manager_account_init_state";
        String WEB_SIZE_TITLE = "web_size_title";
        String WEB_SIZE_DESCRIPTION = "web_size_description";
        String WEB_SIZE_KEYWORDS = "web_size_keywords";
        String WEB_SIZE_VIEW_COUNT = "web_size_view_count";
        String WEB_SIZE_ADVICE = "web_size_advice";
    }
    interface Page{
        int DEFAULT_PAGE = 1;
        int MIN_SIZE = 10;
    }

    /**
     * 时间单位
     * 起步是毫秒
     */
    interface TimeMillions{
        long MIN = 60*1000;
        long MIN_5 = MIN * 5;
        long HOUR = 60 * MIN;
        long HOUR_2 =2* 60 * MIN;
        long DAY = 24 * HOUR;
        long WEEK = 7 * DAY;
        long MONTH = 30 * DAY;
    }
    /**
     * 时间单位
     * 起步是秒
     */
    interface TimeSecond{
        int SECOND_TEN = 10;
        int MIN = 60;
        int QUARTER_MIN = 15;
        int HALF_MIN = MIN / 2;
        int MIN_5 = 5 * MIN;
        int MIN_15 = 3 * MIN_5;
        int HOUR = 60 * MIN;
        int HOUR_2 =2* 60 * MIN;
        int DAY = 24 * HOUR;
        int WEEK = 7 * DAY;
        int MONTH = 30 * DAY;
    }

    interface Article{
        String ES_ARTICLE_INDEX = "czy_blog_article";
        String TYPE_MARKDOWN = "1";
        String TYPE_RICH_TEXT = "0";
        String DEFAULT_COVER = "1602939278002_1317448935937146880.jpeg";
        int TITLE_MAX_LENGTH = 128;
        int SUMMARY_MAX_LENGTH = 256;
        String STATE_DELETE = "0";
        String STATE_PUBLIC = "1";
        String STATE_DRAFT = "2";
        String STATE_TOP = "3";
        String KEY_ARTICLE_CACHE = "key_article_cache_";
        String KEY_ARTICLE_VIEW_COUNT = "key_article_view_count_";
        String KEY_ARTICLE_LIST_FIRST_PAGE = "key_article_list_first_page";
    }

    interface Comment{
        //0表示删除、1表示已经发布、2表示草稿、3表示置顶
        //TODO:评论管理
        String STATE_PUBLIDH = "1";
        String STATE_TOP = "3";
        String KEY_COMMENT_FIRST_PAGE_CACHE = "key_comment_first_page_cache_";
    }
}
