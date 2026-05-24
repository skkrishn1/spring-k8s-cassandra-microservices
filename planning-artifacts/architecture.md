---
stepsCompleted: ['step-01-init', 'step-02-context', 'step-03-starter', 'step-04-decisions', 'step-05-patterns']
inputDocuments: ['planning-artifacts/prd.md', 'planning-artifacts/project-context.md']
workflowType: 'architecture'
project_name: 'Spring Microservices with Apache Cassandra and Kubernetes'
user_name: 'SK'
date: '2026-04-18'
---

# Architecture Decision Document

_This document builds collaboratively through step-by-step discovery. Sections are appended as we work through each architectural decision together._

## Project Context Analysis

### Requirements Overview

**Functional Requirements (38 total — 34 Phase 1, 4 Phase 2):**
Six capability areas drive the architecture:
- API Contract Management (FR01–FR08): OpenAPI 3.1 spec generation, Swagger UI, ReDoc, Spectral linting, URI versioning `/api/v1/`, contract diff on PRs
- Testing & Quality (FR09–FR15): Controller unit tests, service unit tests, Testcontainers integration tests, dependency scan gate (Phase 1); BDD AFT + consumer contracts (Phase 2)
- Security & Authorization (FR16–FR23): Provider-agnostic JWT at gateway, ROLE_WRITER RBAC, rate limiting, BOLA + BFLA enforcement
- Error Handling (FR24–FR28): RFC 7807 Problem+JSON globally, error code catalog, traceId, Retry-After on 429
- Data Access & Domain Integrity (FR29–FR34): DTO boundary (request + response), Bean Validation, internal model decoupling, OWASP API3/API6/API9 compliance
- Developer Experience (FR35–FR38): Docker Compose one-command start, runbook, OpenAPI-driven client generation

**Non-Functional Requirements (24 total):**
- Performance: p95 read < 200ms, p95 write < 500ms; CI test suite < 3 min; startup overhead < 2s
- Security: RS256 mandatory (HS256 forbidden); zero critical CVEs at merge; actuator on management port only; security response headers on all authenticated routes
- Scalability: Horizontal scaling supported; ≥10 concurrent Cassandra connections per instance; no Spring Boot 2.3-specific APIs that block 3.x upgrade
- Reliability: Deterministic idempotent Testcontainers; no test execution order dependency; reproducible builds; pinned Spectral and Cassandra image versions; fast-fail on missing env vars
- Integration: Provider-agnostic JWKS; pinned Cassandra image; correct Docker Compose start order; bundled Spectral rules (no CI network calls)

**Scale & Complexity:**
- Primary domain: API Backend / Developer Infrastructure
- Complexity level: High (brownfield, multi-service, Cassandra key-model constraints, full security enforcement, test pyramid introduction)
- Estimated architectural components: ~12 new/modified components

### Technical Constraints & Dependencies

- **Java 11** — no records, sealed classes, or text blocks
- **Spring Boot 2.3.0 → 2.7.x upgrade** is a Phase 1 prerequisite gate (Sprint 1 spike)
- **Two services must stay architecturally distinct**: Products = raw CqlSession + PreparedStatement; Orders = Spring Data CassandraRepository. No cross-contamination.
- **Cassandra key model**: Products (partition: name, clustering: id); Orders (composite PK: orderId + productId). No ALLOW FILTERING.
- **All PreparedStatements** in ProductDao prepared in constructor — never inside query methods
- **Existing API surface preserved**: ports (8083/8081), paths, and response shapes must remain functional through each migration step
- **Gateway is the security perimeter** — JWT validation at gateway only; services trust propagated X-User-Id/X-Roles headers

### Cross-Cutting Concerns Identified

