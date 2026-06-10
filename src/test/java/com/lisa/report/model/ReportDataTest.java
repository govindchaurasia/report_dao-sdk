package com.lisa.report.model;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ReportDataTest {

    @Test
    void exposesColumnsAndRows() {
        List<String> columns = List.of("a", "b");
        List<Map<String, Object>> rows = List.of(Map.of("a", 1, "b", 2));

        ReportData data = new ReportData(columns, rows);

        assertThat(data.getColumns()).containsExactly("a", "b");
        assertThat(data.getRows()).isEqualTo(rows);
    }
}
