package com.example.springstartup.demo;

import com.example.springstartup.trace.Trace;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class DemoRunner implements ApplicationRunner {
    @Override
    public void run(ApplicationArguments args) {
        Trace.log("applicationRunner.run", "args", String.join(" ", args.getSourceArgs()));
    }
}