1. **Error handling** — global RFC 7807 Problem+JSON via `@ControllerAdvice` on both services; replaces current silent 500s
2. **JWT identity propagation** — gateway validates, forwards headers; all services consume headers
3. **API versioning** — `/api/v1/` prefix on all routes; enforced via Spectral linting
4. **Testcontainers lifecycle** — shared container strategy vs per-test to meet 3-min CI budget
5. **Cassandra connection management** — dual-path (local vs Astra) must be preserved in both services
6. **DTO mapper layer** — consistent mapping strategy across both services (decision pending: manual vs MapStruct)
7. **traceId propagation** — mechanism choice (MDC/Sleuth/manual) affects all services and gateway uniformly

## Foundation Assessment (Brownfield)

### Primary Technology Domain

API Backend — Java 11 / Spring Boot / Maven multi-module. No external starter template required; the existing codebase is the foundation.

### Existing Foundation

The project already provides:
- **Maven multi-module structure** (3 modules: spring-boot, spring-data, gateway)
- **Cassandra connectivity** — dual-path (local + Astra) in both services, preserved as-is
- **Docker Compose** local stack (3-node Cassandra v3.11.6)
- **Kubernetes manifests** under `deploy/` per namespace
- **Existing API surface** (ports 8083/8081/8080, paths `/api/products/**`, `/api/orders/**`)

### Target Stack Delta (Phase 1 Additions)

| Addition | Scope | Replaces |
|---|---|---|
| Spring Boot 2.7.x | All modules | Spring Boot 2.3.0 |
| SpringDoc OpenAPI 2.x | Both services | Springfox (Products) + SpringDoc 1.3.9 (Orders) |
| Spring Security 5.8 OAuth2 Resource Server | Gateway | None (no auth today) |
| Spring Boot Starter Validation | Both services | None |
| Testcontainers (cassandra, junit5) | Both services (test scope) | None |
| OWASP Dependency Check Maven plugin | Root POM | None |
| Spectral CLI (bundled in repo) | CI / developer tooling | None |

**Note:** The Spring Boot 2.7.x upgrade is Epic 1 / Story 1 — all other Phase 1 work depends on it. A 2-day time-boxed spike in Sprint 1 gates this decision.

## Core Architectural Decisions

### Decision Priority Analysis

**Critical Decisions (Block Implementation):**
- Spring Boot 2.7.x upgrade (Sprint 1 spike gates all Phase 1 work)
- DTO layer introduction (blocks FR29–FR34 and OpenAPI contract generation)
- JWT + RBAC at gateway (blocks FR16–FR23)
- Global error handler + error catalog (blocks FR24–FR28)

**Important Decisions (Shape Architecture):**
- Manual DTO mapper pattern (shapes DTO layer implementation)
- Singleton Testcontainers lifecycle (shapes test harness design)
- MDC trace filter (shapes logging and observability)
- Spring Authorization Server for dev/CI (shapes security testing)

**Deferred Decisions (Phase 2+):**
- BOLA enforcement at service layer (blocked by schema changes)
- MapStruct mapper (deferred; Phase 2 if DTO surface grows)
- Redis-backed rate limiting (in-memory sufficient for Phase 1)
- Micrometer Tracing (deferred to Spring Boot 3.x migration)

### Data Architecture

**Cassandra — existing dual-path preserved:**
- Products service: raw `CqlSession` + `PreparedStatement` — all statements prepared in `ProductDao` constructor, never in query methods
- Orders service: Spring Data `CassandraRepository` + `AbstractCassandraConfiguration` — schema loaded from `orders-schema.cql` classpath resource
- Astra dual-path (local vs Astra) preserved unchanged in both services
- No ALLOW FILTERING; no secondary indexes; queries designed around existing key model

**Testcontainers Lifecycle — Singleton Static Container:**
- One Cassandra container per JVM per `mvn test` execution (singleton `@Container` + `static`)
- CQL init scripts: idempotent (`IF NOT EXISTS`, `INSERT IF NOT EXISTS`)
- Scripts organized in `src/test/resources/cassandra/` — versioned with test code
- Pinned Cassandra image version (no `latest` tag)
- Local dev: `testcontainers.reuse.enable=true` for faster inner-loop feedback
- CI: always fresh container — reproducible and deterministic

