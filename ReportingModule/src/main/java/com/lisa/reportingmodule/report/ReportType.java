package com.lisa.reportingmodule.report;

/**
 * Report types implemented inside this library.
 * <p>
 * This is the library's own type enum (deliberately not the host's
 * {@code com.lisa.enums.ReportType}) so the JAR stays self-contained and does not
 * collide with host classes on the classpath. Add a value here when you implement
 * another {@link GenerateReportService} in the library.
 */
public enum ReportType {

    CONNECTED_CAR_ALERT
}
