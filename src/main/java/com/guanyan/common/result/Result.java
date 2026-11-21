package com.guanyan.common.result;

public class Result<T> {
    private final Integer code;
    private final String message;
    private final T data;

    private Result(Integer code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public static <T> Result<T> success(){
        return new Result<>(CommonConstants.s);
    }
}
