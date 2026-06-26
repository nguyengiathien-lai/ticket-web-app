package com.validationgate.repository;

import com.validationgate.entity.MediaAccessRulesPackage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MediaAccessRulesPackageRepository extends JpaRepository<MediaAccessRulesPackage, Long> {
    Optional<MediaAccessRulesPackage> findByStationCode(String stationCode);
}
