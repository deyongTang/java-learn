package com.example.springstartup.trace;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.Ordered;

public class TraceApplicationContextInitializer
        implements ApplicationContextInitializer<ConfigurableApplicationContext>, Ordered {
    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        Trace.log(
                "contextInitializer.initialize",
                "contextId",
                applicationContext.getId(),
                "beanDefinitionCount",
                applicationContext.getBeanFactory().getBeanDefinitionCount()
        );
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}

