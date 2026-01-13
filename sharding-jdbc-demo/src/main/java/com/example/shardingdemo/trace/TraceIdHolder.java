package com.example.shardingdemo.trace;

import com.alibaba.ttl.TransmittableThreadLocal;

public final class TraceIdHolder {
    private static final TransmittableThreadLocal<String> TRACE_ID = new TransmittableThreadLocal<>();

    private TraceIdHolder() {
    }

    public static void set(String traceId) {
        TRACE_ID.set(traceId);
    }

    public static String get() {
        return TRACE_ID.get();
    }

    public static void clear() {
        TRACE_ID.remove();
    }
}
