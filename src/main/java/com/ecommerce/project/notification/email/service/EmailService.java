package com.ecommerce.project.notification.email.service;

import com.ecommerce.project.cart.Cart;
import com.ecommerce.project.order.Order;
import com.ecommerce.project.order.OrderItem;
import com.ecommerce.project.auth.User;
import com.ecommerce.project.seller.SellerApplication;

public interface EmailService {
    void sendVerificationEmail(User user, String token);
    void sendPasswordResetEmail(User user, String token);
    void sendPasswordChangedEmail(User user);
    void sendOrderConfirmation(Order order);
    void sendPaymentFailedEmail(Order order, String reason);
    void sendShippingUpdate(OrderItem orderItem);
    void sendDeliveryConfirmation(OrderItem orderItem);
    void sendAbandonedCartReminder(User user, Cart cart);
    void sendSellerApplicationApprovedEmail(SellerApplication application);
    void sendSellerApplicationRejectedEmail(SellerApplication application);
}
