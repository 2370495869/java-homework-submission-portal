package io.github.homeworkportal.web;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Component
public class PortalTime {
    private static final DateTimeFormatter FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private final ZoneId zoneId;

    public PortalTime(@Value("${app.time-zone:Asia/Shanghai}") String zone) {
        this.zoneId = ZoneId.of(zone);
    }

    public Instant toInstant(LocalDateTime localDateTime) {
        return localDateTime.atZone(zoneId).toInstant();
    }

    public String format(Instant instant) {
        return FORMAT.withZone(zoneId).format(instant);
    }
}
