# Validation Service

Spring Boot service for the validation gate.

This service owns the gate-side validation endpoint and scan-record batching. It is intentionally separate from `../../ticket-web-app/backend`.
It also listens for the Level 4 midnight device information package over RabbitMQ and stores the received package locally.

## Endpoint

- `POST /api/gate/validate-ticket`

The scanner posts ticket/card scan records here. The service validates the QR with the latest local device package, checks local media access rules, and stores accepted records in `tap_event` for retryable batch delivery to Level 4.

```json
{
  "qrPayload": "AFCQR:v1:CARD-000001:TICKET-000001:exp=1765432100:hmac=...",
  "deviceCode": "gate-device-1",
  "stationCode": "METRO-001-ST-001",
  "eventType": "TAP_IN"
}
```

## Run

```bash
./mvnw spring-boot:run
```

Default URL: `http://localhost:8081/api`

## Configuration

- `VALIDATION_SERVICE_PORT` - defaults to `8081`
- `VALIDATION_DATASOURCE_URL` - defaults to embedded H2 at `./data/validation-gate`; Docker overrides it to PostgreSQL
- `VALIDATION_DATASOURCE_USERNAME` - defaults to `postgres`
- `VALIDATION_DB_PASSWORD` or `DB_PASSWORD`
- `DEVICE_CODE` - gate device code, defaults to `gate-device-1`
- `DEVICE_STATION_CODE` - station subscription key, defaults to `station-1`
- `RABBITMQ_HOST`, `RABBITMQ_PORT`, `RABBITMQ_USERNAME`, `RABBITMQ_PASSWORD`
- `DEVICE_PACKAGE_EXCHANGE` - topic exchange for Level 4 packages, defaults to `level4.device.packages`
- `LEVEL4_BASE_URL`
- `LEVEL4_SCAN_RECORD_BATCH_PATH` - defaults to `/scan-record/batch`
- `LEVEL4_MOCK_ENABLED` - defaults to `true`

## Level 4 Device Package

At midnight, Level 4 should publish a combined JSON package to the topic exchange with routing key `device.{stationCode}`. The validation service declares and consumes queue `device.{DEVICE_STATION_CODE}` and stores:

- `deviceConfig` in `device_config_package`
- `stationContext` in `station_context_package`
- `mediaAccessRules` in `media_access_rules_package`

Expected JSON shape:

```json
{
  "publishedAt": "2026-06-25T00:00:00Z",
  "deviceConfig": {
    "version": 13,
    "maxOfflineSeconds": 60,
    "allowOfflineValidation": true,
    "deviceTypes": ["QR_SCANNER_SIMULATOR"],
    "qrVerificationAlgorithm": "HMAC_SHA256",
    "qrVerificationKey": "base64url-encoded-secret",
    "qrMaxTtlSeconds": 60,
    "maxClockDriftSeconds": 60,
    "heartbeatIntervalSeconds": 30
  },
  "stationContext": {
    "version": 1,
    "stationCode": "METRO-001-ST-001",
    "stationName": "Ben Thanh",
    "routeCode": "METRO-001",
    "direction": "ENTRY",
    "stationOrder": 1,
    "distance": 0.00,
    "operatorCode": "HCMC-METRO"
  },
  "mediaAccessRules": {
    "version": 5,
    "cardStatusRules": [
      {
        "cardId": "uuid-1",
        "status": "BLACKLISTED",
        "statusReason": "LOST_CARD",
        "updatedAt": "2026-06-25T00:00:00"
      }
    ]
  }
}
```
