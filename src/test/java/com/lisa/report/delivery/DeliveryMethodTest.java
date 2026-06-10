package com.lisa.report.delivery;

import com.lisa.report.ReportDeliveryException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DeliveryMethodTest {

    @Test
    void parsesEmailSpellings() {
        assertThat(DeliveryMethod.fromString("email")).isEqualTo(DeliveryMethod.EMAIL);
        assertThat(DeliveryMethod.fromString("MAIL")).isEqualTo(DeliveryMethod.EMAIL);
        assertThat(DeliveryMethod.fromString(" e-mail ")).isEqualTo(DeliveryMethod.EMAIL);
    }

    @Test
    void parsesSftpSpellings() {
        assertThat(DeliveryMethod.fromString("sftp")).isEqualTo(DeliveryMethod.SFTP);
        assertThat(DeliveryMethod.fromString("FTP")).isEqualTo(DeliveryMethod.SFTP);
    }

    @Test
    void throwsWhenNullOrBlank() {
        assertThatThrownBy(() -> DeliveryMethod.fromString(null))
                .isInstanceOf(ReportDeliveryException.class)
                .hasMessageContaining("required");
    }

    @Test
    void throwsOnUnsupportedValue() {
        assertThatThrownBy(() -> DeliveryMethod.fromString("carrier-pigeon"))
                .isInstanceOf(ReportDeliveryException.class)
                .hasMessageContaining("Unsupported delivery method");
    }
}
