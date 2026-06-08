package com.lisa.report;

import java.util.Date;

/**
 * Request parameters for generating a report.
 * <p>
 * Field names mirror the host's {@code com.lisa.dto.report.GenerateReportRequestDto}
 * so host code can map values across one-to-one. Immutable; build via {@link #builder()}.
 * <p>
 * Kept hand-written (rather than Lombok) specifically to defensively copy the mutable
 * {@link Date} fields on the way in and out, so a built request can never be mutated
 * by a caller holding a reference to the original/returned {@code Date}.
 */
public class GenerateReportRequestDto {

    private final String serviceType;
    private final Long storeIdFK;
    private final Long enterpriseId;
    private final Date startDate;
    private final Date endDate;

    /** Report scope, e.g. {@code ENTERPRISE} or {@code STORE} (mirrors the host's reportLevel). */
    private final String reportLevel;
    /** Whether the report is consolidated across stores. */
    private final boolean consolidated;
    /** Optional appointment look-ahead window in days. */
    private final Integer appointmentDaysRange;

    private GenerateReportRequestDto(Builder builder) {
        this.serviceType = builder.serviceType;
        this.storeIdFK = builder.storeIdFK;
        this.enterpriseId = builder.enterpriseId;
        this.startDate = copy(builder.startDate);
        this.endDate = copy(builder.endDate);
        this.reportLevel = builder.reportLevel;
        this.consolidated = builder.consolidated;
        this.appointmentDaysRange = builder.appointmentDaysRange;
    }

    public String getServiceType() {
        return serviceType;
    }

    public Long getStoreIdFK() {
        return storeIdFK;
    }

    public Long getEnterpriseId() {
        return enterpriseId;
    }

    public Date getStartDate() {
        return copy(startDate);
    }

    public Date getEndDate() {
        return copy(endDate);
    }

    public String getReportLevel() {
        return reportLevel;
    }

    public boolean isConsolidated() {
        return consolidated;
    }

    public Integer getAppointmentDaysRange() {
        return appointmentDaysRange;
    }

    private static Date copy(Date date) {
        return date == null ? null : new Date(date.getTime());
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String serviceType;
        private Long storeIdFK;
        private Long enterpriseId;
        private Date startDate;
        private Date endDate;
        private String reportLevel;
        private boolean consolidated;
        private Integer appointmentDaysRange;

        public Builder serviceType(String serviceType) {
            this.serviceType = serviceType;
            return this;
        }

        public Builder storeIdFK(Long storeIdFK) {
            this.storeIdFK = storeIdFK;
            return this;
        }

        public Builder enterpriseId(Long enterpriseId) {
            this.enterpriseId = enterpriseId;
            return this;
        }

        public Builder startDate(Date startDate) {
            this.startDate = copy(startDate);
            return this;
        }

        public Builder endDate(Date endDate) {
            this.endDate = copy(endDate);
            return this;
        }

        public Builder reportLevel(String reportLevel) {
            this.reportLevel = reportLevel;
            return this;
        }

        public Builder consolidated(boolean consolidated) {
            this.consolidated = consolidated;
            return this;
        }

        public Builder appointmentDaysRange(Integer appointmentDaysRange) {
            this.appointmentDaysRange = appointmentDaysRange;
            return this;
        }

        public GenerateReportRequestDto build() {
            return new GenerateReportRequestDto(this);
        }
    }
}
