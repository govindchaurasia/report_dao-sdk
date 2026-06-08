package com.lisa.report.delivery;

/**
 * Strategy contract for a single delivery channel (email, SFTP, ...).
 * <p>
 * Implementations are injected into {@link ReportDeliveryService} and indexed by
 * their {@link DeliveryMethod}, so adding a new channel is just adding a new bean.
 */
public interface ReportDeliverySender {

    /** The delivery method this sender handles; used for dispatch. */
    DeliveryMethod getSupportedMethod();

    /**
     * Send the (already zipped, if requested) report payload.
     *
     * @param content     the bytes to deliver
     * @param fileName    the file name to present (attachment name / remote file name)
     * @param contentType the MIME type of {@code content}
     * @param config      the delivery configuration
     */
    void send(byte[] content, String fileName, String contentType, ReportDeliveryConfig config);
}
