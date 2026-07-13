package com.safar_zone_backend.services;

import com.safar_zone_backend.dto.BookingResponse;
import com.safar_zone_backend.dto.CreateBookingRequest;
import com.safar_zone_backend.dto.DriverBookingResponse;
import com.safar_zone_backend.entity.Booking;
import com.safar_zone_backend.entity.TravelPackage;
import com.safar_zone_backend.entity.User;
import com.safar_zone_backend.exception.ResourceNotFoundException;
import com.safar_zone_backend.exception.UnauthorizedAccessException;
import com.safar_zone_backend.repository.BookingRepository;
import com.safar_zone_backend.repository.PackageRepository;
import com.safar_zone_backend.repository.UserRepository;
import com.safar_zone_backend.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepository;

    private final PackageRepository packageRepository;

    private final SecurityUtil securityUtil;

    private final UserRepository userRepository;

    // =====================================================
    // CREATE BOOKING
    // =====================================================

    public BookingResponse createBooking(
            CreateBookingRequest request
    ) {

        String travelerId =
                securityUtil.getCurrentUserId();

        // =====================================================
        // PACKAGE
        // =====================================================

        TravelPackage pkg =
                packageRepository
                        .findById(request.getPackageId())
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Package not found"
                                ));

        // =====================================================
        // TOTAL PRICE
        // =====================================================

        double total =
                pkg.getPrice() *
                        request.getSeats();

        // =====================================================
        // BOOKING
        // =====================================================

        Booking booking =
                Booking.builder()

                        .bookingCode(
                                generateBookingCode()
                        )

                        .travelerId(travelerId)

                        .driverId(pkg.getDriverId())

                        .travelPackage(pkg)

                        .seats(request.getSeats())

                        .totalAmount(total)

                        .couponCode(
                                request.getCouponCode()
                        )

                        .bookingStatus(
                                Booking.BookingStatus.PENDING
                        )

                        .paymentStatus(
                                Booking.PaymentStatus.PENDING
                        )

                        .qrExpiryTime(
                                LocalDateTime.now()
                                        .plusMinutes(10)
                        )

                        .validTill(
                                pkg.getTripDate()
                                        .atStartOfDay()
                                        .plusDays(1)
                        )

                        .build();

        Booking saved =
                bookingRepository.save(booking);

        // CHECK AVAILABLE SEATS

        int availableSeats =
                pkg.getTotalSeats()
                        - pkg.getBookedSeats();

        if(request.getSeats() > availableSeats){

            throw new RuntimeException(
                    "Only "
                            + availableSeats
                            + " seats available"
            );
        }

// UPDATE PACKAGE SEATS

        pkg.setBookedSeats(
                pkg.getBookedSeats()
                        + request.getSeats()
        );

        pkg.setTotalBookings(
                pkg.getTotalBookings()
                        + 1
        );

// PACKAGE FULL

        if(
                pkg.getBookedSeats()
                        >=
                        pkg.getTotalSeats()
        ){
            pkg.setStatus(
                    TravelPackage.PackageStatus.FULL
            );
        }

        packageRepository.save(pkg);

        // =====================================================
        // RESPONSE
        // =====================================================

        return BookingResponse.builder()

                .id(saved.getId())

                .bookingCode(saved.getBookingCode())

                .packageName(pkg.getName())

                .travelerId(saved.getTravelerId())

                .driverId(saved.getDriverId())

                .seats(saved.getSeats())

                .totalAmount(saved.getTotalAmount())

                .paymentStatus(
                        saved.getPaymentStatus().name()
                )

                .bookingStatus(
                        saved.getBookingStatus().name()
                )

                .tripDate(
                        pkg.getTripDate().toString()
                )

                .tripTime(
                        pkg.getTripTime().toString()
                )

                .fromLocation(
                        pkg.getFromLocation()
                )

                .toLocation(
                        pkg.getToLocation()
                )

                .imageUrl(
                        pkg.getImageUrl()
                )

                .build();
    }

    // =====================================================
    // GENERATE BOOKING CODE
    // =====================================================

    private String generateBookingCode() {

        Random random = new Random();

        int number =
                1000 + random.nextInt(9000);

        return "SZ" + number;
    }




    public BookingResponse getBookingById(
            String bookingId
    ) {

        String userId =
                securityUtil.getCurrentUserId();

        Booking booking =
                bookingRepository
                        .findById(bookingId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Booking not found"
                                ));

        // SECURITY CHECK

        if (
                !booking.getTravelerId()
                        .equals(userId)
        ) {

            throw new UnauthorizedAccessException(
                    "Access denied"
            );
        }

        TravelPackage pkg =
                booking.getTravelPackage();

        User traveler =
                userRepository
                        .findById(
                                booking.getTravelerId()
                        )
                        .orElse(null);

        User driver =
                userRepository
                        .findById(
                                pkg.getDriverId()
                        )
                        .orElse(null);

        return BookingResponse.builder()

                .id(booking.getId())

                .bookingCode(
                        booking.getBookingCode()
                )

                .travelerName(
                        traveler != null
                                ? traveler.getName()
                                : "Traveler"
                )

                .travelerEmail(
                        traveler != null
                                ? traveler.getEmail()
                                : ""
                )

                .driverName(
                        driver != null
                                ? driver.getName()
                                : "Driver"
                )

                .driverPhone(
                        driver != null
                                ? driver.getPhone()
                                : ""
                )

                .packageName(
                        pkg.getName()
                )

                .fromLocation(
                        pkg.getFromLocation()
                )

                .toLocation(
                        pkg.getToLocation()
                )

                .tripDate(
                        pkg.getTripDate()
                                .toString()
                )

                .tripTime(
                        pkg.getTripTime()
                                .toString()
                )

                .totalAmount(
                        booking.getTotalAmount()
                )

                .paymentStatus(
                        booking.getPaymentStatus()
                                .name()
                )

                .bookingStatus(
                        booking.getBookingStatus()
                                .name()
                )

                .build();
    }





    public List<DriverBookingResponse>
    getDriverBookings() {

        String driverId =
                securityUtil.getCurrentUserId();

        List<Booking> bookings =
                bookingRepository
                        .findByDriverIdOrderByCreatedAtDesc(
                                driverId
                        );

        return bookings.stream().map(booking -> {

            TravelPackage pkg =
                    booking.getTravelPackage();

            User traveler =
                    userRepository
                            .findById(
                                    booking.getTravelerId()
                            )
                            .orElse(null);

            return DriverBookingResponse
                    .builder()

                    .bookingId(
                            booking.getId()
                    )

                    .packageId(
                            pkg.getId()
                    )

                    .packageName(
                            pkg.getName()
                    )

                    .travelerName(
                            traveler != null
                                    ? traveler.getName()
                                    : "Traveler"
                    )

                    .travelerEmail(
                            traveler != null
                                    ? traveler.getEmail()
                                    : ""
                    )

                    .seats(
                            booking.getSeats()
                    )

                    .totalAmount(
                            booking.getTotalAmount()
                    )

                    .bookingStatus(
                            booking.getBookingStatus()
                                    .name()
                    )

                    .paymentStatus(
                            booking.getPaymentStatus()
                                    .name()
                    )

                    .fromLocation(
                            pkg.getFromLocation()
                    )

                    .toLocation(
                            pkg.getToLocation()
                    )

                    .tripDate(
                            pkg.getTripDate()
                                    .toString()
                    )

                    .imageUrl(
                            pkg.getImageUrl()
                    )

                    .build();

        }).toList();
    }


}