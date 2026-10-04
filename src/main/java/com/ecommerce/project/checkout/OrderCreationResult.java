package com.ecommerce.project.checkout;

import com.ecommerce.project.order.Order;
import com.ecommerce.project.order.OrderItem;

import java.util.List;

record OrderCreationResult(Order order, List<OrderItem> orderItems) {
}
