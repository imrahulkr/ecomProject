package com.ecommerce.project.order.dto;

import com.ecommerce.project.order.FulfillmentStatus;

public record FulfillmentUpdateDTO(FulfillmentStatus status, String trackingNumber, String carrier) {}
