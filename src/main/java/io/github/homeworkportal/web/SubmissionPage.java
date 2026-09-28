package io.github.homeworkportal.web;

import java.util.List;

public record SubmissionPage(String displayName, String username, List<VersionPage> versions) {
}
