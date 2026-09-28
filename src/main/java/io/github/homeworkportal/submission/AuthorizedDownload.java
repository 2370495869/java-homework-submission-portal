package io.github.homeworkportal.submission;

import io.github.homeworkportal.domain.SubmissionVersion;
import java.io.InputStream;

public record AuthorizedDownload(SubmissionVersion version, InputStream input) implements AutoCloseable {
    @Override
    public void close() throws java.io.IOException {
        input.close();
    }
}
