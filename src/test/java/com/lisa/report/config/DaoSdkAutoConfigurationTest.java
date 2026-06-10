package com.lisa.report.config;

import com.lisa.report.service.ReportDataService;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class DaoSdkAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(DaoSdkAutoConfiguration.class));

    @Test
    void reportDataServiceIsNotCreatedWithoutAnEntityManagerFactory() {
        runner.run(context -> assertThat(context).doesNotHaveBean(ReportDataService.class));
    }

    @Test
    void reportDataServiceIsCreatedWhenAnEntityManagerFactoryIsPresent() {
        runner.withBean(EntityManagerFactory.class, () -> mock(EntityManagerFactory.class))
                .run(context -> assertThat(context).hasSingleBean(ReportDataService.class));
    }

    @Test
    void hostReportDataServiceBeanIsNotOverridden() {
        ReportDataService hostBean = new ReportDataService();
        runner.withBean(EntityManagerFactory.class, () -> mock(EntityManagerFactory.class))
                .withBean("hostReportDataService", ReportDataService.class, () -> hostBean)
                .run(context -> {
                    assertThat(context).hasSingleBean(ReportDataService.class);
                    assertThat(context.getBean(ReportDataService.class)).isSameAs(hostBean);
                });
    }
}
