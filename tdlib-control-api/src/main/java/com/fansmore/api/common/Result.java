package com.fansmore.api.common;

import com.fansmore.api.utils.JSONUtils;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.springframework.util.StringUtils;

import java.io.Serializable;

public class Result implements Serializable {
    public static final int SUCCESS = 0;
    public static final int ERROR = 1;

    private int code;
    private String msg;
    private Object data;

    public Result() {
        this(ResultCode.SUCCESS);
    }

    public Result(ResultCode resultCode) {
        this(resultCode, null);
    }

    public Result(ResultCode resultCode, String message) {
        this(resultCode, message, null);
    }

    public Result(ResultCode resultCode, String message, Object data) {
        bindCode(resultCode.getCode()).bindMsg(StringUtils.isEmpty(message) ? resultCode.getMsg() : message).bindData(data);
    }

    public Result bindData(Object data) {
        this.data = data;
        return this;
    }

    public Result bindMsg(String msg) {
        this.msg = msg;
        return this;
    }

    public Result bindCode(int code) {
        this.code = code;
        return this;
    }

    public Object getData() {
        return data;
    }

    public int getCode() {
        return code;
    }

    public String getMsg() {
        return msg;
    }

    @JsonIgnore
    public boolean isSuccess() {
        return this.code == SUCCESS;
    }

    @Override
    public String toString() {
        return JSONUtils.toJSONString(this);
    }
}
