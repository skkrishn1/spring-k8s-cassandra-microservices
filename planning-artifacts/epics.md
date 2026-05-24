---
stepsCompleted: ['step-01-validate-prerequisites', 'step-02-design-epics', 'step-03-create-stories', 'step-04-final-validation']
inputDocuments: ['planning-artifacts/prd.md', 'planning-artifacts/architecture.md']
---

# Spring Microservices with Apache Cassandra and Kubernetes - Epic Breakdown

## Overview

This document provides the complete epic and story breakdown for Spring Microservices with Apache Cassandra and Kubernetes, decomposing the requirements from the PRD and Architecture into implementable stories.

## Requirements Inventory

### Functional Requirements

FR01: API consumers can discover all available endpoints, schemas, and examples via a machine-readable OpenAPI 3.1 specification
FR02: API consumers can interactively explore and test all endpoints via a browser-based Swagger UI
FR03: API consumers can read structured, human-friendly API documentation via a ReDoc interface
FR04: API producers can validate their OpenAPI spec against naming, pagination, error schema, and versioning rules via automated linting on every build
FR05: API consumers can access all endpoints under a stable, versioned URI path (`/api/v1/`)
FR06: API teams can detect breaking and non-breaking contract changes automatically on every pull request
FR07: API consumers can rely on a documented deprecation policy with sunset dates for versioned endpoints
FR08: API consumers can access all endpoints under an optional header-based versioning scheme (documented, Phase 1 optional)
FR09: Developers can run unit tests for all controllers in complete isolation — no running Cassandra or external dependency required
FR10: Developers can run unit tests for all service-layer components with mocked DAO/repository dependencies
FR11: Developers can run integration tests against a real, containerized Cassandra instance using reproducible seed data
FR12: QA engineers can author BDD test scenarios in plain-language Gherkin feature files without writing Java code *(Phase 2)*
FR13: QA engineers can execute BDD scenarios against a pre-seeded, containerized Cassandra dataset without a live cluster *(Phase 2)*
FR14: API consumers can verify their expected consumer contracts against producer API changes via automated contract testing *(Phase 2)*
FR15: The build pipeline can enforce a zero-critical-CVE gate and fail the build when known vulnerabilities are detected in dependencies
FR16: Authenticated users can access API endpoints using a JWT issued by any RFC 7519-compliant OAuth2 provider — no provider lock-in
FR17: The system can validate JWT authenticity, issuer, and audience without code changes when switching identity providers
FR18: Users holding the `ROLE_WRITER` claim can execute write operations (`POST`, `PUT`, `DELETE`) on both services
FR19: Users without `ROLE_WRITER` are denied write operations and receive a machine-readable error response
FR20: All API routes are protected against excessive request volume on a per-authenticated-user, per-route basis
FR21: Clients that exceed rate limits receive a machine-readable error response including retry guidance
FR22: The system can prevent unauthorized cross-user resource access at the service layer (OWASP API1 — BOLA)
FR23: The system can prevent users from invoking functions beyond their role (OWASP API5 — BFLA)
FR24: API consumers receive a standardized, RFC 7807-compliant `Problem+JSON` response for all error scenarios (4xx and 5xx)
FR25: API consumers can identify the specific error type via a stable, versioned machine-readable error code (e.g., `ERR-PRODUCT-001`)
FR26: Operations teams can correlate errors and requests across services using a propagated trace identifier (`traceId`)
FR27: API consumers can determine appropriate retry behaviour from rate-limit error responses (`Retry-After` header)
FR28: The error catalog is versioned and co-located with the codebase — new error codes are added via code, not manual documentation
FR29: API consumers can submit requests containing only the fields explicitly defined in the public API contract — no internal field binding
FR30: API responses expose only the fields explicitly defined in the public API contract — no internal model leakage
FR31: The system rejects requests with invalid field values at the API boundary and returns a descriptive, machine-readable error (OWASP API6)
FR32: Developers can evolve internal domain models and Cassandra schema without changing the public API contract
FR33: The system prevents exposure of internal entity fields in API responses (OWASP API3)
FR34: The system can prevent undocumented or deprecated API endpoints from being silently accessible (OWASP API9)
FR35: New developers can start all services locally using a single Docker Compose command — no manual Cassandra setup
FR36: New developers can understand expected API behaviour by reading Cucumber feature files — no Java code required *(Phase 2)*
FR37: Operations teams can perform standard operational tasks by following a runbook — no tribal knowledge required
FR38: API consumers can generate typed client libraries in any language from the published OpenAPI specification without SDK involvement from the producing team

### NonFunctional Requirements

