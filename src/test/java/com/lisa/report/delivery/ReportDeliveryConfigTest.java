package com.lisa.report.delivery;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReportDeliveryConfigTest {

    @Test
    void securedefaultsApply() {
        ReportDeliveryConfig config = ReportDeliveryConfig.builder()
                .method(DeliveryMethod.SFTP)
                .build();

        assertThat(config.isStartTls()).isTrue();
        assertThat(config.isStrictHostKeyChecking()).isTrue();
        assertThat(config.getMailProperties()).isEmpty();
    }

    @Test
    void buildsAllFields() {
        ReportDeliveryConfig config = ReportDeliveryConfig.builder()
                .method(DeliveryMethod.EMAIL)
                .fileName("report.xlsx")
                .zipPassword("zp")
                .host("smtp.example.com")
                .port(587)
                .username("user")
                .password("pass")
                .toAddresses("a@example.com,b@example.com")
                .fromAddress("from@example.com")
                .senderName("Reports")
                .subject("Daily")
                .body("see attached")
                .startTls(false)
                .mailProperties(Map.of("mail.smtp.ssl.trust", "*"))
                .remoteDirectory("/out")
                .strictHostKeyChecking(false)
                .knownHostsPath("/etc/known_hosts")
                .build();

        assertThat(config.getMethod()).isEqualTo(DeliveryMethod.EMAIL);
        assertThat(config.getFileName()).isEqualTo("report.xlsx");
        assertThat(config.getZipPassword()).isEqualTo("zp");
        assertThat(config.getHost()).isEqualTo("smtp.example.com");
        assertThat(config.getPort()).isEqualTo(587);
        assertThat(config.getUsername()).isEqualTo("user");
        assertThat(config.getPassword()).isEqualTo("pass");
        assertThat(config.getToAddresses()).isEqualTo("a@example.com,b@example.com");
        assertThat(config.getFromAddress()).isEqualTo("from@example.com");
        assertThat(config.getSenderName()).isEqualTo("Reports");
        assertThat(config.getSubject()).isEqualTo("Daily");
        assertThat(config.getBody()).isEqualTo("see attached");
        assertThat(config.isStartTls()).isFalse();
        assertThat(config.getMailProperties()).containsEntry("mail.smtp.ssl.trust", "*");
        assertThat(config.getRemoteDirectory()).isEqualTo("/out");
        assertThat(config.isStrictHostKeyChecking()).isFalse();
        assertThat(config.getKnownHostsPath()).isEqualTo("/etc/known_hosts");
    }

    @Test
    void mailPropertiesAreDefensivelyCopiedAndUnmodifiable() {
        java.util.Map<String, String> source = new java.util.HashMap<>();
        source.put("k", "v");
        ReportDeliveryConfig config = ReportDeliveryConfig.builder().mailProperties(source).build();
        source.put("added", "later");

        assertThat(config.getMailProperties()).containsOnlyKeys("k");
        assertThatThrownBy(() -> config.getMailProperties().put("x", "y"))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
