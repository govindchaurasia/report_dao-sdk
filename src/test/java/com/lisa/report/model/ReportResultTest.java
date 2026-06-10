package com.lisa.report.model;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class ReportResultTest {

    @Test
    void exposesContentTypeAndFilename() {
        byte[] content = "hello".getBytes(StandardCharsets.UTF_8);

        ReportResult result = new ReportResult(content, "text/csv", "report.csv");

        assertThat(result.getContent()).isEqualTo(content);
        assertThat(result.getContentType()).isEqualTo("text/csv");
        assertThat(result.getFilename()).isEqualTo("report.csv");
    }
}
