package com.sky.handler;

import com.sky.constant.MessageConstant;
import com.sky.exception.BaseException;
import com.sky.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.sql.SQLIntegrityConstraintViolationException;

/**
 * 全局异常处理器，处理项目中抛出的业务异常
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * 捕获业务异常
     * @param ex
     * @return
     */
    @ExceptionHandler
    public Result exceptionHandler(BaseException ex){
        log.error("异常信息：{}", ex.getMessage());
        return Result.error(ex.getMessage());
    }

    /**
     * 捕获SQL异常
     * @param ex
     * @return
     */
    @ExceptionHandler
    public Result exceptionHandler(SQLIntegrityConstraintViolationException ex) {
        String message = ex.getMessage();
        // 如果异常信息中含有Duplicate entry，表明是用户名重复异常
        if (message.contains("Duplicate entry")) {
            String[] split = message.split(" ");
            // 取到重复的用户名
            String username = split[2];
            // MessageConstant.ALREADY_EXISTS 为 "账号已存在"
            String msg = username + MessageConstant.ALREADY_EXISTS;
            log.info(msg);
            return Result.error(msg);
        } else {
            // MessageConstant.UNKNOWN_ERROR 为 "未知错误"
            log.info("MessageConstant.UNKNOWN_ERROR");
            return Result.error(MessageConstant.UNKNOWN_ERROR);
        }
    }
}
