package com.lisa.report.scheduling;

import com.lisa.report.config.ReportingProperties;
import com.lisa.report.model.ReportFormat;
import com.lisa.report.model.ReportResult;
import com.lisa.report.service.ReportingService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportGenerationJobTest {

    @Mock
    private ReportingService reportingService;
    @Mock
    private JobExecutionContext context;

    private ReportGenerationJob newJob(ReportingProperties properties) {
        ReportGenerationJob job = new ReportGenerationJob();
        ReflectionTestUtils.setField(job, "reportingService", reportingService);
        ReflectionTestUtils.setField(job, "properties", properties);
        return job;
    }

    @Test
    void generatesAndWritesReportToOutputDirectory(@TempDir Path tempDir) throws Exception {
        ReportingProperties properties = new ReportingProperties();
        properties.setOutputDirectory(tempDir.resolve("nested").toString());
        ReportGenerationJob job = newJob(properties);

        JobDataMap dataMap = new JobDataMap();
        dataMap.put("reportName", "orders");
        dataMap.put("format", "CSV");
        dataMap.put("startDate", "2026-01-01");
        when(context.getMergedJobDataMap()).thenReturn(dataMap);

        byte[] bytes = "x".getBytes(StandardCharsets.UTF_8);
        when(reportingService.generate(eq("orders"), any(), eq(ReportFormat.CSV)))
                .thenReturn(new ReportResult(bytes, "text/csv", "orders.csv"));

        job.execute(context);

        Path written = tempDir.resolve("nested").resolve("orders.csv");
        assertThat(Files.exists(written)).isTrue();
        assertThat(Files.readAllBytes(written)).isEqualTo(bytes);

        @SuppressWarnings("unchecked")
        org.mockito.ArgumentCaptor<Map<String, Object>> captor =
                org.mockito.ArgumentCaptor.forClass(Map.class);
        verify(reportingService).generate(eq("orders"), captor.capture(), eq(ReportFormat.CSV));
        assertThat(captor.getValue())
                .containsEntry("startDate", "2026-01-01")
                .doesNotContainKey("reportName")
                .doesNotContainKey("format");
    }

    @Test
    void missingReportNameThrows() {
        ReportGenerationJob job = newJob(new ReportingProperties());
        when(context.getMergedJobDataMap()).thenReturn(new JobDataMap());

        assertThatThrownBy(() -> job.execute(context))
                .isInstanceOf(JobExecutionException.class)
                .hasMessageContaining("reportName");
    }

    @Test
    void generationFailureIsWrappedAsJobExecutionException(@TempDir Path tempDir) {
        ReportingProperties properties = new ReportingProperties();
        properties.setOutputDirectory(tempDir.toString());
        ReportGenerationJob job = newJob(properties);

        JobDataMap dataMap = new JobDataMap();
        dataMap.put("reportName", "orders");
        when(context.getMergedJobDataMap()).thenReturn(dataMap);
        when(reportingService.generate(eq("orders"), any(), eq(ReportFormat.JSON)))
                .thenThrow(new RuntimeException("boom"));

        assertThatThrownBy(() -> job.execute(context))
                .isInstanceOf(JobExecutionException.class)
                .hasMessageContaining("Failed to generate scheduled report 'orders'");
    }
}
