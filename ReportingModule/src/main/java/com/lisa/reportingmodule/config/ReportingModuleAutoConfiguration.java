package com.lisa.reportingmodule.config;

import com.lisa.daosdk.config.DaoSdkAutoConfiguration;
import com.lisa.daosdk.service.ReportDataService;
import com.lisa.reportingmodule.format.CsvReportWriter;
import com.lisa.reportingmodule.format.ExcelReportWriter;
import com.lisa.reportingmodule.format.JsonReportWriter;
import com.lisa.reportingmodule.format.ReportWriter;
import com.lisa.reportingmodule.model.ReportDefinition;
import com.lisa.reportingmodule.service.ReportDefinitionRegistry;
import com.lisa.reportingmodule.service.ReportingService;
import com.lisa.reportingmodule.web.ReportController;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import java.util.List;
import java.util.Map;

/**
 * Auto-configuration for the reporting module.
 * <p>
 * Registered in {@code META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports}
 * so the host application gets the report writers, registry, service and REST
 * controller simply by having this JAR on the classpath. Runs after
 * {@link DaoSdkAutoConfiguration} so the {@link ReportDataService} is available.
 */
@AutoConfiguration(after = DaoSdkAutoConfiguration.class)
@EnableConfigurationProperties(ReportingProperties.class)
public class ReportingModuleAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public JsonReportWriter jsonReportWriter() {
        return new JsonReportWriter();
    }

    @Bean
    @ConditionalOnMissingBean
    public CsvReportWriter csvReportWriter() {
        return new CsvReportWriter();
    }

    @Bean
    @ConditionalOnMissingBean
    public ExcelReportWriter excelReportWriter() {
        return new ExcelReportWriter();
    }

    @Bean
    @ConditionalOnMissingBean
    public ReportDefinitionRegistry reportDefinitionRegistry() {
        ReportDefinitionRegistry registry = new ReportDefinitionRegistry();
        // Sample report - replace/extend with your own definitions from the host app.
        registry.register(new ReportDefinition(
                "sales-summary",
                "SELECT region AS region, product AS product, SUM(amount) AS total_amount "
                        + "FROM report_record GROUP BY region, product ORDER BY region, product",
                Map.of()));
        return registry;
    }

    @Bean
    @ConditionalOnMissingBean
    public ReportingService reportingService(ReportDataService reportDataService,
                                             ReportDefinitionRegistry reportDefinitionRegistry,
                                             List<ReportWriter> reportWriters) {
        return new ReportingService(reportDataService, reportDefinitionRegistry, reportWriters);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnWebApplication
    @ConditionalOnProperty(prefix = "reporting", name = "expose-rest-endpoint", havingValue = "true", matchIfMissing = true)
    public ReportController reportController(ReportingService reportingService) {
        return new ReportController(reportingService);
    }
}
