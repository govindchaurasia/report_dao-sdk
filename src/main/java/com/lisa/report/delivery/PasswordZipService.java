package com.lisa.report.delivery;

import com.lisa.report.ReportDeliveryException;
import net.lingala.zip4j.io.outputstream.ZipOutputStream;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.model.enums.AesKeyStrength;
import net.lingala.zip4j.model.enums.CompressionMethod;
import net.lingala.zip4j.model.enums.EncryptionMethod;

import java.io.ByteArrayOutputStream;

/**
 * Wraps a report's bytes in a single, AES-256 password-protected zip entry,
 * entirely in memory. Used when the delivery config carries a {@code zipPassword}.
 */
public class PasswordZipService {

    /**
     * @param entryName the name of the file inside the archive (e.g. {@code report.xlsx})
     * @param content   the raw file bytes to compress
     * @param password  the archive password (must not be blank)
     * @return the bytes of the resulting {@code .zip} archive
     */
    public byte[] zip(String entryName, byte[] content, String password) {
        if (password == null || password.isBlank()) {
            throw new ReportDeliveryException("zipPassword must not be blank when zipping a report");
        }
        ZipParameters parameters = new ZipParameters();
        parameters.setCompressionMethod(CompressionMethod.DEFLATE);
        parameters.setEncryptFiles(true);
        parameters.setEncryptionMethod(EncryptionMethod.AES);
        parameters.setAesKeyStrength(AesKeyStrength.KEY_STRENGTH_256);
        parameters.setFileNameInZip(entryName);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos, password.toCharArray())) {
            zos.putNextEntry(parameters);
            zos.write(content);
            zos.closeEntry();
        } catch (Exception ex) {
            throw new ReportDeliveryException("Failed to create password-protected zip for: " + entryName, ex);
        }
        return baos.toByteArray();
    }
}
