# Validation Service

This folder marks the validation service boundary for the validation gate.

The runnable implementation currently lives in the shared Spring Boot backend at `../../ticket-web-app/backend`:

- `src/main/java/com/ticketapp/controller/GateController.java`
- `src/main/java/com/ticketapp/service/GateValidationService.java`
- `src/main/java/com/ticketapp/dto/gate/`
- `src/main/java/com/ticketapp/entity/GateEvent.java`
- `src/main/java/com/ticketapp/repository/GateEventRepository.java`

The scanner posts scans to `/api/gate/validate-ticket`.
