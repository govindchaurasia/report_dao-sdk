package com.lisa.report;

/**
 * Thrown when a generated report cannot be delivered (email send failure,
 * SFTP upload failure, zipping failure, or invalid delivery configuration).
 */
public class ReportDeliveryException extends RuntimeException {

    public ReportDeliveryException(String message) {
        super(message);
    }

    public ReportDeliveryException(String message, Throwable cause) {
        super(message, cause);
    }
}
