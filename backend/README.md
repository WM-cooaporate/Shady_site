# Shady backend

Spring Boot 3.5 / Java 17 API for the Shady landing page. See [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) for
the module layout, the API surface and the design decisions.

> Work in progress: this is step 2 (project setup, shared infrastructure, auth/security, audit).
> Content modules, media, Docker and the full deployment guide come in later steps.

## Run locally

Requirements: JDK 17+, Maven 3.9+, PostgreSQL 16.

```bash
cp .env.example .env            # fill in DB_*, JWT_SECRET, ADMIN_EMAIL, ADMIN_INITIAL_PASSWORD; COOKIE_SECURE=false
set -a && source .env && set +a
mvn spring-boot:run             # profile "dev" by default; Swagger UI at http://localhost:8080/swagger-ui.html
```

On first start the admin account is created from `ADMIN_EMAIL` / `ADMIN_INITIAL_PASSWORD`.

## Tests

```bash
mvn test      # unit tests
mvn verify    # unit + integration tests (starts PostgreSQL via Testcontainers, needs Docker)
```

Without Docker, point the integration tests at a disposable database instead:

```bash
SHADY_TEST_DB_URL=jdbc:postgresql://localhost:5432/shady_test SHADY_TEST_DB_USERNAME=postgres mvn verify
```

## Frontend integration (auth)

1. `POST /api/v1/auth/login` `{email, password}` → `{accessToken, expiresAt, expiresIn}` + cookies
   `shady_rt` (HttpOnly refresh token) and `XSRF-TOKEN` (readable by JS).
2. Call admin endpoints with `Authorization: Bearer <accessToken>`. Keep the token in memory only.
3. Before the token expires (or after a 401), call `POST /api/v1/auth/refresh` with
   `credentials: 'include'` and header `X-XSRF-TOKEN: <value of the XSRF-TOKEN cookie>`.
   Run one refresh at a time: replaying an already-rotated refresh token ends the session.
4. `POST /api/v1/auth/logout` (same header) ends the session.
