package com.lisa.report.model;

import com.lisa.report.ReportGenerationException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReportFormatTest {

    @Test
    void parsesCanonicalValuesCaseInsensitively() {
        assertThat(ReportFormat.fromString("json")).isEqualTo(ReportFormat.JSON);
        assertThat(ReportFormat.fromString("Excel")).isEqualTo(ReportFormat.EXCEL);
        assertThat(ReportFormat.fromString("  CSV  ")).isEqualTo(ReportFormat.CSV);
    }

    @Test
    void defaultsToJsonWhenNullOrBlank() {
        assertThat(ReportFormat.fromString(null)).isEqualTo(ReportFormat.JSON);
        assertThat(ReportFormat.fromString("")).isEqualTo(ReportFormat.JSON);
        assertThat(ReportFormat.fromString("   ")).isEqualTo(ReportFormat.JSON);
    }

    @Test
    void throwsOnUnsupportedValue() {
        assertThatThrownBy(() -> ReportFormat.fromString("pdf"))
                .isInstanceOf(ReportGenerationException.class)
                .hasMessageContaining("Unsupported report format: pdf");
    }
}
