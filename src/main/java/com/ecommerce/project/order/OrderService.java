package com.ecommerce.project.order;

import com.ecommerce.project.order.dto.FulfillmentUpdateDTO;
import com.ecommerce.project.order.dto.OrderDTO;
import com.ecommerce.project.order.dto.OrderItemDTO;
import com.ecommerce.project.order.dto.OrderResponse;

public interface OrderService {

    OrderDTO getOrderByIdForUser(String emailId, Long orderId);

    OrderItemDTO requestReturn(String emailId, Long orderId, Long orderItemId, String reason);


    OrderResponse getOrdersForUser(String emailId, Integer pageNumber, Integer pageSize, String sortBy, String sortOrder);

    // status: optional OrderStatus name to filter by; null/blank = every order.
    OrderResponse getAllOrders(Integer pageNumber, Integer pageSize, String sortBy, String sortOrder, String status);

    OrderDTO updateOrderStatusAsAdmin(Long orderId, String status);

    OrderResponse getAllSellerOrders(Integer pageNumber, Integer pageSize, String sortBy, String sortOrder);

    OrderItemDTO updateFulfillmentStatusAsSeller(Long sellerId, Long orderItemId, FulfillmentUpdateDTO update);

    OrderItemDTO updateFulfillmentStatusAsAdmin(Long orderItemId, FulfillmentUpdateDTO update);
}
