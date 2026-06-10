package com.lisa.report;

import com.lisa.report.model.ReportConfig;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.Date;

import static java.time.DayOfWeek.MONDAY;
import static java.time.DayOfWeek.SUNDAY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReportRequestFactoryTest {

    private static final ZoneId ZONE = ZoneId.of("UTC");

    private final ReportRequestFactory factory = new ReportRequestFactory();

    private static Date toDate(ZonedDateTime zdt) {
        return Date.from(zdt.toInstant());
    }

    private ReportConfig config(String frequency) {
        return ReportConfig.builder()
                .reportFrequency(frequency)
                .serviceInteractionName("svc")
                .serviceInteractionId(42L)
                .enterpriseId(7L)
                .reportLevel("STORE")
                .consolidated(true)
                .appointmentDaysRange(5)
                .build();
    }

    @Test
    void copiesTargetingFieldsFromConfig() {
        GenerateReportRequestDto request = factory.buildRequest(config("DAILY"), ZONE);

        assertThat(request.getServiceType()).isEqualTo("svc");
        assertThat(request.getStoreIdFK()).isEqualTo(42L);
        assertThat(request.getEnterpriseId()).isEqualTo(7L);
        assertThat(request.getReportLevel()).isEqualTo("STORE");
        assertThat(request.isConsolidated()).isTrue();
        assertThat(request.getAppointmentDaysRange()).isEqualTo(5);
    }

    @Test
    void serviceTypeDefaultsToEmptyWhenInteractionNameMissing() {
        ReportConfig config = ReportConfig.builder().reportFrequency("DAILY").build();

        GenerateReportRequestDto request = factory.buildRequest(config, ZONE);

        assertThat(request.getServiceType()).isEmpty();
    }

    @Test
    void dailyWindowIsTheEntireCurrentDay() {
        LocalDate today = LocalDate.now(ZONE);
        GenerateReportRequestDto request = factory.buildRequest(config("DAILY"), ZONE);

        assertThat(request.getStartDate()).isEqualTo(toDate(today.atStartOfDay(ZONE)));
        assertThat(request.getEndDate()).isEqualTo(toDate(today.atStartOfDay(ZONE).plusDays(1).minusSeconds(1)));
    }

    @Test
    void weeklyWindowIsMondayThroughSunday() {
        LocalDate today = LocalDate.now(ZONE);
        GenerateReportRequestDto request = factory.buildRequest(config("WEEKLY"), ZONE);

        assertThat(request.getStartDate())
                .isEqualTo(toDate(today.with(TemporalAdjusters.previousOrSame(MONDAY)).atStartOfDay(ZONE)));
        assertThat(request.getEndDate())
                .isEqualTo(toDate(today.with(TemporalAdjusters.nextOrSame(SUNDAY)).atStartOfDay(ZONE).plusDays(1).minusSeconds(1)));
    }

    @Test
    void monthlyWindowIsThePreviousCalendarMonth() {
        LocalDate today = LocalDate.now(ZONE);
        GenerateReportRequestDto request = factory.buildRequest(config("MONTHLY"), ZONE);

        assertThat(request.getStartDate())
                .isEqualTo(toDate(today.withDayOfMonth(1).minusMonths(1).atStartOfDay(ZONE)));
        assertThat(request.getEndDate())
                .isEqualTo(toDate(today.withDayOfMonth(1).atStartOfDay(ZONE).minusSeconds(1)));
    }

    @Test
    void quarterlyWindowIsRolling91Days() {
        LocalDate today = LocalDate.now(ZONE);
        GenerateReportRequestDto request = factory.buildRequest(config("QUARTERLY"), ZONE);

        assertThat(request.getStartDate())
                .isEqualTo(toDate(today.atStartOfDay(ZONE).minusDays(91)));
        assertThat(request.getEndDate())
                .isEqualTo(toDate(today.atStartOfDay(ZONE).plusDays(1).minusSeconds(1)));
    }

    @Test
    void yearlyWindowIsThePreviousCalendarYear() {
        LocalDate today = LocalDate.now(ZONE);
        GenerateReportRequestDto request = factory.buildRequest(config("YEARLY"), ZONE);

        assertThat(request.getStartDate())
                .isEqualTo(toDate(today.minusYears(1).with(TemporalAdjusters.firstDayOfYear()).atStartOfDay(ZONE)));
        assertThat(request.getEndDate())
                .isEqualTo(toDate(today.minusYears(1).with(TemporalAdjusters.lastDayOfYear()).atStartOfDay(ZONE).plusDays(1).minusSeconds(1)));
    }

    @Test
    void currentMonthlyWindowSpansThisMonth() {
        LocalDate today = LocalDate.now(ZONE);
        GenerateReportRequestDto request = factory.buildRequest(config("CURRENT_MONTHLY"), ZONE);

        assertThat(request.getStartDate())
                .isEqualTo(toDate(today.withDayOfMonth(1).atStartOfDay(ZONE)));
        assertThat(request.getEndDate())
                .isEqualTo(toDate(today.with(TemporalAdjusters.lastDayOfMonth()).atStartOfDay(ZONE).plusDays(1).minusSeconds(1)));
    }

    @Test
    void currentYearlyWindowSpansThisYear() {
        LocalDate today = LocalDate.now(ZONE);
        GenerateReportRequestDto request = factory.buildRequest(config("CURRENT_YEARLY"), ZONE);

        assertThat(request.getStartDate())
                .isEqualTo(toDate(today.with(TemporalAdjusters.firstDayOfYear()).atStartOfDay(ZONE)));
        assertThat(request.getEndDate())
                .isEqualTo(toDate(today.with(TemporalAdjusters.lastDayOfYear()).atStartOfDay(ZONE).plusDays(1).minusSeconds(1)));
    }

    @Test
    void customWindowPassesThroughExplicitDates() {
        Date start = new Date(1_000L);
        Date end = new Date(2_000L);
        ReportConfig config = ReportConfig.builder()
                .reportFrequency("CUSTOM")
                .startDate(start)
                .endDate(end)
                .build();

        GenerateReportRequestDto request = factory.buildRequest(config, ZONE);

        assertThat(request.getStartDate()).isEqualTo(start);
        assertThat(request.getEndDate()).isEqualTo(end);
    }

    @Test
    void nullConfigThrows() {
        assertThatThrownBy(() -> factory.buildRequest(null))
                .isInstanceOf(ReportGenerationException.class)
                .hasMessageContaining("Report config is required");
    }

    @Test
    void nullZoneFallsBackToSystemDefault() {
        GenerateReportRequestDto request = factory.buildRequest(config("DAILY"), null);

        assertThat(request.getStartDate()).isNotNull();
        assertThat(request.getEndDate()).isNotNull();
    }

    @Test
    void invalidFrequencyThrows() {
        ReportConfig config = ReportConfig.builder().reportFrequency("NOPE").build();

        assertThatThrownBy(() -> factory.buildRequest(config, ZONE))
                .isInstanceOf(ReportGenerationException.class);
    }
}
