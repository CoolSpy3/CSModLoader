package com.coolspy3.csmodloader.util;

public class WrapperException extends RuntimeException {

    public WrapperException() {
    }

    public WrapperException(String message) {
        super(message);
    }

    public WrapperException(Throwable cause) {
        super(cause);
    }

    public WrapperException(String message, Throwable cause) {
        super(message, cause);
    }

}
