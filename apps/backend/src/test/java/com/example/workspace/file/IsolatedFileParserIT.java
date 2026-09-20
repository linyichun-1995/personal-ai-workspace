package com.example.workspace.file;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.example.workspace.file.application.IsolatedFileParser;
import com.example.workspace.infrastructure.storage.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

/** Exercises the real restricted Docker process, rather than mocking the parser protocol. */
@EnabledIfEnvironmentVariable(named="WORKSPACE_PARSER_TEST", matches="true")
class IsolatedFileParserIT {
    private final ObjectStorage storage = mock(ObjectStorage.class);
    private final ObjectStorage.Ref ref = new ObjectStorage.Ref("test", "test", "test", null);
    private final IsolatedFileParser parser = new IsolatedFileParser(storage, "workspace-file-parser:0.2");
    private String supply(byte[] bytes) throws Exception {
        when(storage.open(ref)).thenAnswer(call -> new ObjectStorage.Content(
                new ObjectStorage.Metadata(ref, bytes.length, "application/octet-stream", "", Map.of()),
                new ByteArrayInputStream(bytes), () -> {}));
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    }
    @Test void extractsChineseUtf8InContainer() throws Exception {
        byte[] bytes = "中文资料检索\n# Hello world".getBytes(StandardCharsets.UTF_8);
        var result = parser.parse(ref, "md", bytes.length, supply(bytes), false);
        assertThat(result.status()).isEqualTo("READY");
        assertThat(result.text()).isEqualTo(new String(bytes, StandardCharsets.UTF_8));
    }
    @Test void rejectsMalformedPdfWithoutRetrying() throws Exception {
        byte[] bytes = "%PDF-1.7\nnot a document".getBytes(StandardCharsets.UTF_8);
        var result = parser.parse(ref, "pdf", bytes.length, supply(bytes), false);
        assertThat(result.status()).isEqualTo("FAILED");
        assertThat(result.error()).isEqualTo("INVALID_DOCUMENT");
        assertThat(result.retryable()).isFalse();
    }
    @Test void reencodesImageAsPng() throws Exception {
        var image = new java.awt.image.BufferedImage(8, 8, java.awt.image.BufferedImage.TYPE_INT_RGB);
        var out = new ByteArrayOutputStream();
        javax.imageio.ImageIO.write(image, "png", out);
        byte[] bytes = out.toByteArray();
        var result = parser.parse(ref, "png", bytes.length, supply(bytes), true);
        assertThat(result.status()).isEqualTo("READY");
        assertThat(javax.imageio.ImageIO.read(new ByteArrayInputStream(result.content())).getWidth()).isEqualTo(8);
    }
    @Test void truncatesLargeTextAtConfiguredBound() throws Exception {
        byte[] bytes = "资料".repeat(150000).getBytes(StandardCharsets.UTF_8);
        var result = parser.parse(ref, "txt", bytes.length, supply(bytes), false);
        assertThat(result.status()).isEqualTo("READY");
        assertThat(result.truncated()).isTrue();
        assertThat(result.text().length()).isEqualTo(200000);
    }
    @Test void refusesContentThatDiffersFromDatabaseDigest() throws Exception {
        byte[] bytes = "changed".getBytes(StandardCharsets.UTF_8);
        supply(bytes);
        var result = parser.parse(ref, "txt", bytes.length, "0".repeat(64), false);
        assertThat(result.status()).isEqualTo("FAILED");
        assertThat(result.error()).isEqualTo("STORAGE_UNAVAILABLE");
    }
}
