package com.example.workspace.file.application;

import com.example.workspace.common.api.ErrorCode;
import com.example.workspace.common.domain.SourceAccess;
import com.example.workspace.common.exception.AppException;
import com.example.workspace.infrastructure.storage.Sha256;
import java.io.*;
import java.nio.charset.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.ZipInputStream;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class FilePolicy {
    public static final Map<String,String> FORMATS=Map.of("pdf","application/pdf","docx","application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "txt","text/plain","md","text/markdown","png","image/png","jpg","image/jpeg","jpeg","image/jpeg","webp","image/webp");
    private final Semaphore slots;
    public final long maxBytes;
    public final long quotaBytes;
    private final Path tempRoot;
    public FilePolicy(@Value("${app.files.max-size:20971520}") long maxBytes,@Value("${app.files.quota:1073741824}") long quotaBytes,
            @Value("${app.files.max-concurrent-receives:4}") int concurrency,@Value("${app.files.temp-directory:${java.io.tmpdir}/workspace-uploads}") String directory) {
        if(maxBytes<1 || maxBytes>20971520 || quotaBytes<maxBytes || concurrency<1 || concurrency>8) throw new IllegalArgumentException("Invalid file limits");
        this.maxBytes=maxBytes;this.quotaBytes=quotaBytes;this.slots=new Semaphore(concurrency);this.tempRoot=Path.of(directory).toAbsolutePath().normalize();
    }
    public static String name(String value) {
        if(value==null) throw SourceAccess.invalid("请输入文件名称");
        String name=value.strip();
        if(name.isEmpty() || name.codePointCount(0,name.length())>200 || name.codePoints().anyMatch(c->Character.isISOControl(c)||c=='/'||c=='\\'||c==':') || name.equals(".") || name.equals("..")) throw SourceAccess.invalid("文件名称无效");
        return name;
    }
    public static String extension(String name) {
        int dot=name.lastIndexOf('.');String ext=dot<0?"":name.substring(dot+1).toLowerCase(Locale.ROOT);
        if(!FORMATS.containsKey(ext)) throw unsupported();return ext;
    }
    public static void declaredType(String extension,String type) {
        if (!FORMATS.containsKey(extension)) throw unsupported();
        if(type==null || type.isBlank()) return;
        String base=type.split(";",2)[0].strip().toLowerCase(Locale.ROOT);
        if (base.equals("application/octet-stream")) return;
        if(!FORMATS.get(extension).equals(base) && !(extension.equals("md") && base.equals("text/plain"))) throw unsupported();
    }
    public TempFile receive(InputStream input,long declaredSize,String ext,Runnable renew) throws IOException {
        if (declaredSize < 0) throw SourceAccess.invalid("文件大小不能为负数");
        if (declaredSize > maxBytes) throw tooLarge();
        if (!FORMATS.containsKey(ext)) throw unsupported();
        if(!slots.tryAcquire()) throw new AppException(ErrorCode.RATE_LIMITED,HttpStatus.TOO_MANY_REQUESTS,"上传繁忙，请稍后重试");
        Path path=null;
        try {
            Files.createDirectories(tempRoot);
            // Each process owns only its random private directory; never traverse user paths.
            Path dir=Files.createTempDirectory(tempRoot,"receive-");
            path=dir.resolve("content");
            long count=0;long renewed=System.nanoTime();
            var digest=Sha256.digest();
            try(var out=Files.newOutputStream(path,StandardOpenOption.CREATE_NEW)) {
                byte[] buffer=new byte[65536];int read;
                while((read=input.read(buffer))!=-1) {
                    count+=read;
                    if(count>maxBytes || count>declaredSize) throw tooLarge();
                    out.write(buffer,0,read);digest.update(buffer,0,read);
                    if(System.nanoTime()-renewed>20_000_000_000L) { renew.run();renewed=System.nanoTime(); }
                }
            }
            if(count!=declaredSize) throw SourceAccess.invalid("实际字节数与声明大小不一致，请重新上传");
            validate(path,ext);
            return new TempFile(path,count,HexFormat.of().formatHex(digest.digest()),slots);
        } catch(IOException|RuntimeException e) {
            try { if(path!=null) cleanup(path); }
            catch (IOException cleanupFailure) { e.addSuppressed(cleanupFailure); }
            finally { slots.release(); }
            throw e;
        }
    }
    private void validate(Path path,String ext) throws IOException {
        byte[] header;
        try(var in=Files.newInputStream(path)) {header=in.readNBytes(16);}
        if(ext.equals("txt")||ext.equals("md")) {
            var decoder=StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT);
            try(var reader=new InputStreamReader(Files.newInputStream(path),decoder)) {
                char[] buffer=new char[8192];int count;
                while((count=reader.read(buffer))!=-1) for(int i=0;i<count;i++) if(buffer[i]==0 || (Character.isISOControl(buffer[i]) && "\r\n\t\f".indexOf(buffer[i])<0)) throw unsupported();
            } catch(CharacterCodingException e) {throw unsupported();}
        } else if(ext.equals("pdf")) {
            if(!starts(header,new byte[]{37,80,68,70,45})) throw unsupported();
        } else if(ext.equals("docx")) {
            long expanded=0;int entries=0;boolean document=false;boolean types=false;
            try(var zip=new ZipInputStream(Files.newInputStream(path))) {
                java.util.zip.ZipEntry entry;byte[] buffer=new byte[65536];
                while((entry=zip.getNextEntry())!=null) {
                    if(++entries>1000 || entry.getName().contains("..") || entry.getName().startsWith("/")
                            || entry.getName().contains("\\") || entry.getName().contains(":")
                            || entry.getName().toLowerCase(Locale.ROOT).contains("vbaproject")) throw unsupported();
                    document|=entry.getName().equals("word/document.xml");types|=entry.getName().equals("[Content_Types].xml");
                    int n;while((n=zip.read(buffer))!=-1) {expanded+=n;if(expanded>104857600) throw unsupported();}
                }
            } catch(java.util.zip.ZipException e) {throw unsupported();}
            if(!document||!types) throw unsupported();
        } else if(ext.equals("png")) {
            if(!starts(header,new byte[]{(byte)137,80,78,71,13,10,26,10})) throw unsupported();
        } else if(ext.equals("jpg")||ext.equals("jpeg")) {
            if(!starts(header,new byte[]{(byte)255,(byte)216,(byte)255})) throw unsupported();
        } else if(header.length<12 || !new String(header,0,4,StandardCharsets.US_ASCII).equals("RIFF") || !new String(header,8,4,StandardCharsets.US_ASCII).equals("WEBP")) throw unsupported();
    }
    private boolean starts(byte[] bytes,byte[] prefix) {return bytes.length>=prefix.length && Arrays.equals(Arrays.copyOf(bytes,prefix.length),prefix);}
    public static AppException unsupported() {return new AppException(ErrorCode.FILE_TYPE_NOT_ALLOWED,HttpStatus.UNSUPPORTED_MEDIA_TYPE,"文件内容、编码或格式不受支持");}
    public static AppException tooLarge() {return new AppException(ErrorCode.FILE_TOO_LARGE,HttpStatus.PAYLOAD_TOO_LARGE,"文件超过允许大小");}
    private static void cleanup(Path path) throws IOException {
        Files.deleteIfExists(path);
        Files.deleteIfExists(path.getParent());
    }
    public static final class TempFile implements AutoCloseable {
        private final Path path;
        private final long size;
        private final String sha256;
        private final Semaphore slots;
        private final AtomicBoolean released = new AtomicBoolean();
        private TempFile(Path path, long size, String sha256, Semaphore slots) {
            this.path=path;this.size=size;this.sha256=sha256;this.slots=slots;
        }
        public Path path() { return path; }
        public long size() { return size; }
        public String sha256() { return sha256; }
        @Override public void close() throws IOException {
            try { cleanup(path); }
            finally { if (released.compareAndSet(false, true)) slots.release(); }
        }
        public void closeQuietly() {
            try { close(); } catch (IOException ignored) {
                // The random receive directory has no published content; retain it for operational cleanup.
            }
        }
    }
}
