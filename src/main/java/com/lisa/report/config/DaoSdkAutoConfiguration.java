package com.lisa.report.config;

import com.lisa.report.service.ReportDataService;
import jakarta.persistence.EntityManagerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.context.annotation.Bean;

/**
 * Auto-configuration for the dao-sdk module.
 * <p>
 * Registered in {@code META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports}
 * so that simply placing this JAR on the host application's classpath exposes the
 * {@link ReportDataService} — no manual {@code @ComponentScan} needed in the host.
 * <p>
 * The SDK intentionally owns no JPA entities or repositories: the host application
 * owns the domain entities (e.g. {@code fsi_custom_search_outbox}). Reports are run
 * as read-only native SQL through the shared persistence context provided by the
 * host's JPA auto-configuration, so the SDK never needs to redeclare host entities
 * (and cannot accidentally break the host's {@code hibernate.ddl-auto=validate}).
 */
@AutoConfiguration(after = HibernateJpaAutoConfiguration.class)
public class DaoSdkAutoConfiguration {

    @Bean
    @ConditionalOnBean(EntityManagerFactory.class)
    @ConditionalOnMissingBean
    public ReportDataService reportDataService() {
        return new ReportDataService();
    }
}