NFR-P01: API read endpoints (`GET`) return responses within 200ms at p95 under normal load (single-node local Cassandra or Docker Compose)
NFR-P02: API write endpoints (`POST`, `PUT`, `DELETE`) complete within 500ms at p95 under normal load
NFR-P03: The full unit + integration test suite completes within 3 minutes in CI — slow tests are a build-breaking concern
NFR-P04: AFT suite (Cucumber + Testcontainers, Phase 2) completes within 5 minutes in CI — Testcontainers startup included
NFR-P05: OpenAPI spec generation adds no more than 2 seconds to application startup time
NFR-S01: All API traffic is JWT-authenticated — unauthenticated requests are rejected at the gateway with `401` before reaching downstream services
NFR-S02: JWT validation uses asymmetric signing (RS256 minimum) — symmetric (HS256) is not permitted
NFR-S03: All credentials (`DB_USERNAME`, `DB_PASSWORD`, `JWKS_URI`) are injected via environment variables or K8s secrets — zero secrets in source code or committed config files
NFR-S04: The Maven build fails if any dependency carries a critical (CVSS ≥ 9.0) CVE — high CVEs generate a warning report but do not fail the build
NFR-S05: Actuator endpoints are accessible only on the management port — never exposed on the public API port
NFR-S06: All HTTP responses include security headers: `X-Content-Type-Options: nosniff`, `X-Frame-Options: DENY`, `Cache-Control: no-store` on authenticated routes
NFR-S07: Rate limiting is enforced in-memory at the gateway — no shared state required for Phase 1 (Redis-backed rate limiting deferred to Phase 2+)
NFR-SC01: The system architecture supports horizontal scaling of each service independently — no shared in-process state between service instances
NFR-SC02: Cassandra connection pooling is configured to handle at least 10 concurrent requests per service instance without connection exhaustion
NFR-SC03: Spring Boot upgrade path to 3.x is not blocked by new Phase 1 code — no Spring Boot 2.3-specific APIs introduced
NFR-R01: Testcontainers-based tests are deterministic and idempotent — the same CQL init scripts produce identical results on every run
NFR-R02: No test has a dependency on test execution order — each test sets up and tears down its own state
NFR-R03: The build pipeline is reproducible — given the same source commit, the build produces an identical result on any CI runner
NFR-R04: Spectral linting rules are pinned to a fixed ruleset version — no unexpected rule additions from upstream auto-updates
NFR-R05: Application startup fails fast with a clear error message if required environment variables (`JWKS_URI`, `DB_USERNAME`, `DB_PASSWORD`) are missing
NFR-I01: The OAuth2 Resource Server configuration supports any JWKS-endpoint-compatible provider — switching providers requires only environment variable changes, no code changes
NFR-I02: Testcontainers uses a pinned Cassandra image version — no `latest` tag in test configuration
NFR-I03: The Docker Compose stack starts all services in the correct dependency order (Cassandra before application services) without manual intervention
NFR-I04: Spectral CLI is executable in CI without a network call to an external service — rules are bundled in the repository

### Additional Requirements

