package com.safar_zone_backend.dto;

import lombok.Data;

@Data
public class VerifyPaymentRequest {

    private String bookingId;

    private String razorpayPaymentId;

    private String razorpayOrderId;

    private String razorpaySignature;
}