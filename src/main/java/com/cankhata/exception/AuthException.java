package com.cankhata.exception;

/** Not logged in or wrong login (401) */
public class AuthException extends AppException {
    public AuthException(String message) {
        super(401, message);
    }
}
