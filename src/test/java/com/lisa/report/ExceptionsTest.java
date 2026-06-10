package com.lisa.report;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ExceptionsTest {

    @Test
    void reportGenerationExceptionCarriesMessageAndCause() {
        Throwable cause = new IllegalStateException("root");
        ReportGenerationException ex = new ReportGenerationException("failed", cause);

        assertThat(ex).hasMessage("failed");
        assertThat(ex.getCause()).isSameAs(cause);
        assertThat(new ReportGenerationException("only-message").getCause()).isNull();
    }

    @Test
    void reportDeliveryExceptionCarriesMessageAndCause() {
        Throwable cause = new IllegalStateException("root");
        ReportDeliveryException ex = new ReportDeliveryException("failed", cause);

        assertThat(ex).hasMessage("failed");
        assertThat(ex.getCause()).isSameAs(cause);
        assertThat(new ReportDeliveryException("only-message").getCause()).isNull();
    }
}
