package com.fansmore.api.common.exception;

import com.fansmore.api.common.ResultCode;

public class BaseException extends RuntimeException {
    private static final long serialVersionUID = 6413136607285578265L;

    public BaseException() {
        this(ResultCode.CUSTOMIZE_ERROR.getMsg());
    }

    public BaseException(String message) {
        super(message);
    }
}
