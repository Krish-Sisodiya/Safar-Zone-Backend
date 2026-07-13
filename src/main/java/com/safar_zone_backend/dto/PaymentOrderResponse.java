package com.safar_zone_backend.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PaymentOrderResponse {

    private String bookingId;

    private String bookingCode;

    private String razorpayOrderId;

    private Double amount;

    private String key;

    private String currency;
}