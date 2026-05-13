package com.kurashnation.exception;

public class SystemException extends BaseException {
    public SystemException(String message) {
        super(message);
    }

    public SystemException(String message, Throwable cause) {
        super(message, cause);
    }
}

