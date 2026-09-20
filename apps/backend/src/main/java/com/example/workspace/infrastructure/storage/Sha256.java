package com.example.workspace.infrastructure.storage;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public final class Sha256 {
    private Sha256() {}
    public static MessageDigest digest() {
        try { return MessageDigest.getInstance("SHA-256"); }
        catch (NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }
    public static String of(Path path) throws IOException {
        try (var in = Files.newInputStream(path)) { return of(in); }
    }
    public static String of(InputStream in) throws IOException {
        var digest = digest();
        byte[] buffer = new byte[65536];
        int count;
        while ((count = in.read(buffer)) != -1) { digest.update(buffer, 0, count); }
        return HexFormat.of().formatHex(digest.digest());
    }
}
