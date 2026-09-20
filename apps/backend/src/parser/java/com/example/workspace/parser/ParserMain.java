package com.example.workspace.parser;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Set;
import java.util.zip.ZipFile;
import javax.imageio.ImageIO;
import javax.xml.stream.*;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;

/** Standalone, container-only parser: no Spring, database, credentials or network. */
public final class ParserMain {
    private static final int MAX_TEXT = 200000;
    private static final Path INPUT = Path.of("/input/content");

    public static void main(String[] args) throws IOException {
        String status = "FAILED", error = "INVALID_DOCUMENT";
        boolean truncated = false;
        byte[] bytes = new byte[0];
        try {
            if (args.length != 2 || !Set.of("text", "preview").contains(args[0])
                    || !Set.of("pdf", "docx", "txt", "md", "png", "jpg", "jpeg", "webp").contains(args[1])) throw new IOException();
            if (Files.size(INPUT) > 20971520) throw new LimitExceeded();
            if (args[0].equals("preview")) {
                bytes = preview(args[1]);
                status = "READY";
            } else if (Set.of("png", "jpg", "jpeg", "webp").contains(args[1])) {
                preview(args[1]); // Decode under the same limits before declaring a usable image.
                status = "SKIPPED";
            } else {
                var output = new LimitedWriter();
                try { extract(args[1], output); }
                catch (TextLimit reached) { truncated = true; }
                String text = output.text.toString().replace("\u0000", "").strip();
                if (!text.isEmpty() && Character.isHighSurrogate(text.charAt(text.length() - 1))) text = text.substring(0, text.length() - 1);
                bytes = text.getBytes(StandardCharsets.UTF_8);
                status = text.isEmpty() ? "EMPTY" : "READY";
            }
            error = "";
        } catch (InvalidPasswordException | EncryptedDocument e) { error = "PASSWORD_PROTECTED"; }
        catch (LimitExceeded | OutOfMemoryError e) { error = "RESOURCE_LIMIT"; }
        catch (Exception e) { error = "INVALID_DOCUMENT"; }
        try (var out = new DataOutputStream(System.out)) {
            out.writeUTF(status);
            out.writeUTF(error);
            out.writeBoolean(truncated);
            out.writeInt(bytes.length);
            out.write(bytes);
        }
    }

    private static void extract(String ext, Writer output) throws Exception {
        switch (ext) {
            case "txt", "md" -> {
                try (var reader = Files.newBufferedReader(INPUT, StandardCharsets.UTF_8)) {
                    int first = reader.read();
                    if (first >= 0 && first != 0xfeff) output.write(first);
                    reader.transferTo(output);
                }
            }
            case "pdf" -> {
                try (var document = Loader.loadPDF(INPUT.toFile())) {
                    if (document.isEncrypted()) throw new EncryptedDocument();
                    if (document.getNumberOfPages() > 2000) throw new LimitExceeded();
                    new PDFTextStripper().writeText(document, output);
                }
            }
            case "docx" -> {
                try (var zip = new ZipFile(INPUT.toFile())) {
                    if (zip.size() > 1000) throw new LimitExceeded();
                    long expanded = 0;
                    var entries = zip.entries();
                    while (entries.hasMoreElements()) {
                        var entry = entries.nextElement();
                        if (entry.getSize() < 0 || (expanded += entry.getSize()) > 104857600) throw new LimitExceeded();
                        if (entry.getName().toLowerCase(java.util.Locale.ROOT).contains("vbaproject")) throw new IOException();
                    }
                    var entry = zip.getEntry("word/document.xml");
                    if (entry == null || zip.getEntry("[Content_Types].xml") == null) throw new IOException();
                    var factory = XMLInputFactory.newFactory();
                    factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
                    factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
                    factory.setXMLResolver((a,b,c,d) -> { throw new XMLStreamException("External entities disabled"); });
                    try (var stream = zip.getInputStream(entry)) {
                        var xml = factory.createXMLStreamReader(stream);
                        try {
                            while (xml.hasNext()) {
                                int event = xml.next();
                                if (event == XMLStreamConstants.DTD) throw new IOException();
                                if (event == XMLStreamConstants.START_ELEMENT && "t".equals(xml.getLocalName())) output.write(xml.getElementText());
                                if (event == XMLStreamConstants.END_ELEMENT && "p".equals(xml.getLocalName())) output.write('\n');
                            }
                        } finally { xml.close(); }
                    }
                }
            }
            default -> throw new IOException();
        }
    }

    private static byte[] preview(String ext) throws Exception {
        BufferedImage image;
        if (ext.equals("pdf")) {
            try (var document = Loader.loadPDF(INPUT.toFile())) {
                if (document.isEncrypted()) throw new EncryptedDocument();
                if (document.getNumberOfPages() == 0) throw new IOException();
                var rect = document.getPage(0).getCropBox();
                if (rect.getWidth() <= 0 || rect.getHeight() <= 0 || rect.getWidth() > 20000 || rect.getHeight() > 20000) throw new LimitExceeded();
                float scale = Math.min(1.5f, 1600f / Math.max(rect.getWidth(), rect.getHeight()));
                image = new PDFRenderer(document).renderImage(0, scale);
            }
        } else {
            if (!Set.of("png", "jpg", "jpeg", "webp").contains(ext)) throw new IOException();
            try (var in = ImageIO.createImageInputStream(INPUT.toFile())) {
                var readers = ImageIO.getImageReaders(in);
                if (!readers.hasNext()) throw new IOException();
                var reader = readers.next();
                try {
                    reader.setInput(in, true, true);
                    int width = reader.getWidth(0), height = reader.getHeight(0);
                    if (width < 1 || height < 1 || (long) width * height > 20000000) throw new LimitExceeded();
                    image = reader.read(0);
                } finally { reader.dispose(); }
            }
        }
        double scale = Math.min(1d, 1600d / Math.max(image.getWidth(), image.getHeight()));
        var sanitized = new BufferedImage(Math.max(1, (int)(image.getWidth() * scale)), Math.max(1, (int)(image.getHeight() * scale)), BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = sanitized.createGraphics();
        try { graphics.drawImage(image, 0, 0, sanitized.getWidth(), sanitized.getHeight(), null); }
        finally { graphics.dispose(); image.flush(); }
        var bytes = new ByteArrayOutputStream();
        ImageIO.write(sanitized, "png", bytes);
        sanitized.flush();
        return bytes.toByteArray();
    }

    private static final class LimitedWriter extends Writer {
        final StringBuilder text = new StringBuilder();
        @Override public void write(char[] chars, int offset, int length) throws IOException {
            int keep = Math.min(length, MAX_TEXT - text.length());
            text.append(chars, offset, keep);
            if (keep < length) throw new TextLimit();
        }
        @Override public void flush() {}
        @Override public void close() {}
    }
    private static final class TextLimit extends IOException {}
    private static final class EncryptedDocument extends IOException {}
    private static final class LimitExceeded extends IOException {}
}
