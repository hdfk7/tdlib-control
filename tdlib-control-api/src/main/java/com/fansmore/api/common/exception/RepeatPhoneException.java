package com.fansmore.api.common.exception;

public class RepeatPhoneException extends BaseException {
    private static final long serialVersionUID = 3664313768520754319L;

    public RepeatPhoneException() {
        this("当前系统已登录改手机号");
    }

    public RepeatPhoneException(String message) {
        super(message);
    }
}
