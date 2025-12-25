package com.example.springstartup.demo;

import com.example.springstartup.trace.Trace;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

@Component
public class DemoLifecycleBean implements InitializingBean, DisposableBean {
    public DemoLifecycleBean() {
        Trace.log("demoLifecycleBean.ctor");
    }

    @PostConstruct
    public void postConstruct() {
        Trace.log("demoLifecycleBean.@PostConstruct");
    }

    @Override
    public void afterPropertiesSet() {
        Trace.log("demoLifecycleBean.afterPropertiesSet");
    }

    @PreDestroy
    public void preDestroy() {
        Trace.log("demoLifecycleBean.@PreDestroy");
    }

    @Override
    public void destroy() {
        Trace.log("demoLifecycleBean.destroy");
    }
}

