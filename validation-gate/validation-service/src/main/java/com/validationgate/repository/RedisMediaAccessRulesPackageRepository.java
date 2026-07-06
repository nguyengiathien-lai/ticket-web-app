package com.validationgate.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.validationgate.entity.MediaAccessRulesPackage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;

@Repository
public class RedisMediaAccessRulesPackageRepository
        extends RedisStationPackageRepository<MediaAccessRulesPackage>
        implements MediaAccessRulesPackageRepository {

    public RedisMediaAccessRulesPackageRepository(
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper,
            @Value("${app.cache.validation-package-ttl:12h}") Duration ttl) {
        super(redisTemplate, objectMapper, ttl, MediaAccessRulesPackage.class, "validation:media-access-rules:station:");
    }

    @Override
    protected void afterRead(MediaAccessRulesPackage entity) {
        entity.getCardStatusRules().forEach(rule -> rule.setPackageEntity(entity));
    }

    @Override
    protected String stationCode(MediaAccessRulesPackage entity) {
        return entity.getStationCode();
    }
}
