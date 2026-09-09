# Online Bookstore

A full-stack online bookstore application, built as two independent projects:

- **bookstore-backend**  REST API built with Spring Boot 4.1.1 (Java 17)
- **bookstore-frontend** React JS application with Typescript

The two projects are decoupled and communicate over HTTP. Each has its own
README with setup instructions.

## Getting started

Run both projects, backend first:

1. [`bookstore-backend/README.md`](./bookstore-backend/README.md) — start the API
2. [`bookstore-frontend/README.md`](./bookstore-frontend/README.md) — start the UI

## Tech stack

| Layer    | Technology                                             |
|----------|--------------------------------------------------------|
| Backend  | Java 17, Spring Boot 4.1.1, Spring Framework 7, Spring Data JPA |
| Database | H2 (local dev), MySQL (production), Flyway migrations  |
| Frontend | React, Axios, Vite                                     |
| Testing  | JUnit 5, Mockito, MockMvc, Spring Boot Test, Vitest, React Testing Library |
| Build    | Maven (backend, via Maven Wrapper no local install needed), npm/Vite (frontend) |

## Repository layout

```
online-bookstore/
bookstore-backend/     Spring Boot REST API
bookstore-frontend/    React application
```

## Note on IDE setup

This project uses Lombok. Building from the command line (`./mvnw`) works
with zero setup. If you open the backend in an IDE, Lombok needs to be
recognized by that IDE's own compiler:

- **IntelliJ IDEA**: built-in support or a one-click plugin
  install (Community) — no separate installer needed.
- **Eclipse**: run `java -jar lombok.jar` (`~/.m2/repository/org/projectlombok/lombok/`, or download from (https://projectlombok.org/download)) and
  follow its installer, then restart Eclipse and do a full Maven project
  update.