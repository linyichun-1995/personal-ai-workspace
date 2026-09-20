package com.example.workspace.file.application;

import com.example.workspace.infrastructure.storage.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Starts only the configured image; user-controlled paths/commands/configuration are never accepted. */
@Component
public class IsolatedFileParser {
    private final ObjectStorage storage;
    private final String image;
    private final Semaphore concurrency = new Semaphore(2);
    public IsolatedFileParser(ObjectStorage storage, @Value("${app.file-extraction.image:workspace-file-parser:0.2}") String image) {
        this.storage = storage;
        this.image = image;
    }
    public record Result(String status, String error, boolean truncated, byte[] content) {
        public String text() { return new String(content, StandardCharsets.UTF_8); }
        public boolean retryable() { return Set.of("PARSER_UNAVAILABLE", "PARSE_TIMEOUT", "STORAGE_UNAVAILABLE").contains(error); }
    }
    public Result parse(ObjectStorage.Ref ref, String extension, long size, String sha256, boolean preview) {
        if (!concurrency.tryAcquire()) return failed("PARSER_UNAVAILABLE");
        Path directory = null;
        Process process = null;
        String name = "workspace-parser-" + UUID.randomUUID();
        try {
            directory = Files.createTempDirectory("workspace-parse-");
            Path input = directory.resolve("content");
            try (var content = storage.open(ref); var out = Files.newOutputStream(input)) {
                long count = 0; byte[] buffer = new byte[65536]; int n;
                while ((n = content.stream().read(buffer)) != -1) {
                    count += n;
                    if (count > size || count > 20971520) return failed("INVALID_DOCUMENT");
                    out.write(buffer, 0, n);
                }
                if (count != size) return failed("STORAGE_UNAVAILABLE");
            }
            if (!Sha256.of(input).equals(sha256)) return failed("STORAGE_UNAVAILABLE");
            // The host temp directory remains private; only this read-only file is mounted for UID 65534.
            if (Files.getFileStore(input).supportsFileAttributeView("posix"))
                Files.setPosixFilePermissions(input, java.nio.file.attribute.PosixFilePermissions.fromString("r--r--r--"));
            process = new ProcessBuilder("docker", "run", "--rm", "--name", name, "--network", "none",
                    "--read-only", "--memory", "384m", "--memory-swap", "384m", "--cpus", "1", "--pids-limit", "64",
                    "--cap-drop", "ALL", "--security-opt", "no-new-privileges", "--tmpfs", "/tmp:rw,noexec,nosuid,size=32m",
                    "--mount", "type=bind,source=" + input.toAbsolutePath() + ",target=/input/content,readonly",
                    image, preview ? "preview" : "text", extension).redirectError(ProcessBuilder.Redirect.DISCARD).start();
            final Process running = process;
            try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
                var result = executor.submit(() -> {
                    try (var stream = new DataInputStream(running.getInputStream())) {
                        String status = stream.readUTF(), error = stream.readUTF();
                        boolean truncated = stream.readBoolean();
                        int length = stream.readInt();
                        if (!Set.of("READY", "EMPTY", "SKIPPED", "FAILED").contains(status) || length < 0 || length > (preview ? 12000000 : 800000)) throw new IOException();
                        byte[] bytes = stream.readNBytes(length);
                        if (bytes.length != length) throw new EOFException();
                        return new Result(status, error, truncated, bytes);
                    }
                });
                try {
                    Result parsed = result.get(30, TimeUnit.SECONDS);
                    if (!running.waitFor(2, TimeUnit.SECONDS) || running.exitValue() != 0) return failed("RESOURCE_LIMIT");
                    return parsed;
                } catch (TimeoutException e) {
                    running.destroyForcibly(); result.cancel(true); removeContainer(name);
                    return failed("PARSE_TIMEOUT");
                } catch (ExecutionException e) {
                    running.destroyForcibly(); removeContainer(name);
                    return failed("PARSER_UNAVAILABLE");
                }
            }
        } catch (StorageException e) { return failed("STORAGE_UNAVAILABLE"); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); return failed("PARSER_UNAVAILABLE"); }
        catch (IOException e) { return failed("PARSER_UNAVAILABLE"); }
        finally {
            if (process != null && process.isAlive()) { process.destroyForcibly(); removeContainer(name); }
            if (directory != null) {
                try { Files.deleteIfExists(directory.resolve("content")); Files.deleteIfExists(directory); }
                catch (IOException ignored) { /* Only a random input copy remains; no original storage is modified. */ }
            }
            concurrency.release();
        }
    }
    private void removeContainer(String name) {
        try {
            var cleanup = new ProcessBuilder("docker", "rm", "--force", name).redirectOutput(ProcessBuilder.Redirect.DISCARD)
                    .redirectError(ProcessBuilder.Redirect.DISCARD).start();
            if (!cleanup.waitFor(5, TimeUnit.SECONDS)) cleanup.destroyForcibly();
        } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        catch (IOException ignored) {}
    }
    private Result failed(String error) { return new Result("FAILED", error, false, new byte[0]); }
}
