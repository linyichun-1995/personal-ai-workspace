package com.example.workspace.common.util;

import com.example.workspace.workspace.domain.Workspace;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;

public final class WorkspaceClock {

    private WorkspaceClock() {
    }

    public record Windows(
            ZoneId zone,
            Instant startOfToday,
            Instant startOfTomorrow,
            Instant startOfUpcomingEnd,
            Instant startOfWeek,
            Instant startOfNextWeek
    ) {
    }

    public static Windows of(Workspace workspace, Instant now) {
        ZoneId zone = ZoneId.of(workspace.timezone());
        ZonedDateTime zoned = now.atZone(zone);
        LocalDate today = zoned.toLocalDate();
        Instant startOfToday = today.atStartOfDay(zone).toInstant();
        Instant startOfTomorrow = today.plusDays(1).atStartOfDay(zone).toInstant();
        Instant startOfUpcomingEnd = today.plusDays(8).atStartOfDay(zone).toInstant();
        DayOfWeek weekStart = DayOfWeek.of(workspace.weekStartsOn());
        LocalDate startOfWeekDate = today;
        while (startOfWeekDate.getDayOfWeek() != weekStart) {
            startOfWeekDate = startOfWeekDate.minusDays(1);
        }
        return new Windows(
                zone,
                startOfToday,
                startOfTomorrow,
                startOfUpcomingEnd,
                startOfWeekDate.atStartOfDay(zone).toInstant(),
                startOfWeekDate.plusDays(7).atStartOfDay(zone).toInstant()
        );
    }
}
