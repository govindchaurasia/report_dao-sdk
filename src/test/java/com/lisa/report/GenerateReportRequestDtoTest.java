package com.lisa.report;

import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

class GenerateReportRequestDtoTest {

    @Test
    void builderPopulatesAllFields() {
        Date start = new Date(1_000L);
        Date end = new Date(2_000L);

        GenerateReportRequestDto dto = GenerateReportRequestDto.builder()
                .serviceType("svc")
                .storeIdFK(7L)
                .enterpriseId(3L)
                .startDate(start)
                .endDate(end)
                .reportLevel("STORE")
                .consolidated(true)
                .appointmentDaysRange(14)
                .build();

        assertThat(dto.getServiceType()).isEqualTo("svc");
        assertThat(dto.getStoreIdFK()).isEqualTo(7L);
        assertThat(dto.getEnterpriseId()).isEqualTo(3L);
        assertThat(dto.getStartDate()).isEqualTo(start);
        assertThat(dto.getEndDate()).isEqualTo(end);
        assertThat(dto.getReportLevel()).isEqualTo("STORE");
        assertThat(dto.isConsolidated()).isTrue();
        assertThat(dto.getAppointmentDaysRange()).isEqualTo(14);
    }

    @Test
    void nullDatesStayNull() {
        GenerateReportRequestDto dto = GenerateReportRequestDto.builder().build();

        assertThat(dto.getStartDate()).isNull();
        assertThat(dto.getEndDate()).isNull();
    }

    @Test
    void defensivelyCopiesDatesOnTheWayIn() {
        Date start = new Date(1_000L);
        GenerateReportRequestDto dto = GenerateReportRequestDto.builder().startDate(start).build();

        start.setTime(9_999L);

        assertThat(dto.getStartDate().getTime()).isEqualTo(1_000L);
    }

    @Test
    void defensivelyCopiesDatesOnTheWayOut() {
        GenerateReportRequestDto dto = GenerateReportRequestDto.builder()
                .endDate(new Date(2_000L))
                .build();

        dto.getEndDate().setTime(9_999L);

        assertThat(dto.getEndDate().getTime()).isEqualTo(2_000L);
    }
}
