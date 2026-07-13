package com.safar_zone_backend.controller;

import com.safar_zone_backend.dto.ApiResponse;
import com.safar_zone_backend.dto.BookingResponse;
import com.safar_zone_backend.dto.CreateBookingRequest;
import com.safar_zone_backend.services.BookingService;
import com.safar_zone_backend.util.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    // =====================================================
    // CREATE BOOKING
    // =====================================================

    @PostMapping
    public ResponseEntity<ApiResponse<BookingResponse>>
    createBooking(
            @Valid
            @RequestBody
            CreateBookingRequest request
    ) {

        BookingResponse response =
                bookingService
                        .createBooking(request);

        return ResponseEntity.ok(
                ApiResponse.success(
                        response,
                        "Booking created successfully"
                )
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<BookingResponse>>
    getBookingById(
            @PathVariable String id
    ) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        bookingService.getBookingById(id),
                        "Booking fetched successfully"
                )
        );
    }



    @GetMapping("/driver")
    public ResponseEntity<ApiResponse<?>>
    getDriverBookings() {

        return ResponseEntity.ok(

                ApiResponse.success(

                        bookingService
                                .getDriverBookings(),

                        "Driver bookings fetched"
                )
        );
    }
}