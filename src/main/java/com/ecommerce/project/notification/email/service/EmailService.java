package com.ecommerce.project.notification.email.service;

import com.ecommerce.project.cart.Cart;
import com.ecommerce.project.order.Order;
import com.ecommerce.project.auth.User;

public interface EmailService {
    void sendVerificationEmail(User user, String token);
    void sendPasswordResetEmail(User user, String token);
    void sendPasswordChangedEmail(User user);
    void sendOrderConfirmation(Order order);
    void sendShippingUpdate(Order order, String trackingNumber);
    void sendAbandonedCartReminder(User user, Cart cart);
}
