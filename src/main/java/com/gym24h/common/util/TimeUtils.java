package com.gym24h.common.util;

import java.time.Clock;
import java.time.Instant;

public final class TimeUtils {

    private TimeUtils() {
    }

    public static Instant utcNow(Clock clock) {
        return Instant.now(clock);
    }

    public static boolean isExpired(Instant validUntil, Instant now, long bufferSeconds) {
        return !now.isBefore(validUntil.plusSeconds(bufferSeconds));
    }
}
