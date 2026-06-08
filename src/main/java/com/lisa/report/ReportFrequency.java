package com.lisa.report;

import java.util.Locale;

/**
 * How often a report runs. Mirrors the host's {@code com.lisa.enums.ReportFrequency}
 * but is library-owned (different package) so the JAR stays self-contained and does
 * not collide with host classes on the classpath.
 * <p>
 * Drives the date window computed by {@link SdkReportRequestFactory}.
 */
public enum ReportFrequency {

    /** Entire current day. */
    DAILY,
    /** Full current week, Monday–Sunday. */
    WEEKLY,
    /** Entire previous calendar month. */
    MONTHLY,
    /** Rolling last ~3 months (91 days). */
    QUARTERLY,
    /** Entire previous calendar year. */
    YEARLY,
    /** First day of the current month through the last day of the current month. */
    CURRENT_MONTHLY,
    /** First day of the current year through the last day of the current year. */
    CURRENT_YEARLY,
    /** Explicit {@code startDate}/{@code endDate} supplied on the config. */
    CUSTOM;

    /**
     * Lenient, case-insensitive parser.
     */
    public static ReportFrequency fromString(String value) {
        if (value == null || value.isBlank()) {
            throw new ReportGenerationException("Report frequency is required");
        }
        try {
            return ReportFrequency.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new ReportGenerationException("Unsupported report frequency: " + value);
        }
    }
}
