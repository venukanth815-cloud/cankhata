package com.cankhata.exception;

/** Logged in but not allowed (403) */
public class ForbiddenException extends AppException {
    public ForbiddenException(String message) {
        super(403, message);
    }
}
