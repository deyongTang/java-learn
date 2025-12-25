package com.example.springstartup.trace;

import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.boot.context.event.ApplicationPreparedEvent;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.boot.context.event.ApplicationStartingEvent;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.Ordered;

public class TraceEarlyApplicationListener implements ApplicationListener<ApplicationEvent>, Ordered {
    @Override
    public void onApplicationEvent(ApplicationEvent event) {
        if (event instanceof ApplicationStartingEvent) {
            Trace.log("event.ApplicationStartingEvent");
            return;
        }
        if (event instanceof ApplicationEnvironmentPreparedEvent) {
            Trace.log("event.ApplicationEnvironmentPreparedEvent");
            return;
        }
        if (event instanceof ApplicationPreparedEvent) {
            Trace.log("event.ApplicationPreparedEvent");
            return;
        }
        if (event instanceof ApplicationStartedEvent) {
            Trace.log("event.ApplicationStartedEvent");
            return;
        }
        if (event instanceof ApplicationReadyEvent) {
            Trace.log("event.ApplicationReadyEvent");
        }
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}

