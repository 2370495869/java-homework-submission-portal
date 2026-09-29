package io.github.homeworkportal.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.regex.Pattern;

@Component
public class LocalFileStorage implements FileStorage {
    private static final Pattern KEY_PATTERN =
            Pattern.compile("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.(pdf|doc|docx)");

    private final Path root;

    public LocalFileStorage(@Value("${app.storage.directory:./uploads}") String directory) {
        this.root = Path.of(directory).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.root);
        } catch (IOException ex) {
            throw new IllegalStateException("Cannot create upload storage directory.", ex);
        }
    }

    @Override
    public StoredObject store(String storageKey, InputStream content) throws IOException {
        Path destination = resolveKey(storageKey);
        if (Files.exists(destination) || Files.isSymbolicLink(destination)) {
            throw new IOException("Storage key already exists.");
        }
        MessageDigest digest = sha256();
        long size = 0;
        byte[] buffer = new byte[8192];
        try (InputStream input = content;
             OutputStream output = Files.newOutputStream(destination,
                     StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)) {
            int read;
            while ((read = input.read(buffer)) != -1) {
                size += read;
                if (size > UploadLimits.MAX_FILE_SIZE_BYTES) {
                    throw new IOException("File exceeds the configured size limit.");
                }
                digest.update(buffer, 0, read);
                output.write(buffer, 0, read);
            }
        } catch (IOException | RuntimeException ex) {
            Files.deleteIfExists(destination);
            throw ex;
        }
        return new StoredObject(size, HexFormat.of().formatHex(digest.digest()));
    }

    @Override
    public InputStream open(String storageKey) throws IOException {
        Path path = resolveKey(storageKey);
        if (!Files.isRegularFile(path) || Files.isSymbolicLink(path)) {
            throw new IOException("Stored file is unavailable.");
        }
        return Files.newInputStream(path, StandardOpenOption.READ);
    }

    @Override
    public void delete(String storageKey) throws IOException {
        Files.deleteIfExists(resolveKey(storageKey));
    }

    private Path resolveKey(String key) throws IOException {
        if (key == null || !KEY_PATTERN.matcher(key).matches()) {
            throw new IOException("Invalid storage key.");
        }
        Path result = root.resolve(key).normalize();
        if (!result.startsWith(root) || !root.equals(result.getParent())) {
            throw new IOException("Storage path is outside the configured root.");
        }
        return result;
    }

    private static MessageDigest sha256() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is unavailable.", ex);
        }
    }
}
