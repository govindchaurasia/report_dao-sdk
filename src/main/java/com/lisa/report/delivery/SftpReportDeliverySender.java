package com.lisa.report.delivery;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;
import com.lisa.report.ReportDeliveryException;
import org.springframework.util.StringUtils;

import java.io.ByteArrayInputStream;
import java.util.Properties;

/**
 * Delivers a report by uploading it to an SFTP server (JSch).
 * <p>
 * The connection block on {@link ReportDeliveryConfig} ({@code host}/{@code port}/
 * {@code username}/{@code password}) is the SFTP server; the file is written to
 * {@code remoteDirectory/fileName}, creating the directory path if needed.
 */
public class SftpReportDeliverySender implements ReportDeliverySender {

    private static final int DEFAULT_PORT = 22;
    private static final int CONNECT_TIMEOUT_MS = 30_000;

    @Override
    public DeliveryMethod getSupportedMethod() {
        return DeliveryMethod.SFTP;
    }

    @Override
    public void send(byte[] content, String fileName, String contentType, ReportDeliveryConfig config) {
        if (!StringUtils.hasText(config.getHost())) {
            throw new ReportDeliveryException("SFTP delivery requires a host");
        }
        if (!StringUtils.hasText(config.getUsername())) {
            throw new ReportDeliveryException("SFTP delivery requires a username");
        }

        Session session = null;
        ChannelSftp channel = null;
        try {
            JSch jsch = new JSch();
            int port = config.getPort() != null ? config.getPort() : DEFAULT_PORT;
            session = jsch.getSession(config.getUsername(), config.getHost(), port);
            session.setPassword(config.getPassword());

            Properties sessionConfig = new Properties();
            sessionConfig.put("StrictHostKeyChecking", config.isStrictHostKeyChecking() ? "yes" : "no");
            session.setConfig(sessionConfig);
            session.connect(CONNECT_TIMEOUT_MS);

            channel = (ChannelSftp) session.openChannel("sftp");
            channel.connect(CONNECT_TIMEOUT_MS);

            String directory = config.getRemoteDirectory();
            if (StringUtils.hasText(directory)) {
                ensureRemoteDirectory(channel, directory);
                channel.cd(directory);
            }

            try (ByteArrayInputStream in = new ByteArrayInputStream(content)) {
                channel.put(in, fileName);
            }
        } catch (ReportDeliveryException ex) {
            throw ex;
        } catch (JSchException | SftpException ex) {
            throw new ReportDeliveryException("Failed to upload report to SFTP host: " + config.getHost(), ex);
        } catch (Exception ex) {
            throw new ReportDeliveryException("Failed to upload report to SFTP host: " + config.getHost(), ex);
        } finally {
            if (channel != null && channel.isConnected()) {
                channel.disconnect();
            }
            if (session != null && session.isConnected()) {
                session.disconnect();
            }
        }
    }

    /**
     * Creates the (possibly nested) remote directory if it does not yet exist,
     * walking each path segment from the appropriate root.
     */
    private static void ensureRemoteDirectory(ChannelSftp channel, String directory) throws SftpException {
        boolean absolute = directory.startsWith("/");
        String[] segments = directory.split("/");
        StringBuilder path = new StringBuilder(absolute ? "/" : "");
        for (String segment : segments) {
            if (segment.isEmpty()) {
                continue;
            }
            if (path.length() > 0 && path.charAt(path.length() - 1) != '/') {
                path.append('/');
            }
            path.append(segment);
            String current = path.toString();
            try {
                channel.stat(current);
            } catch (SftpException statEx) {
                channel.mkdir(current);
            }
        }
    }
}
