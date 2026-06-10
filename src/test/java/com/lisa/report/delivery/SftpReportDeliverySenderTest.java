package com.lisa.report.delivery;

import com.lisa.report.ReportDeliveryException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SftpReportDeliverySenderTest {

    private final SftpReportDeliverySender sender = new SftpReportDeliverySender();

    @Test
    void supportsSftpMethod() {
        assertThat(sender.getSupportedMethod()).isEqualTo(DeliveryMethod.SFTP);
    }

    @Test
    void requiresHost() {
        ReportDeliveryConfig config = ReportDeliveryConfig.builder()
                .method(DeliveryMethod.SFTP)
                .username("user")
                .build();

        assertThatThrownBy(() -> sender.send(new byte[]{1}, "r.csv", "text/csv", config))
                .isInstanceOf(ReportDeliveryException.class)
                .hasMessageContaining("SFTP delivery requires a host");
    }

    @Test
    void requiresUsername() {
        ReportDeliveryConfig config = ReportDeliveryConfig.builder()
                .method(DeliveryMethod.SFTP)
                .host("sftp.example.com")
                .build();

        assertThatThrownBy(() -> sender.send(new byte[]{1}, "r.csv", "text/csv", config))
                .isInstanceOf(ReportDeliveryException.class)
                .hasMessageContaining("SFTP delivery requires a username");
    }
}
