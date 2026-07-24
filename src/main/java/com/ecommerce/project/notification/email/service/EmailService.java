package com.ecommerce.project.notification.email.service;

import com.ecommerce.project.model.Cart;
import com.ecommerce.project.model.Order;
import com.ecommerce.project.model.User;

public interface EmailService {
    void sendVerificationEmail(User user, String token);
    void sendPasswordResetEmail(User user, String token);
    void sendOrderConfirmation(Order order);
    void sendShippingUpdate(Order order, String trackingNumber);
    void sendAbandonedCartReminder(User user, Cart cart);
}
