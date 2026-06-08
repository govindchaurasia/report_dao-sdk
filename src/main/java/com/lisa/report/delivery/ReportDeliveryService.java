package com.lisa.report.delivery;

import com.lisa.report.ReportDeliveryException;
import com.lisa.report.model.ReportResult;
import org.springframework.util.StringUtils;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Dispatches a generated report to the configured delivery channel.
 * <p>
 * Applies password-zipping first (when {@link ReportDeliveryConfig#getZipPassword()}
 * is set), then routes to the {@link ReportDeliverySender} for the configured
 * {@link DeliveryMethod}. Delivery senders are injected and indexed by method.
 */
public class ReportDeliveryService {

    private static final String ZIP_CONTENT_TYPE = "application/zip";

    private final Map<DeliveryMethod, ReportDeliverySender> sendersByMethod;
    private final PasswordZipService passwordZipService;

    public ReportDeliveryService(List<ReportDeliverySender> senders, PasswordZipService passwordZipService) {
        this.sendersByMethod = new EnumMap<>(DeliveryMethod.class);
        for (ReportDeliverySender sender : senders) {
            this.sendersByMethod.put(sender.getSupportedMethod(), sender);
        }
        this.passwordZipService = passwordZipService;
    }

    /**
     * Deliver a rendered report according to {@code config}.
     */
    public void deliver(ReportResult result, ReportDeliveryConfig config) {
        if (result == null) {
            throw new ReportDeliveryException("Cannot deliver a null report result");
        }
        if (config == null || config.getMethod() == null) {
            throw new ReportDeliveryException("Delivery method is required (EMAIL or SFTP)");
        }

        String fileName = StringUtils.hasText(config.getFileName())
                ? config.getFileName()
                : result.getFilename();
        byte[] payload = result.getContent();
        String contentType = result.getContentType();

        if (StringUtils.hasText(config.getZipPassword())) {
            payload = passwordZipService.zip(fileName, payload, config.getZipPassword());
            fileName = fileName + ".zip";
            contentType = ZIP_CONTENT_TYPE;
        }

        ReportDeliverySender sender = sendersByMethod.get(config.getMethod());
        if (sender == null) {
            throw new ReportDeliveryException("No delivery sender registered for method: " + config.getMethod());
        }
        sender.send(payload, fileName, contentType, config);
    }
}
