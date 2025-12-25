package com.example.springstartup.trace;

import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public final class Trace {
    private static final AtomicInteger SEQ = new AtomicInteger(0);
    private static final long START_NANOS = System.nanoTime();

    private Trace() {
    }

    public static void log(String phase) {
        log(phase, (Object[]) null);
    }

    public static void log(String phase, Object... keyValues) {
        int seq = SEQ.incrementAndGet();
        long elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - START_NANOS);
        String thread = Thread.currentThread().getName();

        StringBuilder extras = new StringBuilder();
        if (keyValues != null && keyValues.length > 0) {
            if (keyValues.length % 2 != 0) {
                throw new IllegalArgumentException("keyValues must be even length: k1,v1,k2,v2...");
            }
            for (int i = 0; i < keyValues.length; i += 2) {
                Object k = keyValues[i];
                Object v = keyValues[i + 1];
                if (k == null) {
                    continue;
                }
                extras.append(" ").append(k).append("=").append(Objects.toString(v));
            }
        }

        System.out.printf("%03d +%04dms [%s] %s%s @%s%n", seq, elapsedMs, thread, phase, extras, Instant.now());
    }
}

