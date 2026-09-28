package io.github.homeworkportal.storage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import static org.junit.jupiter.api.Assertions.*;

class LocalFileStorageTest {
    @TempDir
    Path root;

    @Test
    void storesWithoutOverwriteAndVerifiesDigest() throws Exception {
        LocalFileStorage storage = new LocalFileStorage(root.toString());
        String key = "12345678-1234-1234-1234-123456789abc.pdf";
        byte[] content = "%PDF-1.7 example".getBytes(StandardCharsets.US_ASCII);

        StoredObject stored = storage.store(key, new ByteArrayInputStream(content));
        assertEquals(content.length, stored.size());
        assertEquals(HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content)), stored.sha256());
        try (var input = storage.open(key)) {
            assertArrayEquals(content, input.readAllBytes());
        }
        assertThrows(IOException.class, () -> storage.store(key, new ByteArrayInputStream(content)));
        assertThrows(IOException.class, () -> storage.open("../outside.pdf"));
    }

    @Test
    void removesPartialFileWhenSizeLimitIsExceeded() throws Exception {
        LocalFileStorage storage = new LocalFileStorage(root.toString());
        String key = "12345678-1234-1234-1234-123456789abc.pdf";
        byte[] oversized = new byte[10 * 1024 * 1024 + 1];
        assertThrows(IOException.class, () -> storage.store(key, new ByteArrayInputStream(oversized)));
        assertFalse(Files.exists(root.resolve(key)));
    }
}
