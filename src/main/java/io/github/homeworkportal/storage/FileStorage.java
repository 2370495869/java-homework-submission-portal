package io.github.homeworkportal.storage;

import java.io.IOException;
import java.io.InputStream;

public interface FileStorage {
    StoredObject store(String storageKey, InputStream content) throws IOException;
    InputStream open(String storageKey) throws IOException;
    void delete(String storageKey) throws IOException;
}
