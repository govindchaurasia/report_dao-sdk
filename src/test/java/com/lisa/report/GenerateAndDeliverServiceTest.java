package com.lisa.report;

import com.lisa.report.delivery.DeliveryMethod;
import com.lisa.report.delivery.ReportDeliveryConfig;
import com.lisa.report.delivery.ReportDeliveryService;
import com.lisa.report.model.ReportConfig;
import com.lisa.report.model.ReportFormat;
import com.lisa.report.model.ReportResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GenerateAndDeliverServiceTest {

    @Mock
    private GenerateReportInstanceFactory instanceFactory;
    @Mock
    private ReportDeliveryService deliveryService;
    @Mock
    private GenerateReportService reportService;

    private final ReportRequestFactory requestFactory = new ReportRequestFactory();

    @Test
    void explicitOverloadGeneratesThenDelivers() {
        GenerateAndDeliverService service =
                new GenerateAndDeliverService(instanceFactory, deliveryService, requestFactory);
        GenerateReportRequestDto request = GenerateReportRequestDto.builder().build();
        ReportDeliveryConfig delivery = ReportDeliveryConfig.builder().method(DeliveryMethod.EMAIL).build();
        ReportResult rendered = new ReportResult(new byte[0], "x", "r.xlsx");
        when(instanceFactory.getInstanceByType(ReportType.CONNECTED_CAR_ALERT)).thenReturn(reportService);
        when(reportService.generate(request, ReportFormat.EXCEL)).thenReturn(rendered);

        ReportResult result = service.generateAndDeliver(
                ReportType.CONNECTED_CAR_ALERT, request, ReportFormat.EXCEL, delivery);

        assertThat(result).isSameAs(rendered);
        verify(deliveryService).deliver(rendered, delivery);
    }

    @Test
    void configOverloadResolvesTypeFormatWindowAndMapsDelivery() {
        GenerateAndDeliverService service =
                new GenerateAndDeliverService(instanceFactory, deliveryService, requestFactory);
        ReportConfig config = ReportConfig.builder()
                .reportType("CONNECTED_CAR_ALERT")
                .reportFrequency("DAILY")
                .reportFormat("EXCEL")
                .sendReportType("EMAIL")
                .hostname("smtp.example.com")
                .port(587)
                .username("user")
                .password("pass")
                .toEmailAddress("a@example.com")
                .senderFirstName("Jane")
                .senderLastName("Doe")
                .reportFileName("daily.xlsx")
                .build();
        ReportResult rendered = new ReportResult(new byte[0], "x", "fallback.xlsx");
        when(instanceFactory.getInstanceByType(ReportType.CONNECTED_CAR_ALERT)).thenReturn(reportService);
        when(reportService.generate(any(), eq(ReportFormat.EXCEL))).thenReturn(rendered);

        ReportResult result = service.generateAndDeliver(config);

        assertThat(result).isSameAs(rendered);

        ArgumentCaptor<ReportDeliveryConfig> captor = ArgumentCaptor.forClass(ReportDeliveryConfig.class);
        verify(deliveryService).deliver(eq(rendered), captor.capture());
        ReportDeliveryConfig delivery = captor.getValue();
        assertThat(delivery.getMethod()).isEqualTo(DeliveryMethod.EMAIL);
        assertThat(delivery.getHost()).isEqualTo("smtp.example.com");
        assertThat(delivery.getPort()).isEqualTo(587);
        assertThat(delivery.getToAddresses()).isEqualTo("a@example.com");
        assertThat(delivery.getSenderName()).isEqualTo("Jane Doe");
        assertThat(delivery.getFileName()).isEqualTo("daily.xlsx");
    }

    @Test
    void configOverloadFallsBackToRenderedFilenameWhenNoneProvided() {
        GenerateAndDeliverService service =
                new GenerateAndDeliverService(instanceFactory, deliveryService, requestFactory);
        ReportConfig config = ReportConfig.builder()
                .reportType("CONNECTED_CAR_ALERT")
                .reportFrequency("DAILY")
                .reportFormat("JSON")
                .sendReportType("SFTP")
                .build();
        ReportResult rendered = new ReportResult(new byte[0], "x", "fallback.json");
        when(instanceFactory.getInstanceByType(ReportType.CONNECTED_CAR_ALERT)).thenReturn(reportService);
        when(reportService.generate(any(), eq(ReportFormat.JSON))).thenReturn(rendered);

        service.generateAndDeliver(config);

        ArgumentCaptor<ReportDeliveryConfig> captor = ArgumentCaptor.forClass(ReportDeliveryConfig.class);
        verify(deliveryService).deliver(eq(rendered), captor.capture());
        assertThat(captor.getValue().getFileName()).isEqualTo("fallback.json");
    }

    @Test
    void nullConfigThrows() {
        GenerateAndDeliverService service =
                new GenerateAndDeliverService(instanceFactory, deliveryService, requestFactory);

        assertThatThrownBy(() -> service.generateAndDeliver(null))
                .isInstanceOf(ReportGenerationException.class)
                .hasMessageContaining("Report config is required");
    }
}
