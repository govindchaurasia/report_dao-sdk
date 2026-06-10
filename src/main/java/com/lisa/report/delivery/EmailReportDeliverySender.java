package com.lisa.report.delivery;

import com.lisa.report.ReportDeliveryException;
import jakarta.mail.NoSuchProviderException;
import jakarta.mail.Provider;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.util.StringUtils;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Properties;

/**
 * Delivers a report as an email attachment over SMTP.
 * <p>
 * Self-contained: it builds a {@link JavaMailSenderImpl} from the delivery config
 * ({@code host}/{@code port}/{@code username}/{@code password}) per send, so it does
 * not depend on the host configuring {@code spring.mail.*}.
 */
public class EmailReportDeliverySender implements ReportDeliverySender {

    @Override
    public DeliveryMethod getSupportedMethod() {
        return DeliveryMethod.EMAIL;
    }

    @Override
    public void send(byte[] content, String fileName, String contentType, ReportDeliveryConfig config) {
        if (!StringUtils.hasText(config.getHost())) {
            throw new ReportDeliveryException("Email delivery requires an SMTP host");
        }
        if (!StringUtils.hasText(config.getToAddresses())) {
            throw new ReportDeliveryException("Email delivery requires at least one recipient (toAddresses)");
        }

        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(config.getHost());
        if (config.getPort() != null) {
            mailSender.setPort(config.getPort());
        }
        mailSender.setUsername(config.getUsername());
        mailSender.setPassword(config.getPassword());
        mailSender.setDefaultEncoding(StandardCharsets.UTF_8.name());

        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        if (StringUtils.hasText(config.getUsername())) {
            props.put("mail.smtp.auth", "true");
        }
        props.put("mail.smtp.starttls.enable", String.valueOf(config.isStartTls()));
        for (Map.Entry<String, String> entry : config.getMailProperties().entrySet()) {
            props.put(entry.getKey(), entry.getValue());
        }

        try {
            ensureSmtpProvider(mailSender.getSession());

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());

            String from = StringUtils.hasText(config.getFromAddress())
                    ? config.getFromAddress()
                    : config.getUsername();
            if (!StringUtils.hasText(from)) {
                throw new ReportDeliveryException("Email delivery requires a fromAddress (or a username to send as)");
            }
            if (StringUtils.hasText(config.getSenderName())) {
                helper.setFrom(from, config.getSenderName());
            } else {
                helper.setFrom(from);
            }

            helper.setTo(splitAddresses(config.getToAddresses()));
            helper.setSubject(StringUtils.hasText(config.getSubject()) ? config.getSubject() : fileName);
            helper.setText(config.getBody() != null ? config.getBody() : "", false);
            helper.addAttachment(fileName, new ByteArrayResource(content), contentType);

            mailSender.send(message);
        } catch (ReportDeliveryException ex) {
            throw ex;
        } catch (UnsupportedEncodingException ex) {
            throw new ReportDeliveryException("Failed to set email sender name", ex);
        } catch (Exception ex) {
            throw new ReportDeliveryException("Failed to send report email to: " + config.getToAddresses(), ex);
        }
    }

    /**
     * Ensure the {@code smtp}/{@code smtps} transport providers are registered on the session.
     * <p>
     * JavaMail discovers transport providers from the {@code META-INF/javamail.providers}
     * resource shipped inside the mail implementation jar (Eclipse Angus Mail). When the host
     * application repackages itself into a shaded/uber jar, that resource is frequently dropped
     * or overwritten, even though the Angus implementation classes remain on the classpath. The
     * result is {@code NoSuchProviderException: smtp} at send time. Registering the providers
     * explicitly here makes delivery resilient to how the host packages itself.
     */
    private static void ensureSmtpProvider(Session session) {
        if (hasProvider(session, "smtp")) {
            return;
        }
        try {
            // Confirm the Angus implementation classes are actually present before registering.
            Class.forName("org.eclipse.angus.mail.smtp.SMTPTransport");
            session.addProvider(new Provider(Provider.Type.TRANSPORT, "smtp",
                    "org.eclipse.angus.mail.smtp.SMTPTransport", "Eclipse Angus", null));
            session.addProvider(new Provider(Provider.Type.TRANSPORT, "smtps",
                    "org.eclipse.angus.mail.smtp.SMTPSSLTransport", "Eclipse Angus", null));
        } catch (Throwable ignored) {
            // Angus is not on the classpath (or its layout changed); nothing more we can do here.
            // The original NoSuchProviderException will surface from send() with a clear message.
        }
    }

    private static boolean hasProvider(Session session, String protocol) {
        try {
            session.getProvider(protocol);
            return true;
        } catch (NoSuchProviderException ex) {
            return false;
        }
    }

    private static String[] splitAddresses(String addresses) {
        String[] parts = addresses.split("[,;]");
        return java.util.Arrays.stream(parts)
                .map(String::trim)
                .filter(StringUtils::hasText)
                .toArray(String[]::new);
    }
}
