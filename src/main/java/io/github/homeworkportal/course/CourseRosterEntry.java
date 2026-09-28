package io.github.homeworkportal.course;

import java.time.Instant;

public record CourseRosterEntry(String displayName, String username, Instant joinedAt) {
}
