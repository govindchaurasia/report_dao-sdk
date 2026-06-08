package com.lisa.report.model;

import lombok.Builder;
import lombok.Getter;

import java.util.Date;

/**
 * Library-owned, immutable report configuration — the single object the host passes
 * to {@code SdkGenerateAndDeliverService.generateAndDeliver(SdkReportConfig)}.
 * <p>
 * It mirrors the fields of the host's {@code ReportConfigMasterDto} that the library
 * framework needs (what to generate, the date-window frequency, and where to deliver),
 * but is library-owned so the JAR never depends on host DTO/VO types. The host maps
 * its own config row onto this builder; persistence and Quartz scheduling stay host-side.
 * <p>
 * {@code reportType}, {@code reportFrequency} and {@code reportFormat} are carried as
 * strings and parsed leniently by the framework into the library's own enums.
 */
@Getter
@Builder
public class SdkReportConfig {

    // --- Identity / targeting ---
    private final Long id;
    private final Long storeId;
    private final String storeName;
    private final Long enterpriseId;
    private final Long serviceInteractionId;
    private final String serviceInteractionName;

    // --- What / when ---
    /** Maps to {@link com.lisa.report.ReportType}. */
    private final String reportType;
    /** Maps to {@link com.lisa.report.ReportFrequency}. */
    private final String reportFrequency;
    /** Maps to {@link ReportFormat}; defaults to JSON when blank. */
    private final String reportFormat;
    /** Report scope, e.g. {@code ENTERPRISE} or {@code STORE}. */
    private final String reportLevel;
    private final Integer appointmentDaysRange;
    private final boolean allInteractions;
    private final boolean consolidated;
    private final String timeZone;
    private final String reportFileName;
    /** Used only for the {@code CUSTOM} frequency. */
    private final Date startDate;
    /** Used only for the {@code CUSTOM} frequency. */
    private final Date endDate;

    // --- Delivery (connection block reused per method) ---
    /** Maps to {@link com.lisa.report.delivery.DeliveryMethod}, e.g. {@code EMAIL}/{@code SFTP}. */
    private final String sendReportType;
    private final String hostname;
    private final Integer port;
    private final String username;
    private final String password;
    private final String zipFilePassword;

    // Email
    private final String toEmailAddress;
    private final String fromAddress;
    private final String senderFirstName;
    private final String senderLastName;
    private final String emailSubject;
    private final String emailBody;

    // SFTP
    private final String sftpFilePath;
    private final String knownHostsPath;
    /** Optional override of the secure-by-default SFTP host-key check. Null ⇒ keep the default (true). */
    private final Boolean strictHostKeyChecking;
}
