package com.example.springstartup.lifecyclecase;

import com.example.springstartup.trace.Trace;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

@Component
@RequestScope
public class CaseRequestBean {
    private final String id = Integer.toHexString(System.identityHashCode(this));

    public CaseRequestBean() {
        Trace.log("caseRequest.ctor", "id", id);
    }

    @PostConstruct
    public void postConstruct() {
        Trace.log("caseRequest.@PostConstruct", "id", id);
    }

    public String id() {
        Trace.log("caseRequest.id", "id", id);
        return id;
    }

    @PreDestroy
    public void preDestroy() {
        Trace.log("caseRequest.@PreDestroy", "id", id);
    }
}
