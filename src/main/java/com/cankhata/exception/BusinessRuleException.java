package com.cankhata.exception;

/** A business rule was broken, e.g. duplicate (409) */
public class BusinessRuleException extends AppException {
    public BusinessRuleException(String message) {
        super(409, message);
    }
}
