package com.validationgate.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.validationgate.entity.BaseEntity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

abstract class RedisStationPackageRepository<T extends BaseEntity> {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final Duration ttl;
    private final Class<T> entityType;
    private final String keyPrefix;

    protected RedisStationPackageRepository(
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper,
            @Value("${app.cache.validation-package-ttl:12h}") Duration ttl,
            Class<T> entityType,
            String keyPrefix) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.ttl = ttl;
        this.entityType = entityType;
        this.keyPrefix = keyPrefix;
    }

    public Optional<T> findByStationCode(String stationCode) {
        return findByCacheKey(stationCode);
    }

    protected Optional<T> findByCacheKey(String cacheKey) {
        String json = redisTemplate.opsForValue().get(key(cacheKey));
        if (json == null || json.isBlank()) {
            return Optional.empty();
        }
        try {
            T entity = objectMapper.readValue(json, entityType);
            afterRead(entity);
            return Optional.of(entity);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to read validation package cache entry: " + key(cacheKey), exception);
        }
    }

    public T save(T entity) {
        LocalDateTime now = LocalDateTime.now();
        if (entity.getCreatedAt() == null) {
            entity.setCreatedAt(now);
        }
        entity.setUpdatedAt(now);

        try {
            redisTemplate.opsForValue().set(key(cacheKey(entity)), objectMapper.writeValueAsString(entity), ttl);
            return entity;
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to write validation package cache entry: " + key(cacheKey(entity)), exception);
        }
    }

    protected void afterRead(T entity) {
    }

    protected abstract String stationCode(T entity);

    protected String cacheKey(T entity) {
        return stationCode(entity);
    }

    private String key(String cacheKey) {
        if (cacheKey == null || cacheKey.isBlank()) {
            throw new IllegalArgumentException("Validation package cache key is required");
        }
        return keyPrefix + cacheKey.trim();
    }
}
