package com.lisa.daosdk.config;

import com.lisa.daosdk.service.ReportDataService;
import jakarta.persistence.EntityManager;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Auto-configuration for the dao-sdk module.
 * <p>
 * Registered in {@code META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports}
 * so that simply placing this JAR on the host application's classpath makes the
 * entities, repositories and {@link ReportDataService} available — no manual
 * {@code @ComponentScan}, {@code @EntityScan} or {@code @EnableJpaRepositories}
 * needed in the host.
 */
@AutoConfiguration
@EntityScan(basePackages = "com.lisa.daosdk.entity")
@EnableJpaRepositories(basePackages = "com.lisa.daosdk.repository")
public class DaoSdkAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public ReportDataService reportDataService(EntityManager entityManager) {
        return new ReportDataService(entityManager);
    }
}
