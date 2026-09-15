package com.example.workspace.common.util;

import com.example.workspace.common.exception.BusinessException;
import java.time.DateTimeException;
import java.time.ZoneId;

public final class TimeZones {

    public static final String DEFAULT = "UTC";

    private TimeZones() {
    }

    public static String normalizeOrDefault(String timezone) {
        if (timezone == null || timezone.isBlank()) {
            return DEFAULT;
        }
        return requireValid(timezone);
    }

    public static String requireValid(String timezone) {
        try {
            return ZoneId.of(timezone.trim()).getId();
        }
        catch (DateTimeException exception) {
            throw new BusinessException("时区无效");
        }
    }
}
