package com.example.springstartup.demo;

import com.example.springstartup.trace.Trace;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.stereotype.Component;

@Component
public class DemoSmartInitializingSingleton implements SmartInitializingSingleton {
    @Override
    public void afterSingletonsInstantiated() {
        Trace.log("smartInitializingSingleton.afterSingletonsInstantiated");
    }
}

