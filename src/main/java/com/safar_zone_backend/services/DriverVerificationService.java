package com.safar_zone_backend.services;

import com.safar_zone_backend.dto.DriverVerificationRequest;
import com.safar_zone_backend.dto.DriverVerificationResponse;
import com.safar_zone_backend.entity.DriverVerification;
import com.safar_zone_backend.entity.DriverVerificationStatus;
import com.safar_zone_backend.entity.User;
import com.safar_zone_backend.repository.DriverVerificationRepository;
import com.safar_zone_backend.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class DriverVerificationService {

    private final DriverVerificationRepository driverVerificationRepository;
    private final UserRepository userRepository;

// =====================================================
// SUBMIT DRIVER VERIFICATION
// =====================================================

    public DriverVerification submitVerification(
            String userId,
            DriverVerificationRequest request
    ) {

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new EntityNotFoundException("User not found"));

        DriverVerification verification =
                driverVerificationRepository
                        .findByDriverId(userId)
                        .orElse(new DriverVerification());

        verification.setDriverId(userId);
        verification.setFullName(request.getFullName());
        verification.setLicenseNumber(request.getLicenseNumber());
        verification.setLicenseImage(request.getLicenseImage());
        verification.setIdProofImage(request.getIdProofImage());
        verification.setSelfieImage(request.getSelfieImage());

        verification.setStatus(
                DriverVerificationStatus.PENDING
        );

        DriverVerification saved =
                driverVerificationRepository.save(
                        verification
                );

        user.setDriverVerificationStatus(
                DriverVerificationStatus.PENDING
        );

        userRepository.save(user);

        return saved;
    }

// =====================================================
// GET ALL REQUESTS
// =====================================================

    @Transactional(readOnly = true)
    public List<DriverVerification> getAllRequests() {

        return driverVerificationRepository.findAll();
    }

// =====================================================
// VERIFY DRIVER
// =====================================================

    public DriverVerification verifyDriver(
            String adminId,
            String verificationId
    ) {

        DriverVerification verification =
                driverVerificationRepository
                        .findById(verificationId)
                        .orElseThrow(() ->
                                new EntityNotFoundException(
                                        "Verification request not found"
                                ));

        verification.setStatus(
                DriverVerificationStatus.VERIFIED
        );

        DriverVerification saved =
                driverVerificationRepository.save(
                        verification
                );

        User driver =
                userRepository.findById(
                        verification.getDriverId()
                ).orElseThrow(() ->
                        new EntityNotFoundException(
                                "Driver not found"
                        ));

        driver.setDriverVerificationStatus(
                DriverVerificationStatus.VERIFIED
        );

        userRepository.save(driver);

        return saved;
    }

// =====================================================
// REJECT DRIVER
// =====================================================

    public DriverVerification rejectDriver(
            String verificationId
    ) {

        DriverVerification verification =
                driverVerificationRepository
                        .findById(verificationId)
                        .orElseThrow(() ->
                                new EntityNotFoundException(
                                        "Verification request not found"
                                ));

        verification.setStatus(
                DriverVerificationStatus.REJECTED
        );

        DriverVerification saved =
                driverVerificationRepository.save(
                        verification
                );

        User driver =
                userRepository.findById(
                        verification.getDriverId()
                ).orElseThrow(() ->
                        new EntityNotFoundException(
                                "Driver not found"
                        ));

        driver.setDriverVerificationStatus(
                DriverVerificationStatus.REJECTED
        );

        userRepository.save(driver);

        return saved;
    }


    @Transactional(readOnly = true)
    public List<DriverVerificationResponse> getAllDrivers() {

        return driverVerificationRepository.findAll()
                .stream()
                .map(v -> {

                    User user =
                            userRepository.findById(
                                    v.getDriverId()
                            ).orElse(null);

                    return DriverVerificationResponse.builder()
                            .id(v.getId())
                            .driverId(v.getDriverId())
                            .fullName(v.getFullName())
                            .email(
                                    user != null
                                            ? user.getEmail()
                                            : null
                            )
                            .phone(
                                    user != null
                                            ? user.getPhone()
                                            : null
                            )
                            .licenseNumber(
                                    v.getLicenseNumber()
                            )
                            .licenseImage(
                                    v.getLicenseImage()
                            )
                            .idProofImage(
                                    v.getIdProofImage()
                            )
                            .selfieImage(
                                    v.getSelfieImage()
                            )
                            .status(
                                    v.getStatus().name()
                            )
                            .build();

                }).toList();
    }


    @Transactional(readOnly = true)
    public DriverVerification getDriverVerification(
            String driverId
    ) {

        return driverVerificationRepository
                .findByDriverId(driverId)
                .orElseThrow(() ->
                        new EntityNotFoundException(
                                "Verification not found"
                        ));
    }

    @Transactional(readOnly = true)
    public String getVerificationStatus(
            String driverId
    ) {

        DriverVerification verification =
                driverVerificationRepository
                        .findByDriverId(driverId)
                        .orElseThrow(() ->
                                new EntityNotFoundException(
                                        "Verification not found"
                                ));

        return verification.getStatus().name();
    }

    @Transactional(readOnly = true)
    public DriverVerificationResponse
    getDriverById(
            String verificationId
    ) {

        DriverVerification v =
                driverVerificationRepository
                        .findById(
                                verificationId
                        )
                        .orElseThrow(() ->
                                new EntityNotFoundException(
                                        "Verification not found"
                                ));

        User user =
                userRepository
                        .findById(
                                v.getDriverId()
                        )
                        .orElseThrow(() ->
                                new EntityNotFoundException(
                                        "Driver not found"
                                ));

        return DriverVerificationResponse
                .builder()

                .id(v.getId())

                .driverId(v.getDriverId())

                .fullName(v.getFullName())

                .email(user.getEmail())

                .phone(user.getPhone())

                .licenseNumber(
                        v.getLicenseNumber()
                )

                .licenseImage(
                        v.getLicenseImage()
                )

                .idProofImage(
                        v.getIdProofImage()
                )

                .selfieImage(
                        v.getSelfieImage()
                )

                .status(
                        v.getStatus().name()
                )

                .build();
    }


    @Transactional(readOnly = true)
    public List<DriverVerificationResponse>
    getPendingDrivers() {

        return driverVerificationRepository
                .findByStatus(
                        DriverVerificationStatus.PENDING
                )
                .stream()
                .map(v -> {

                    User user =
                            userRepository
                                    .findById(
                                            v.getDriverId()
                                    )
                                    .orElse(null);

                    return DriverVerificationResponse
                            .builder()

                            .id(v.getId())

                            .driverId(v.getDriverId())

                            .fullName(v.getFullName())

                            .email(
                                    user != null
                                            ? user.getEmail()
                                            : null
                            )

                            .phone(
                                    user != null
                                            ? user.getPhone()
                                            : null
                            )

                            .licenseNumber(
                                    v.getLicenseNumber()
                            )

                            .licenseImage(
                                    v.getLicenseImage()
                            )

                            .idProofImage(
                                    v.getIdProofImage()
                            )

                            .selfieImage(
                                    v.getSelfieImage()
                            )

                            .status(
                                    v.getStatus().name()
                            )

                            .build();

                })
                .toList();
    }



}