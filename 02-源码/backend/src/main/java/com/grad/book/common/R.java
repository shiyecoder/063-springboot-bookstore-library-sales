package com.grad.book.common;

import lombok.Data;

import java.util.UUID;

/**
 * 统一响应体。业务码 200 表示成功，其余为业务异常；requestId 便于排查日志。
 */
@Data
public class R<T> {
    private int code;
    private String message;
    private T data;
    private String requestId;

    public static <T> R<T> ok(T data) {
        R<T> r = new R<>();
        r.code = 200;
        r.message = "success";
        r.data = data;
        r.requestId = UUID.randomUUID().toString().substring(0, 8);
        return r;
    }

    public static <T> R<T> ok() {
        return ok(null);
    }

    public static <T> R<T> fail(int code, String message) {
        R<T> r = new R<>();
        r.code = code;
        r.message = message;
        r.requestId = UUID.randomUUID().toString().substring(0, 8);
        return r;
    }
}
