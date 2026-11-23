# Note-Taking API

A production-style REST backend for a note-taking app, built for the *Desktop Application Programming II – Backend Development* final project.

- **Rich text notes**: HTML content, sanitized on save to block XSS, with a plain-text copy kept for search
- **Categories and tags**: every note has one optional category and any number of tags (Many-to-Many)
- **Search and filtering**: text search, category, tags, pinned/archived, created/updated date ranges, pagination and sorting

## Technology stack

| Area | Choice |
|---|---|
| Language / framework | Java 17+, Spring Boot 3.3 |
| Persistence | Spring Data JPA (Hibernate 6), H2 (dev/test), PostgreSQL (prod) |
| Security | Spring Security 6, JWT access tokens (jjwt), DB-stored refresh tokens, BCrypt |
| Validation | Jakarta Bean Validation + custom validators |
| Docs | springdoc-openapi (Swagger UI) |
| Rich text | jsoup HTML sanitizer |
| Tests | JUnit 5, Mockito, Spring Boot Test / MockMvc, `@DataJpaTest`, JaCoCo |

## Requirements coverage

| Requirement | Where |
|---|---|
| Registration and login, BCrypt, JWT, refresh tokens | `AuthController`, `AuthService`, `JwtService`, `RefreshTokenService` |
| Roles USER / ADMIN, role-specific endpoints | `SecurityConfig` (`/api/admin/**`), `@PreAuthorize` on `AdminController` |
| 3+ entities with CRUD and relationships | `Note`, `Category`, `Tag` (+ `User`, `NoteHistory`, `RefreshToken`) |
| Cascades | `Note → NoteHistory` (ALL + orphanRemoval); category delete un-sets notes; tag delete unlinks notes |
| DTOs only, request and response DTOs | `dto/**` records, `mapper/**` |
| Bean validation and custom validators | `@StrongPassword`, `@HexColor`, `@ValidDateRange` (cross-field) |
| Global exception handling | `GlobalExceptionHandler`, same `ErrorResponse` JSON for every error |
| Indexed, normalized schema | `@Index` on entities, see `docs/schema.sql` |
| Data seeder | `DataSeeder` (dev profile) |
| OpenAPI at `/swagger-ui.html` | `OpenApiConfig`, annotations on controllers and DTOs |
| Unit, integration and repository tests | `src/test/java/**` |

**Additional features (7 implemented, 3 required):**

1. **Pagination and sorting**: `page`, `size` (max 100), multi-field `sort`, with total count metadata (`PageResponse`)
2. **Advanced search and filtering**: dynamic JPA `Specification`s (`NoteSpecifications`), multi-criteria, date ranges
3. **Soft delete**: trash, restore, permanent delete, and admin restore for any user's note
4. **Audit trail**: `createdAt/updatedAt/createdBy/updatedBy` via JPA auditing, plus a per-note change history
5. **Export**: CSV export with the same filters (`/api/notes/export`), protected against spreadsheet formula injection
6. **Scheduled tasks**: nightly purge of old trash and of expired or revoked refresh tokens (`CleanupJobs`)
7. **Advanced validation**: custom annotations, cross-field validation, business rules (e.g. "an archived note cannot be pinned", "admins cannot demote themselves")

## Getting started in VS Code

### Prerequisites
- **JDK 17 or newer** (`java -version`)
- **Maven 3.9+** (`mvn -v`). If you don't have it, install it or use the Maven tooling that comes with the VS Code Java extension pack.
- VS Code extensions (VS Code offers them automatically from `.vscode/extensions.json`):
  - *Extension Pack for Java* (`vscjava.vscode-java-pack`)
  - *Spring Boot Extension Pack* (`vmware.vscode-boot-dev-pack`)
  - *REST Client* (`humao.rest-client`), optional, for `docs/requests.http`

