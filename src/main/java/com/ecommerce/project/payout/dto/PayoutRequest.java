package com.ecommerce.project.payout.dto;

import jakarta.validation.constraints.Size;

// reference: the bank transfer / UTR id the admin used to pay the seller (optional).
public record PayoutRequest(@Size(max = 255) String reference) {}
