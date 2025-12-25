package com.example.springstartup.trace;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.core.PriorityOrdered;
import org.springframework.stereotype.Component;

@Component
public class TraceBeanDefinitionRegistryPostProcessor implements BeanDefinitionRegistryPostProcessor, PriorityOrdered {
    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
        Trace.log("bdRegistryPostProcessor.registry", "beanDefinitionCount", registry.getBeanDefinitionCount());

        if (!registry.containsBeanDefinition("extraRegisteredBean")) {
            BeanDefinition bd = BeanDefinitionBuilder
                    .genericBeanDefinition(TraceExtraRegisteredBean.class)
                    .getBeanDefinition();
            registry.registerBeanDefinition("extraRegisteredBean", bd);
            Trace.log("bdRegistryPostProcessor.registerBeanDefinition", "name", "extraRegisteredBean");
        }
    }

    @Override
    public void postProcessBeanFactory(org.springframework.beans.factory.config.ConfigurableListableBeanFactory beanFactory)
            throws BeansException {
        Trace.log("bdRegistryPostProcessor.beanFactory", "beanDefinitionCount", beanFactory.getBeanDefinitionCount());
    }

    @Override
    public int getOrder() {
        return PriorityOrdered.HIGHEST_PRECEDENCE;
    }
}

