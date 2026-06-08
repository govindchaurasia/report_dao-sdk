package com.lisa.report.format;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.lisa.report.model.ReportData;
import com.lisa.report.ReportGenerationException;
import com.lisa.report.model.ReportFormat;
import com.lisa.report.model.ReportResult;

/**
 * Renders a report as pretty-printed JSON (an array of row objects).
 */
public class JsonReportWriter implements ReportWriter {

    private final ObjectMapper objectMapper;

    public JsonReportWriter() {
        this.objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Override
    public ReportFormat format() {
        return ReportFormat.JSON;
    }

    @Override
    public ReportResult write(String reportName, ReportData data) {
        try {
            byte[] content = objectMapper
                    .writerWithDefaultPrettyPrinter()
                    .writeValueAsBytes(data.getRows());
            return new ReportResult(content, "application/json", reportName + ".json");
        } catch (Exception ex) {
            throw new ReportGenerationException("Failed to render JSON report '" + reportName + "'", ex);
        }
    }
}
