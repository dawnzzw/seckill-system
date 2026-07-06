package com.example.seckill.exception;

public class Result<T> {
    private Integer code;
    private String message;
    private T data;

    // ========== 成功 ==========
    public static <T> Result<T> success(T data) {
        Result<T> result = new Result<>();
        result.setCode(ResultCode.SUCCESS.getCode());
        result.setMessage(ResultCode.SUCCESS.getMessage());
        result.setData(data);
        return result;
    }

    // ========== 失败（自定义消息） ==========
    public static <T> Result<T> error(String message) {
        Result<T> result = new Result<>();
        result.setCode(ResultCode.ERROR.getCode());
        result.setMessage(message);
        return result;
    }

    // ========== 失败（使用枚举） ==========
    public static <T> Result<T> error(ResultCode resultCode) {
        Result<T> result = new Result<>();
        result.setCode(resultCode.getCode());
        result.setMessage(resultCode.getMessage());
        return result;
    }

    // ========== 失败（枚举 + 自定义消息） ==========
    public static <T> Result<T> error(ResultCode resultCode, String customMessage) {
        Result<T> result = new Result<>();
        result.setCode(resultCode.getCode());
        result.setMessage(customMessage);
        return result;
    }

    // ========== 自定义（灵活场景） ==========
    public static <T> Result<T> custom(Integer code, String message, T data) {
        Result<T> result = new Result<>();
        result.setCode(code);
        result.setMessage(message);
        result.setData(data);
        return result;
    }

    // getter / setter ...
    public Integer getCode() { return code; }
    public void setCode(Integer code) { this.code = code; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public T getData() { return data; }
    public void setData(T data) { this.data = data; }
}