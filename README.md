# Ticket Project

This repository is organized into two main product areas:

- `ticket-web-app/` - the main ticket web application.
- `validation-gate/` - the QR scanner and gate validation simulation.

Shared project docs live in `docs/`.

## Structure

```text
ticket-web-app/
  backend/      Spring Boot ticket API
  frontend/     Ticket web frontend placeholder
validation-gate/
  scanner/      Vite/React QR scanner simulator
  validation-service/
                Spring Boot validation API
docs/           API contracts and reference notes
```

## Run With Docker

Copy `.env.example` to `.env`, fill the local secrets, then run:

```bash
docker compose up --build
```

Docker starts the ticket backend from `ticket-web-app/backend` and the validation service from `validation-gate/validation-service`.
