package com.cankhata.exception;

/**
 * Base class for every error we expect. It carries the HTTP status code,
 * so one central handler (ErrorHandler) can turn any of them into a proper response.
 */
public class AppException extends RuntimeException {
    private final int status;

    public AppException(int status, String message) {
        super(message);
        this.status = status;
    }

    public AppException(int status, String message, Throwable cause) {
        super(message, cause);
        this.status = status;
    }

    public int getStatus() {
        return status;
    }
}
