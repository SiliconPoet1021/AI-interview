package com.guanyan.common.log;

public final class ErrorLogSanitizer {
//    只输出异常类名
    public static String summarize(Throwable e){
        if (e == null) {
            return "unknown";
        }
        return e.getClass().getSimpleName();
    }
//    用来接收第三方调用返回的错误信息，不打印第三方返回的原始报错字符串,防止第三方返回的内容带密钥、账号、身份证等
    public static String summarize(String message){
        if (message == null||message.isBlank()) {
            return "";
        }
        return "external-error";
    }
//
    public static Throwable forLogging(Throwable error){
        String errorType = error == null ? "unknown" : error.getClass().getSimpleName();
        SanitizedLogException sanitized = new SanitizedLogException(errorType);
        if (error != null) {
            sanitized.setStackTrace(error.getStackTrace());
        }
        return sanitized;
    }

    private static final class SanitizedLogException extends Exception{
        private SanitizedLogException(String errorType){
            super("sanitized-error type: " + errorType,null,false,true);
        }
    }
}