- Spring Boot upgrade from 2.3.0 → 2.7.x is a Sprint 1 prerequisite gate (time-boxed 2-day spike); all Phase 1 work depends on this — no Spring Boot 2.3-specific APIs in new code
- Two services must remain architecturally distinct: Products = raw CqlSession + PreparedStatement; Orders = Spring Data CassandraRepository — no cross-contamination
- All PreparedStatements in ProductDao must be prepared in the constructor, never inside query methods — no lazy preparation
- Existing API surface must be preserved through each migration step: ports (8083/8081), paths, and response shapes remain functional during transition
- SpringDoc OpenAPI 2.x replaces Springfox (Products) and SpringDoc 1.3.9 (Orders) — both services must align to the same SpringDoc version
- Global RFC 7807 Problem+JSON via `@ControllerAdvice` on both services — replaces current silent 500s; `ProblemResponse` class (not Spring's `ProblemDetail`) must be used
- DTO layer: `{Domain}RequestDto` / `{Domain}ResponseDto` naming; `fromEntity()` static factory on ResponseDto; `toEntity()` on RequestDto — manual mapper pattern (no MapStruct in Phase 1)
- Gateway is the security perimeter: JWT validated at gateway only; services trust propagated `X-User-Id` / `X-Roles` headers — no re-validation at service layer
- `AbstractCassandraIntegrationTest` base class: singleton Testcontainers lifecycle shared across all DAO and integration tests in a module — never create own container per test class
- traceId propagation: `TraceIdFilter` sets MDC key `traceId` from `X-Trace-Id` header or generated UUID; included in all `ProblemResponse` objects and Logback pattern
- Error catalog versioned in `src/main/resources/error-catalog.yml` — new codes added via code, catalog co-located with codebase
- OWASP Dependency Check Maven plugin added to root POM — fails build on CVSS ≥ 9.0 critical CVEs
- Spectral CLI bundled in repository — no CI network calls; rules pinned to fixed version
- Keycloak (Docker Compose) as local/CI identity provider in Phase 1 — swap via Spring profile for production; zero provider-specific code in application layer
- Rate limiting enforced in-memory at gateway per route (100 req/min read, 20 req/min write per authenticated user); `Retry-After` header on 429
- Security response headers required on all authenticated routes: `X-Content-Type-Options: nosniff`, `X-Frame-Options: DENY`, `Cache-Control: no-store`
- BOLA enforcement: resource-level ownership checks at service layer (guard by `X-User-Id` header); no shared data across users on ownership-scoped endpoints
- BFLA enforcement: method-level RBAC at gateway and at service controller layer; `ROLE_WRITER` required for POST/PUT/DELETE
- Anti-patterns explicitly forbidden: `@RequestBody Product` (entity binding), `return ResponseEntity.ok(product)` (entity in response), `session.execute()` inside query methods, `ALLOW FILTERING`, `ProblemDetail` class, shared DTO/exception classes across services, test methods without Given/When/Then + trailing `verify()`

### UX Design Requirements

N/A — API-only project, no UI layer.

### FR Coverage Map

FR01: Epic 1 — OpenAPI 3.1 spec via SpringDoc
FR02: Epic 1 — Swagger UI on both services
FR03: Epic 1 — ReDoc on both services
FR04: Epic 2 — Spectral linting in CI
FR05: Epic 1 — URI versioning `/api/v1/`
FR06: Epic 2 — OpenAPI contract diff on every PR
FR07: Epic 1 — Deprecation policy in OpenAPI spec
FR08: Epic 1 — Header-based versioning (documented, optional)
FR09: Epic 3 — Controller unit tests (`@WebMvcTest`)
FR10: Epic 3 — Service unit tests (`@ExtendWith(MockitoExtension.class)`)
FR11: Epic 3 — Testcontainers integration tests + CQL init scripts
FR12: Phase 2 — BDD/Gherkin feature file authoring (deferred)
FR13: Phase 2 — BDD scenarios against Testcontainers snapshot (deferred)
FR14: Phase 2 — Consumer contract testing (deferred)
FR15: Epic 3 — OWASP Dependency Check — zero critical CVE gate
FR16: Epic 4 — JWT via OAuth2 Resource Server at gateway
FR17: Epic 4 — Provider-agnostic JWKS config
FR18: Epic 4 — `ROLE_WRITER` grants write access
FR19: Epic 4 — `ROLE_WRITER` denial + Problem+JSON error
FR20: Epic 4 — Per-route, per-user rate limiting
FR21: Epic 4 — 429 + `Retry-After` header
FR22: Epic 4 — BOLA — resource ownership check at service layer
FR23: Epic 4 — BFLA — method-level RBAC enforcement
FR24: Epic 2 — Global `@ControllerAdvice` + Problem+JSON
FR25: Epic 2 — Error code catalog (`ERR-PRODUCT-001`, etc.)
FR26: Epic 2 — `traceId` propagation via MDC
FR27: Epic 2 — `Retry-After` in rate-limit error response model
FR28: Epic 2 — Error catalog versioned in `error-catalog.yml`
FR29: Epic 1 — Request DTO — no entity binding
FR30: Epic 1 — Response DTO — no entity in response
FR31: Epic 1 — Bean Validation at DTO boundary (400 + Problem+JSON)
FR32: Epic 1 — DTO decouples domain evolution from API contract
FR33: Epic 1 — No internal entity fields in responses (OWASP API3)
FR34: Epic 1 — API versioning prevents undocumented access (OWASP API9)
FR35: Epic 5 — Docker Compose one-command local start
FR36: Phase 2 — Cucumber feature files as living docs (deferred)
FR37: Epic 5 — Operations runbook
FR38: Epic 5 — OpenAPI-driven client generation guide

## Epic List

### Epic 1: Governed API Contract & DTO Boundary
Developers and API consumers can trust the API contract — versioned endpoints, machine-readable specs, interactive docs, and a strict DTO boundary that prevents mass assignment and data leakage.
**FRs covered:** FR01, FR02, FR03, FR05, FR07, FR08, FR29, FR30, FR31, FR32, FR33, FR34
**Note:** Spring Boot 2.7.x upgrade is Story 1 of this epic — prerequisite gate for all Phase 1 work.

### Epic 2: API Governance, Error Standards & Observability
Every error path returns a consistent, machine-readable Problem+JSON response with a stable error code and a traceable request ID. Spectral linting enforces contract rules on every PR, and breaking changes are detected automatically.
**FRs covered:** FR04, FR06, FR24, FR25, FR26, FR27, FR28

### Epic 3: Test Pyramid — Unit & Integration Coverage
Developers can run a full, trustworthy test suite (controller unit → service unit → Testcontainers integration) in under 3 minutes, with zero dependency on a live Cassandra cluster. Build fails on critical CVEs.
**FRs covered:** FR09, FR10, FR11, FR15

### Epic 4: Security, Authorization & Rate Protection
All API routes are JWT-protected at the gateway, write operations enforce ROLE_WRITER RBAC, rate limits are applied per route, and BOLA/BFLA protections guard resource access. Security posture is a build artifact — verifiable and auditable.
**FRs covered:** FR16, FR17, FR18, FR19, FR20, FR21, FR22, FR23

### Epic 5: Developer Experience & Operational Readiness
New developers can onboard in under one working day — one Docker Compose command starts everything, a runbook guides operations, and the published OpenAPI spec enables any consumer to generate a typed client without team involvement.
**FRs covered:** FR35, FR37, FR38

---

## Epic 1: Governed API Contract & DTO Boundary

Developers and API consumers can trust the API contract — versioned endpoints, machine-readable specs, interactive docs, and a strict DTO boundary that prevents mass assignment and data leakage.

### Story 1.1: Spring Boot 2.7.x Upgrade

As a developer,
I want the project upgraded from Spring Boot 2.3.0 to 2.7.x,
So that SpringDoc OpenAPI 2.x, Spring Security 5.8, and Testcontainers integration are available without 2.3-specific API constraints.

**Acceptance Criteria:**

**Given** the root `pom.xml` declares `spring-boot.version=2.3.0` and `spring-cloud.version=2.2.3`
**When** the upgrade spike is completed
**Then** all three modules (`microservice-spring-boot`, `microservice-spring-data`, `gateway-service`) build with Spring Boot 2.7.x and compatible Spring Cloud 2021.x
**And** Springfox is removed from the Products service POM and replaced with SpringDoc OpenAPI 2.x
**And** the Orders service SpringDoc dependency is upgraded from 1.3.9 to SpringDoc 2.x (aligned version)
**And** `mvn package -DskipTests` completes successfully on all modules with no deprecated API warnings introduced by new code
**And** all existing integration paths (Cassandra connection, Docker Compose profile, Kubernetes profile) remain functional

### Story 1.2: DTO Boundary — Products Service

As a developer,
I want the Products service to use explicit `ProductRequestDto` and `ProductResponseDto` classes,
So that internal entity fields are never exposed in the API response and consumer inputs are validated at the controller boundary.

**Acceptance Criteria:**

**Given** the Products controller currently binds `@RequestBody Product` and returns `ResponseEntity<Product>`
**When** the DTO layer is introduced
**Then** all controller methods accept `@RequestBody @Valid ProductRequestDto` — `Product` entity is never bound directly from the request
**And** all controller methods return `ResponseEntity<ProductResponseDto>` — `Product` entity is never serialized directly to the response
**And** `ProductResponseDto` has a static `fromEntity(Product p)` factory method
**And** `ProductRequestDto` has a `toEntity()` method returning a `Product`
**And** `ProductRequestDto` fields carry Bean Validation annotations (`@NotBlank`, `@NotNull`, etc.) matching domain constraints
**And** `@Valid` on `@RequestBody ProductRequestDto` rejects invalid input with HTTP 400 before reaching the service layer
**And** no `Product` entity fields absent from the DTO contract appear in any API response

### Story 1.3: DTO Boundary — Orders Service

As a developer,
I want the Orders service to use explicit `OrderRequestDto` and `OrderResponseDto` classes,
So that the `Order` entity and its composite primary key are never exposed directly via the API.

**Acceptance Criteria:**

**Given** the Orders controller and Spring Data REST endpoints currently expose `Order` entity fields directly
**When** the DTO layer is introduced
**Then** all `OrderController` methods accept `@RequestBody @Valid OrderRequestDto` — `Order` entity is never bound directly from request
**And** all `OrderController` methods return `ResponseEntity<OrderResponseDto>` — `Order` entity is never serialized directly
**And** `OrderResponseDto` has a static `fromEntity(Order o)` factory method
**And** `OrderRequestDto` carries Bean Validation annotations matching domain constraints
**And** Spring Data REST auto-generated endpoints are disabled or wrapped — no raw `Order` entity exposed at `/api/orders` via HAL
**And** `@Valid` on `@RequestBody OrderRequestDto` rejects invalid input with HTTP 400 before reaching the repository layer

### Story 1.4: URI Versioning — `/api/v1/` on All Routes

As an API consumer,
I want all endpoints accessible under a stable `/api/v1/` URI prefix,
So that I can build integrations against a versioned contract that won't silently break when the API evolves.

**Acceptance Criteria:**

**Given** the Products service currently exposes endpoints at `/api/products/**`
**When** URI versioning is applied
**Then** all Products endpoints are accessible at `/api/v1/products/**` and the unversioned paths return 404
**And** all Orders endpoints are accessible at `/api/v1/orders/**` and the unversioned paths return 404
**And** the Spring Cloud Gateway route config is updated to forward `/api/v1/products/**` and `/api/v1/orders/**` correctly
**And** no new code introduces endpoints outside the `/api/v1/` prefix
**And** the Kubernetes ConfigMap gateway routes are updated to the `/api/v1/` paths

### Story 1.5: OpenAPI 3.1 Spec, Swagger UI & ReDoc — Both Services

As an API consumer,
I want machine-readable OpenAPI 3.1 specs with Swagger UI and ReDoc on both services,
So that I can discover, explore, and test all available endpoints without reading source code.

**Acceptance Criteria:**

**Given** SpringDoc OpenAPI 2.x is on the classpath (from Story 1.1) and DTOs carry `@Schema` annotations
**When** I navigate to the Products service
**Then** `GET /api/v1/api-docs` returns a valid OpenAPI 3.1 JSON document covering all product endpoints
**And** `GET /swagger-ui.html` renders an interactive Swagger UI listing all product endpoints with request/response schemas derived from DTOs
**And** `GET /api/docs` renders a ReDoc interface for the Products service
**And** the same three endpoints are available on the Orders service
**And** OpenAPI `@Schema` annotations on DTO fields define `required`, `nullable`, and `example` values
**And** OpenAPI spec generation adds no more than 2 seconds to startup time (NFR-P05)

### Story 1.6: Deprecation Policy & Header Versioning Documentation

As an API producer,
I want a documented deprecation policy and optional header-based versioning scheme in the OpenAPI spec,
So that consumers have clear guidance on endpoint lifecycle and can plan for future changes without surprise.

**Acceptance Criteria:**

**Given** the OpenAPI spec is generated by SpringDoc (from Story 1.5)
**When** the deprecation policy is added
**Then** the OpenAPI spec `info` section includes a `x-deprecation-policy` extension documenting the sunset timeline (minimum 90 days notice before removal)
**And** any endpoint marked `@Deprecated` in the controller is rendered as `deprecated: true` in the OpenAPI spec
**And** a `README` or runbook section documents the header-based versioning scheme (`Accept-Version: v1`) as supported-but-optional for Phase 1
**And** the Spectral ruleset (Epic 2) can lint for the presence of `deprecated: true` with a matching sunset date extension
**And** no currently active endpoint is marked deprecated — the mechanism is in place for future use

---

## Epic 2: API Governance, Error Standards & Observability

Every error path returns a consistent, machine-readable Problem+JSON response with a stable error code and a traceable request ID. Spectral linting enforces contract rules on every PR, and breaking changes are detected automatically.

### Story 2.1: Global Problem+JSON Error Handler — Both Services

As an API consumer,
I want all error responses (4xx and 5xx) to return RFC 7807-compliant Problem+JSON,
So that I can handle errors programmatically without parsing inconsistent error shapes.

**Acceptance Criteria:**

**Given** both services currently return inconsistent or empty error bodies on failures
**When** the global exception handler is implemented
**Then** a `ProblemResponse` class exists in each service with fields: `type`, `title`, `status`, `detail`, `instance`, `traceId`
**And** a `@ControllerAdvice GlobalExceptionHandler` handles `ProductNotFoundException` → 404, `ProductAlreadyExistsException` → 409, `OrderNotFoundException` → 404, `MethodArgumentNotValidException` → 400, and catch-all `RuntimeException` → 500
**And** every error response sets `Content-Type: application/problem+json`
**And** Spring's `ProblemDetail` class is NOT used — `ProblemResponse` is the only error response type
**And** each service owns its own `ProblemResponse` and `GlobalExceptionHandler` — no shared class across services
**And** a `@WebMvcTest` unit test asserts Problem+JSON shape for each mapped exception type

### Story 2.2: Error Code Catalog

As an API consumer,
I want each error response to include a stable, versioned error code (e.g., `ERR-PRODUCT-001`),
So that I can identify and handle specific error types without parsing human-readable messages.

**Acceptance Criteria:**

**Given** the `GlobalExceptionHandler` from Story 2.1 is in place
**When** the error catalog is implemented
**Then** an `ErrorCodes` enum or constants class exists in each service defining all error codes for that domain
**And** `error-catalog.yml` is present at `src/main/resources/error-catalog.yml` in each service, listing all codes with their HTTP status and description
**And** the initial catalog includes: `ERR-PRODUCT-001` (404), `ERR-PRODUCT-002` (409), `ERR-PRODUCT-003` (400), `ERR-ORDER-001` (404), `ERR-ORDER-002` (400), `ERR-VALIDATION-001` (400), `ERR-INTERNAL-001` (500)
**And** the `type` field in `ProblemResponse` is set to `https://api.example.com/errors/{error-code}` using the catalog code
**And** adding a new error code requires only adding an entry to `ErrorCodes` and `error-catalog.yml` — no changes to `GlobalExceptionHandler` structure

### Story 2.3: TraceId Propagation via MDC

As an operations engineer,
I want every request and error response to carry a `traceId`,
So that I can correlate logs, error responses, and requests across services for a single user interaction.

**Acceptance Criteria:**

**Given** requests flow through the gateway to downstream services
**When** the `TraceIdFilter` is implemented on both services
**Then** a `TraceIdFilter` (`OncePerRequestFilter`) reads the `X-Trace-Id` request header; if absent, generates a UUID
**And** the `traceId` value is stored in MDC under key `traceId` for the duration of the request
**And** the Logback pattern on both services includes `%X{traceId}` so every log line carries the trace ID
**And** `ProblemResponse.traceId` is populated from MDC on every error response
**And** the gateway forwards `X-Trace-Id` to downstream services on all routed requests
**And** a unit test verifies that a request with `X-Trace-Id: abc123` produces a `ProblemResponse` with `traceId: abc123`

### Story 2.4: Spectral Linting — Bundled Rules & CI Enforcement

As an API producer,
I want the OpenAPI spec linted automatically on every build against a pinned, bundled Spectral ruleset,
So that naming violations, missing versioning, incorrect error schemas, and pagination shape issues are caught before they reach a PR.

**Acceptance Criteria:**

**Given** the OpenAPI spec is generated by SpringDoc (from Epic 1)
**When** Spectral linting is configured
**Then** a `.spectral.yml` ruleset file is committed to the repository root, pinning the Spectral ruleset version
**And** Spectral CLI is bundled in the repository (e.g., via `package.json` devDependency or a committed binary wrapper) — no network call to an external service at lint time (NFR-I04)
**And** the ruleset enforces: all paths must start with `/api/v1/`, all error responses must reference the Problem+JSON schema, all `operationId` values must be camelCase
**And** `mvn verify` (or a bound Maven exec plugin goal) runs Spectral lint and fails the build on any violation
**And** the ruleset version is pinned — no auto-updates from upstream (NFR-R04)
**And** a passing lint run produces a report with zero errors on the current spec

### Story 2.5: OpenAPI Contract Diff on Pull Requests

As an API team member,
I want breaking and non-breaking contract changes detected automatically on every PR,
So that consumers are notified of breaking changes before they merge to main.

**Acceptance Criteria:**

**Given** the OpenAPI spec is generated and committed (or published as a build artifact)
**When** a PR is opened or updated
**Then** an `openapi-diff` or `oasdiff` step runs in CI comparing the PR branch spec against the base branch spec
**And** the diff output is posted as a PR comment or build artifact — visible without downloading files
**And** breaking changes (removed endpoints, renamed required fields, changed response status codes) cause the CI step to exit non-zero and block merge
**And** non-breaking changes (added optional fields, new endpoints) produce a warning comment but do not block merge
**And** the diff tool version is pinned in the CI config

---

## Epic 3: Test Pyramid — Unit & Integration Coverage

Developers can run a full, trustworthy test suite (controller unit → service unit → Testcontainers integration) in under 3 minutes with zero dependency on a live Cassandra cluster. The build fails on critical CVEs.

### Story 3.1: Controller Unit Tests — Products Service

As a developer,
I want `@WebMvcTest` unit tests for all `ProductController` endpoints,
So that I can verify controller behaviour — routing, validation, error mapping — with no Cassandra or external dependency.

**Acceptance Criteria:**

**Given** `ProductController` and `GlobalExceptionHandler` exist (from Epics 1 & 2)
**When** controller unit tests are written
**Then** a `ProductControllerTest` class annotated `@WebMvcTest(ProductController.class)` exists in `microservice-spring-boot`
**And** `ProductService` is mocked via `@MockBean` — no real service or Cassandra wired
**And** tests cover: successful GET (200), product not found (404 + Problem+JSON), invalid POST payload (400 + Problem+JSON), successful POST (201), successful DELETE (204)
**And** each test method follows Given/When/Then comment structure with a trailing `verify(productService)…` call
**And** `@Autowired MockMvc` performs requests; `@Autowired ObjectMapper` serializes expected JSON — no hand-written JSON strings
**And** the full `ProductControllerTest` suite runs in under 10 seconds with no Cassandra running

### Story 3.2: Controller Unit Tests — Orders Service

As a developer,
I want `@WebMvcTest` unit tests for all `OrderController` endpoints,
So that I can verify controller behaviour for the Orders service with no Cassandra or external dependency.

**Acceptance Criteria:**

**Given** `OrderController` and its `GlobalExceptionHandler` exist (from Epics 1 & 2)
**When** controller unit tests are written
**Then** an `OrderControllerTest` class annotated `@WebMvcTest(OrderController.class)` exists in `microservice-spring-data`
**And** `OrderService` (or `OrderRepository`) is mocked via `@MockBean`
**And** tests cover: successful order creation (201), order not found (404 + Problem+JSON), invalid payload (400 + Problem+JSON), delete order (204), search by orderId (200)
**And** each test method follows Given/When/Then comment structure with a trailing `verify()` call
**And** the full `OrderControllerTest` suite runs in under 10 seconds with no Cassandra running

### Story 3.3: Service-Layer Unit Tests — Both Services

As a developer,
I want `@ExtendWith(MockitoExtension.class)` unit tests for all service-layer components,
So that I can verify business logic and DAO/repository interactions in isolation without Spring context or Cassandra.

**Acceptance Criteria:**

**Given** `ProductService` (products) and `OrderService` (orders) exist with DAO/repository dependencies
**When** service unit tests are written
**Then** a `ProductServiceTest` class annotated `@ExtendWith(MockitoExtension.class)` exists, with `ProductDao` mocked via `@Mock`
**And** tests cover: find by name (returns list), find by name+id (not found throws `ProductNotFoundException`), add product (delegates to DAO), delete product (delegates to DAO)
**And** an `OrderServiceTest` class annotated `@ExtendWith(MockitoExtension.class)` exists, with `OrderRepository` mocked via `@Mock`
**And** each test method follows Given/When/Then structure with trailing `verify()` on the mock
**And** no Spring context is loaded — pure Mockito; suite runs in under 5 seconds

### Story 3.4: Testcontainers Integration Tests & OWASP Dependency Gate

As a developer,
I want integration tests that run against a real containerized Cassandra instance with reproducible seed data, and a build gate that fails on critical CVEs,
So that DAO and repository logic is verified against the real Cassandra stack and no critical vulnerabilities ship undetected.

**Acceptance Criteria:**

**Given** Testcontainers and the Cassandra module are on the test classpath
**When** integration tests are written
**Then** an `AbstractCassandraIntegrationTest` base class exists in each service's test tree, starting a single shared `CassandraContainer` with a pinned image version (e.g., `cassandra:4.1`) — no `latest` tag (NFR-I02)
**And** the container is started once per test module using `@Container` with `static` scope — not restarted per test class
**And** CQL init scripts (`init.cql`) in `src/test/resources/cassandra/` create the keyspace, tables, and seed data using `IF NOT EXISTS` and `INSERT IF NOT EXISTS` for idempotency (NFR-R01)
**And** a `ProductDaoTest` extends `AbstractCassandraIntegrationTest` and tests: insert product, find by name, find by name+id, delete
**And** an `OrderRepositoryTest` (or `OrderDaoTest`) extends `AbstractCassandraIntegrationTest` and tests: save order, find by orderId, delete product from order
**And** no test has a dependency on another test's data — each test cleans up or uses unique keys (NFR-R02)
**And** the full unit + integration suite completes in under 3 minutes in CI (NFR-P03)
**And** the OWASP Dependency Check Maven plugin is configured in the root POM with `failBuildOnCVSS=9` — `mvn verify` fails if any dependency carries a CVSS ≥ 9.0 CVE (FR15)

---

## Epic 4: Security, Authorization & Rate Protection

All API routes are JWT-protected at the gateway, write operations enforce ROLE_WRITER RBAC, rate limits are applied per route, and BOLA/BFLA protections guard resource access. Security posture is a build artifact — verifiable and auditable.

### Story 4.1: JWT Authentication at Spring Cloud Gateway

As an authenticated user,
I want all API routes protected by JWT validation at the gateway,
So that unauthenticated requests are rejected before reaching any downstream service.

**Acceptance Criteria:**

**Given** the gateway currently forwards all requests without authentication
**When** Spring Security OAuth2 Resource Server is configured on the gateway
**Then** `spring-security-oauth2-resource-server` and `spring-security-oauth2-jose` are added to the gateway POM
**And** the gateway validates the JWT signature via `JWKS_URI` (injected as env var — never hardcoded), `issuer-uri`, and `audience` (NFR-S03)
**And** all requests without a valid `Authorization: Bearer <token>` header receive `401 Unauthorized` + Problem+JSON `ERR-AUTH-001` before reaching Products or Orders services
**And** JWT validation uses RS256 asymmetric signing — HS256 is explicitly rejected (NFR-S02)
**And** switching identity providers requires only changing `JWKS_URI` and `issuer-uri` env vars — zero code changes (NFR-I01, FR17)
**And** application startup fails fast with a clear error message if `JWKS_URI` is missing or blank (NFR-R05)
**And** a Keycloak instance is added to `docker-compose.yml` as the local/CI identity provider with correct startup dependency order (NFR-I03)

### Story 4.2: Identity Propagation & RBAC — Write Endpoint Protection

As a user with `ROLE_WRITER`,
I want write operations (`POST`, `PUT`, `DELETE`) to be accessible only with the correct role claim,
So that read-only users cannot modify data and every write is traceable to an authenticated identity.

**Acceptance Criteria:**

**Given** the gateway validates JWT (from Story 4.1)
**When** RBAC is enforced
**Then** the gateway extracts `sub` (user ID) and `roles` claims from the validated JWT and forwards them as `X-User-Id` and `X-Roles` headers to downstream services
**And** the gateway route config denies `POST`, `PUT`, `DELETE` requests that lack the `ROLE_WRITER` claim — returning `403 Forbidden` + Problem+JSON `ERR-AUTH-002`
**And** `GET` requests require only a valid JWT — no role requirement beyond authentication
**And** downstream services extract identity via `@RequestHeader(value = "X-User-Id", required = false) String userId` — no JWT re-validation at service layer
**And** a unit test on the gateway verifies that a token without `ROLE_WRITER` is denied on POST/DELETE endpoints
**And** a unit test verifies that a token with `ROLE_WRITER` is permitted on write endpoints

### Story 4.3: Per-Route Rate Limiting with Retry-After

As an API operator,
I want per-route, per-user rate limiting enforced at the gateway with a `Retry-After` response header,
So that no single user can exhaust service capacity and clients can implement correct backoff.

**Acceptance Criteria:**

**Given** JWT authentication is in place (from Story 4.1) and authenticated user identity is available at the gateway
**When** rate limiting is configured
**Then** `RequestRateLimiter` filter is applied to all gateway routes using in-memory rate limiting — no Redis dependency (NFR-S07)
**And** read endpoints (`GET`) allow 100 requests/minute per authenticated user; write endpoints (`POST`/`PUT`/`DELETE`) allow 20 requests/minute per authenticated user
**And** requests exceeding the limit receive `429 Too Many Requests` + Problem+JSON `ERR-AUTH-003` with a `Retry-After` header indicating seconds until the next request is permitted (FR21, FR27)
**And** the rate limit values are configurable via the gateway ConfigMap — no hardcoded limits in source code
**And** unauthenticated requests receive `401` (from Story 4.1) — they never reach the rate limiter

### Story 4.4: BOLA Enforcement — Resource Ownership at Service Layer

As a security engineer,
I want resource-level ownership checks enforced at the service layer,
So that authenticated users cannot read or modify resources belonging to other users (OWASP API1 — BOLA).

**Acceptance Criteria:**

**Given** `X-User-Id` is propagated by the gateway (from Story 4.2) and services extract it via `@RequestHeader`
**When** ownership checks are implemented
**Then** any endpoint that retrieves or modifies a user-scoped resource validates that `X-User-Id` matches the resource owner before returning data
**And** a request where `X-User-Id` does not match the resource owner returns `403 Forbidden` + Problem+JSON `ERR-AUTH-002`
**And** ownership check logic is in the service layer — not duplicated in the controller
**And** a unit test (mocked service) verifies that mismatched `X-User-Id` triggers a 403 on ownership-scoped endpoints
**And** endpoints that are not user-scoped (e.g., product search by name — public catalogue) do not apply ownership checks

### Story 4.5: Security Response Headers & BFLA Enforcement

As a security engineer,
I want security response headers on all authenticated routes and method-level BFLA enforcement,
So that browsers cannot be exploited via content sniffing or framing, and users cannot invoke functions beyond their assigned role (OWASP API5 — BFLA).

**Acceptance Criteria:**

**Given** all routes pass through the gateway
**When** security headers and BFLA enforcement are configured
**Then** all authenticated responses include: `X-Content-Type-Options: nosniff`, `X-Frame-Options: DENY`, `Cache-Control: no-store` (NFR-S06)
**And** headers are added at the gateway via a global filter — not duplicated per service
**And** method-level RBAC at the service controller layer uses `X-Roles` header to validate that `ROLE_WRITER` is present for any write handler — service-level guard in addition to gateway enforcement
**And** a request reaching the service with `X-Roles` missing `ROLE_WRITER` on a write endpoint returns `403` + Problem+JSON `ERR-AUTH-002`
**And** actuator endpoints remain accessible only on the management port — never on the public API port (NFR-S05)
**And** a unit test verifies that security headers are present on a sample authenticated response
**And** a unit test verifies that a service-layer write endpoint rejects a request with insufficient roles

---

## Epic 5: Developer Experience & Operational Readiness

New developers can onboard in under one working day — one Docker Compose command starts everything, a runbook guides operations, and the published OpenAPI spec enables any consumer to generate a typed client without team involvement.

### Story 5.1: Docker Compose Local Stack — One-Command Start

As a new developer,
I want to start all services — Cassandra, both microservices, the gateway, and Keycloak — with a single `docker-compose up -d`,
So that I can have a fully functional local environment without manual setup or tribal knowledge.

**Acceptance Criteria:**

**Given** the existing `docker-compose.yml` starts a 3-node Cassandra cluster
**When** the Docker Compose stack is updated
**Then** `docker-compose up -d` starts: 3-node Cassandra cluster, Keycloak (identity provider), `microservice-spring-boot` (Products), `microservice-spring-data` (Orders), `gateway-service`
**And** service startup order is enforced via `depends_on` with `condition: service_healthy` — Cassandra starts before services, Keycloak starts before gateway (NFR-I03)
**And** Keycloak is pre-configured with a realm, a client, and two test users (`reader` without `ROLE_WRITER`, `writer` with `ROLE_WRITER`) via an imported realm config file — no manual Keycloak setup
**And** the Products and Orders services connect to Cassandra at `host.docker.internal:9042` using the `docker` Spring profile
**And** `docker-compose down && docker-compose up -d` produces an identical running state — fully reproducible (NFR-R03)
**And** a smoke test (curl command in the runbook) confirms the gateway returns `401` on an unauthenticated request and `200` on an authenticated `GET` using a test token

### Story 5.2: Operations Runbook

As an operations engineer,
I want a runbook co-located with the codebase covering all standard operational tasks,
So that I can perform day-to-day operations without tribal knowledge or Slack escalations.

**Acceptance Criteria:**

**Given** the project has Docker Compose, Kubernetes, and Keycloak configurations
**When** the runbook is written
**Then** a `RUNBOOK.md` is committed to the repository root covering: starting/stopping local stack, obtaining a test JWT from Keycloak, running the full test suite, deploying to Kubernetes (minikube), checking service health via actuator, rotating DB credentials (K8s secret update), and interpreting common error responses (Problem+JSON codes)
**And** every procedure in the runbook is executable using only tools listed in the prerequisites section — no undocumented tool dependencies
**And** the runbook includes sample `curl` commands for all API endpoints using the versioned `/api/v1/` paths with JWT headers
**And** the runbook documents the `SPRING_PROFILES_ACTIVE` values (`default`, `docker`, `kubernetes`) and when to use each
**And** a new team member following the runbook from a clean checkout can complete the "start local stack and make a first API call" procedure in under 20 minutes

### Story 5.3: OpenAPI Client Generation Guide

As an API consumer,
I want clear instructions for generating a typed client library from the published OpenAPI spec,
So that I can integrate with the Products and Orders APIs in any language without involving the producing team.

**Acceptance Criteria:**

**Given** OpenAPI 3.1 specs are published at `/api/v1/api-docs` on both services (from Epic 1)
**When** the client generation guide is written
**Then** a `CLIENT_GENERATION.md` (or section in the runbook) documents how to generate a client using `openapi-generator-cli` for at least two target languages (e.g., Java and TypeScript)
**And** the guide includes the exact `openapi-generator-cli` command with the spec URL, generator name, and output directory
**And** the guide documents how to obtain the spec as a static JSON file from a running service for offline generation
**And** the generated client example is verified against the actual running service — not a hypothetical example
**And** the OpenAPI spec `info` section includes `contact` and `license` fields so consumers know where to report issues
