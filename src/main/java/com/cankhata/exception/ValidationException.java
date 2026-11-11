package com.cankhata.exception;

/** Bad input (400) */
public class ValidationException extends AppException {
    public ValidationException(String message) {
        super(400, message);
    }
}
