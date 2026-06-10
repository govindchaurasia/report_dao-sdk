package com.lisa.report;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReportFrequencyTest {

    @Test
    void parsesCaseInsensitivelyAndTrims() {
        assertThat(ReportFrequency.fromString("daily")).isEqualTo(ReportFrequency.DAILY);
        assertThat(ReportFrequency.fromString("  CURRENT_MONTHLY ")).isEqualTo(ReportFrequency.CURRENT_MONTHLY);
        assertThat(ReportFrequency.fromString("custom")).isEqualTo(ReportFrequency.CUSTOM);
    }

    @Test
    void throwsWhenNullOrBlank() {
        assertThatThrownBy(() -> ReportFrequency.fromString(null))
                .isInstanceOf(ReportGenerationException.class)
                .hasMessageContaining("required");
    }

    @Test
    void throwsOnUnknownFrequency() {
        assertThatThrownBy(() -> ReportFrequency.fromString("HOURLY"))
                .isInstanceOf(ReportGenerationException.class)
                .hasMessageContaining("Unsupported report frequency");
    }
}
