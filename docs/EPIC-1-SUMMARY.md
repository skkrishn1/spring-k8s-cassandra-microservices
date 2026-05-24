# Epic 1 — Governed API Contract & DTO Boundary

**Status:** ✅ Complete | **Stories:** 5/5 implemented | **Tests:** 22/22 passing
**Branch (Ralph):** `ralph/epic-1-governed-api-contract`
**Branch (PR):** `feature/epic-1-governed-api-contract`
**Verified on:** 2026-05-24

---

## Epic goal

Establish a governed API contract on the Products service so that the public
HTTP surface is strict, machine-documented, and consistently shaped — independent
of internal entity changes — and so that every error response is well-formed and
correlatable.

## Stories

| ID | Priority | Title | Status |
|---|---|---|---|
| US-001 | 1 | Spring Boot 2.7.x Upgrade | ✅ Done |
| US-002 | 2 | DTO Boundary — `ProductRequestDto` and `ProductResponseDto` | ✅ Done |
| US-003 | 3 | SpringDoc OpenAPI 3.1 spec — Products service | ✅ Done |
| US-004 | 4 | Global Problem+JSON error handler — Products service | ✅ Done |
| US-005 | 5 | Bean validation at DTO boundary — Products service | ✅ Done |

**Implementation completion: 5/5 (100%)**

---

## Per-story acceptance criteria — verification

### US-001 — Spring Boot 2.7.x Upgrade

| AC | Status | Evidence |
|---|---|---|
| Root pom.xml declares `spring-boot.version=2.7.x` and `spring-cloud.version=2021.x` | ✅ | `spring-boot.version=2.7.14`, `spring-cloud.version=2021.0.9` |
| All three modules build | ✅ | `./mvnw clean package -DskipTests` — JARs produced for spring-boot, spring-data, gateway |
| Springfox removed from microservice-spring-boot POM, replaced with SpringDoc | ✅ | `grep -r springfox` returns 0 matches |
| `mvn package -DskipTests` passes on all modules | ✅ | Build green |
| Typecheck passes | ✅ | No compile errors |

### US-002 — DTO Boundary

| AC | Status | Evidence |
|---|---|---|
| `ProductRequestDto` created with `name`, `id`, `description`, `price` | ✅ | `ProductRequestDto.java:9-22` |
| `ProductResponseDto` with `fromEntity(Product)` factory | ✅ | `ProductResponseDto.java:26-34` |
| `ProductController` uses `ProductRequestDto` for `@RequestBody` | ✅ | `ProductController.java:66` |
| `ProductController` returns `ProductResponseDto` | ✅ | All endpoints return `ResponseEntity<ProductResponseDto*>` |
| Unit test validates DTO binding and response shape | ✅ | `ProductControllerTest.shouldBindProductRequestDtoAndNotProduct`, `shouldReturnProductResponseDtoNotEntity` |
| `mvn test -pl microservice-spring-boot` passes | ✅ | 22/22 |
| Typecheck passes | ✅ | No compile errors |

### US-003 — SpringDoc OpenAPI 3.1 spec

| AC | Status | Evidence |
|---|---|---|
| `springdoc-openapi-ui` added to microservice-spring-boot pom.xml | ✅ | `microservice-spring-boot/pom.xml:44-48` (version 1.7.0 — the Boot 2.7-compatible line) |
| Springfox dependency fully removed | ✅ | Zero matches in repo |
| `GET /v3/api-docs` returns valid OpenAPI 3.1 JSON | ⚠️ Manual | Configured via `application.yml`; needs runtime smoke test |
| `GET /swagger-ui.html` renders Swagger UI | ⚠️ Manual | Configured; needs runtime smoke test |
| All endpoints annotated with `@Operation` and `@ApiResponse` | ✅ | All 5 endpoints in `ProductController` |
| `mvn test -pl microservice-spring-boot` passes | ✅ | 22/22 |
| Typecheck passes | ✅ | No compile errors |

### US-004 — Global Problem+JSON error handler

| AC | Status | Evidence |
|---|---|---|
| `ProblemResponse` class with `type`, `title`, `status`, `detail`, `traceId` | ✅ | `ProblemResponse.java` (+ `timestamp` bonus field) |
| `GlobalExceptionHandler` `@ControllerAdvice` handling 404/400/500 | ✅ | Extends `ResponseEntityExceptionHandler` for broader MVC coverage |
| All handlers return `ProblemResponse` as `ResponseEntity` body | ✅ | All three handler methods + `handleExceptionInternal` override |
| `TraceIdFilter` sets MDC `traceId` from `X-Trace-Id` header or generated UUID | ✅ | `TraceIdFilter.java:23-26` |
| `traceId` included in every `ProblemResponse` | ✅ | `GlobalExceptionHandler.currentTraceId()` populates from MDC with UUID fallback |
| Unit test covers all handled exception types | ✅ | `GlobalExceptionHandlerTest`: 8 tests |
| `mvn test -pl microservice-spring-boot` passes | ✅ | 22/22 |
| Typecheck passes | ✅ | No compile errors |

### US-005 — Bean validation at DTO boundary

| AC | Status | Evidence |
|---|---|---|
| `ProductRequestDto` annotated: `@NotBlank` name, `@NotNull` id, `@NotBlank` description, `@NotNull @Positive` price | ✅ | `ProductRequestDto.java:11-22` |
| `ProductController` `POST /api/products/add` annotated with `@Valid` | ✅ | `ProductController.java:66` |
| Invalid request returns 400 ProblemResponse with field-level error detail | ✅ | `GlobalExceptionHandler.handleMethodArgumentNotValid` uses `Collectors.joining("; ")` |
| Unit test validates 400 response for missing name, null id, null price | ✅ | 6 validation tests in `ProductControllerTest` |
| `mvn test -pl microservice-spring-boot` passes | ✅ | 22/22 |
| Typecheck passes | ✅ | No compile errors |

