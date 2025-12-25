package com.example.springstartup.lifecyclecase;

import com.example.springstartup.trace.Trace;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

public class CasePrototypeBean implements InitializingBean, DisposableBean {
    private final String id = Integer.toHexString(System.identityHashCode(this));

    public CasePrototypeBean() {
        Trace.log("casePrototype.ctor", "id", id);
    }

    @PostConstruct
    public void postConstruct() {
        Trace.log("casePrototype.@PostConstruct", "id", id);
    }

    @Override
    public void afterPropertiesSet() {
        Trace.log("casePrototype.afterPropertiesSet", "id", id);
    }

    public void initMethod() {
        Trace.log("casePrototype.initMethod", "id", id);
    }

    public String id() {
        Trace.log("casePrototype.id", "id", id);
        return id;
    }

    @PreDestroy
    public void preDestroy() {
        Trace.log("casePrototype.@PreDestroy", "id", id);
    }

    @Override
    public void destroy() {
        Trace.log("casePrototype.destroy(DisposableBean)", "id", id);
    }

    public void destroyMethod() {
        Trace.log("casePrototype.destroyMethod", "id", id);
    }
}

