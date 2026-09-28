package io.github.homeworkportal.web;

public record VersionPage(Long id, int number, String filename, String submittedAt,
                          boolean late, long byteSize, String sha256) {
}