**DTO Mapping Strategy — Manual Static Factory Methods:**
- Pattern: `ProductRequestDto.toEntity()`, `Product.toDto()`, `ProductResponseDto.fromEntity()`
- Applied to both services independently — no shared mapper class
- No MapStruct annotation processing in Phase 1 (deferred to Phase 2)
- Rationale: minimal dependency tree, no annotation processor overhead, transparent mapping, Java 11 compatible

### Authentication & Security

**JWT Validation — Spring Security OAuth2 Resource Server at Gateway:**
- Provider-agnostic: configured via `JWKS_URI`, `issuer-uri`, `audience` env vars
- RS256 minimum — HS256 not permitted (NFR-S02)
- Gateway validates token; forwards `X-User-Id` and `X-Roles` headers to services
- Services trust gateway-propagated headers; no re-validation at service level

**RBAC — ROLE_WRITER on Write Endpoints:**
- `GET`: valid JWT required, no role restriction
- `POST`, `PUT`, `DELETE`: require `ROLE_WRITER` claim
- Enforced at gateway route level + validated at service `@PreAuthorize`
- `ROLE_ADMIN` deferred to Phase 2

**Rate Limiting — In-Memory at Gateway:**
- Per-route, per-authenticated-user
- Read: 100 req/min; Write: 20 req/min; Unauthenticated: 0 (all routes require JWT)
- `429 Too Many Requests` + `ERR-AUTH-003` Problem+JSON + `Retry-After` header
- Redis-backed rate limiting deferred to Phase 2

**Local/CI JWT Provider — Spring Authorization Server:**
- Lightweight, Spring-native OAuth2 with full JWT issuance and JWKS support
- No separate JVM required (unlike Keycloak)
- Swap to production provider via Spring profile (`default` = Spring Auth Server; `production` = Keycloak or external IdP via env vars)
- Keycloak documented as production option, not required for dev/CI

**BOLA (API1) — Deferred to Phase 2:**
- Current Cassandra schema has no user-ownership fields (`created_by`, `owner_id`)
- Phase 1: gap documented in architecture and error catalog; no enforcement possible
- Phase 2: add `created_by` field to `products` and `orders` schemas; enforce ownership assertion at service layer using `X-User-Id` header
- No Cucumber BOLA scenarios in Phase 1 (Phase 2)

**Security Headers (NFR-S06):**
- `X-Content-Type-Options: nosniff`
- `X-Frame-Options: DENY`
- `Cache-Control: no-store` on authenticated routes
- Applied at gateway response filter

### API & Communication Patterns

**API Design — REST with URI Versioning:**
- All routes: `/api/v1/` prefix enforced by Spectral linting
- Backward-compatibility rule: breaking changes trigger `/api/v2/` route addition
- Non-breaking additions (new optional fields) are non-versioned changes

**OpenAPI Contract Generation — SpringDoc OpenAPI 2.x:**
- Spec generated from service annotations — not hand-authored
- Products service: Springfox removed, SpringDoc 2.x added
- Orders service: SpringDoc 1.3.9 upgraded to 2.x
- Spec published at `/api/v1/api-docs` on each service; aggregated at gateway
- Swagger UI: `/swagger-ui.html`; ReDoc: `/api/docs`

**Contract Diff — openapi-diff Maven Plugin:**
- Runs during `mvn verify` — no external CI binary required
- Generates HTML/JSON diff artifacts as build outputs
- Fails build on breaking (incompatible) changes
- Non-breaking changes logged as warnings in diff report

**Spectral Linting — Phase 1 Ruleset:**

| Rule | Enforcement |
|------|------------|
| Naming conventions (operationIds camelCase, schema PascalCase) | Fail |
| Versioning — all paths must start with `/v{n}/` | Fail |
| Error schema — all 4xx/5xx must reference Problem+JSON schema | Fail |
| Required fields — `summary` on operations, `description` on schemas | Fail |
| Security — non-GET operations must declare security scheme | Fail |
| Pagination shape — `content[]` + `page{}` where applicable | Fail |

