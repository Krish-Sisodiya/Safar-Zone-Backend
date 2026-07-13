package com.safar_zone_backend.services;

import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.safar_zone_backend.dto.PaymentOrderResponse;
import com.safar_zone_backend.dto.VerifyPaymentRequest;
import com.safar_zone_backend.entity.Booking;
import com.safar_zone_backend.entity.TravelPackage;
import com.safar_zone_backend.entity.User;
import com.safar_zone_backend.exception.ResourceNotFoundException;
import com.safar_zone_backend.repository.BookingRepository;
import com.safar_zone_backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final BookingRepository bookingRepository;

    private final UserRepository userRepository;

    private final PDFService pdfService;

    private final EmailService emailService;


    @Value("${razorpay.key.id}")
    private String razorpayKey;

    @Value("${razorpay.key.secret}")
    private String razorpaySecret;

    // =====================================================
    // CREATE ORDER
    // =====================================================

    public PaymentOrderResponse createOrder(
            String bookingId
    ) throws Exception {

        Booking booking =
                bookingRepository
                        .findById(bookingId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Booking not found"
                                ));

        RazorpayClient razorpay =
                new RazorpayClient(
                        razorpayKey,
                        razorpaySecret
                );

        JSONObject options =
                new JSONObject();

        options.put(
                "amount",
                booking.getTotalAmount() * 100
        );

        options.put(
                "currency",
                "INR"
        );

        options.put(
                "receipt",
                booking.getBookingCode()
        );

        Order order =
                razorpay.orders.create(options);

        // SAVE ORDER ID

        booking.setRazorpayOrderId(
                order.get("id")
        );

        bookingRepository.save(booking);

        return PaymentOrderResponse
                .builder()

                .bookingId(
                        booking.getId()
                )

                .bookingCode(
                        booking.getBookingCode()
                )

                .razorpayOrderId(
                        order.get("id")
                )

                .amount(
                        booking.getTotalAmount()
                )

                .currency("INR")

                .key(razorpayKey)

                .build();
    }

    // =====================================================
    // VERIFY PAYMENT
    // =====================================================

    public void verifyPayment(
            VerifyPaymentRequest request
    ) throws Exception {

        Booking booking =
                bookingRepository
                        .findById(
                                request.getBookingId()
                        )
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Booking not found"
                                ));

        // =====================================================
        // TEMP SUCCESS
        // =====================================================

        booking.setPaymentId(
                request.getRazorpayPaymentId()
        );

        booking.setPaymentStatus(
                Booking.PaymentStatus.PAID
        );

        booking.setBookingStatus(
                Booking.BookingStatus.CONFIRMED
        );



        User traveler =
                userRepository
                        .findById(
                                booking.getTravelerId()
                        )
                        .orElseThrow();

        User driver =
                userRepository
                        .findById(
                                booking.getDriverId()
                        )
                        .orElseThrow();

        TravelPackage pkg =
                booking.getTravelPackage();

// =====================================================
// PDF DATA
// =====================================================

        Map<String, Object> data =
                new HashMap<>();

        data.put(
                "bookingCode",
                booking.getBookingCode()
        );

        data.put(
                "travelerName",
                traveler.getName()
        );

        data.put(
                "travelerEmail",
                traveler.getEmail()
        );

        data.put(
                "driverName",
                driver.getName()
        );

        data.put(
                "driverPhone",
                driver.getPhone()
        );

        data.put(
                "packageName",
                pkg.getName()
        );

        data.put(
                "fromLocation",
                pkg.getFromLocation()
        );

        data.put(
                "toLocation",
                pkg.getToLocation()
        );

        data.put(
                "tripDate",
                pkg.getTripDate().toString()
        );

        data.put(
                "tripTime",
                pkg.getTripTime().toString()
        );

        data.put(
                "seats",
                booking.getSeats()
        );

        data.put(
                "paymentStatus",
                booking.getPaymentStatus().name()
        );

        data.put(
                "totalAmount",
                booking.getTotalAmount()
        );

        try {

            byte[] pdf =
                    pdfService.generateTicket(data);

            emailService.sendTicketEmail(
                    traveler.getEmail(),
                    traveler.getName(),
                    pdf
            );

        } catch (Exception e) {

            e.printStackTrace();
        }


        bookingRepository.save(booking);
    }
}