package com.example.springstartup;

import com.example.springstartup.trace.Trace;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class SpringStartupLearningApplication {
    public static void main(String[] args) {
        Trace.log("main.enter");
        SpringApplication.run(SpringStartupLearningApplication.class, args);
        Trace.log("main.exit");
    }
}

