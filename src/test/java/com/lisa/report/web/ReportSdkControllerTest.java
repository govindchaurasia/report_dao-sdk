package com.lisa.report.web;

import com.lisa.report.model.ReportFormat;
import com.lisa.report.model.ReportResult;
import com.lisa.report.service.ReportingService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportSdkControllerTest {

    @Mock
    private ReportingService reportingService;

    @Test
    void listReportsDelegatesToService() {
        ReportSdkController controller = new ReportSdkController(reportingService);
        when(reportingService.availableReports()).thenReturn(List.of("a", "b"));

        assertThat(controller.listReports()).containsExactly("a", "b");
    }

    @Test
    void generateStripsFormatParamAndSetsDownloadHeaders() {
        ReportSdkController controller = new ReportSdkController(reportingService);
        byte[] bytes = "data".getBytes(StandardCharsets.UTF_8);
        ReportResult result = new ReportResult(bytes, "text/csv", "orders.csv");
        when(reportingService.generate(eq("orders"), org.mockito.ArgumentMatchers.any(), eq(ReportFormat.CSV)))
                .thenReturn(result);

        Map<String, String> queryParams = new LinkedHashMap<>();
        queryParams.put("format", "csv");
        queryParams.put("startDate", "2026-01-01");

        ResponseEntity<byte[]> response = controller.generate("orders", "csv", queryParams);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isEqualTo(bytes);
        assertThat(response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION))
                .isEqualTo("attachment; filename=\"orders.csv\"");
        assertThat(response.getHeaders().getContentType().toString()).isEqualTo("text/csv");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(reportingService).generate(eq("orders"), captor.capture(), eq(ReportFormat.CSV));
        assertThat(captor.getValue()).doesNotContainKey("format").containsEntry("startDate", "2026-01-01");
    }
}
