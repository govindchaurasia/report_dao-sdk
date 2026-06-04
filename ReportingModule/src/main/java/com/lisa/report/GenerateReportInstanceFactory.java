package com.lisa.report;

import com.lisa.reportingmodule.ReportGenerationException;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Resolves a {@link GenerateReportService} by {@link ReportType}, mirroring the
 * host's {@code GenerateReportInstanceFactory}.
 * <p>
 * All available report services are injected and indexed by their supported type,
 * so adding a new report is just adding a new {@link GenerateReportService} bean.
 */
public class GenerateReportInstanceFactory {

    private final Map<ReportType, GenerateReportService> servicesByType;

    public GenerateReportInstanceFactory(List<GenerateReportService> services) {
        this.servicesByType = new EnumMap<>(ReportType.class);
        for (GenerateReportService service : services) {
            this.servicesByType.put(service.getSupportedType(), service);
        }
    }

    public GenerateReportService getInstanceByType(ReportType reportType) {
        GenerateReportService service = servicesByType.get(reportType);
        if (service == null) {
            throw new ReportGenerationException("No report service registered for type: " + reportType);
        }
        return service;
    }
}
