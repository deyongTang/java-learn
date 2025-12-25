package com.example.springstartup.lifecyclecase;

import com.example.springstartup.trace.Trace;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/case")
public class LifecycleCaseController {
    private final CaseSingletonBean singletonBean;
    private final ObjectProvider<CasePrototypeBean> prototypeProvider;
    private final AutowireCapableBeanFactory beanFactory;
    private final CaseRequestBean requestBean;

    public LifecycleCaseController(CaseSingletonBean singletonBean,
                                   ObjectProvider<CasePrototypeBean> prototypeProvider,
                                   AutowireCapableBeanFactory beanFactory,
                                   CaseRequestBean requestBean) {
        this.singletonBean = singletonBean;
        this.prototypeProvider = prototypeProvider;
        this.beanFactory = beanFactory;
        this.requestBean = requestBean;
    }

    @GetMapping("/singleton")
    public String singleton() {
        Trace.log("http.GET /case/singleton");
        return singletonBean.hello();
    }

    @GetMapping("/prototype")
    public String prototype() {
        Trace.log("http.GET /case/prototype");
        CasePrototypeBean bean = prototypeProvider.getObject();
        return "prototype id=" + bean.id() + " (note: container will NOT auto-destroy prototypes)";
    }

    @GetMapping("/prototype/manual-destroy")
    public String prototypeManualDestroy() {
        Trace.log("http.GET /case/prototype/manual-destroy");
        CasePrototypeBean bean = prototypeProvider.getObject();
        String id = bean.id();
        beanFactory.destroyBean(bean);
        return "prototype id=" + id + " destroyed manually";
    }

    @GetMapping("/request")
    public String request() {
        Trace.log("http.GET /case/request");
        return "requestScope id=" + requestBean.id();
    }
}

