package com.lisa.report.config;

import com.lisa.report.config.DaoSdkAutoConfiguration;
import com.lisa.report.service.ReportDataService;
import com.lisa.report.format.CsvReportWriter;
import com.lisa.report.format.ExcelReportWriter;
import com.lisa.report.format.JsonReportWriter;
import com.lisa.report.format.ReportWriter;
import com.lisa.report.model.ReportDefinition;
import com.lisa.report.GenerateAndDeliverService;
import com.lisa.report.GenerateConnectedCarAlertReportService;
import com.lisa.report.GenerateReportInstanceFactory;
import com.lisa.report.GenerateReportService;
import com.lisa.report.ReportRequestFactory;
import com.lisa.report.delivery.EmailReportDeliverySender;
import com.lisa.report.delivery.PasswordZipService;
import com.lisa.report.delivery.ReportDeliverySender;
import com.lisa.report.delivery.ReportDeliveryService;
import com.lisa.report.delivery.SftpReportDeliverySender;
import com.lisa.report.service.ReportDefinitionRegistry;
import com.lisa.report.service.ReportingService;
import com.lisa.report.web.ReportSdkController;
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

        // 6. Connected Car Alert report — daily per-store engagement funnel for
        // connected-car alert interactions (planned_service_type LIKE '%ALERT%'),
        // over jag_future_service_interaction joined to jag_store_master (store /
        // ASD attributes) and svc_messages_instance (central / dealer e-mail
        // handoff counts). Windowed by actual_i_date; optionally scoped to one
        // enterprise and/or store (0 = all). Params map to GenerateReportRequestDto:
        // enterpriseId / storeIdFK / startDate / endDate.
        registry.register(new ReportDefinition(
                GenerateConnectedCarAlertReportService.REPORT_NAME,
                "SELECT ROW_NUMBER() OVER (ORDER BY DATE(fsi.actual_i_date), sm.store_id) AS sno, "
                        + "DATE(fsi.actual_i_date) AS reportDate, "
                        + "SUBSTRING_INDEX(sm.store_id, '~', -1) AS storeCode, "
                        + "sm.store_name AS storeName, "
                        + "fsi.planned_service_type AS interaction, "
                        + "sm.boc_id AS asdMarket, "
                        + "sm.area_service_director AS asd, "
                        + "COUNT(DISTINCT CASE WHEN fsi.appt_phase IN ('CONTACTED','RESPONDED','ENGAGED','APPT_BOOKING') THEN fsi.id END) AS contacted, "
                        + "COUNT(DISTINCT CASE WHEN fsi.appt_phase IN ('RESPONDED','ENGAGED','APPT_BOOKING') THEN fsi.id END) AS responded, "
                        + "COUNT(DISTINCT CASE WHEN fsi.appt_phase IN ('ENGAGED','APPT_BOOKING') THEN fsi.id END) AS engaged, "
                        + "COUNT(DISTINCT CASE WHEN svc.notificationType = 'INTERNAL_NOTIFICATION' AND svc.messageType = 'EMAIL' THEN fsi.id END) AS centralHandoff, "
                        + "COUNT(DISTINCT CASE WHEN svc.notificationType = 'INTERNAL_NOTIFICATION_GM' AND svc.messageType = 'EMAIL' THEN fsi.id END) AS dealerHandoff, "
                        + "COUNT(DISTINCT CASE WHEN fsi.appt_phase = 'APPT_BOOKING' THEN fsi.id END) AS appointments "
                        + "FROM jag_future_service_interaction fsi "
                        + "INNER JOIN jag_store_master sm ON sm.id = fsi.store_id_fk "
                        + "LEFT JOIN svc_messages_instance svc "
                        + "ON svc.future_service_interaction_id_fk = fsi.id "
                        + "AND svc.messageType = 'EMAIL' "
                        + "AND svc.notificationType IN ('INTERNAL_NOTIFICATION','INTERNAL_NOTIFICATION_GM') "
                        + "WHERE (:enterpriseId = 0 OR sm.enterprise_id_fk = :enterpriseId) "
                        + "AND fsi.planned_service_type LIKE '%ALERT%' "
                        + "AND fsi.actual_i_date BETWEEN :startDate AND :endDate "
                        + "AND (:storeId = 0 OR fsi.store_id_fk = :storeId) "
                        + "GROUP BY DATE(fsi.actual_i_date), sm.store_id, sm.store_name, sm.boc_id, sm.area_service_director, fsi.planned_service_type "
                        + "ORDER BY DATE(fsi.actual_i_date), sm.store_id",
                Map.of("enterpriseId", "0", "storeId", "0", "startDate", "1970-01-01", "endDate", "2999-12-31 23:59:59")));

        // 7. Daily store engagement funnel, joined to jag_store_master for the
        // store name and ASD attributes. Column aliases match the requested Excel
        // layout. Grouped by day so each row is one date / store / service-type
        // (matching the sample report). Override the window via
        // ?startDate=2026-03-01&endDate=2026-03-31
        registry.register(new ReportDefinition(
                "store-engagement-funnel",
                "SELECT DATE_FORMAT(fsi.actual_i_date, '%d-%m-%Y') AS `Date`, "
                        + "jsm.store_name AS `Store Name`, "
                        + "fsi.planned_service_type AS `Interaction`, "
                        + "jsm.boc_id AS `ASD Market`, "
                        + "jsm.area_service_director AS `ASD`, "
                        + "SUM(CASE WHEN fsi.appt_phase IN ('CONTACTED','RESPONDED','ENGAGED','APPT_BOOKING') THEN 1 ELSE 0 END) AS `Contacted`, "
                        + "SUM(CASE WHEN fsi.appt_phase IN ('RESPONDED','ENGAGED','APPT_BOOKING') THEN 1 ELSE 0 END) AS `Responded`, "
                        + "SUM(CASE WHEN fsi.appt_phase IN ('ENGAGED','APPT_BOOKING') THEN 1 ELSE 0 END) AS `Engaged`, "
                        + "SUM(CASE WHEN fsi.appt_phase = 'APPT_BOOKING' THEN 1 ELSE 0 END) AS `Appointments` "
                        + "FROM fsi_custom_search_outbox fsi "
                        + "JOIN jag_store_master jsm ON fsi.store_id_fk = jsm.id "
                        + "WHERE fsi.actual_i_date BETWEEN :startDate AND :endDate "
                        + "GROUP BY DATE(fsi.actual_i_date), jsm.store_name, fsi.planned_service_type, jsm.boc_id, jsm.area_service_director "
                        + "ORDER BY DATE(fsi.actual_i_date), jsm.store_name",
                Map.of("startDate", "1970-01-01", "endDate", "2999-12-31 23:59:59")));

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
    public ReportSdkController reportSdkController(ReportingService reportingService) {
        return new ReportSdkController(reportingService);
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

    @Bean
    @ConditionalOnMissingBean
    public ReportRequestFactory reportRequestFactory() {
        return new ReportRequestFactory();
    }

    // --- Delivery (email / SFTP / password-zip) ---

    @Bean
    @ConditionalOnMissingBean
    public PasswordZipService passwordZipService() {
        return new PasswordZipService();
    }

    @Bean
    @ConditionalOnMissingBean
    public EmailReportDeliverySender emailReportDeliverySender() {
        return new EmailReportDeliverySender();
    }

    @Bean
    @ConditionalOnMissingBean
    public SftpReportDeliverySender sftpReportDeliverySender() {
        return new SftpReportDeliverySender();
    }

    @Bean
    @ConditionalOnMissingBean
    public ReportDeliveryService reportDeliveryService(List<ReportDeliverySender> deliverySenders,
                                                       PasswordZipService passwordZipService) {
        return new ReportDeliveryService(deliverySenders, passwordZipService);
    }

    @Bean
    @ConditionalOnMissingBean
    public GenerateAndDeliverService generateAndDeliverService(GenerateReportInstanceFactory generateReportInstanceFactory,
                                                                     ReportDeliveryService reportDeliveryService,
                                                                     ReportRequestFactory reportRequestFactory) {
        return new GenerateAndDeliverService(generateReportInstanceFactory, reportDeliveryService, reportRequestFactory);
    }
}
