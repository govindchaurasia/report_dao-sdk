package com.lisa.report.service;

import com.lisa.daosdk.model.ReportData;
import com.lisa.daosdk.service.ReportDataService;
import com.lisa.report.ReportGenerationException;
import com.lisa.report.format.ReportWriter;
import com.lisa.report.model.ReportDefinition;
import com.lisa.report.model.ReportFormat;
import com.lisa.report.model.ReportResult;

import java.util.Collection;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Core orchestration for reporting. Looks up a named report, fetches its data
 * through the dao-sdk, and renders it into the requested format.
 * <p>
 * This is the bean both the REST controller and a Quartz job call into.
 */
public class ReportingService {

    private final ReportDataService dataService;
    private final ReportDefinitionRegistry registry;
    private final Map<ReportFormat, ReportWriter> writers;

    public ReportingService(ReportDataService dataService,
                            ReportDefinitionRegistry registry,
                            List<ReportWriter> writers) {
        this.dataService = dataService;
        this.registry = registry;
        this.writers = new EnumMap<>(ReportFormat.class);
        for (ReportWriter writer : writers) {
            this.writers.put(writer.format(), writer);
        }
    }

    /**
     * Generate a named report in the requested format.
     *
     * @param reportName the registered report name
     * @param parameters runtime parameters merged over the definition's defaults
     * @param format     the output format
     */
    public ReportResult generate(String reportName, Map<String, Object> parameters, ReportFormat format) {
        ReportDefinition definition = registry.get(reportName);
        if (definition == null) {
            throw new ReportGenerationException("Unknown report: " + reportName);
        }
        ReportWriter writer = writers.get(format);
        if (writer == null) {
            throw new ReportGenerationException("Unsupported report format: " + format);
        }

        Map<String, Object> merged = new LinkedHashMap<>(definition.getDefaultParameters());
        if (parameters != null) {
            merged.putAll(parameters);
        }

        ReportData data = dataService.runNativeQuery(definition.getSql(), merged);
        return writer.write(reportName, data);
    }

    public Collection<String> availableReports() {
        return registry.names();
    }
}