### Run
1. `File → Open Folder…` and pick the `note-taking-api` folder. Wait for "Java: Ready" in the status bar while Maven downloads dependencies the first time.
2. Do one of these:
   - Open the **Run and Debug** panel and start **"Run API (dev, H2)"**, or
   - open `NoteTakingApiApplication.java` and click **Run** above `main`, or
   - in the terminal (`` Ctrl+` ``), run:
     ```bash
     mvn spring-boot:run
     ```
3. Open:
   - Swagger UI: http://localhost:8080/swagger-ui.html
   - OpenAPI JSON: http://localhost:8080/v3/api-docs
   - H2 console (dev only): http://localhost:8080/h2-console (JDBC URL `jdbc:h2:mem:notesdb`, user `sa`, empty password)

### Sample credentials (seeded in the dev profile)

| Username | Password | Role |
|---|---|---|
| `admin` | `Admin@123` | ADMIN |
| `demo` | `Demo@1234` | USER (owns 6 notes, 3 categories, 5 tags) |

In Swagger, call `POST /api/auth/login`, copy `accessToken`, click **Authorize** and paste it.

### Profiles and environment variables

Configuration lives in `src/main/resources/application.properties` (shared) plus one file per profile: `application-dev.properties`, `application-prod.properties`, and `src/test/resources/application-test.properties`.

| Profile | Database | Notes |
|---|---|---|
| `dev` (default) | in-memory H2 | demo data, H2 console, debug logging |
| `test` | in-memory H2 | used by the tests, scheduling disabled |
| `prod` | PostgreSQL | all secrets come from environment variables |

| Variable | Purpose | Default |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | profile | `dev` |
| `JWT_SECRET` | HMAC key, **at least 32 characters** | dev-only value |
| `JWT_ACCESS_EXPIRATION_MS` | access token lifetime | 900000 (15 min) |
| `JWT_REFRESH_EXPIRATION_MS` | refresh token lifetime | 604800000 (7 days) |
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | PostgreSQL connection (prod) | none |
| `CORS_ALLOWED_ORIGINS` | comma-separated frontend origins | localhost:3000, 5173, 4200 |
| `TRASH_RETENTION_DAYS` | days before trash is purged | 30 |
| `SEED_DATA` | seed demo data in prod | false |

Running with PostgreSQL:
```bash
docker run --name notes-db -e POSTGRES_DB=notesdb -e POSTGRES_PASSWORD=postgres -p 5432:5432 -d postgres:16

export SPRING_PROFILES_ACTIVE=prod DB_USERNAME=postgres DB_PASSWORD=postgres \
       JWT_SECRET='replace-with-a-long-random-secret-of-32-plus-chars'
mvn spring-boot:run
```
(Or use the **"Run API (prod, PostgreSQL)"** launch configuration.)

## Running the tests

```bash
mvn test
```
- Coverage report: `target/site/jacoco/index.html`
- In VS Code, the **Testing** panel (flask icon) runs individual tests.

| Type | Classes |
|---|---|
| Unit (Mockito) | `NoteServiceTest`, `CategoryServiceTest`, `TagServiceTest`, `AuthServiceTest`, `RefreshTokenServiceTest`, `UserServiceTest`, `HtmlSanitizerAndCsvTest`, `JwtServiceTest`, `ValidatorsTest` |
| Integration (full app + MockMvc) | `AuthControllerIntegrationTest`, `NoteControllerIntegrationTest` |
| Repository (`@DataJpaTest`) | `NoteRepositoryTest` |

## API overview

All endpoints except `/api/auth/**` need the header `Authorization: Bearer <accessToken>`.

### Auth: `/api/auth` (public)
| Method | Path | Description |
|---|---|---|
| POST | `/register` | Create an account (role USER) and return tokens |
| POST | `/login` | Log in with username or email |
| POST | `/refresh` | New access + refresh token; the old refresh token is revoked |
| POST | `/logout` | Revoke a refresh token |

### Notes: `/api/notes`
| Method | Path | Description |
|---|---|---|
| GET | `/` | Search, filter and paginate (see below) |
| GET | `/{id}` | Full note with HTML content |
| POST | `/` | Create (201 and `Location` header) |
| PUT | `/{id}` | Replace |
| DELETE | `/{id}` | Move to trash (soft delete) |
| GET | `/trash` | List trashed notes |
| POST | `/{id}/restore` | Restore from trash |
| DELETE | `/{id}/permanent` | Delete permanently |
| GET | `/{id}/history` | Change history |
| GET | `/export` | CSV download (same filters as search) |

**Search parameters (all optional):** `q`, `categoryId`, `tags` (repeat or comma-separate; matches any), `pinned`, `archived` (archived notes are hidden by default), `createdFrom`, `createdTo`, `updatedFrom`, `updatedTo` (`yyyy-MM-dd`), `page`, `size`, `sort` (`title`, `createdAt`, `updatedAt`, `pinned`; e.g. `sort=pinned,desc&sort=title,asc`).

```
GET /api/notes?q=meeting&tags=work,urgent&createdFrom=2026-01-01&sort=title,asc&page=0&size=10
```

### Categories: `/api/categories`
`GET /`, `GET /{id}`, `POST /`, `PUT /{id}`, `DELETE /{id}` (notes in a deleted category become uncategorised)

### Tags: `/api/tags`
`GET /`, `GET /{id}`, `POST /`, `PUT /{id}` (rename), `DELETE /{id}` (removed from all notes). Tags named in a note request are also created automatically.

### Users
`GET /api/users/me`: the current user's profile

### Admin: `/api/admin` (ADMIN only)
| Method | Path | Description |
|---|---|---|
| GET | `/users` | List users (paginated) |
| PATCH | `/users/{id}/role` | `{ "role": "ADMIN" }` |
| PATCH | `/users/{id}/status` | `{ "enabled": false }`, which also revokes the user's refresh tokens |
| GET | `/notes/deleted` | Trashed notes of all users |
| POST | `/notes/{id}/restore` | Restore any user's note |

### Error format
Every error has the same shape:
```json
{
  "timestamp": "2026-11-17T13:22:05.120Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/api/notes",
  "validationErrors": { "title": "must not be blank" }
}
```
| Status | When |
|---|---|
| 400 | validation errors, malformed JSON, bad parameters, business rule violations |
| 401 | missing, invalid or expired token, wrong credentials, invalid refresh token |
| 403 | wrong role, disabled account |
| 404 | resource not found, or it belongs to another user |
| 409 | duplicate username, email, category or tag |

## Project structure

```
src/main/java/com/notesapp
├── config/          Security, OpenAPI, JPA auditing, scheduling, properties, data seeder
├── controller/      REST controllers (HTTP only, no business logic)
├── dto/             Request/response records (auth, note, category, tag, user, common)
├── entity/          JPA entities
├── exception/       Custom exceptions + GlobalExceptionHandler
├── mapper/          Entity → DTO mapping
├── repository/      Spring Data repositories
├── scheduler/       Background cleanup jobs
├── security/        JWT service and filter, UserDetails, 401/403 handlers
├── service/         Business logic, sanitizer, CSV export
├── specification/   Dynamic search queries (JPA Criteria)
└── validation/      Custom validation annotations
```

Design notes, useful for the defense:
- **Layered architecture with constructor injection.** Controllers pass the authenticated user's id to services, so services are easy to unit test.
- **Ownership checks are done in the queries** (`findByIdAndOwnerId…`). Another user's note returns 404, not 403, so its id is never confirmed to exist.
- **Stateless JWT.** Access tokens are short-lived. Refresh tokens are random, stored server-side, rotated on every use and revocable.
- **Rich text safety.** HTML is cleaned against an allow-list (jsoup) before saving. Search runs on the plain-text copy, so markup never produces false matches.
- **Soft delete is explicit** (`DeletedFalse` in queries and specifications), which keeps admin restore and trash queries simple.

## Other files
- `postman/Note-Taking-API.postman_collection.json`: Postman collection. Run *Login (demo)* first; the token is saved automatically.
- `docs/requests.http`: the same flows for the VS Code REST Client
- `docs/schema.sql`: PostgreSQL schema and relationship overview

## Git
```bash
git init
git add .
git commit -m "Initial commit: Note-Taking API"
git branch -M main
git remote add origin https://github.com/<your-user>/note-taking-api.git
git push -u origin main
```
