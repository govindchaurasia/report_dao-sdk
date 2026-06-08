package com.lisa.report;

import com.lisa.report.model.ReportFormat;
import com.lisa.report.model.ReportResult;

/**
 * Strategy contract for a single report type, mirroring the host's
 * {@code com.lisa.service.report.GenerateReportService} pattern.
 * <p>
 * Library-owned type in package {@code com.lisa.report}. Note the simple name
 * matches the host's own {@code com.lisa.service.report.GenerateReportService}; if
 * the host component-scans both, give the beans explicit names to disambiguate.
 * <p>
 * Implementations fetch their data (through the dao-sdk) and return the rendered
 * output. Notification/delivery is intentionally left to the host — this library
 * only produces the report bytes.
 */
public interface GenerateReportService {

    /**
     * The report type this service handles; used by
     * {@link GenerateReportInstanceFactory} to dispatch.
     */
    ReportType getSupportedType();

    /**
     * Generate the report for the given request in the requested format.
     */
    ReportResult generate(GenerateReportRequestDto request, ReportFormat format);
}
