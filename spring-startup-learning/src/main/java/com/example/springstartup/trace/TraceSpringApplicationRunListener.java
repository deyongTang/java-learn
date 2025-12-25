package com.example.springstartup.trace;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringApplicationRunListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;

import java.time.Duration;

public class TraceSpringApplicationRunListener implements SpringApplicationRunListener {
    public TraceSpringApplicationRunListener(SpringApplication application, String[] args) {
        Trace.log("runListener.ctor", "mainApplicationClass", application.getMainApplicationClass());
    }

    @Override
    public void starting(org.springframework.boot.ConfigurableBootstrapContext bootstrapContext) {
        Trace.log("runListener.starting");
    }

    @Override
    public void environmentPrepared(org.springframework.boot.ConfigurableBootstrapContext bootstrapContext,
                                   ConfigurableEnvironment environment) {
        Trace.log(
                "runListener.environmentPrepared",
                "property.trace.added-by-envpp",
                environment.getProperty("trace.added-by-envpp")
        );
    }

    @Override
    public void contextPrepared(ConfigurableApplicationContext context) {
        Trace.log("runListener.contextPrepared", "context", context.getClass().getSimpleName());
    }

    @Override
    public void contextLoaded(ConfigurableApplicationContext context) {
        Trace.log("runListener.contextLoaded", "beanDefinitionCount", context.getBeanFactory().getBeanDefinitionCount());
    }

    @Override
    public void started(ConfigurableApplicationContext context, Duration timeTaken) {
        Trace.log("runListener.started", "timeTaken", timeTaken);
    }

    @Override
    public void ready(ConfigurableApplicationContext context, Duration timeTaken) {
        Trace.log("runListener.ready", "timeTaken", timeTaken);
    }

    @Override
    public void failed(ConfigurableApplicationContext context, Throwable exception) {
        Trace.log("runListener.failed", "ex", exception.getClass().getName(), "message", exception.getMessage());
    }
}
