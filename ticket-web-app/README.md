# Ticket Web App

This folder contains the passenger/admin ticket web application.

## Parts

- `backend/` - Spring Boot API for accounts, authentication, catalog sync, ticket/card purchase, history, and integration with upstream systems.
- `frontend/` - Vite React frontend for the passenger/admin ticket web application.

## Backend

Run from this folder:

```bash
cd backend
mvn spring-boot:run
```

The backend serves API routes under `http://localhost:8080/api`.
