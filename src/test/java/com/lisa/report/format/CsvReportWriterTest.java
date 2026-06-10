package com.lisa.report.format;

import com.lisa.report.model.ReportData;
import com.lisa.report.model.ReportFormat;
import com.lisa.report.model.ReportResult;
import com.opencsv.CSVReader;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class CsvReportWriterTest {

    private final CsvReportWriter writer = new CsvReportWriter();

    @Test
    void reportsItsFormat() {
        assertThat(writer.format()).isEqualTo(ReportFormat.CSV);
    }

    @Test
    void writesHeaderAndRowsWithNullsAsEmpty() throws Exception {
        Map<String, Object> row1 = new LinkedHashMap<>();
        row1.put("name", "alpha");
        row1.put("count", 3);
        Map<String, Object> row2 = new LinkedHashMap<>();
        row2.put("name", "beta");
        row2.put("count", null);
        ReportData data = new ReportData(List.of("name", "count"), List.of(row1, row2));

        ReportResult result = writer.write("orders", data);

        assertThat(result.getContentType()).isEqualTo("text/csv");
        assertThat(result.getFilename()).isEqualTo("orders.csv");

        try (CSVReader reader = new CSVReader(new InputStreamReader(
                new ByteArrayInputStream(result.getContent()), StandardCharsets.UTF_8))) {
            List<String[]> lines = reader.readAll();
            assertThat(lines.get(0)).containsExactly("name", "count");
            assertThat(lines.get(1)).containsExactly("alpha", "3");
            assertThat(lines.get(2)).containsExactly("beta", "");
        }
    }
}
