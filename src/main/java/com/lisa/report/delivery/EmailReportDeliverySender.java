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

    /** Candidate Jakarta Mail 2.x SMTP transport implementations, in preference order:
     *  {@code {smtpClass, smtpsClass}}. Eclipse Angus is the reference impl since Jakarta Mail
     *  2.0.2; the legacy {@code com.sun.mail} classes cover hosts still on that implementation. */
    private static final String[][] SMTP_IMPL_CANDIDATES = {
            {"org.eclipse.angus.mail.smtp.SMTPTransport", "org.eclipse.angus.mail.smtp.SMTPSSLTransport"},
            {"com.sun.mail.smtp.SMTPTransport", "com.sun.mail.smtp.SMTPSSLTransport"},
    };

    /**
     * Ensure the {@code smtp}/{@code smtps} transport providers are registered on the session.
     * <p>
     * JavaMail discovers transport providers from the {@code META-INF/javamail.providers}
     * resource shipped inside the mail implementation jar (Eclipse Angus Mail). When the host
     * application repackages itself into a shaded/uber jar, that resource is frequently dropped
     * or overwritten, even though the implementation classes remain on the classpath. The result
     * is {@code NoSuchProviderException: smtp} at send time. Registering the providers explicitly
     * here makes delivery resilient to how the host packages itself.
     * <p>
     * If discovery already works (the provider is present), this is a no-op. If no mail
     * implementation can be loaded at all, a clear {@link ReportDeliveryException} is thrown with
     * remediation guidance instead of letting a cryptic {@code NoSuchProviderException} surface.
     */
    private static void ensureSmtpProvider(Session session) {
        if (hasProvider(session, "smtp")) {
            return;
        }
        for (String[] impl : SMTP_IMPL_CANDIDATES) {
            try {
                // Confirm the implementation classes are actually present before registering.
                Class.forName(impl[0]);
                session.addProvider(new Provider(Provider.Type.TRANSPORT, "smtp", impl[0], "Jakarta Mail", null));
                session.addProvider(new Provider(Provider.Type.TRANSPORT, "smtps", impl[1], "Jakarta Mail", null));
                return;
            } catch (Throwable ignored) {
                // This implementation is not on the classpath; try the next candidate.
            }
        }
        throw new ReportDeliveryException(
                "No Jakarta Mail SMTP implementation is available on the host classpath: the 'jakarta.mail' "
                + "API is present, but neither Eclipse Angus Mail (org.eclipse.angus:jakarta.mail) nor the "
                + "legacy com.sun.mail implementation could be loaded, so the 'smtp' transport cannot be "
                + "created (NoSuchProviderException: smtp). Add 'org.eclipse.angus:jakarta.mail' to the host "
                + "runtime classpath (report_dao-sdk already declares it transitively via "
                + "spring-boot-starter-mail, so this usually means the host excludes it or the dependency was "
                + "not refreshed). If the host builds a shaded/uber JAR, also merge META-INF service resources "
                + "(e.g. the Maven Shade plugin's ServicesResourceTransformer) so the mail provider "
                + "registration is preserved.");
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
