package com.example.springstartup.trace;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;

@Component
public class TraceBeanPostProcessor implements BeanPostProcessor, Ordered {
    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        if (shouldSkip(beanName, bean)) {
            return bean;
        }
        Trace.log("beanPostProcessor.beforeInit", "beanName", beanName, "type", bean.getClass().getSimpleName());
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (shouldSkip(beanName, bean)) {
            return bean;
        }
        Trace.log("beanPostProcessor.afterInit", "beanName", beanName, "type", bean.getClass().getSimpleName());
        return bean;
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }

    private boolean shouldSkip(String beanName, Object bean) {
        if (!bean.getClass().getName().startsWith("com.example.springstartup.")) {
            return true;
        }
        return beanName.contains("trace");
    }
}
