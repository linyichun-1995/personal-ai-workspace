package com.example.workspace.file;

import static org.assertj.core.api.Assertions.*;

import com.example.workspace.common.api.ErrorCode;
import com.example.workspace.common.exception.AppException;
import com.example.workspace.file.application.FilePolicy;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FilePolicyTest {
    @TempDir Path directory;
    private FilePolicy policy() { return new FilePolicy(1024, 4096, 1, directory.toString()); }
    private ByteArrayInputStream text(String value) { return new ByteArrayInputStream(value.getBytes(StandardCharsets.UTF_8)); }

    @Test void normalizesMimeParametersAndRejectsUnsupportedFormats() {
        FilePolicy.declaredType("txt", "TEXT/PLAIN; charset=UTF-8");
        FilePolicy.declaredType("txt", "APPLICATION/OCTET-STREAM; charset=binary");
        assertThatThrownBy(() -> FilePolicy.declaredType("txt", "text/html")).isInstanceOf(AppException.class);
        assertThatThrownBy(() -> FilePolicy.name("../secret.txt")).isInstanceOf(AppException.class);
        assertThatThrownBy(() -> FilePolicy.extension("example.exe")).isInstanceOf(AppException.class);
    }

    @Test void validatesSizeAndUtf8AndRemovesOnlyOwnedTemporaryDirectories() throws Exception {
        var p = policy();
        Path unrelated = Files.writeString(directory.resolve("keep.txt"), "untouched");
        assertThatThrownBy(() -> p.receive(text("oversize"), 2, "txt", () -> {}))
                .isInstanceOfSatisfying(AppException.class, error -> assertThat(error.code()).isEqualTo(ErrorCode.FILE_TOO_LARGE));
        assertThatThrownBy(() -> p.receive(text("small"), 20, "txt", () -> {})).isInstanceOf(AppException.class);
        assertThatThrownBy(() -> p.receive(new ByteArrayInputStream(new byte[]{(byte) 0xff}), 1, "txt", () -> {}))
                .isInstanceOfSatisfying(AppException.class, error -> assertThat(error.code()).isEqualTo(ErrorCode.FILE_TYPE_NOT_ALLOWED));
        assertThatThrownBy(() -> p.receive(text("not a pdf"), 9, "pdf", () -> {})).isInstanceOf(AppException.class);
        try (var file = p.receive(text("hello"), 5, "txt", () -> {})) {
            assertThat(Files.readString(file.path())).isEqualTo("hello");
            assertThat(file.sha256()).isEqualTo("2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824");
        }
        try (var remaining = Files.list(directory)) { assertThat(remaining.toList()).containsExactly(unrelated); }
    }

    @Test void cleanupFailureStillReleasesExactlyOneReceiveSlot() throws Exception {
        var p = policy();
        var first = p.receive(text("a"), 1, "txt", () -> {});
        Path extra = Files.writeString(first.path().getParent().resolve("extra"), "unexpected");
        assertThatThrownBy(first::close).isInstanceOf(DirectoryNotEmptyException.class);
        Files.delete(extra);
        first.close();
        var second = p.receive(text("b"), 1, "txt", () -> {});
        assertThatThrownBy(() -> p.receive(text("c"), 1, "txt", () -> {}))
                .isInstanceOfSatisfying(AppException.class, error -> assertThat(error.code()).isEqualTo(ErrorCode.RATE_LIMITED));
        second.close();
        second.close();
        try (var third = p.receive(text("d"), 1, "txt", () -> {})) { assertThat(third.size()).isEqualTo(1); }
    }

    @Test void interruptedInputReleasesPermitAndDeletesPartialBytes() throws Exception {
        var p = policy();
        InputStream failed = new InputStream() { public int read() throws IOException { throw new IOException("disconnect"); } };
        assertThatThrownBy(() -> p.receive(failed, 4, "txt", () -> {})).isInstanceOf(IOException.class);
        try (var file = p.receive(text("ok"), 2, "txt", () -> {})) { assertThat(file.size()).isEqualTo(2); }
        try (var remaining = Files.list(directory)) { assertThat(remaining.toList()).isEmpty(); }
    }
}
