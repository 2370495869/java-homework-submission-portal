package io.github.homeworkportal.submission;

import io.github.homeworkportal.domain.SubmissionVersion;
import java.util.List;

public record SubmissionSummary(String displayName, String username, List<SubmissionVersion> versions) {
}
