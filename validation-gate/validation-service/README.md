# Validation Service

Spring Boot service for the validation gate.

This service owns the gate-side validation endpoint and scan-record batching. It is intentionally separate from `../../ticket-web-app/backend`.

## Endpoint

- `POST /api/gate/validate-ticket`

The scanner posts ticket/card scan records here. The service stores records in `tap_event` for retryable batch delivery to Level 4.

## Run

```bash
./mvnw spring-boot:run
```

Default URL: `http://localhost:8081/api`

## Configuration

- `VALIDATION_SERVICE_PORT` - defaults to `8081`
- `VALIDATION_DATASOURCE_URL` - defaults to `jdbc:postgresql://localhost:5432/ticket_app`
- `VALIDATION_DATASOURCE_USERNAME` - defaults to `postgres`
- `VALIDATION_DB_PASSWORD` or `DB_PASSWORD`
- `LEVEL4_BASE_URL`
- `LEVEL4_SCAN_RECORD_BATCH_PATH` - defaults to `/scan-record/batch`
- `LEVEL4_MOCK_ENABLED` - defaults to `true`