- Rules bundled in repository (`spectral/` folder) — no CI network calls (NFR-I04)
- Spectral CLI pinned version (NFR-R04)
- `additionalProperties: false` and mandatory examples deferred to Phase 2

**Error Handling — RFC 7807 Problem+JSON:**
- Global `@ControllerAdvice` (`GlobalExceptionHandler`) on both services
- Replaces current silent 500s from `row.one()` NPEs
- Standard fields: `type`, `title`, `status`, `detail`, `instance`, `traceId`
- Error codes in `type` URI: `https://api.example.com/errors/{ERR-CODE}`

**Error Catalog — YAML + Constants:**
- Single source of truth: `src/main/resources/error-catalog.yml`
- Thin Java constants class (`ErrorCodes.java`) references codes for compile-time safety
- Discovery endpoint: `GET /api/v1/error-catalog` exposes catalog for client integration
- 10 initial codes defined in PRD (ERR-PRODUCT-001/002/003, ERR-ORDER-001/002, ERR-AUTH-001/002/003, ERR-VALIDATION-001, ERR-INTERNAL-001)

**traceId Propagation — MDC Servlet Filter:**
- `TraceIdFilter` (servlet filter): reads `X-Trace-Id` header or generates UUID if absent
- Stores in MDC (`traceId`) for Logback pattern inclusion
- Forwards `X-Trace-Id` header to downstream services via gateway request filter
- Included in all Problem+JSON error responses as `traceId` extension field
- Clean migration path: replace filter with Micrometer Tracing at Spring Boot 3.x upgrade
- No Spring Cloud Sleuth (deprecated in Spring Boot 3.x)

### Infrastructure & Deployment

**Build:** `mvn verify` is the full quality gate — compiles, tests, Spectral linting, openapi-diff, OWASP dependency check

**Testing Pyramid:**
1. Unit: `@WebMvcTest` (controllers) + `@ExtendWith(MockitoExtension.class)` (services)
2. Integration: Testcontainers singleton Cassandra + CQL init scripts
3. AFT: Cucumber/Gherkin + REST Assured (Phase 2)
4. Contract: Spring Cloud Contract consumer-driven (Phase 2)

**Docker Compose** — local + CI Tier 1:
- Services start in dependency order (Cassandra before app services, NFR-I03)
- Spring Authorization Server added to Docker Compose stack for local JWT issuance
- Profile: `docker` → `host.docker.internal:9042`

**Kubernetes** — production + CI Tier 2 (Phase 2):
- Profile: `kubernetes` → Spring Cloud Kubernetes ConfigMap
- Each service in its own namespace (`spring-boot-service`, `spring-data-service`, `gateway-service`)
- Secrets: `db-secret` (DB creds) + IdP config via ConfigMap

**Actuator:** management port only (8084 products, 8082 orders, 8085 gateway) — never on public API port (NFR-S05)

### Decision Impact Analysis

**Implementation Sequence (derived from dependencies):**
1. Spring Boot 2.7.x upgrade spike → determines all downstream version choices
2. SpringDoc OpenAPI 2.x migration (both services) → unblocks contract generation
3. DTO layer + manual mapper → unblocks FR29–FR34 and clean OpenAPI schema
4. Global error handler + error-catalog.yml → unblocks FR24–FR28
5. MDC trace filter → unblocks FR26 (traceId in all responses)
6. Testcontainers harness + CQL init scripts → unblocks FR11 (integration tests)
7. Unit test layer → unblocks FR09, FR10
8. OWASP Dependency Check plugin → unblocks FR15
9. Spectral linting + openapi-diff → unblocks FR04, FR06
10. URI versioning + deprecation policy → unblocks FR05, FR07
11. Spring Security OAuth2 Resource Server at gateway → unblocks FR16–FR21
12. Spring Authorization Server (Docker Compose) → unblocks JWT-protected local testing
13. RBAC `ROLE_WRITER` → unblocks FR18, FR19, FR22, FR23
14. Rate limiting + security headers → unblocks FR20, FR21 + NFR-S06

