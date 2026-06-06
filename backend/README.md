# Ticket Backend

Spring Boot backend for the Ticket Web Application.

## Prerequisites

- Java 17+
- Maven 3.8+
- PostgreSQL 12+

## Setup

1. Install PostgreSQL and create database:
```sql
CREATE DATABASE ticket_app;
```

2. Update database credentials in `src/main/resources/application.yml`

3. Build the project:
```bash
mvn clean install
```

4. Run the application:
```bash
mvn spring-boot:run
```

The application will start on `http://localhost:8080/api`

## Health Check

```bash
curl http://localhost:8080/api/health
```

## Project Structure

```
backend/
├── src/
│   ├── main/
│   │   ├── java/com/ticketapp/
│   │   │   ├── config/          # Spring configurations
│   │   │   ├── controller/      # REST controllers
│   │   │   ├── entity/          # JPA entities
│   │   │   ├── dto/             # Data Transfer Objects
│   │   │   ├── repository/      # Data repositories
│   │   │   ├── service/         # Business logic
│   │   │   └── TicketBackendApplication.java
│   │   └── resources/
│   │       ├── application.yml
│   │       └── application-dev.yml
│   └── test/
└── pom.xml
```

## Development

- The application runs in development profile by default
- Database schema is auto-created/updated by Hibernate
- Lombok is used to reduce boilerplate code
