package com.lisa.report;

import com.lisa.report.model.SdkReportConfig;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.Date;

import static java.time.DayOfWeek.MONDAY;
import static java.time.DayOfWeek.SUNDAY;

/**
 * Builds a {@link GenerateReportRequestDto} (the data window + targeting params) from
 * a {@link SdkReportConfig}, driven by its {@link ReportFrequency}.
 * <p>
 * This is the library-owned port of the host scheduler's {@code buildRequest(...)}:
 * the date-window math is host-agnostic, so it belongs in the JAR. {@code serviceType}
 * and {@code storeIdFK} mirror the host's mapping from the service interaction (name/id).
 */
public class SdkReportRequestFactory {

    /** Build using the JVM default time zone (matches the host's original behavior). */
    public GenerateReportRequestDto buildRequest(SdkReportConfig config) {
        return buildRequest(config, ZoneId.systemDefault());
    }

    /** Build using an explicit time zone for the date-window boundaries. */
    public GenerateReportRequestDto buildRequest(SdkReportConfig config, ZoneId zoneId) {
        if (config == null) {
            throw new ReportGenerationException("Report config is required to build a request");
        }
        ZoneId zone = zoneId != null ? zoneId : ZoneId.systemDefault();
        ReportFrequency frequency = ReportFrequency.fromString(config.getReportFrequency());

        GenerateReportRequestDto.Builder builder = GenerateReportRequestDto.builder()
                .serviceType(config.getServiceInteractionName() != null ? config.getServiceInteractionName() : "")
                .storeIdFK(config.getServiceInteractionId())
                .enterpriseId(config.getEnterpriseId())
                .reportLevel(config.getReportLevel())
                .consolidated(config.isConsolidated())
                .appointmentDaysRange(config.getAppointmentDaysRange());

        LocalDate today = LocalDate.now(zone);
        switch (frequency) {
            case DAILY:
                // Entire current day.
                return builder
                        .startDate(toDate(today.atStartOfDay(zone)))
                        .endDate(toDate(today.atStartOfDay(zone).plusDays(1).minusSeconds(1)))
                        .build();
            case WEEKLY:
                // Full current week, Monday–Sunday.
                return builder
                        .startDate(toDate(today.with(TemporalAdjusters.previousOrSame(MONDAY)).atStartOfDay(zone)))
                        .endDate(toDate(today.with(TemporalAdjusters.nextOrSame(SUNDAY)).atStartOfDay(zone).plusDays(1).minusSeconds(1)))
                        .build();
            case MONTHLY:
                // Entire previous calendar month.
                return builder
                        .startDate(toDate(today.withDayOfMonth(1).minusMonths(1).atStartOfDay(zone)))
                        .endDate(toDate(today.withDayOfMonth(1).atStartOfDay(zone).minusSeconds(1)))
                        .build();
            case QUARTERLY:
                // Rolling last ~3 months (91 days) through end of today.
                return builder
                        .startDate(toDate(today.atStartOfDay(zone).minusDays(91)))
                        .endDate(toDate(today.atStartOfDay(zone).plusDays(1).minusSeconds(1)))
                        .build();
            case YEARLY:
                // Entire previous calendar year.
                return builder
                        .startDate(toDate(today.minusYears(1).with(TemporalAdjusters.firstDayOfYear()).atStartOfDay(zone)))
                        .endDate(toDate(today.minusYears(1).with(TemporalAdjusters.lastDayOfYear()).atStartOfDay(zone).plusDays(1).minusSeconds(1)))
                        .build();
            case CURRENT_MONTHLY:
                // First through last day of the current month.
                return builder
                        .startDate(toDate(today.withDayOfMonth(1).atStartOfDay(zone)))
                        .endDate(toDate(today.with(TemporalAdjusters.lastDayOfMonth()).atStartOfDay(zone).plusDays(1).minusSeconds(1)))
                        .build();
            case CURRENT_YEARLY:
                // First through last day of the current year.
                return builder
                        .startDate(toDate(today.with(TemporalAdjusters.firstDayOfYear()).atStartOfDay(zone)))
                        .endDate(toDate(today.with(TemporalAdjusters.lastDayOfYear()).atStartOfDay(zone).plusDays(1).minusSeconds(1)))
                        .build();
            case CUSTOM:
                // Explicit window supplied on the config.
                return builder
                        .startDate(config.getStartDate())
                        .endDate(config.getEndDate())
                        .build();
            default:
                throw new ReportGenerationException("Unknown report frequency: " + frequency);
        }
    }

    private static Date toDate(ZonedDateTime zonedDateTime) {
        return Date.from(zonedDateTime.toInstant());
    }
}
