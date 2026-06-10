package com.lisa.report.model;

import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

class ReportConfigTest {

    @Test
    void lombokBuilderPopulatesFields() {
        Date start = new Date(1_000L);
        Date end = new Date(2_000L);

        ReportConfig config = ReportConfig.builder()
                .id(1L)
                .storeId(5L)
                .enterpriseId(9L)
                .serviceInteractionId(11L)
                .serviceInteractionName("Connected Car Alert")
                .reportType("CONNECTED_CAR_ALERT")
                .reportFrequency("DAILY")
                .reportFormat("EXCEL")
                .reportLevel("STORE")
                .consolidated(true)
                .startDate(start)
                .endDate(end)
                .sendReportType("EMAIL")
                .hostname("smtp.example.com")
                .port(587)
                .toEmailAddress("a@example.com")
                .build();

        assertThat(config.getId()).isEqualTo(1L);
        assertThat(config.getStoreId()).isEqualTo(5L);
        assertThat(config.getEnterpriseId()).isEqualTo(9L);
        assertThat(config.getServiceInteractionId()).isEqualTo(11L);
        assertThat(config.getServiceInteractionName()).isEqualTo("Connected Car Alert");
        assertThat(config.getReportType()).isEqualTo("CONNECTED_CAR_ALERT");
        assertThat(config.getReportFrequency()).isEqualTo("DAILY");
        assertThat(config.getReportFormat()).isEqualTo("EXCEL");
        assertThat(config.getReportLevel()).isEqualTo("STORE");
        assertThat(config.isConsolidated()).isTrue();
        assertThat(config.getStartDate()).isEqualTo(start);
        assertThat(config.getEndDate()).isEqualTo(end);
        assertThat(config.getSendReportType()).isEqualTo("EMAIL");
        assertThat(config.getHostname()).isEqualTo("smtp.example.com");
        assertThat(config.getPort()).isEqualTo(587);
        assertThat(config.getToEmailAddress()).isEqualTo("a@example.com");
    }

    @Test
    void primitiveBooleansDefaultToFalse() {
        ReportConfig config = ReportConfig.builder().build();

        assertThat(config.isConsolidated()).isFalse();
        assertThat(config.isAllInteractions()).isFalse();
        assertThat(config.getStrictHostKeyChecking()).isNull();
    }
}
