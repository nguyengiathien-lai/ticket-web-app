package com.validationgate.repository;

import com.validationgate.entity.StationContextPackage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StationContextPackageRepository extends JpaRepository<StationContextPackage, Long> {
    Optional<StationContextPackage> findByStationCode(String stationCode);
}
