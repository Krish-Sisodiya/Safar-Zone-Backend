package com.safar_zone_backend.controller;

import com.safar_zone_backend.dto.ApiResponse;
import com.safar_zone_backend.dto.PaymentOrderResponse;
import com.safar_zone_backend.dto.VerifyPaymentRequest;
import com.safar_zone_backend.services.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    // =====================================================
    // CREATE ORDER
    // =====================================================

    @PostMapping("/create-order/{bookingId}")
    public ResponseEntity<ApiResponse<PaymentOrderResponse>>
    createOrder(
            @PathVariable
            String bookingId
    ) throws Exception {

        PaymentOrderResponse response =
                paymentService
                        .createOrder(bookingId);

        return ResponseEntity.ok(
                ApiResponse.success(
                        response,
                        "Order created successfully"
                )
        );
    }

    // =====================================================
    // VERIFY PAYMENT
    // =====================================================

    @PostMapping("/verify")
    public ResponseEntity<ApiResponse<String>>
    verifyPayment(
            @RequestBody
            VerifyPaymentRequest request
    ) throws Exception {

        paymentService.verifyPayment(request);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "SUCCESS",
                        "Payment verified"
                )
        );
    }
}