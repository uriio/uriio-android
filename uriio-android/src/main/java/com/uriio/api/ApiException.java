package com.uriio.api;

public class ApiException extends Throwable {
    private final int statusCode;
    private final String message;

    ApiException(int code, String message) {
        this.statusCode = code;
        this.message = message;
    }

    public int getStatusCode() {
        return statusCode;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
