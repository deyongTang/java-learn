package com.example.springstartup.lifecyclecase;

import com.example.springstartup.trace.Trace;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

public class CaseSingletonBean implements BeanNameAware, BeanFactoryAware, ApplicationContextAware,
        InitializingBean, DisposableBean {
    private String beanName;

    public CaseSingletonBean() {
        Trace.log("caseSingleton.ctor");
    }

    @Override
    public void setBeanName(String name) {
        this.beanName = name;
        Trace.log("caseSingleton.BeanNameAware", "beanName", name);
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        Trace.log("caseSingleton.BeanFactoryAware", "beanFactory", beanFactory.getClass().getSimpleName());
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        Trace.log("caseSingleton.ApplicationContextAware", "context", applicationContext.getClass().getSimpleName());
    }

    @PostConstruct
    public void postConstruct() {
        Trace.log("caseSingleton.@PostConstruct", "beanName", beanName);
    }

    @Override
    public void afterPropertiesSet() {
        Trace.log("caseSingleton.afterPropertiesSet", "beanName", beanName);
    }

    public void initMethod() {
        Trace.log("caseSingleton.initMethod", "beanName", beanName);
    }

    public String hello() {
        Trace.log("caseSingleton.hello", "beanName", beanName);
        return "hello from " + beanName;
    }

    @PreDestroy
    public void preDestroy() {
        Trace.log("caseSingleton.@PreDestroy", "beanName", beanName);
    }

    @Override
    public void destroy() {
        Trace.log("caseSingleton.destroy(DisposableBean)", "beanName", beanName);
    }

    public void destroyMethod() {
        Trace.log("caseSingleton.destroyMethod", "beanName", beanName);
    }
}