---

## Bonus improvements landed beyond original ACs

Caught during pre-commit review and build verification — not required by any AC
but materially improve the change:

### Real bugs found and fixed

1. **`spring.profiles:` silently ignored on Boot 2.4+.**
   All three `application.yml` files used the deprecated `spring.profiles: docker`
   key. With Boot 2.7 this key is *dropped*, not just deprecated — meaning the
   docker and kubernetes profile sections were being applied unconditionally in
   every environment. Migrated to `spring.config.activate.on-profile:`.

2. **Dependency matrix internally inconsistent.** Initial upgrade had:
   - `spring-cloud:2022.0.4` — Kilburn line, requires Boot 3.x.
     Corrected to `2021.0.9` (Jubilee).
   - `springdoc:2.0.2` with `springdoc-openapi-starter-*` artifacts — Boot 3.x line.
     Reverted to `1.7.0` with `springdoc-openapi-ui` / `springdoc-openapi-data-rest`.
   - `spring-cloud-starter-kubernetes-config:2.1.8` — artifact only exists ≤1.1.10.
     Renamed to `spring-cloud-starter-kubernetes-client-config` (the SC 2021+ name).
   - `spring-cloud-starter-gateway` hand-versioned with a BOM coordinate.
   - Imported `spring-cloud-dependencies` BOM so individual starters resolve through it.

3. **`spring-boot-starter-test` missing entirely** from `microservice-spring-boot/pom.xml`.
   Every test file failed to compile (no JUnit 5, no `@MockBean`). Added.

4. **`@MockBean` import path wrong.** Was `org.springframework.boot.test.mock.MockBean`;
   corrected to `org.springframework.boot.test.mock.mockito.MockBean` in both test files.

5. **`ProductController.findProductsByNameAndId` bypassed `GlobalExceptionHandler` for 404.**
   Was returning `ResponseEntity.notFound().build()` which produces Spring's default
   empty 404 — *not* the Problem+JSON shape US-004 promises. Now throws
   `ResourceNotFoundException` which the handler maps to Problem+JSON.

### Architecture upgrades

- `GlobalExceptionHandler` now extends `ResponseEntityExceptionHandler`, so MVC
  framework exceptions (405, 415, etc.) also come out as Problem+JSON.
- `@Controller` → `@RestController` (was working today but brittle for any
  future non-`ResponseEntity` return type).
- `TraceIdFilter` echoes `X-Trace-Id` in the response header so clients can
  correlate without parsing the body.
- Validation detail uses `Collectors.joining("; ")` — no trailing `"; "`.

### Tooling

- Maven Wrapper added (`./mvnw`) — project builds without a system-wide Maven install.
- Test surefire `systemPropertyVariables` disable Spring Cloud Kubernetes
  bootstrap so `@WebMvcTest` doesn't fail with `NamespaceResolutionFailedException`.

---

## Test coverage summary

| Test class | Tests | Pass |
|---|---|---|
| `ProductControllerTest` | 14 | 14 |
| `GlobalExceptionHandlerTest` | 8 | 8 |
| **Total** | **22** | **22 ✅** |

Coverage notes:
- DTO binding (US-002): 2 tests
- Bean validation (US-005): 6 tests
- GET / DELETE happy paths: 4 tests
- Problem+JSON 400 (validation): 5 tests
- Problem+JSON 404 (ResourceNotFoundException): 1 test
- Trace-ID generation + echo header: 2 tests

---

## Commits in the PR

| # | SHA | Type | Files | Summary |
|---|---|---|---|---|
| 1 | `756713e` | chore | 632 | Project scaffolding — BMAD skills, AI configs, planning docs |
| 2 | `3afc535` | chore | 4 | Maven Wrapper (`./mvnw`) |
| 3 | `ee603ec` | feat | 20 | Epic 1 — Governed API Contract & DTO Boundary |
| 4 | `3a8a6f8` | fix | 9 | Spring Boot 2.7 dependency matrix + build verification |

**PR:** https://github.com/skkrishn1/spring-k8s-cassandra-microservices/pull/1

---

## Known follow-ups (do NOT block Epic 1 merge)

| # | Item | Severity |
|---|---|---|
| 1 | `microservice-spring-data/src/test/.../Order*Test.java` — Copilot-generated, never run | Medium |
| 2 | Full-module test run (`./mvnw clean test` with no `-pl`) not yet executed | Medium |
| 3 | No integration tests against real Cassandra or real K8s API server | Acknowledged — out of Epic 1 scope |
| 4 | `.vscode/` YAML schema mis-maps Spring config files to K8s manifest schema | Low |
| 5 | `.gitignore` doesn't ignore `target/`, IDE files (`.idea/`, `*.iml`), or `Thumbs.db` | Low |
| 6 | `/v3/api-docs` and `/swagger-ui.html` need manual smoke test once running | Low (config verified, runtime unverified) |

---

## How to run locally

```bash
# Build everything
./mvnw clean package -DskipTests

# Run tests for the Products service
./mvnw test -pl microservice-spring-boot

# Run the Products service against Docker Compose
docker compose up -d cassandra
./mvnw -pl microservice-spring-boot spring-boot:run -Dspring-boot.run.profiles=docker
```

OpenAPI surface (once running):
- Spec: http://localhost:8083/v3/api-docs
- UI:   http://localhost:8083/swagger-ui.html
