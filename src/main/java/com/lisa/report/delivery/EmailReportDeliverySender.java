package com.lisa.report.delivery;

import com.lisa.report.ReportDeliveryException;
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

        boolean implicitSsl = useImplicitSsl(config);

        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        if (StringUtils.hasText(config.getUsername())) {
            props.put("mail.smtp.auth", "true");
        }
        if (implicitSsl) {
            // Port 465 (SMTPS): the socket is TLS-wrapped from the first byte, so we must NOT
            // speak plaintext (the server replies with EOF / "bad greeting"). Use implicit SSL
            // and disable STARTTLS, which is mutually exclusive with an already-encrypted socket.
            props.put("mail.smtp.ssl.enable", "true");
            props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
            props.put("mail.smtp.socketFactory.fallback", "false");
            if (config.getPort() != null) {
                props.put("mail.smtp.socketFactory.port", String.valueOf(config.getPort()));
            }
            props.put("mail.smtp.starttls.enable", "false");
        } else {
            props.put("mail.smtp.starttls.enable", String.valueOf(config.isStartTls()));
        }
        for (Map.Entry<String, String> entry : config.getMailProperties().entrySet()) {
            props.put(entry.getKey(), entry.getValue());
        }

        try {
            ensureSmtpProvider(mailSender.getSession(), props);

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

    /** Conventional implicit-SSL SMTP port (SMTPS). */
    private static final int SMTPS_PORT = 465;

    /**
     * Whether to use implicit SSL/TLS (the whole connection is TLS-wrapped, as SMTPS port 465
     * requires). Honors an explicit {@link ReportDeliveryConfig#getSslEnable()} when set;
     * otherwise auto-detects from the port (465 ⇒ implicit SSL).
     */
    private static boolean useImplicitSsl(ReportDeliveryConfig config) {
        if (config.getSslEnable() != null) {
            return config.getSslEnable();
        }
        return config.getPort() != null && config.getPort() == SMTPS_PORT;
    }

    /** Candidate Jakarta Mail 2.x SMTP transport implementations, in preference order:
     *  {@code {smtpClass, smtpsClass}}. Eclipse Angus is the reference impl since Jakarta Mail
     *  2.0.2; the legacy {@code com.sun.mail} classes cover hosts still on that implementation. */
    private static final String[][] SMTP_IMPL_CANDIDATES = {
            {"org.eclipse.angus.mail.smtp.SMTPTransport", "org.eclipse.angus.mail.smtp.SMTPSSLTransport"},
            {"com.sun.mail.smtp.SMTPTransport", "com.sun.mail.smtp.SMTPSSLTransport"},
    };

    /**
     * Ensure the session can actually create an {@code smtp} transport, repairing the provider
     * registration in-place when it cannot.
     * <p>
     * JavaMail resolves a transport in two steps: it looks up the provider <em>metadata</em>
     * (from a {@code META-INF/javamail.providers} resource or an explicit registration), then it
     * instantiates the provider's implementation class. When a host repackages into a shaded/uber
     * jar, a surviving {@code javamail.providers} resource frequently points the {@code smtp}
     * protocol at an implementation class that is not on the classpath (e.g. a legacy
     * {@code com.sun.mail.*} class) while the Eclipse Angus classes that <em>are</em> present go
     * unreferenced. The metadata lookup then succeeds but the class load fails, and JavaMail masks
     * that as {@code NoSuchProviderException: smtp} at send time.
     * <p>
     * So we health-check by actually instantiating the transport (not just checking that provider
     * metadata exists). If that fails, we register a verified-loadable implementation and force the
     * session to use it via the {@code mail.<proto>.class} property — which {@link Session#getProvider}
     * consults <em>before</em> the protocol map, and which therefore overrides a broken mapping that
     * {@link Session#addProvider} alone would not (addProvider does not replace an existing
     * protocol→provider entry). If no implementation can be instantiated at all, a clear
     * {@link ReportDeliveryException} with remediation guidance is thrown instead of the cryptic
     * {@code NoSuchProviderException}.
     */
    private static void ensureSmtpProvider(Session session, Properties props) {
        if (canCreateTransport(session, "smtp")) {
            return;
        }
        for (String[] impl : SMTP_IMPL_CANDIDATES) {
            try {
                // Confirm the implementation class is actually loadable before forcing it.
                Class.forName(impl[0]);
                session.addProvider(new Provider(Provider.Type.TRANSPORT, "smtp", impl[0], "Jakarta Mail", null));
                session.addProvider(new Provider(Provider.Type.TRANSPORT, "smtps", impl[1], "Jakarta Mail", null));
                // Force getProvider() to resolve our (loadable) class, bypassing any broken mapping.
                props.put("mail.smtp.class", impl[0]);
                props.put("mail.smtps.class", impl[1]);
                if (canCreateTransport(session, "smtp")) {
                    return;
                }
            } catch (ReflectiveOperationException | LinkageError | RuntimeException ignored) {
                // This implementation is missing or not usable; try the next candidate.
            }
        }
        throw new ReportDeliveryException(
                "Unable to create a Jakarta Mail 'smtp' transport on the host classpath. The 'jakarta.mail' "
                + "API is present, but no usable SMTP implementation could be instantiated: neither Eclipse "
                + "Angus Mail (org.eclipse.angus.mail.smtp.SMTPTransport) nor the legacy com.sun.mail "
                + "implementation is loadable, so the registered provider cannot be created "
                + "(NoSuchProviderException: smtp). Ensure 'org.eclipse.angus:jakarta.mail' (and its runtime "
                + "dependency 'org.eclipse.angus:angus-activation') are on the host runtime classpath; "
                + "report_dao-sdk declares them transitively via spring-boot-starter-mail, so this usually "
                + "means the host excludes them, ships only the API jar, or dropped them while building a "
                + "shaded/uber JAR. If you build a shaded JAR, also merge META-INF resources (e.g. the Maven "
                + "Shade plugin's ServicesResourceTransformer) so the mail provider registration survives.");
    }

    /** Whether the session can instantiate a transport for the protocol (no network connection is made). */
    private static boolean canCreateTransport(Session session, String protocol) {
        try {
            session.getTransport(protocol);
            return true;
        } catch (Exception ex) {
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
