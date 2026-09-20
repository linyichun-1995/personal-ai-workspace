package com.example.workspace.infrastructure.storage;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/** Protocol boundary. Business code must never depend on an AWS SDK type. */
public interface ObjectStorage {
    Metadata put(Put command, Path replayableFile);
    Metadata head(Ref object);
    Content open(Ref object);
    DeleteResult delete(Ref object);
    Page list(ListQuery query);
    Probe probe();

    record Ref(String profileId, String bucket, String key, String versionId) {}
    record Put(Ref ref, long size, String contentType, String sha256, Map<String, String> metadata) {
        public Put { metadata = Map.copyOf(metadata); }
    }
    record Metadata(Ref ref, long size, String contentType, String etag, Map<String, String> metadata) {
        public Metadata { metadata = Map.copyOf(metadata); }
    }
    /** Closing must release the connection even when a caller abandons a partial download. */
    record Content(Metadata metadata, InputStream stream, Runnable release) implements AutoCloseable {
        @Override public void close() { release.run(); }
    }
    record ListQuery(String profileId, String bucket, String prefix, String token, int limit) {}
    record Page(List<Metadata> items, String nextToken) {
        public Page { items = List.copyOf(items); }
    }
    record Probe(boolean writable, String profileId, String bucket) {}
    enum DeleteResult { ABSENT, DELETED }
}
