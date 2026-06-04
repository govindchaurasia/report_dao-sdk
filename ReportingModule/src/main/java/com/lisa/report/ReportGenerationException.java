package com.lisa.report;

/**
 * Thrown when a report cannot be generated (unknown report, unsupported format,
 * or a failure while rendering the output).
 */
public class ReportGenerationException extends RuntimeException {

    public ReportGenerationException(String message) {
        super(message);
    }

    public ReportGenerationException(String message, Throwable cause) {
        super(message, cause);
    }
}
