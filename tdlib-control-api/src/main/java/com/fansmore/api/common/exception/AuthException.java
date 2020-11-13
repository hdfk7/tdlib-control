package com.fansmore.api.common.exception;

public class AuthException extends BaseException {
    private static final long serialVersionUID = 3664313768520754319L;

    public AuthException() {
        this("权限认证失败");
    }

    public AuthException(String message) {
        super(message);
    }
}
