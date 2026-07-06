package com.validationgate.repository;

import com.validationgate.entity.StationContextPackage;

import java.util.Optional;

public interface StationContextPackageRepository {
    Optional<StationContextPackage> findByStationCode(String stationCode);

    StationContextPackage save(StationContextPackage packageEntity);
}
