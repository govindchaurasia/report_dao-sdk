package com.lisa.report;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReportTypeTest {

    @Test
    void parsesCaseInsensitivelyAndTrims() {
        assertThat(ReportType.fromString("connected_car_alert")).isEqualTo(ReportType.CONNECTED_CAR_ALERT);
        assertThat(ReportType.fromString("  LEAD_STATUS  ")).isEqualTo(ReportType.LEAD_STATUS);
    }

    @Test
    void throwsWhenNullOrBlank() {
        assertThatThrownBy(() -> ReportType.fromString(null))
                .isInstanceOf(ReportGenerationException.class)
                .hasMessageContaining("required");
        assertThatThrownBy(() -> ReportType.fromString("  "))
                .isInstanceOf(ReportGenerationException.class)
                .hasMessageContaining("required");
    }

    @Test
    void throwsOnUnknownType() {
        assertThatThrownBy(() -> ReportType.fromString("NOT_A_TYPE"))
                .isInstanceOf(ReportGenerationException.class)
                .hasMessageContaining("Unsupported report type");
    }
}
