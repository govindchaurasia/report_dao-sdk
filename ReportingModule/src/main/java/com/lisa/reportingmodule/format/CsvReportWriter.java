package com.lisa.reportingmodule.format;

import com.lisa.daosdk.model.ReportData;
import com.lisa.reportingmodule.ReportGenerationException;
import com.lisa.reportingmodule.model.ReportFormat;
import com.lisa.reportingmodule.model.ReportResult;
import com.opencsv.CSVWriter;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * Renders a report as CSV using OpenCSV.
 */
public class CsvReportWriter implements ReportWriter {

    @Override
    public ReportFormat format() {
        return ReportFormat.CSV;
    }

    @Override
    public ReportResult write(String reportName, ReportData data) {
        List<String> columns = data.getColumns();
        try (ByteArrayOutputStream out = new ByteArrayOutputStream();
             OutputStreamWriter writer = new OutputStreamWriter(out, StandardCharsets.UTF_8);
             CSVWriter csvWriter = new CSVWriter(writer)) {

            csvWriter.writeNext(columns.toArray(new String[0]));

            for (Map<String, Object> row : data.getRows()) {
                String[] line = new String[columns.size()];
                for (int i = 0; i < columns.size(); i++) {
                    Object value = row.get(columns.get(i));
                    line[i] = value == null ? "" : String.valueOf(value);
                }
                csvWriter.writeNext(line);
            }

            csvWriter.flush();
            writer.flush();
            return new ReportResult(out.toByteArray(), "text/csv", reportName + ".csv");
        } catch (Exception ex) {
            throw new ReportGenerationException("Failed to render CSV report '" + reportName + "'", ex);
        }
    }
}
