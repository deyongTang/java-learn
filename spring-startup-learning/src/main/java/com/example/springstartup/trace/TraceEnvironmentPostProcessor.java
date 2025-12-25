package com.example.springstartup.trace;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.Map;

public class TraceEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {
    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        Trace.log("envPostProcessor.enter", "activeProfiles", String.join(",", environment.getActiveProfiles()));

        MapPropertySource ps = new MapPropertySource(
                "traceEnvironmentPostProcessor",
                Map.of("trace.added-by-envpp", "true")
        );
        environment.getPropertySources().addFirst(ps);

        Trace.log(
                "envPostProcessor.exit",
                "trace.added-by-envpp",
                environment.getProperty("trace.added-by-envpp"),
                "spring.main.web-application-type",
                environment.getProperty("spring.main.web-application-type")
        );
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}

