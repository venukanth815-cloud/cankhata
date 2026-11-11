package com.cankhata.exception;

import java.sql.SQLException;

/** Wraps SQLException so the service layer does not have to catch checked exceptions. Shows a safe message to users. */
public class DataAccessException extends AppException {
    public DataAccessException(String where, SQLException cause) {
        super(500, "Database error. Please try again.", cause);
        // The detailed reason goes to the log, never to the browser.
        org.slf4j.LoggerFactory.getLogger(DataAccessException.class)
                .error("{} failed: {} (state={}, code={})", where, cause.getMessage(), cause.getSQLState(), cause.getErrorCode());
    }
}
