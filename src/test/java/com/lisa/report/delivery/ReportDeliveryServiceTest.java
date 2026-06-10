package com.lisa.report.delivery;

import com.lisa.report.ReportDeliveryException;
import com.lisa.report.model.ReportResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportDeliveryServiceTest {

    @Mock
    private PasswordZipService passwordZipService;

    private ReportDeliverySender emailSender(DeliveryMethod method) {
        ReportDeliverySender sender = mock(ReportDeliverySender.class);
        lenient().when(sender.getSupportedMethod()).thenReturn(method);
        return sender;
    }

    @Test
    void routesToSenderForConfiguredMethodWithoutZipping() {
        ReportDeliverySender email = emailSender(DeliveryMethod.EMAIL);
        ReportDeliveryService service = new ReportDeliveryService(List.of(email), passwordZipService);
        ReportResult result = new ReportResult("data".getBytes(StandardCharsets.UTF_8), "text/csv", "r.csv");
        ReportDeliveryConfig config = ReportDeliveryConfig.builder()
                .method(DeliveryMethod.EMAIL)
                .build();

        service.deliver(result, config);

        verify(email).send(result.getContent(), "r.csv", "text/csv", config);
        verify(passwordZipService, never()).zip(any(), any(), any());
    }

    @Test
    void zipsFirstWhenZipPasswordPresent() {
        ReportDeliverySender sftp = emailSender(DeliveryMethod.SFTP);
        ReportDeliveryService service = new ReportDeliveryService(List.of(sftp), passwordZipService);
        ReportResult result = new ReportResult("data".getBytes(StandardCharsets.UTF_8), "text/csv", "r.csv");
        ReportDeliveryConfig config = ReportDeliveryConfig.builder()
                .method(DeliveryMethod.SFTP)
                .zipPassword("p@ss")
                .build();
        byte[] zipped = "zipped".getBytes(StandardCharsets.UTF_8);
        when(passwordZipService.zip("r.csv", result.getContent(), "p@ss")).thenReturn(zipped);

        service.deliver(result, config);

        verify(sftp).send(eq(zipped), eq("r.csv.zip"), eq("application/zip"), eq(config));
    }

    @Test
    void usesConfigFileNameWhenProvided() {
        ReportDeliverySender email = emailSender(DeliveryMethod.EMAIL);
        ReportDeliveryService service = new ReportDeliveryService(List.of(email), passwordZipService);
        ReportResult result = new ReportResult(new byte[]{1}, "text/csv", "fallback.csv");
        ReportDeliveryConfig config = ReportDeliveryConfig.builder()
                .method(DeliveryMethod.EMAIL)
                .fileName("custom.csv")
                .build();

        service.deliver(result, config);

        verify(email).send(any(), eq("custom.csv"), eq("text/csv"), eq(config));
    }

    @Test
    void duplicateSenderForMethodFailsFast() {
        assertThatThrownBy(() -> new ReportDeliveryService(
                List.of(emailSender(DeliveryMethod.EMAIL), emailSender(DeliveryMethod.EMAIL)),
                passwordZipService))
                .isInstanceOf(ReportDeliveryException.class)
                .hasMessageContaining("Multiple delivery senders registered for method EMAIL");
    }

    @Test
    void nullResultThrows() {
        ReportDeliveryService service = new ReportDeliveryService(
                List.of(emailSender(DeliveryMethod.EMAIL)), passwordZipService);

        assertThatThrownBy(() -> service.deliver(null,
                ReportDeliveryConfig.builder().method(DeliveryMethod.EMAIL).build()))
                .isInstanceOf(ReportDeliveryException.class)
                .hasMessageContaining("null report result");
    }

    @Test
    void missingMethodThrows() {
        ReportDeliveryService service = new ReportDeliveryService(
                List.of(emailSender(DeliveryMethod.EMAIL)), passwordZipService);
        ReportResult result = new ReportResult(new byte[]{1}, "text/csv", "r.csv");

        assertThatThrownBy(() -> service.deliver(result, ReportDeliveryConfig.builder().build()))
                .isInstanceOf(ReportDeliveryException.class)
                .hasMessageContaining("Delivery method is required");
    }

    @Test
    void noSenderRegisteredForMethodThrows() {
        ReportDeliveryService service = new ReportDeliveryService(
                List.of(emailSender(DeliveryMethod.EMAIL)), passwordZipService);
        ReportResult result = new ReportResult(new byte[]{1}, "text/csv", "r.csv");
        ReportDeliveryConfig config = ReportDeliveryConfig.builder().method(DeliveryMethod.SFTP).build();

        assertThatThrownBy(() -> service.deliver(result, config))
                .isInstanceOf(ReportDeliveryException.class)
                .hasMessageContaining("No delivery sender registered for method: SFTP");
    }
}