**Cross-Component Dependencies:**
- SpringDoc 2.x depends on Spring Boot 2.7.x (cannot upgrade SpringDoc independently)
- DTO layer must be in place before OpenAPI spec generation produces clean schemas
- Error handler must be in place before Spectral error schema rule can be validated
- JWT validation at gateway must be deployed before RBAC at service layer is meaningful
- Testcontainers singleton lifecycle requires idempotent CQL scripts (co-dependent)

## Implementation Patterns & Consistency Rules

### Critical Conflict Points Identified

9 areas where AI agents could make incompatible choices without explicit rules:
1. DTO class naming and package location
2. Static factory method naming on DTOs
3. Custom RFC 7807 response class (Spring Boot 2.7.x — no native ProblemDetail)
4. Exception class naming and hierarchy
5. Testcontainers base class sharing strategy
6. API response shape (wrapper vs direct)
7. HTTP status code usage per operation type
8. Security header extraction pattern at service layer
9. MDC field names for traceId and userId in logs

---

### Naming Patterns

**Cassandra — already established (project-context.md):**
- Table/column names: `snake_case` (`product_name`, `last_updated`, `added_to_order_at`)
- Java field names do NOT need to match column names — use `@Column("column_name")`
- Keyspace: `betterbotz`, local DC: `dc1`

**API Paths:**
- All paths: `kebab-case`, plural nouns: `/api/v1/products`, `/api/v1/orders`
- Path parameters: `camelCase` in Java `@PathVariable`, `kebab-case` in URL: `/api/v1/products/{productId}`
- Query parameters: `camelCase`: `?orderId=`, `?productId=`
- No verbs in paths — HTTP method is the verb

**DTO Class Naming:**
- Request DTOs: `{Domain}RequestDto` → `ProductRequestDto`, `OrderRequestDto`
- Response DTOs: `{Domain}ResponseDto` → `ProductResponseDto`, `OrderResponseDto`
- List/collection responses: `List<{Domain}ResponseDto>` directly — no wrapper class
- Package: `com.datastax.examples.{domain}.dto` (e.g., `com.datastax.examples.product.dto`)
- One request DTO and one response DTO per domain — do not split by use case in Phase 1

**DTO Static Factory Method Names:**

| Direction | Method | Location | Example |
|---|---|---|---|
| Request → Entity | `toEntity()` | on RequestDto class | `productRequestDto.toEntity()` |
| Entity → ResponseDto | `fromEntity(Entity e)` | static on ResponseDto class | `ProductResponseDto.fromEntity(product)` |
| No `toDto()` on entity | — | Entity classes have no DTO knowledge | — |

**Exception Class Naming:**
- Pattern: `{Domain}{Reason}Exception` extends `RuntimeException`
- Examples: `ProductNotFoundException`, `OrderNotFoundException`, `ProductAlreadyExistsException`
- Package: `com.datastax.examples.{domain}.exception`
- One exception class per distinct error scenario — do not reuse across domains

**Java Naming — already established (project-context.md):**
- Classes: `PascalCase`; Methods/variables: `camelCase`; Constants: `UPPER_SNAKE_CASE`
- Test methods: `should{ExpectedBehavior}When{Condition}` (e.g., `shouldReturn404WhenProductNotFound`)

---

### Structure Patterns

**Package Organisation (both services):**
```
com.datastax.examples
├── {domain}/                  # e.g., product/, order/
│   ├── dto/                   # ProductRequestDto, ProductResponseDto
│   ├── exception/             # ProductNotFoundException etc.
│   └── {Domain}.java          # Domain entity (Product, Order)
├── {Domain}Controller.java    # At root of service package, not in domain sub-package
├── {Domain}Service.java       # At root
├── {Domain}Dao.java           # Products service only
├── error/                     # ProblemResponse.java, GlobalExceptionHandler.java, ErrorCodes.java
└── config/                    # SpringBootCassandraConfiguration etc. (already exists)
```

