package com.lisa.reportingmodule.scheduling;

import com.lisa.reportingmodule.config.ReportingProperties;
import com.lisa.report.model.ReportFormat;
import com.lisa.report.model.ReportResult;
import com.lisa.reportingmodule.service.ReportingService;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Quartz job that generates a report on a schedule and writes it to the
 * configured output directory.
 * <p>
 * The host application schedules this with a {@code JobDataMap} containing at
 * least {@code reportName} (and optionally {@code format}; any other entries are
 * passed through as SQL parameters). Spring's {@code SpringBeanJobFactory}
 * (enabled automatically by spring-boot-starter-quartz) autowires the
 * dependencies below into each job instance.
 */
@DisallowConcurrentExecution
public class ReportGenerationJob implements Job {

    @Autowired
    private ReportingService reportingService;

    @Autowired
    private ReportingProperties properties;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        JobDataMap dataMap = context.getMergedJobDataMap();
        String reportName = dataMap.getString("reportName");
        if (reportName == null || reportName.isBlank()) {
            throw new JobExecutionException("Quartz job requires a 'reportName' entry in the JobDataMap");
        }
        String formatValue = dataMap.containsKey("format") ? dataMap.getString("format") : "JSON";

        Map<String, Object> parameters = new LinkedHashMap<>();
        for (String key : dataMap.getKeys()) {
            if (!"reportName".equals(key) && !"format".equals(key)) {
                parameters.put(key, dataMap.get(key));
            }
        }

        try {
            ReportResult result = reportingService.generate(
                    reportName, parameters, ReportFormat.fromString(formatValue));
            Path directory = Paths.get(properties.getOutputDirectory());
            Files.createDirectories(directory);
            Files.write(directory.resolve(result.getFilename()), result.getContent());
        } catch (Exception ex) {
            throw new JobExecutionException(
                    "Failed to generate scheduled report '" + reportName + "'", ex);
        }
    }
}
