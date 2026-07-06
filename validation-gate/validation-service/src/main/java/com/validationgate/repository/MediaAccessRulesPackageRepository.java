package com.validationgate.repository;

import com.validationgate.entity.MediaAccessRulesPackage;

import java.util.Optional;

public interface MediaAccessRulesPackageRepository {
    Optional<MediaAccessRulesPackage> findByStationCode(String stationCode);

    MediaAccessRulesPackage save(MediaAccessRulesPackage packageEntity);
}