**Test File Organisation:**
- Mirror production package structure under `src/test/java/`
- Controller tests: `{Domain}ControllerTest.java` — `@WebMvcTest`
- Service tests: `{Domain}ServiceTest.java` — `@ExtendWith(MockitoExtension.class)`
- DAO tests: `{Domain}DaoTest.java` — real `CqlSession` via Testcontainers
- Integration base: `AbstractCassandraIntegrationTest.java` — shared singleton container
- CQL scripts: `src/test/resources/cassandra/` — `schema.cql`, `seed-data.cql`

**Testcontainers Base Class — Singleton Pattern:**
```java
// AbstractCassandraIntegrationTest.java (both services)
@Testcontainers
public abstract class AbstractCassandraIntegrationTest {
    @Container
    static CassandraContainer<?> cassandra = new CassandraContainer<>("cassandra:3.11.6")
        .withInitScript("cassandra/schema.cql");
    // shared across all subclasses in same JVM
}
```
- All DAO and integration tests extend this class — never instantiate their own container
- CQL init script: `cassandra/schema.cql` creates keyspace + tables; `cassandra/seed-data.cql` runs separately per test via `@BeforeEach` if needed

**Configuration Files:**
- `spectral/` folder at project root — Spectral ruleset + config bundled in repo
- `error-catalog.yml` in each service's `src/main/resources/` — not shared across services
- `ErrorCodes.java` constants class — one per service, in `error` package

---

### Format Patterns

**API Response Shapes:**

| Operation | HTTP Status | Response Body |
|---|---|---|
| `GET` single resource | `200 OK` | `{Domain}ResponseDto` directly (no wrapper) |
| `GET` list | `200 OK` | `List<{Domain}ResponseDto>` directly |
| `POST` create | `201 Created` | `{Domain}ResponseDto` of created resource |
| `PUT` update | `200 OK` | `{Domain}ResponseDto` of updated resource |
| `DELETE` | `204 No Content` | Empty body |
| Any error (4xx/5xx) | Appropriate code | `ProblemResponse` (RFC 7807) |

**RFC 7807 Error Response — Custom Class (Spring Boot 2.7.x):**

Spring Boot 2.7.x uses Spring Framework 5.3.x — `ProblemDetail` is NOT available (added in Spring Framework 6 / Boot 3.x). Use a custom class:

```java
// ProblemResponse.java — one per service, in error package
public class ProblemResponse {
    private String type;      // "https://api.example.com/errors/ERR-PRODUCT-001"
    private String title;     // "Product Not Found"
    private int status;       // 404
    private String detail;    // "No product found with name 'mobile'"
    private String instance;  // "/api/v1/products/search/mobile/123..."
    private String traceId;   // from MDC
}
```

- Class name: `ProblemResponse` — consistent across both services
- Package: `com.datastax.examples.error` (root-level, not in domain package)
- `GlobalExceptionHandler` class name consistent across both services
- `Content-Type` on error responses: `application/problem+json`

**JSON Field Naming:**
- API JSON fields: `camelCase` always (`productName`, `orderId`, `lastUpdated`)
- Cassandra column names: `snake_case` (mapped via `@Column`)
- All timestamps in JSON: ISO 8601 UTC — `"2026-04-18T12:00:00.000Z"`
- Java type: `Instant` for all Cassandra timestamp fields (never `Date` or `LocalDateTime`)

---

### Communication Patterns

**MDC Fields — Consistent Naming:**

| MDC Key | Value | Set By |
|---|---|---|
| `traceId` | `X-Trace-Id` header value or generated UUID | `TraceIdFilter` |
| `userId` | `X-User-Id` header value (post-auth) | `UserContextFilter` (Phase 2) |

- Logback pattern must include `%X{traceId}` in all log lines
- `traceId` value included in `ProblemResponse.traceId` field

