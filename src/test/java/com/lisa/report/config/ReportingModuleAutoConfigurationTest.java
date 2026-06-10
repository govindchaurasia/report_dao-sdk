package com.lisa.report.config;

import com.lisa.report.GenerateAndDeliverService;
import com.lisa.report.GenerateConnectedCarAlertReportService;
import com.lisa.report.GenerateReportInstanceFactory;
import com.lisa.report.ReportRequestFactory;
import com.lisa.report.delivery.EmailReportDeliverySender;
import com.lisa.report.delivery.PasswordZipService;
import com.lisa.report.delivery.ReportDeliveryService;
import com.lisa.report.delivery.SftpReportDeliverySender;
import com.lisa.report.format.CsvReportWriter;
import com.lisa.report.format.ExcelReportWriter;
import com.lisa.report.format.JsonReportWriter;
import com.lisa.report.service.ReportDataService;
import com.lisa.report.service.ReportDefinitionRegistry;
import com.lisa.report.service.ReportingService;
import com.lisa.report.web.ReportSdkController;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class ReportingModuleAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withBean(EntityManagerFactory.class, () -> mock(EntityManagerFactory.class))
            .withBean(ReportDataService.class, () -> mock(ReportDataService.class))
            .withConfiguration(AutoConfigurations.of(ReportingModuleAutoConfiguration.class));

    private final WebApplicationContextRunner webRunner = new WebApplicationContextRunner()
            .withBean(EntityManagerFactory.class, () -> mock(EntityManagerFactory.class))
            .withBean(ReportDataService.class, () -> mock(ReportDataService.class))
            .withConfiguration(AutoConfigurations.of(ReportingModuleAutoConfiguration.class));

    @Test
    void registersTheCoreReportingBeans() {
        runner.run(context -> {
            assertThat(context).hasSingleBean(JsonReportWriter.class);
            assertThat(context).hasSingleBean(CsvReportWriter.class);
            assertThat(context).hasSingleBean(ExcelReportWriter.class);
            assertThat(context).hasSingleBean(ReportDefinitionRegistry.class);
            assertThat(context).hasSingleBean(ReportingService.class);
            assertThat(context).hasSingleBean(ReportRequestFactory.class);
            assertThat(context).hasSingleBean(GenerateReportInstanceFactory.class);
            assertThat(context).hasSingleBean(GenerateConnectedCarAlertReportService.class);
            assertThat(context).hasSingleBean(GenerateAndDeliverService.class);
        });
    }

    @Test
    void registersTheDeliveryBeans() {
        runner.run(context -> {
            assertThat(context).hasSingleBean(PasswordZipService.class);
            assertThat(context).hasSingleBean(EmailReportDeliverySender.class);
            assertThat(context).hasSingleBean(SftpReportDeliverySender.class);
            assertThat(context).hasSingleBean(ReportDeliveryService.class);
        });
    }

    @Test
    void seedsTheBuiltInReportDefinitions() {
        runner.run(context -> {
            ReportDefinitionRegistry registry = context.getBean(ReportDefinitionRegistry.class);
            assertThat(registry.names()).contains(
                    "outbox-status-summary",
                    "ai-engagement-by-store",
                    "planned-service-breakdown",
                    "appointment-funnel",
                    "repair-orders",
                    GenerateConnectedCarAlertReportService.REPORT_NAME,
                    "store-engagement-funnel");
        });
    }

    @Test
    void controllerIsNotRegisteredInANonWebContext() {
        runner.run(context -> assertThat(context).doesNotHaveBean(ReportSdkController.class));
    }

    @Test
    void controllerIsRegisteredInAWebContextByDefault() {
        webRunner.run(context -> assertThat(context).hasSingleBean(ReportSdkController.class));
    }

    @Test
    void controllerIsSuppressedWhenRestEndpointIsDisabled() {
        webRunner.withPropertyValues("reporting.expose-rest-endpoint=false")
                .run(context -> assertThat(context).doesNotHaveBean(ReportSdkController.class));
    }

    @Test
    void hostBeansOverrideTheLibraryDefaults() {
        JsonReportWriter hostWriter = new JsonReportWriter();
        runner.withBean("hostJsonWriter", JsonReportWriter.class, () -> hostWriter)
                .run(context -> {
                    assertThat(context).hasSingleBean(JsonReportWriter.class);
                    assertThat(context.getBean(JsonReportWriter.class)).isSameAs(hostWriter);
                });
    }
}
