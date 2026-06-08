package com.lisa.report;

import java.util.Locale;

/**
 * Report types the library framework can dispatch.
 * <p>
 * This is the library's own type enum (deliberately not the host's
 * {@code com.lisa.enums.ReportType}) so the JAR stays self-contained and does not
 * collide with host classes on the classpath. The values mirror the host's report
 * types so a host config's {@code reportType} string maps straight across via
 * {@link #fromString(String)}.
 * <p>
 * A {@link SdkGenerateReportService} bean must be registered for each type that is
 * actually generated. The library ships {@link #CONNECTED_CAR_ALERT}; the host
 * registers its own {@code SdkGenerateReportService} beans for the rest.
 */
public enum ReportType {

    LEAD_STATUS,
    OPT_OUT,
    APPT_ACTIVITY,
    RESPONSE_INSIGHT,
    INBOUND_ACTIVITY_REPORT,
    PERFORMANCE_SUMMARY,
    PERFORMANCE_SUMMARY_PER_STORE,
    CONTROL_GROUP_TREATMENT,
    LEAD_STATUS_ACTIVITY,
    MB_CONNECTEDCAR_NOTTEXTTABLE_NRAA,
    AUTONATION_EXCLUSION_REPORT,
    AUTONATION_INCLUSION_REPORT,
    AUTONATION_INCLUSION_PLANNED_DATE_REPORT,
    AFTER_HOURS_DETAIL,
    NO_SERVICE_FULL,
    NO_SERVICE_PARTIAL,
    CONNECTED_CAR_ALERT;

    /**
     * Lenient, case-insensitive parser.
     */
    public static ReportType fromString(String value) {
        if (value == null || value.isBlank()) {
            throw new ReportGenerationException("Report type is required");
        }
        try {
            return ReportType.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new ReportGenerationException("Unsupported report type: " + value);
        }
    }
}