**Security Header Extraction at Service Layer:**
- Services extract identity from gateway-propagated headers, NOT from JWT
- Pattern: `@RequestHeader(value = "X-User-Id", required = false) String userId`
- `required = false` — Phase 1 services don't enforce presence (gateway enforces auth)
- Phase 2: add `@RequestHeader(value = "X-Roles", required = false) String roles` for service-level RBAC validation

**Inter-Service Communication:** None — Products and Orders are independent services. Gateway routes to each. No direct service-to-service HTTP calls.

---

### Process Patterns

**Error Handling Flow:**
```
Request → Controller → Service → DAO
                                  ↓ throws {Domain}Exception
              GlobalExceptionHandler catches
                          ↓
              builds ProblemResponse with:
              - error code from ErrorCodes.java
              - traceId from MDC
              - instance = request URI
              - status = mapped HTTP code
                          ↓
              returns ResponseEntity<ProblemResponse>
              with Content-Type: application/problem+json
```

**Exception → HTTP Status Mapping (GlobalExceptionHandler):**

| Exception | HTTP Status | Error Code |
|---|---|---|
| `ProductNotFoundException` | 404 | `ERR-PRODUCT-001` |
| `ProductAlreadyExistsException` | 409 | `ERR-PRODUCT-002` |
| `OrderNotFoundException` | 404 | `ERR-ORDER-001` |
| `MethodArgumentNotValidException` | 400 | `ERR-VALIDATION-001` |
| `RuntimeException` (catch-all) | 500 | `ERR-INTERNAL-001` |
| Auth exceptions (gateway) | 401/403 | `ERR-AUTH-001/002` |
| Rate limit (gateway) | 429 | `ERR-AUTH-003` |

**Bean Validation Pattern:**
- Validate at controller entry point only — `@Valid` on `@RequestBody` parameter
- Validation annotations on DTO fields — NEVER on entity fields
- Custom messages via `{domain}.{field}.{constraint}` keys in `messages.properties`

**Test Structure (Given/When/Then):**
```java
@Test
void shouldReturn404WhenProductNotFound() {
    // Given
    given(productService.findByName("unknown")).willReturn(Collections.emptyList());

    // When / Then
    mockMvc.perform(get("/api/v1/products/search/unknown"))
           .andExpect(status().isNotFound())
           .andExpect(jsonPath("$.status").value(404))
           .andExpect(jsonPath("$.traceId").exists());

    verify(productService).findByName("unknown");
}
```
- Always `verify()` service calls after assertions
- `@Autowired ObjectMapper` for JSON serialization — never manual JSON strings
- Given/When/Then comments are mandatory in every test method

---

### Enforcement Guidelines

**All AI Agents MUST:**
- Use `ProblemResponse` (not Spring's `ProblemDetail`) for all error responses — Boot 2.7.x
- Name DTO classes `{Domain}RequestDto` / `{Domain}ResponseDto` — no deviation
- Place `fromEntity()` static factory on the ResponseDto class, `toEntity()` on RequestDto
- Extend `AbstractCassandraIntegrationTest` for all DAO + integration tests — never create own container
- Use `@RequestHeader` to extract identity — never re-validate JWT at service layer
- Include `traceId` from MDC in all `ProblemResponse` objects
- Follow the two-service split: no `CqlSession` in spring-data service, no `CassandraRepository` in spring-boot service

**Anti-Patterns — Explicitly Forbidden:**
- `@RequestBody Product product` — entity binding from request body (OWASP API6)
- `return ResponseEntity.ok(product)` — entity in response (OWASP API3)
- `session.execute(...)` inside a query method — must be `PreparedStatement` prepared in constructor
- `ALLOW FILTERING` in any CQL statement
- `ProblemDetail` class — not available in Spring Boot 2.7.x / Spring 5.3.x
- Shared DTO or exception classes across services — each service owns its own
- Test methods without Given/When/Then structure and trailing `verify()`
