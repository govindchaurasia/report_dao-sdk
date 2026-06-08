package com.lisa.report;

import com.lisa.report.ReportGenerationException;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Resolves a {@link SdkGenerateReportService} by {@link ReportType}, mirroring the
 * host's {@code GenerateReportInstanceFactory}.
 * <p>
 * Renamed with an {@code Sdk} prefix so the library's bean/type names do not
 * collide with the host's own {@code GenerateReportInstanceFactory} on the classpath.
 * <p>
 * All available report services are injected and indexed by their supported type,
 * so adding a new report is just adding a new {@link SdkGenerateReportService} bean.
 */
public class SdkGenerateReportInstanceFactory {

    private final Map<ReportType, SdkGenerateReportService> servicesByType;

    public SdkGenerateReportInstanceFactory(List<SdkGenerateReportService> services) {
        this.servicesByType = new EnumMap<>(ReportType.class);
        for (SdkGenerateReportService service : services) {
            this.servicesByType.put(service.getSupportedType(), service);
        }
    }

    public SdkGenerateReportService getInstanceByType(ReportType reportType) {
        SdkGenerateReportService service = servicesByType.get(reportType);
        if (service == null) {
            throw new ReportGenerationException("No report service registered for type: " + reportType);
        }
        return service;
    }
}
