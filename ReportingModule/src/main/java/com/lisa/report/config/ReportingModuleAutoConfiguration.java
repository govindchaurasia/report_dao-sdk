package com.lisa.report.config;

import com.lisa.daosdk.config.DaoSdkAutoConfiguration;
import com.lisa.daosdk.service.ReportDataService;
import com.lisa.report.format.CsvReportWriter;
import com.lisa.report.format.ExcelReportWriter;
import com.lisa.report.format.JsonReportWriter;
import com.lisa.report.format.ReportWriter;
import com.lisa.report.model.ReportDefinition;
import com.lisa.report.GenerateConnectedCarAlertReportService;
import com.lisa.report.GenerateReportInstanceFactory;
import com.lisa.report.GenerateReportService;
import com.lisa.report.service.ReportDefinitionRegistry;
import com.lisa.report.service.ReportingService;
import com.lisa.report.web.ReportController;
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

        // Reports over the host table `fsi_custom_search_outbox`.
        // All run as read-only native SQL (the host owns the JPA entity).
        // The encrypted `vin` column is intentionally excluded — a native query
        // would return ciphertext, not the decrypted value.

        // 1. Record count grouped by pipeline status.
        registry.register(new ReportDefinition(
                "outbox-status-summary",
                "SELECT status AS status, COUNT(*) AS record_count "
                        + "FROM fsi_custom_search_outbox "
                        + "GROUP BY status "
                        + "ORDER BY record_count DESC",
                Map.of()));

        // 2. AI engagement funnel per store.
        registry.register(new ReportDefinition(
                "ai-engagement-by-store",
                "SELECT store_id_fk AS store_id, "
                        + "COUNT(*) AS total_records, "
                        + "SUM(CASE WHEN ai_enabled = 1 THEN 1 ELSE 0 END) AS ai_enabled_count, "
                        + "SUM(CASE WHEN conversation_initiated = 1 THEN 1 ELSE 0 END) AS conversations_initiated, "
                        + "SUM(CASE WHEN responded_to_ai = 1 THEN 1 ELSE 0 END) AS responded_to_ai_count, "
                        + "SUM(CASE WHEN appointment_set IS NOT NULL AND appointment_set <> '' THEN 1 ELSE 0 END) AS appointments_set "
                        + "FROM fsi_custom_search_outbox "
                        + "GROUP BY store_id_fk "
                        + "ORDER BY total_records DESC",
                Map.of()));

        // 3. Planned-service breakdown by customer segment.
        registry.register(new ReportDefinition(
                "planned-service-breakdown",
                "SELECT planned_service_type AS planned_service_type, "
                        + "customer_segment AS customer_segment, "
                        + "COUNT(*) AS record_count "
                        + "FROM fsi_custom_search_outbox "
                        + "GROUP BY planned_service_type, customer_segment "
                        + "ORDER BY record_count DESC",
                Map.of()));

        // 4. Appointment funnel by phase and status.
        registry.register(new ReportDefinition(
                "appointment-funnel",
                "SELECT appt_phase AS appt_phase, "
                        + "appt_status AS appt_status, "
                        + "COUNT(*) AS record_count "
                        + "FROM fsi_custom_search_outbox "
                        + "GROUP BY appt_phase, appt_status "
                        + "ORDER BY record_count DESC",
                Map.of()));

        // 5. Repair-order detail list, filtered by ro_opened_date range.
        // Override startDate/endDate via query params, e.g. ?startDate=2026-01-01&endDate=2026-03-31
        registry.register(new ReportDefinition(
                "repair-orders",
                "SELECT id AS id, "
                        + "store_id_fk AS store_id, "
                        + "customer_id_fk AS customer_id, "
                        + "ro_status_code AS ro_status_code, "
                        + "ro_advisor_no AS ro_advisor_no, "
                        + "ro_opened_date AS ro_opened_date, "
                        + "ro_close_date AS ro_close_date, "
                        + "planned_service_type AS planned_service_type, "
                        + "status AS status "
                        + "FROM fsi_custom_search_outbox "
                        + "WHERE ro_opened_date BETWEEN :startDate AND :endDate "
                        + "ORDER BY ro_opened_date DESC",
                Map.of("startDate", "1970-01-01", "endDate", "2999-12-31 23:59:59")));

        // 6. Connected Car Alert report.
        // Rows that carry a connected-car service alert (serviceAlertId present),
        // windowed by planner_run_date and optionally scoped to a single store.
        // Params map 1:1 to GenerateReportRequestDto: storeIdFK / startDate / endDate.
        // storeId = 0 (default) means "all stores". `vin` excluded (encrypted).
        registry.register(new ReportDefinition(
                GenerateConnectedCarAlertReportService.REPORT_NAME,
                "SELECT id AS id, "
                        + "store_id_fk AS store_id, "
                        + "customer_id_fk AS customer_id, "
                        + "service_vehicle_id_fk AS service_vehicle_id, "
                        + "serviceAlertId AS service_alert_id, "
                        + "brand AS brand, "
                        + "predicted_mileage AS predicted_mileage, "
                        + "planned_service_type AS planned_service_type, "
                        + "planned_date AS planned_date, "
                        + "estimated_due_date AS estimated_due_date, "
                        + "status AS status, "
                        + "appt_status AS appt_status, "
                        + "appt_date AS appt_date, "
                        + "line_type AS line_type, "
                        + "type AS type, "
                        + "predictive_planner_id AS predictive_planner_id, "
                        + "planner_run_date AS planner_run_date "
                        + "FROM fsi_custom_search_outbox "
                        + "WHERE serviceAlertId IS NOT NULL "
                        + "AND (:storeId = 0 OR store_id_fk = :storeId) "
                        + "AND planner_run_date BETWEEN :startDate AND :endDate "
                        + "ORDER BY planner_run_date DESC",
                Map.of("storeId", "0", "startDate", "1970-01-01", "endDate", "2999-12-31 23:59:59")));

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

    @Bean
    @ConditionalOnMissingBean
    public GenerateConnectedCarAlertReportService generateConnectedCarAlertReportService(ReportingService reportingService) {
        return new GenerateConnectedCarAlertReportService(reportingService);
    }

    @Bean
    @ConditionalOnMissingBean
    public GenerateReportInstanceFactory generateReportInstanceFactory(List<GenerateReportService> reportServices) {
        return new GenerateReportInstanceFactory(reportServices);
    }
}
