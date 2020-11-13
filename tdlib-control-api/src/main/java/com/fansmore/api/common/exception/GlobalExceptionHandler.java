package com.fansmore.api.common.exception;

import com.fansmore.api.common.ResultCode;
import lombok.extern.slf4j.Slf4j;
import com.fansmore.api.common.Result;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(value = Exception.class)
    public Result handler(Exception e) {
        log.error(e.getMessage(), e);
        Result result = ResultCode.SYS_ERROR.bindResult();
        return result.bindMsg(result.getMsg() + " | " + e.getMessage());
    }

    @ExceptionHandler(BaseException.class)
    public Result handler(BaseException e) {
        log.error(e.getMessage(), e);
        Result result = ResultCode.CUSTOMIZE_ERROR.bindResult();
        return result.bindMsg(e.getMessage());
    }

    @ExceptionHandler(value = NoHandlerFoundException.class)
    public Result handler(NoHandlerFoundException e) {
        log.error(e.getMessage(), e);
        Result result = ResultCode.CUSTOMIZE_ERROR.bindResult();
        return result.bindMsg(e.getMessage());
    }
}

