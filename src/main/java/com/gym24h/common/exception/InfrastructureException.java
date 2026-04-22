package com.gym24h.common.exception;

public class InfrastructureException extends RuntimeException {

    private final String code;

    public InfrastructureException(String code, String message) {
        super(message);
        this.code = code;
    }

    public InfrastructureException(String code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
