package com.lisa.report.format;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lisa.report.model.ReportData;
import com.lisa.report.model.ReportFormat;
import com.lisa.report.model.ReportResult;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class JsonReportWriterTest {

    private final JsonReportWriter writer = new JsonReportWriter();

    @Test
    void reportsItsFormat() {
        assertThat(writer.format()).isEqualTo(ReportFormat.JSON);
    }

    @Test
    void rendersRowsAsJsonArray() throws Exception {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("name", "alpha");
        row.put("count", 3);
        ReportData data = new ReportData(List.of("name", "count"), List.of(row));

        ReportResult result = writer.write("orders", data);

        assertThat(result.getContentType()).isEqualTo("application/json");
        assertThat(result.getFilename()).isEqualTo("orders.json");

        List<Map<String, Object>> parsed = new ObjectMapper()
                .readValue(result.getContent(), List.class);
        assertThat(parsed).hasSize(1);
        assertThat(parsed.get(0)).containsEntry("name", "alpha").containsEntry("count", 3);
    }

    @Test
    void rendersEmptyArrayForNoRows() throws Exception {
        ReportData data = new ReportData(List.of(), List.of());

        ReportResult result = writer.write("empty", data);

        List<?> parsed = new ObjectMapper().readValue(result.getContent(), List.class);
        assertThat(parsed).isEmpty();
    }
}
