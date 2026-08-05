package com.ecommerce.project.notification.email.event;

import com.ecommerce.project.seller.SellerApplication;
import org.springframework.context.ApplicationEvent;

public class OnSellerApplicationRejectedEvent extends ApplicationEvent {

    private final SellerApplication application;

    public OnSellerApplicationRejectedEvent(Object source, SellerApplication application) {
        super(source);
        this.application = application;
    }

    public SellerApplication getApplication() {
        return application;
    }
}
