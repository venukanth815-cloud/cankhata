package com.cankhata.exception;

/** Record not found (404) */
public class NotFoundException extends AppException {
    public NotFoundException(String message) {
        super(404, message);
    }
}
