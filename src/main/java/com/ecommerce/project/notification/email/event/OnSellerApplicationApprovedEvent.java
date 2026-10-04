package com.ecommerce.project.notification.email.event;

import com.ecommerce.project.seller.SellerApplication;
import org.springframework.context.ApplicationEvent;

public class OnSellerApplicationApprovedEvent extends ApplicationEvent {

    private final SellerApplication application;

    public OnSellerApplicationApprovedEvent(Object source, SellerApplication application) {
        super(source);
        this.application = application;
    }

    public SellerApplication getApplication() {
        return application;
    }
}
