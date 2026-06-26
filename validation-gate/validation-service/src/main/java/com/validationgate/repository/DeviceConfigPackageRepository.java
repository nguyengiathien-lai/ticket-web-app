package com.validationgate.repository;

import com.validationgate.entity.DeviceConfigPackage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DeviceConfigPackageRepository extends JpaRepository<DeviceConfigPackage, Long> {
    Optional<DeviceConfigPackage> findByStationCode(String stationCode);
}
