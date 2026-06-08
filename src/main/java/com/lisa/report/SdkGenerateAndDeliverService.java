package com.lisa.report;

import com.lisa.report.delivery.DeliveryMethod;
import com.lisa.report.delivery.ReportDeliveryConfig;
import com.lisa.report.delivery.ReportDeliveryService;
import com.lisa.report.model.ReportFormat;
import com.lisa.report.model.ReportResult;
import com.lisa.report.model.SdkReportConfig;

/**
 * One-call orchestrator: generate a report and then deliver it (email / SFTP,
 * optionally password-zipped) in a single step.
 * <p>
 * Two entry points:
 * <ul>
 *   <li>{@link #generateAndDeliver(SdkReportConfig)} — the framework path. The host's
 *       Quartz job maps its config row onto a {@link SdkReportConfig}; the library
 *       resolves the type/format, builds the date window from the frequency, dispatches
 *       to the matching {@link SdkGenerateReportService}, and delivers.</li>
 *   <li>{@link #generateAndDeliver(ReportType, GenerateReportRequestDto, ReportFormat, ReportDeliveryConfig)}
 *       — the explicit path when the caller has already built the request and delivery
 *       config itself.</li>
 * </ul>
 */
public class SdkGenerateAndDeliverService {

    private final SdkGenerateReportInstanceFactory instanceFactory;
    private final ReportDeliveryService deliveryService;
    private final SdkReportRequestFactory requestFactory;

    public SdkGenerateAndDeliverService(SdkGenerateReportInstanceFactory instanceFactory,
                                        ReportDeliveryService deliveryService,
                                        SdkReportRequestFactory requestFactory) {
        this.instanceFactory = instanceFactory;
        this.deliveryService = deliveryService;
        this.requestFactory = requestFactory;
    }

    /**
     * Generate and deliver a report from a single host-supplied config object.
     * Resolves the report type and format, builds the date window from the
     * frequency, generates via the matching service, then delivers.
     */
    public ReportResult generateAndDeliver(SdkReportConfig config) {
        if (config == null) {
            throw new ReportGenerationException("Report config is required");
        }
        ReportType type = ReportType.fromString(config.getReportType());
        ReportFormat format = ReportFormat.fromString(config.getReportFormat());
        GenerateReportRequestDto request = requestFactory.buildRequest(config);

        ReportResult result = instanceFactory.getInstanceByType(type).generate(request, format);
        deliveryService.deliver(result, toDeliveryConfig(config, result));
        return result;
    }

    /**
     * Generate the report for {@code type} in {@code format}, deliver it per
     * {@code deliveryConfig}, and return the rendered result (so the caller can
     * also persist or log it if desired).
     */
    public ReportResult generateAndDeliver(ReportType type,
                                           GenerateReportRequestDto request,
                                           ReportFormat format,
                                           ReportDeliveryConfig deliveryConfig) {
        ReportResult result = instanceFactory.getInstanceByType(type).generate(request, format);
        deliveryService.deliver(result, deliveryConfig);
        return result;
    }

    private static ReportDeliveryConfig toDeliveryConfig(SdkReportConfig config, ReportResult result) {
        ReportDeliveryConfig.Builder builder = ReportDeliveryConfig.builder()
                .method(DeliveryMethod.fromString(config.getSendReportType()))
                .fileName(config.getReportFileName() != null ? config.getReportFileName() : result.getFilename())
                .zipPassword(config.getZipFilePassword())
                .host(config.getHostname())
                .port(config.getPort())
                .username(config.getUsername())
                .password(config.getPassword())
                // email
                .toAddresses(config.getToEmailAddress())
                .fromAddress(config.getFromAddress())
                .senderName(buildSenderName(config))
                .subject(config.getEmailSubject())
                .body(config.getEmailBody())
                // sftp
                .remoteDirectory(config.getSftpFilePath())
                .knownHostsPath(config.getKnownHostsPath());

        if (config.getStrictHostKeyChecking() != null) {
            builder.strictHostKeyChecking(config.getStrictHostKeyChecking());
        }
        return builder.build();
    }

    private static String buildSenderName(SdkReportConfig config) {
        String first = config.getSenderFirstName();
        String last = config.getSenderLastName();
        boolean hasFirst = first != null && !first.isBlank();
        boolean hasLast = last != null && !last.isBlank();
        if (hasFirst && hasLast) {
            return first.trim() + " " + last.trim();
        }
        if (hasFirst) {
            return first.trim();
        }
        if (hasLast) {
            return last.trim();
        }
        return null;
    }
}
