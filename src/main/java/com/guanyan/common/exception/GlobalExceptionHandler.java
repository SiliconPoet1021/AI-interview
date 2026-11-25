package com.guanyan.common.exception;

import com.guanyan.common.log.ErrorLogSanitizer;
import com.guanyan.common.result.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.net.SocketTimeoutException;
import java.util.stream.Collectors;


@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
//  处理业务异常
    @ExceptionHandler(BusinessException.class)
    @ResponseStatus(HttpStatus.OK)
    public Result<Void> handleBusinessException(BusinessException e) {
        log.error("业务异常:code={}",e.getCode());
        return Result.error(e.getCode(),e.getMessage());
    }
//  处理绑定异常
    @ExceptionHandler(BindException.class)
    @ResponseStatus(HttpStatus.OK)
    public Result<Void> handleBindException(BindException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(","));
        log.warn("参数绑定失败:{}",message);
        return Result.error(ErrorCode.BAD_REQUEST,message);
    }
//    处理文件上传大小超限异常
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    @ResponseStatus(HttpStatus.OK)
    public Result<Void> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException e) {
        log.warn("文件上传大小超限:{}", ErrorLogSanitizer.summarize(e));
        return Result.error(ErrorCode.BAD_REQUEST,e.getMessage());
    }
//
    @ExceptionHandler(ResourceAccessException.class)
    @ResponseStatus(HttpStatus.OK)
    public Result<Void> handleResourceAccessException(ResourceAccessException e) {
        log.error("AI服务链接失败:{}",ErrorLogSanitizer.summarize(e),
            ErrorLogSanitizer.forLogging(e));
        return switch (e.getCause()){
            case SocketTimeoutException _ ->
                Result.error(ErrorCode.AI_SERVICE_TIMEOUT,"AI服务响应超时，请稍后重试");
            case null,default -> {
                String message = e.getMessage();
                if (message != null && message.contains("handshake")) {
                    yield Result.error(ErrorCode.AI_SERVICE_UNAVAILABLE, "AI服务链接失败（网络不稳定），请检查网络或稍后重试");
                }
                yield Result.error(ErrorCode.AI_SERVICE_UNAVAILABLE,"AI服务暂时不可用，请稍后重试");
            }
        };
    }

    @ExceptionHandler(RestClientException.class)
    @ResponseStatus(HttpStatus.OK)
    public Result<Void> handleRestClientException(RestClientException e) {
        log.error("AI服务调用失败：{}",ErrorLogSanitizer.summarize(e),
                ErrorLogSanitizer.forLogging(e));
        return switch (e.getMessage()){
            case null -> Result.error(ErrorCode.AI_SERVICE_ERROR,"AI服务调用失败，请稍后重试");
            case String m when m.contains("401") || m.contains("Unauthorized")->
                Result.error(ErrorCode.AI_API_KEY_INVALID,"AI服务密钥无效，请联系管理员");
            case String m when m.contains("429") || m.contains("Too Many Request")->
                Result.error(ErrorCode.AI_RATE_LIMIT_EXCEEDED,"AI服务调用过于频繁，请稍后重试");
            default -> Result.error(ErrorCode.AI_SERVICE_ERROR,"AI服务调用失败，请稍后重试");
        };
    }

    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.OK)
    public Result<Void> handleNoResourceFoundException(NoResourceFoundException e) {
        log.warn("资源未找到:{}",e.getResourcePath());
        return Result.error(ErrorCode.NOT_FOUND,"API接口不存在");
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    @ResponseStatus(HttpStatus.OK)
    public Result<Void> handleHttpRequestMethodNotSupportedException(HttpRequestMethodNotSupportedException e) {
        log.warn("请求方法不支持:{}{}",e.getMethod(),e.getSupportedHttpMethods());
        return Result.error(ErrorCode.METHOD_NOT_ALLOWED,"请求方法不支持"+e.getMethod());
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.OK)
    public Result<Void> handleException(Exception e) {
        log.error("系统异常:{}",ErrorLogSanitizer.summarize(e),
                ErrorLogSanitizer.forLogging(e));
        return Result.error(ErrorCode.INTERNAL_ERROR,"系统繁忙，请稍后重试");
    }
}
