package com.lisa.report.model;

import com.lisa.reportingmodule.ReportGenerationException;

import java.util.Locale;

/**
 * Supported report output formats.
 */
public enum ReportFormat {

    JSON,
    EXCEL,
    CSV;

    /**
     * Lenient, case-insensitive parser. Defaults to {@link #JSON} when blank.
     */
    public static ReportFormat fromString(String value) {
        if (value == null || value.isBlank()) {
            return JSON;
        }
        try {
            return ReportFormat.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new ReportGenerationException("Unsupported report format: " + value);
        }
    }
}
