package com.ecommerce.project.order.dto;

import com.ecommerce.project.order.FulfillmentStatus;
import lombok.Data;

@Data
public class FulfillmentUpdateDTO {
    private FulfillmentStatus status;
    private String trackingNumber;
    private String carrier;
}
