package com.lisa.report;

import com.lisa.report.model.ReportFormat;
import com.lisa.report.model.ReportResult;

/**
 * Strategy contract for a single report type, mirroring the host's
 * {@code com.lisa.service.report.GenerateReportService} pattern.
 * <p>
 * Renamed with an {@code Sdk} prefix so the library's bean/type names do not
 * collide with the host's own {@code GenerateReportService} on the classpath.
 * <p>
 * Implementations fetch their data (through the dao-sdk) and return the rendered
 * output. Notification/delivery is intentionally left to the host — this library
 * only produces the report bytes.
 */
public interface SdkGenerateReportService {

    /**
     * The report type this service handles; used by
     * {@link SdkGenerateReportInstanceFactory} to dispatch.
     */
    ReportType getSupportedType();

    /**
     * Generate the report for the given request in the requested format.
     */
    ReportResult generate(GenerateReportRequestDto request, ReportFormat format);
}
