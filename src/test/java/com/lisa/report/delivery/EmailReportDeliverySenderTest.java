package com.lisa.report.delivery;

import com.lisa.report.ReportDeliveryException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EmailReportDeliverySenderTest {

    private final EmailReportDeliverySender sender = new EmailReportDeliverySender();

    @Test
    void supportsEmailMethod() {
        assertThat(sender.getSupportedMethod()).isEqualTo(DeliveryMethod.EMAIL);
    }

    @Test
    void requiresSmtpHost() {
        ReportDeliveryConfig config = ReportDeliveryConfig.builder()
                .method(DeliveryMethod.EMAIL)
                .toAddresses("a@example.com")
                .build();

        assertThatThrownBy(() -> sender.send(new byte[]{1}, "r.csv", "text/csv", config))
                .isInstanceOf(ReportDeliveryException.class)
                .hasMessageContaining("requires an SMTP host");
    }

    @Test
    void requiresAtLeastOneRecipient() {
        ReportDeliveryConfig config = ReportDeliveryConfig.builder()
                .method(DeliveryMethod.EMAIL)
                .host("smtp.example.com")
                .build();

        assertThatThrownBy(() -> sender.send(new byte[]{1}, "r.csv", "text/csv", config))
                .isInstanceOf(ReportDeliveryException.class)
                .hasMessageContaining("at least one recipient");
    }
}
