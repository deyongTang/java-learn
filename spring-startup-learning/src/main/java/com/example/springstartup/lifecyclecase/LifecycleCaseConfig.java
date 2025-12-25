package com.example.springstartup.lifecyclecase;

import com.example.springstartup.trace.Trace;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;

@Configuration
public class LifecycleCaseConfig {
    @Bean(initMethod = "initMethod", destroyMethod = "destroyMethod")
    public CaseSingletonBean caseSingletonBean() {
        Trace.log("@Bean.caseSingletonBean");
        return new CaseSingletonBean();
    }

    @Bean(initMethod = "initMethod", destroyMethod = "destroyMethod")
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public CasePrototypeBean casePrototypeBean() {
        Trace.log("@Bean.casePrototypeBean");
        return new CasePrototypeBean();
    }
}
