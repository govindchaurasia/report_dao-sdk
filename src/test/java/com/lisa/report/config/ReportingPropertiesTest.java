package com.lisa.report.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class ReportingPropertiesTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(PropertiesConfig.class);

    @EnableConfigurationProperties(ReportingProperties.class)
    static class PropertiesConfig {
    }

    @Test
    void defaultsAreApplied() {
        runner.run(context -> {
            ReportingProperties props = context.getBean(ReportingProperties.class);
            assertThat(props.getOutputDirectory()).isEqualTo("reports");
            assertThat(props.isExposeRestEndpoint()).isTrue();
            assertThat(props.getBasePath()).isEqualTo("/api/reports");
        });
    }

    @Test
    void bindsOverridesFromTheReportingNamespace() {
        runner.withPropertyValues(
                "reporting.output-directory=/var/out",
                "reporting.expose-rest-endpoint=false",
                "reporting.base-path=/custom/reports"
        ).run(context -> {
            ReportingProperties props = context.getBean(ReportingProperties.class);
            assertThat(props.getOutputDirectory()).isEqualTo("/var/out");
            assertThat(props.isExposeRestEndpoint()).isFalse();
            assertThat(props.getBasePath()).isEqualTo("/custom/reports");
        });
    }
}
