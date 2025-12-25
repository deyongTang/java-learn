package com.example.springstartup.demo;

import com.example.springstartup.trace.Trace;

public class DemoInitMethodBean {
    public DemoInitMethodBean() {
        Trace.log("demoInitMethodBean.ctor");
    }

    public void init() {
        Trace.log("demoInitMethodBean.initMethod");
    }

    public void destroy() {
        Trace.log("demoInitMethodBean.destroyMethod");
    }
}

