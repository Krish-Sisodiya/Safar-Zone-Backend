package com.safar_zone_backend.services;

import com.safar_zone_backend.entity.TravelPackage;
import com.safar_zone_backend.repository.PackageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PackageStatusScheduler {

    private final PackageRepository packageRepository;

    @Scheduled(fixedRate = 60000)
    @Transactional
    public void updatePackageStatus() {

        LocalDateTime now = LocalDateTime.now();

        List<TravelPackage> packages =
                packageRepository.findByDeletedFalse();

        for (TravelPackage pkg : packages) {

            LocalDateTime startTime =
                    LocalDateTime.of(
                            pkg.getTripDate(),
                            pkg.getTripTime()
                    );

            LocalDateTime endTime =
                    startTime.plusHours(24);

            if (now.isAfter(startTime)
                    && now.isBefore(endTime)
                    && pkg.getStatus() ==
                    TravelPackage.PackageStatus.UPCOMING) {

                pkg.setStatus(
                        TravelPackage.PackageStatus.ONGOING
                );
            }

            if (now.isAfter(endTime)
                    && pkg.getStatus() !=
                    TravelPackage.PackageStatus.COMPLETED) {

                pkg.setStatus(
                        TravelPackage.PackageStatus.COMPLETED
                );

                pkg.setCompletedAt(now);
            }
        }

        packageRepository.saveAll(packages);
    }


    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void softDeletePackages() {

        LocalDateTime cutoff =
                LocalDateTime.now().minusDays(30);

        List<TravelPackage> packages =
                packageRepository.findByDeletedFalse();

        for (TravelPackage pkg : packages) {

            if (pkg.getStatus() ==
                    TravelPackage.PackageStatus.COMPLETED

                    && pkg.getCompletedAt() != null

                    && pkg.getCompletedAt().isBefore(cutoff)) {

                pkg.setDeleted(true);
                pkg.setDeletedAt(LocalDateTime.now());
            }
        }

        packageRepository.saveAll(packages);
    }
}