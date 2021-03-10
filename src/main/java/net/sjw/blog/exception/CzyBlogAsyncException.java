package net.sjw.blog.exception;

import lombok.Data;

@Data
public class CzyBlogAsyncException extends Exception {
    private int code;
    private String message;
}
