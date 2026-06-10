package com.lisa.report;

import com.lisa.report.model.ReportFormat;
import com.lisa.report.model.ReportResult;
import com.lisa.report.service.ReportingService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GenerateConnectedCarAlertReportServiceTest {

    @Mock
    private ReportingService reportingService;

    @Test
    void supportsConnectedCarAlertType() {
        GenerateConnectedCarAlertReportService service =
                new GenerateConnectedCarAlertReportService(reportingService);

        assertThat(service.getSupportedType()).isEqualTo(ReportType.CONNECTED_CAR_ALERT);
    }

    @Test
    void mapsAllNonNullRequestFieldsToParameters() {
        GenerateConnectedCarAlertReportService service =
                new GenerateConnectedCarAlertReportService(reportingService);
        Date start = new Date(1_000L);
        Date end = new Date(2_000L);
        GenerateReportRequestDto request = GenerateReportRequestDto.builder()
                .enterpriseId(7L)
                .storeIdFK(42L)
                .startDate(start)
                .endDate(end)
                .build();
        ReportResult expected = new ReportResult(new byte[0], "x", "x.xlsx");
        when(reportingService.generate(eq("connected-car-alerts"), any(), eq(ReportFormat.EXCEL)))
                .thenReturn(expected);

        ReportResult result = service.generate(request, ReportFormat.EXCEL);

        assertThat(result).isSameAs(expected);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(reportingService).generate(eq("connected-car-alerts"), captor.capture(), eq(ReportFormat.EXCEL));
        assertThat(captor.getValue())
                .containsEntry("enterpriseId", 7L)
                .containsEntry("storeId", 42L)
                .containsEntry("startDate", start)
                .containsEntry("endDate", end);
    }

    @Test
    void nullRequestSendsEmptyParametersSoDefaultsApply() {
        GenerateConnectedCarAlertReportService service =
                new GenerateConnectedCarAlertReportService(reportingService);
        when(reportingService.generate(eq("connected-car-alerts"), any(), eq(ReportFormat.JSON)))
                .thenReturn(new ReportResult(new byte[0], "x", "x.json"));

        service.generate(null, ReportFormat.JSON);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(reportingService).generate(eq("connected-car-alerts"), captor.capture(), eq(ReportFormat.JSON));
        assertThat(captor.getValue()).isEmpty();
    }
}
