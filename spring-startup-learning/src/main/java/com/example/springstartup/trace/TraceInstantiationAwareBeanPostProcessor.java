package com.example.springstartup.trace;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.InstantiationAwareBeanPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;

@Component
public class TraceInstantiationAwareBeanPostProcessor implements InstantiationAwareBeanPostProcessor, Ordered {
    @Override
    public Object postProcessBeforeInstantiation(Class<?> beanClass, String beanName) throws BeansException {
        if (!beanClass.getName().startsWith("com.example.springstartup.")) {
            return null;
        }
        if (beanName.contains("trace")) {
            return null;
        }
        Trace.log(
                "instantiationAware.beforeInstantiation",
                "beanName",
                beanName,
                "beanClass",
                beanClass.getSimpleName()
        );
        return null;
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
