package com.example.seckill.exception;

/**
 * 统一状态码枚举
 */
public enum ResultCode {

    // 成功
    SUCCESS(200, "success"),

    // 客户端错误（4xx）
    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未登录或登录已过期"),
    FORBIDDEN(403, "没有权限访问"),
    NOT_FOUND(404, "请求的资源不存在"),

    // 业务错误（5xx）
    ERROR(500, "系统繁忙，请稍后再试"),
    STOCK_INSUFFICIENT(5001, "库存不足"),
    SECKILL_ENDED(5002, "秒杀已结束"),
    SECKILL_NOT_START(5003, "秒杀尚未开始"),
    ORDER_NOT_EXIST(5004, "订单不存在"),
    ORDER_STATUS_ERROR(5005, "订单状态不允许操作"),
    RATE_LIMIT(5006, "操作过于频繁，请稍后再试"),
    PRODUCT_NOT_EXIST(5007, "商品不存在");

    private final Integer code;
    private final String message;

    ResultCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }

    public Integer getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}