package com.fansmore.api.common;

public enum ResultCode {
    CUSTOMIZE_ERROR(-2, "前方道路拥挤，请稍后再试"),
    SYS_ERROR(-1, "前方路滑，请稍后再试"),
    SUCCESS(0, "成功"),
    NET_ERROR(-2, "网络请求异常，请稍后重试"),
    MISS_PARAM(10002, "缺少参数"),
    ERROR_PARAM(10003, "参数解析错误"),
    INVALID_PARAM(10004, "无效参数"),
    DATABASE_ERROR(10005, "数据库异常"),
    DATABASE_NULL(10005, "没有记录"),
    AUTH_ERROR(10006, "未授权"),
    FORBID_ERROR(10007, "禁止操作");

    private int code;
    private String msg;

    ResultCode(int code, String msg) {
        bindCode(code).bindMsg(msg);
    }

    public ResultCode bindMsg(String msg) {
        this.msg = msg;
        return this;
    }

    public ResultCode bindCode(int code) {
        this.code = code;
        return this;
    }

    public Result bindResult(Result result) {
        return result.bindCode(this.code).bindMsg(this.msg);
    }

    public Result bindResult() {
        return new Result().bindCode(this.code).bindMsg(this.msg);
    }

    public String getMsg() {
        return msg;
    }

    public int getCode() {
        return code;
    }

}
