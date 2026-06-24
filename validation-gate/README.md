# Validation Gate

This folder contains the gate-side validation simulation.

## Parts

- `scanner/` - Vite/React QR scanner simulator.
- `validation-service/` - Spring Boot validation API for ticket/card scan records.

The scanner calls the validation API and sends ticket/card scan records for processing.

## Validation Service

```bash
cd validation-service
./mvnw spring-boot:run
```

The validation service runs on `http://localhost:8081/api` by default and exposes:

- `POST /api/gate/validate-ticket`

## Scanner

```bash
cd scanner
npm install
npm run dev
```

Set `VITE_API_BASE_URL` in `scanner/.env` if the validation service is not running at `http://localhost:8081`.
