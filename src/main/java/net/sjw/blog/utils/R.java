package net.sjw.blog.utils;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.HashMap;
import java.util.Map;

/**
 * 统一返回结果类
 */
@Data
@Accessors(chain = true)
public class R {
    private boolean success;
    private int code;
    private String message;
    private Map<String, Object> data = new HashMap<>();
    private R() {}
    public static R SUCCESS() {
        return new R().setSuccess(true)
                .setCode(20000)
                .setMessage("操作成功！！！");
    }
    public static R SUCCESS(String msg) {
        return new R().setSuccess(true)
                .setCode(20000)
                .setMessage(msg);
    }
    public static R LOGIN_SUCCESS() {
        return new R().setSuccess(true)
                .setCode(20001)
                .setMessage("登录成功");
    }
    public static R JOIN_IN_SUCCESS() {
        return new R().setSuccess(true)
                .setCode(20002)
                .setMessage("注册成功");
    }

    public static R FAILED() {
        return new R().setSuccess(false)
                .setCode(40000)
                .setMessage("操作失败...");
    }
    public static R FAILED(String msg) {
        return new R().setSuccess(false)
                .setCode(40000)
                .setMessage(msg);
    }
    public static R LOGIN_FAILED() {
        return new R().setSuccess(false)
                .setCode(49999)
                .setMessage("登录失败...");
    }
    public static R GET_RESOURCE_FAILED() {
        return new R().setSuccess(false)
                .setCode(40001)
                .setMessage("获取资源失败...");
    }

    public static R ACCOUNT_NOT_LOGIN() {
        return new R().setSuccess(false)
                .setCode(40002)
                .setMessage("账号未登录");
    }
    public static R PERMISSION_DENIED() {
        return new R().setSuccess(false)
                .setCode(40002)
                .setMessage("账号权限不够");
    }
    public static R ACCOUNT_DENIED() {
        return new R().setSuccess(false)
                .setCode(40003)
                .setMessage("账号被禁止");
    }

    public static R ERROR_404() {
        return new R().setSuccess(false)
                .setCode(40004)
                .setMessage("页面丢失");
    }
    public static R ERROR_403() {
        return new R().setSuccess(false)
                .setCode(40005)
                .setMessage("权限不够");
    }
    public static R ERROR_504() {
        return new R().setSuccess(false)
                .setCode(40006)
                .setMessage("系统繁忙，请稍后再试");
    }
    public static R ERROR_505() {
        return new R().setSuccess(false)
                .setCode(40007)
                .setMessage("请求错误，请检查所提交的数据");
    }

    public static R WAITING_FOR_SCAN() {
        return new R().setSuccess(false)
                .setCode(40008)
                .setMessage("等待扫码");
    }

    public static R QR_CODE_DEPRECATE() {
        return new R().setSuccess(false)
                .setCode(40009)
                .setMessage("请求错误，请检查所提交的数据");
    }

    public R data(String key, Object value) {
        this.getData().put(key, value);
        return this;
    }
    public R data(Map<String, Object> data) {
        this.getData().putAll(data);
        return this;
    }

    public static void main(String[] args) {
        System.out.println(R.SUCCESS());
    }
}
