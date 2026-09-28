package io.github.homeworkportal.web;

import io.github.homeworkportal.domain.Assignment;
import java.util.List;

public record AssignmentPage(Assignment assignment, String dueText, List<SubmissionPage> submissions,
                             boolean canSubmit) {
}
