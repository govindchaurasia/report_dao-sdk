package com.lisa.report.service;

import com.lisa.report.ReportGenerationException;
import com.lisa.report.format.ReportWriter;
import com.lisa.report.model.ReportData;
import com.lisa.report.model.ReportDefinition;
import com.lisa.report.model.ReportFormat;
import com.lisa.report.model.ReportResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportingServiceTest {

    @Mock
    private ReportDataService dataService;
    @Mock
    private ReportDefinitionRegistry registry;
    @Mock
    private ReportWriter jsonWriter;

    private ReportingService service;

    @BeforeEach
    void setUp() {
        when(jsonWriter.format()).thenReturn(ReportFormat.JSON);
        service = new ReportingService(dataService, registry, List.of(jsonWriter));
    }

    @Test
    void mergesRuntimeParametersOverDefaultsAndRenders() {
        ReportDefinition def = new ReportDefinition(
                "orders", "SELECT 1", Map.of("storeId", "0", "limit", "10"));
        when(registry.get("orders")).thenReturn(def);
        ReportData data = new ReportData(List.of("a"), List.of());
        when(dataService.runNativeQuery(eq("SELECT 1"), any())).thenReturn(data);
        ReportResult rendered = new ReportResult(new byte[0], "application/json", "orders.json");
        when(jsonWriter.write("orders", data)).thenReturn(rendered);

        ReportResult result = service.generate("orders", Map.of("storeId", "5"), ReportFormat.JSON);

        assertThat(result).isSameAs(rendered);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        org.mockito.Mockito.verify(dataService).runNativeQuery(eq("SELECT 1"), captor.capture());
        assertThat(captor.getValue())
                .containsEntry("storeId", "5")   // runtime overrides default
                .containsEntry("limit", "10");   // default preserved
    }

    @Test
    void nullRuntimeParametersUsesDefaultsOnly() {
        ReportDefinition def = new ReportDefinition("orders", "SELECT 1", Map.of("limit", "10"));
        when(registry.get("orders")).thenReturn(def);
        ReportData data = new ReportData(List.of(), List.of());
        when(dataService.runNativeQuery(eq("SELECT 1"), any())).thenReturn(data);
        when(jsonWriter.write(eq("orders"), any())).thenReturn(
                new ReportResult(new byte[0], "application/json", "orders.json"));

        service.generate("orders", null, ReportFormat.JSON);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        org.mockito.Mockito.verify(dataService).runNativeQuery(eq("SELECT 1"), captor.capture());
        assertThat(captor.getValue()).containsEntry("limit", "10");
    }

    @Test
    void unknownReportThrows() {
        when(registry.get("missing")).thenReturn(null);

        assertThatThrownBy(() -> service.generate("missing", Map.of(), ReportFormat.JSON))
                .isInstanceOf(ReportGenerationException.class)
                .hasMessageContaining("Unknown report: missing");
    }

    @Test
    void unsupportedFormatThrows() {
        when(registry.get("orders")).thenReturn(new ReportDefinition("orders", "SELECT 1", Map.of()));

        assertThatThrownBy(() -> service.generate("orders", Map.of(), ReportFormat.EXCEL))
                .isInstanceOf(ReportGenerationException.class)
                .hasMessageContaining("Unsupported report format");
    }

    @Test
    void availableReportsDelegatesToRegistry() {
        when(registry.names()).thenReturn(List.of("a", "b"));

        assertThat(service.availableReports()).containsExactly("a", "b");
    }
}
