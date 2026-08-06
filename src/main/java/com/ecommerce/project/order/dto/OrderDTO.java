package com.ecommerce.project.order.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderDTO {
    private Long orderId;
    private String email;
    private List<OrderItemDTO> orderItems;
    private LocalDate orderDate;
    private long amountMinorUnits;
    private String currency;
    private String orderStatus;
    private PaymentDTO payment;
    private Long addressId;
    private Instant createdAt;
    private Instant updatedAt;
}

