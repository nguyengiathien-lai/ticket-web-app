package com.validationgate.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.validationgate.entity.DeviceConfigPackage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;

@Repository
public class RedisDeviceConfigPackageRepository
        extends RedisStationPackageRepository<DeviceConfigPackage>
        implements DeviceConfigPackageRepository {

    public RedisDeviceConfigPackageRepository(
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper,
            @Value("${app.cache.validation-package-ttl:12h}") Duration ttl) {
        super(redisTemplate, objectMapper, ttl, DeviceConfigPackage.class, "validation:device-config:station:");
    }

    @Override
    protected String stationCode(DeviceConfigPackage entity) {
        return entity.getStationCode();
    }

    @Override
    protected String cacheKey(DeviceConfigPackage entity) {
        return deviceCacheKey(entity.getStationCode(), entity.getDeviceCode());
    }

    @Override
    public java.util.Optional<DeviceConfigPackage> findByStationAndDeviceCode(
            String stationCode, String deviceCode) {
        return findByCacheKey(deviceCacheKey(stationCode, deviceCode));
    }

    private String deviceCacheKey(String stationCode, String deviceCode) {
        if (stationCode == null || stationCode.isBlank() || deviceCode == null || deviceCode.isBlank()) {
            throw new IllegalArgumentException("Station code and device code are required");
        }
        return stationCode.trim() + ":device:" + deviceCode.trim();
    }
}
