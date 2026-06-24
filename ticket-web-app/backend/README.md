# Ticket Backend

Spring Boot backend for the Ticket Web App.

## Prerequisites

- Java 17+
- Maven 3.8+
- PostgreSQL 12+

## Setup

1. Install PostgreSQL and create the database:

```sql
CREATE DATABASE ticket_app;
```

2. Update database credentials in `src/main/resources/application.yml`.

3. Build the project:

```bash
mvn clean install
```

4. Run the application:

```bash
mvn spring-boot:run
```

The application starts at `http://localhost:8080/api`.

## Health Check

```bash
curl http://localhost:8080/api/health
```

## Project Structure

```text
backend/
  src/
    main/
      java/com/ticketapp/
        config/          Spring configuration
        controller/      REST controllers
        entity/          JPA entities
        dto/             Data transfer objects
        repository/      Data repositories
        service/         Business logic
        TicketBackendApplication.java
      resources/
        application.yml
        application-dev.yml
    test/
  pom.xml
```

Gate validation endpoints are currently hosted here and consumed by `../../validation-gate/scanner`.

## Development

- The application runs in the development profile by default.
- Database schema is auto-created/updated by Hibernate.
- Lombok is used to reduce boilerplate code.
