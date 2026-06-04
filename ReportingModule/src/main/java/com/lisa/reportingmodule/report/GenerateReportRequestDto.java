package com.lisa.reportingmodule.report;

import java.util.Date;

/**
 * Request parameters for generating a report.
 * <p>
 * Field names mirror the host's {@code com.lisa.dto.report.GenerateReportRequestDto}
 * so host code can map values across one-to-one. Immutable; build via {@link #builder()}.
 */
public class GenerateReportRequestDto {

    private final String serviceType;
    private final Long storeIdFK;
    private final Long enterpriseId;
    private final Date startDate;
    private final Date endDate;

    private GenerateReportRequestDto(Builder builder) {
        this.serviceType = builder.serviceType;
        this.storeIdFK = builder.storeIdFK;
        this.enterpriseId = builder.enterpriseId;
        this.startDate = copy(builder.startDate);
        this.endDate = copy(builder.endDate);
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
            this.startDate = startDate;
            return this;
        }

        public Builder endDate(Date endDate) {
            this.endDate = endDate;
            return this;
        }

        public GenerateReportRequestDto build() {
            return new GenerateReportRequestDto(this);
        }
    }
}
