package com.lisa.report.delivery;

import com.lisa.report.ReportDeliveryException;
import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.model.FileHeader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PasswordZipServiceTest {

    private final PasswordZipService service = new PasswordZipService();

    @Test
    void producesPasswordProtectedZipThatRoundTrips(@TempDir Path tempDir) throws Exception {
        byte[] content = "secret-payload".getBytes(StandardCharsets.UTF_8);

        byte[] zipped = service.zip("report.csv", content, "p@ss");

        Path zipPath = tempDir.resolve("out.zip");
        Files.write(zipPath, zipped);

        try (ZipFile zipFile = new ZipFile(zipPath.toFile(), "p@ss".toCharArray())) {
            assertThat(zipFile.isEncrypted()).isTrue();
            FileHeader header = zipFile.getFileHeader("report.csv");
            assertThat(header).isNotNull();
            try (InputStream in = zipFile.getInputStream(header)) {
                assertThat(in.readAllBytes()).isEqualTo(content);
            }
        }
    }

    @Test
    void rejectsBlankPassword() {
        assertThatThrownBy(() -> service.zip("report.csv", new byte[]{1}, "  "))
                .isInstanceOf(ReportDeliveryException.class)
                .hasMessageContaining("zipPassword must not be blank");
        assertThatThrownBy(() -> service.zip("report.csv", new byte[]{1}, null))
                .isInstanceOf(ReportDeliveryException.class);
    }
}
