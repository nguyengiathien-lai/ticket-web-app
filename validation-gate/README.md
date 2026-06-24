# Validation Gate

This folder contains the gate-side validation simulation.

## Parts

- `scanner/` - Vite/React QR scanner simulator.
- `validation-service/` - ownership notes for the validation service. The runnable API is currently implemented in the Spring Boot backend at `../ticket-web-app/backend` through:
  - `com.ticketapp.controller.GateController`
  - `com.ticketapp.service.GateValidationService`
  - `/api/gate/validate-ticket`

The scanner calls the validation API and sends ticket/card scan records for processing.

## Scanner

```bash
cd scanner
npm install
npm run dev
```

Set `VITE_API_BASE_URL` in `scanner/.env` if the backend is not running at `http://localhost:8080`.
