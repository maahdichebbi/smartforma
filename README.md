# SmartForma

SmartForma is an intelligent platform for managing professional training and registrations, developed as an ESPRIT Integrated Project.

## Selected functional modules

- **Catalogue de formation**: categories, training offers, chapters, search, filters and available sessions.
- **Inscriptions & sessions**: session planning, capacities, learner registrations and registration status management.

The project intentionally does not implement trainer management, assessments, pedagogical tracking, or certification modules.

## Advanced features

### FIFO waiting list with automatic promotion

When a session is full, a new registration enters a FIFO waiting list. Cancelling a confirmed registration releases a seat and automatically promotes the first learner in the queue. Waiting positions are re-indexed transactionally.

### Explainable MLA recommendations

The embedded content-based recommendation engine analyses learner skills, interests, level and registration history against training tags, titles, descriptions, categories and chapter titles.

`Final score = 0.40 × skills + 0.35 × interests + 0.25 × level`

It returns a score out of 100 with human-readable explanations. This is a deliberate explainable MCDA/content-based approach: no external AI API, Python microservice, or black-box model is required.

## Technology

- Backend: Java 17, Spring Boot, Spring Security, JWT, JPA/Hibernate, Maven, MySQL
- Frontend: Angular 21
- CI: GitHub Actions

## Roles and security

- **LEARNER**: register, sign in, browse the public catalogue, manage their own profile and registrations, and view personal recommendations.
- **ADMIN**: manage categories, formations, chapters, sessions and all registrations.

Passwords use BCrypt hashing. JWT is stateless. Learner-specific APIs use `/me` endpoints, so the backend derives identity from the authenticated token rather than trusting a learner ID from the browser.

## Run locally

### Prerequisites

- JDK 17+
- MySQL running locally
- Node.js 22+ and npm

### Backend

1. Copy `src/main/resources/application-local.properties.example` to `src/main/resources/application-local.properties`.
2. Set a private JWT secret (at least 32 characters) and a local admin password in that ignored file.
3. Run:

```powershell
.\mvnw.cmd spring-boot:run
```

API: `http://localhost:8080`

### Frontend

```powershell
cd smartforma-frontend
npm ci
npm start
```

Application: `http://localhost:4200`

## Validation

```powershell
.\mvnw.cmd test
cd smartforma-frontend
npm run build
```

The GitHub Actions workflow runs Maven tests, an Angular production build, and an API smoke test that verifies the public catalogue, JWT login, protected admin writes, and `/auth/me`.

## Configuration safety

`application-local.properties`, logs, generated frontend output and `node_modules` are ignored by Git. Do not commit passwords, JWT secrets, or database credentials.
