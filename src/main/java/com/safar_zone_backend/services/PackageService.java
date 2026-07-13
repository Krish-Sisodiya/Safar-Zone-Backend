// src/main/java/com/safar_zone_backend/service/PackageService.java
package com.safar_zone_backend.services;

import com.safar_zone_backend.dto.CreatePackageRequest;
import com.safar_zone_backend.dto.PackageResponse;
import com.safar_zone_backend.dto.PackageStatsResponse;
import com.safar_zone_backend.dto.UpdatePackageRequest;
import com.safar_zone_backend.entity.TravelPackage;
import com.safar_zone_backend.entity.User;
import com.safar_zone_backend.entity.Vehicle;
import com.safar_zone_backend.exception.ResourceNotFoundException;
import com.safar_zone_backend.exception.UnauthorizedAccessException;
import com.safar_zone_backend.repository.PackageRepository;
import com.safar_zone_backend.repository.VehicleRepository;
import com.safar_zone_backend.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PackageService {

    private final PackageRepository packageRepository;
    private final VehicleRepository vehicleRepository;
    private final SecurityUtil securityUtil;  // ✅ Inject security utility

    /**
     * ✅ Create package - Driver can only create with their OWN vehicles
     */
    @Transactional
    public PackageResponse create(CreatePackageRequest req) {
        // ✅ Get authenticated driver's userId
        String driverId = securityUtil.getCurrentUserId();
        log.info("📦 Creating package for driver: {}", driverId);

        // ✅ Verify vehicle belongs to THIS driver (critical security check)
        Vehicle vehicle = vehicleRepository.findByIdAndDriverId(req.getVehicleId(), driverId)
                .orElseThrow(() -> {
                    log.warn("⚠️ Vehicle {} not found or not owned by driver {}", req.getVehicleId(), driverId);
                    return new ResourceNotFoundException("Vehicle not found or access denied");
                });



        // ✅ Build & save package
        User driver =
                vehicle.getDriver();

        TravelPackage pkg = TravelPackage.builder()
                .driverId(driverId)// ✅ Store userId directly (not email)
                .driverName(driver.getName())
                //.driverImage(driver.getProfileImage())
                .driverPhone(driver.getPhone())
                .vehicle(vehicle)
                .name(req.getName())
                .fromLocation(req.getFromLocation())
                .toLocation(req.getToLocation())
                .price(req.getPrice())
                .priceCategory(req.getPriceCategory())
                .totalSeats(req.getTotalSeats())
                .bookedSeats(0)
                .tripDate(LocalDate.parse(req.getTripDate()))
                .tripTime(LocalTime.parse(req.getTripTime()))
                .description(req.getDescription())
                .imageUrl(req.getImageUrl())
                .status(TravelPackage.PackageStatus.UPCOMING)
                .build();

        TravelPackage saved = packageRepository.save(pkg);
        log.info("✅ Package created: {} for driver: {}", saved.getId(), driverId);

        return toResponse(saved, vehicle.getVehicleNumber(), 0L);
    }

    /**
     * ✅ Get ALL packages for authenticated driver ONLY
     */
    public List<PackageResponse> getAllByCurrentDriver() {
        String driverId = securityUtil.getCurrentUserId();
        log.debug("📋 Fetching packages for driver: {}", driverId);

        return packageRepository
                .findByDriverIdAndDeletedFalseOrderByTripDateDesc(driverId)
                .stream()
                .map(pkg -> {
                    long distance = calculateMockDistance(pkg.getFromLocation(), pkg.getToLocation());
                    return toResponse(pkg, pkg.getVehicle().getVehicleNumber(), distance);
                })
                .toList();
    }

    /**
     * ✅ Get SINGLE package by ID - with ownership verification
     */
    public PackageResponse getById(String packageId) {
        String driverId = securityUtil.getCurrentUserId();
        log.debug("🔍 Fetching package: {} for driver: {}", packageId, driverId);

        // ✅ Critical: Verify package belongs to requesting driver
        TravelPackage pkg = packageRepository.findByIdAndDriverId(packageId, driverId)
                .orElseThrow(() -> {
                    log.warn("⚠️ Package {} not found or access denied for driver {}", packageId, driverId);
                    return new ResourceNotFoundException("Package not found");
                });

        long distance = calculateMockDistance(pkg.getFromLocation(), pkg.getToLocation());
        return toResponse(pkg, pkg.getVehicle().getVehicleNumber(), distance);
    }

    /**
     * ✅ Update package - with ownership check
     */
    @Transactional
    public PackageResponse update(String packageId, CreatePackageRequest req) {
        String driverId = securityUtil.getCurrentUserId();

        TravelPackage pkg = packageRepository.findByIdAndDriverId(packageId, driverId)
                .orElseThrow(() -> new ResourceNotFoundException("Package not found"));

        if(pkg.getStatus() != TravelPackage.PackageStatus.UPCOMING){
            throw new UnauthorizedAccessException(
                    "Only upcoming package can be updated"
            );
        }

        if(pkg.getBookedSeats() > 0){

            throw new RuntimeException(
                    "Package already has bookings"
            );
        }

        // ✅ Allow updates only if no bookings yet (business logic)
        if (pkg.getBookedSeats() > 0 && pkg.getStatus() != TravelPackage.PackageStatus.UPCOMING) {
            throw new UnauthorizedAccessException("Cannot update package with active bookings");
        }

        // ✅ Update fields
        pkg.setName(req.getName());
        pkg.setFromLocation(req.getFromLocation());
        pkg.setToLocation(req.getToLocation());
        pkg.setPrice(req.getPrice());
        pkg.setPriceCategory(req.getPriceCategory());
        pkg.setTotalSeats(req.getTotalSeats());
        pkg.setTripDate(LocalDate.parse(req.getTripDate()));
        pkg.setTripTime(LocalTime.parse(req.getTripTime()));
        pkg.setDescription(req.getDescription());
        pkg.setImageUrl(req.getImageUrl());

        TravelPackage updated = packageRepository.save(pkg);
        log.info("✏️ Package updated: {} by driver: {}", packageId, driverId);

        return toResponse(updated, pkg.getVehicle().getVehicleNumber(),
                calculateMockDistance(pkg.getFromLocation(), pkg.getToLocation()));
    }


    @Transactional
    public PackageResponse updatePackage(
            String packageId,
            UpdatePackageRequest req
    ){

        String driverId =
                securityUtil.getCurrentUserId();

        TravelPackage pkg =
                packageRepository
                        .findByIdAndDriverId(
                                packageId,
                                driverId
                        )
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Package not found"
                                ));

        if(pkg.getStatus()
                != TravelPackage.PackageStatus.UPCOMING){

            throw new RuntimeException(
                    "Only upcoming package can be edited"
            );
        }

        if(pkg.getBookedSeats() > 0){

            throw new RuntimeException(
                    "Package already has bookings"
            );
        }

        if(req.getName() != null){
            pkg.setName(req.getName());
        }

        if(req.getImageUrl() != null){
            pkg.setImageUrl(req.getImageUrl());
        }

        if(req.getPrice() != null){
            pkg.setPrice(req.getPrice());
        }

        if(req.getPriceCategory() != null){
            pkg.setPriceCategory(
                    req.getPriceCategory()
            );
        }

        if(req.getTotalSeats() != null){
            pkg.setTotalSeats(
                    req.getTotalSeats()
            );
        }

        if(req.getDescription() != null){
            pkg.setDescription(
                    req.getDescription()
            );
        }

        if(req.getTripDate() != null){
            pkg.setTripDate(
                    LocalDate.parse(
                            req.getTripDate()
                    )
            );
        }

        if(req.getTripTime() != null){
            pkg.setTripTime(
                    LocalTime.parse(
                            req.getTripTime()
                    )
            );
        }

        packageRepository.save(pkg);

        return toResponse(
                pkg,
                pkg.getVehicle()
                        .getVehicleNumber(),
                calculateMockDistance(
                        pkg.getFromLocation(),
                        pkg.getToLocation()
                )
        );
    }

    /**
     * ✅ Delete package - with ownership verification
     */
    @Transactional
    public void delete(String packageId) {
        String driverId = securityUtil.getCurrentUserId();

        // ✅ Verify ownership before delete
        if (!packageRepository.existsByIdAndDriverId(packageId, driverId)) {
            log.warn("⚠️ Delete attempt denied: Package {} not owned by driver {}", packageId, driverId);
            throw new ResourceNotFoundException("Package not found");
        }

        packageRepository.deleteById(packageId);
        log.info("🗑️ Package deleted: {} by driver: {}", packageId, driverId);
    }

    // ==================== 🔹 HELPERS ====================

    private PackageResponse toResponse(TravelPackage pkg, String vehicleNumber, long distance) {
        return PackageResponse.builder()
                .id(pkg.getId())
                .name(pkg.getName())
                .vehicleNumber(vehicleNumber)
                .fromLocation(pkg.getFromLocation())
                .toLocation(pkg.getToLocation())
                .price(pkg.getPrice())
                .priceCategory(pkg.getPriceCategory())
                .totalSeats(pkg.getTotalSeats())
                .bookedSeats(pkg.getBookedSeats())
                .availableSeats(
                        pkg.getTotalSeats()
                                -
                                pkg.getBookedSeats()
                )
                .tripDate(pkg.getTripDate().toString())
                .tripTime(pkg.getTripTime().toString())
                .status(pkg.getStatus().name())
                .distance(distance)
                .description(pkg.getDescription())
                .imageUrl(pkg.getImageUrl())
                .driverId(pkg.getDriverId())
                .driverName(pkg.getDriverName())
               // .driverImage(pkg.getDriverImage())
                .driverPhone(pkg.getDriverPhone())
                .bookingAllowed(

                        pkg.getStatus()
                                ==
                                TravelPackage.PackageStatus.UPCOMING

                                &&

                                pkg.getBookedSeats()
                                        <
                                        pkg.getTotalSeats()

                )
                .build();
    }

    private long calculateMockDistance(String from, String to) {
        // 🗺️ Replace with Google Maps Distance Matrix API later
        return (long) (Math.random() * 400 + 100);
    }


    public List<PackageResponse> getPackages(
            String keyword,
            String status,
            String sortBy,
            String sortDir
    ) {

        String driverId =
                securityUtil.getCurrentUserId();

        List<TravelPackage> packages =
                packageRepository
                        .findByDriverIdAndDeletedFalseOrderByTripDateDesc(
                                driverId
                        );

        // SEARCH

        if(keyword != null && !keyword.isBlank()){

            String search =
                    keyword.toLowerCase();

            packages =
                    packages.stream()
                            .filter(pkg ->

                                    pkg.getName()
                                            .toLowerCase()
                                            .contains(search)

                                            ||

                                            pkg.getFromLocation()
                                                    .toLowerCase()
                                                    .contains(search)

                                            ||

                                            pkg.getToLocation()
                                                    .toLowerCase()
                                                    .contains(search)

                            )
                            .toList();
        }

        // STATUS FILTER

        if(status != null
                && !status.isBlank()
                && !status.equalsIgnoreCase("ALL")) {

            packages =
                    packages.stream()
                            .filter(pkg ->

                                    pkg.getStatus()
                                            .name()
                                            .equalsIgnoreCase(status)

                            )
                            .toList();
        }


        // SORTING

        if(sortBy != null){

            if(sortBy.equals("price")){

                packages =
                        packages.stream()
                                .sorted(
                                        Comparator.comparing(
                                                TravelPackage::getPrice
                                        )
                                )
                                .toList();
            }

            else if(sortBy.equals("tripDate")){

                packages =
                        packages.stream()
                                .sorted(
                                        Comparator.comparing(
                                                TravelPackage::getTripDate
                                        )
                                )
                                .toList();
            }

            else if(sortBy.equals("createdAt")){

                packages =
                        packages.stream()
                                .sorted(
                                        Comparator.comparing(
                                                TravelPackage::getCreatedAt,
                                                Comparator.nullsLast(
                                                        Comparator.naturalOrder()
                                                )
                                        )
                                )
                                .toList();
            }
        }

// DESC ORDER

        if(sortDir != null
                && sortDir.equalsIgnoreCase("desc")){

            packages = new ArrayList<>(packages);

            Collections.reverse(packages);
        }

        return packages
                .stream()
                .map(pkg ->

                        toResponse(
                                pkg,
                                pkg.getVehicle()
                                        .getVehicleNumber(),
                                calculateMockDistance(
                                        pkg.getFromLocation(),
                                        pkg.getToLocation()
                                )
                        )

                )
                .toList();
    }






    // PUBLIC SERVICES

    // ✅ PUBLIC PACKAGES FOR TRAVELERS

    public List<PackageResponse> getAllPackages() {

        return packageRepository
                .findByStatusAndDeletedFalse(
                        TravelPackage.PackageStatus.UPCOMING
                )
                .stream()
                .map(pkg -> {

                    long distance =
                            calculateMockDistance(
                                    pkg.getFromLocation(),
                                    pkg.getToLocation()
                            );

                    return toResponse(
                            pkg,
                            pkg.getVehicle().getVehicleNumber(),
                            distance
                    );
                })
                .toList();
    }


    public PackageResponse
    getPublicPackageById(String id) {

        TravelPackage pkg =
                packageRepository
                        .findById(id)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Package not found"
                                ));

        return toResponse(
                pkg,
                pkg.getVehicle().getVehicleNumber(),
                calculateMockDistance(
                        pkg.getFromLocation(),
                        pkg.getToLocation()
                )
        );
    }


    public PackageStatsResponse getStats() {

        String driverId =
                securityUtil.getCurrentUserId();

        long upcoming =
                packageRepository.countByDriverIdAndStatus(
                        driverId,
                        TravelPackage.PackageStatus.UPCOMING
                );

        long ongoing =
                packageRepository.countByDriverIdAndStatus(
                        driverId,
                        TravelPackage.PackageStatus.ONGOING
                );

        long completed =
                packageRepository.countByDriverIdAndStatus(
                        driverId,
                        TravelPackage.PackageStatus.COMPLETED
                );

        long cancelled =
                packageRepository.countByDriverIdAndStatus(
                        driverId,
                        TravelPackage.PackageStatus.CANCELLED
                );

        return PackageStatsResponse.builder()
                .upcoming(upcoming)
                .ongoing(ongoing)
                .completed(completed)
                .cancelled(cancelled)
                .total(
                        upcoming +
                                ongoing +
                                completed +
                                cancelled
                )
                .build();
    }
}