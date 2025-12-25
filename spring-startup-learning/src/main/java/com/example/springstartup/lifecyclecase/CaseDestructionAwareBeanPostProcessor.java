package com.example.springstartup.lifecyclecase;

import com.example.springstartup.trace.Trace;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.DestructionAwareBeanPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;

@Component
public class CaseDestructionAwareBeanPostProcessor implements DestructionAwareBeanPostProcessor, Ordered {
    @Override
    public void postProcessBeforeDestruction(Object bean, String beanName) throws BeansException {
        if (!bean.getClass().getName().startsWith("com.example.springstartup.lifecyclecase.")) {
            return;
        }
        Trace.log("destructionAware.beforeDestruction", "beanName", beanName, "type", bean.getClass().getSimpleName());
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
}

