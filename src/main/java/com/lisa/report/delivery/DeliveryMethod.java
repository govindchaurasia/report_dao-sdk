package com.lisa.report.delivery;

import com.lisa.report.ReportDeliveryException;

import java.util.Locale;

/**
 * How a generated report is delivered. Mirrors the host's {@code sendReportType}
 * concept on {@code ReportConfigMasterEntityDto}.
 */
public enum DeliveryMethod {

    EMAIL,
    SFTP;

    /**
     * Lenient, case-insensitive parser. Accepts common host spellings such as
     * {@code EMAIL}/{@code MAIL} and {@code SFTP}/{@code FTP}.
     */
    public static DeliveryMethod fromString(String value) {
        if (value == null || value.isBlank()) {
            throw new ReportDeliveryException("Delivery method is required (EMAIL or SFTP)");
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        switch (normalized) {
            case "EMAIL":
            case "MAIL":
            case "E-MAIL":
                return EMAIL;
            case "SFTP":
            case "FTP":
                return SFTP;
            default:
                throw new ReportDeliveryException("Unsupported delivery method: " + value);
        }
    }
}
