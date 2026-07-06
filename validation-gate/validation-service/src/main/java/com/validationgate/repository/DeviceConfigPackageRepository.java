package com.validationgate.repository;

import com.validationgate.entity.DeviceConfigPackage;

import java.util.Optional;

public interface DeviceConfigPackageRepository {
    Optional<DeviceConfigPackage> findByStationCode(String stationCode);

    DeviceConfigPackage save(DeviceConfigPackage packageEntity);
}
