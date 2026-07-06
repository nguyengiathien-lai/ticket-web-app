package com.validationgate.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.validationgate.entity.StationContextPackage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;

@Repository
public class RedisStationContextPackageRepository
        extends RedisStationPackageRepository<StationContextPackage>
        implements StationContextPackageRepository {

    public RedisStationContextPackageRepository(
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper,
            @Value("${app.cache.validation-package-ttl:12h}") Duration ttl) {
        super(redisTemplate, objectMapper, ttl, StationContextPackage.class, "validation:station-context:station:");
    }

    @Override
    protected String stationCode(StationContextPackage entity) {
        return entity.getStationCode();
    }
}
