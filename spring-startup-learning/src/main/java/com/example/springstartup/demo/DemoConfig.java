package com.example.springstartup.demo;

import com.example.springstartup.trace.Trace;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DemoConfig {
    @Bean(initMethod = "init", destroyMethod = "destroy")
    public DemoInitMethodBean demoInitMethodBean() {
        Trace.log("@Bean.demoInitMethodBean");
        return new DemoInitMethodBean();
    }
}

