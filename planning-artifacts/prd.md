---
stepsCompleted: ['step-01-init', 'step-02-discovery', 'step-02b-vision', 'step-02c-executive-summary', 'step-03-success', 'step-04-journeys', 'step-05-domain', 'step-06-innovation', 'step-07-project-type', 'step-08-scoping', 'step-09-functional', 'step-10-nonfunctional', 'step-11-polish']
inputDocuments: ['planning-artifacts/project-context.md']
workflowType: 'prd'
projectDocsCount: 1
briefCount: 0
researchCount: 0
brainstormingCount: 0
classification:
  projectType: api_backend
  domain: general
  complexity: high
  projectContext: brownfield
  phase1Pillars: ['api_contract_enhancements', 'testing_enhancements', 'security_enhancements']
  deferredToLaterPhases: ['kubernetes_deployment', 'observability', 'data_cassandra', 'cicd', 'developer_experience', 'bmad_integration', 'advanced_features']
---

# Product Requirements Document - Spring Microservices with Apache Cassandra and Kubernetes

**Author:** SK
**Date:** 2026-04-18

## Executive Summary

This project delivers a **Phase 1 governed engineering blueprint** for Spring Boot microservices on Kubernetes — transforming an existing reference architecture into a production-grade system where API contracts, test coverage, and security posture are fully aligned to a single enforced standard. The primary users are software engineering teams who build and operate distributed Java services; the problem being solved is the inconsistency that makes teams afraid to refactor — drifting OpenAPI contracts, untested integration boundaries, and security gaps that accumulate silently until they become incidents.

Phase 1 targets three pillars: **API & Contract Enhancements** (OpenAPI 3.1, versioning, global error model, contract tests), **Testing Enhancements** (full pyramid: unit → Testcontainers integration → AFT → chaos/resilience), and **Security Enhancements** (JWT/OAuth2 via gateway, RBAC, rate limiting, secrets management, dependency scanning). Together these pillars remove the friction that slows delivery: executable documentation replaces tribal knowledge, fast automated tests replace manual regression, and verified security posture replaces hope.

### What Makes This Special

Unlike typical Spring Boot samples that demonstrate one concept in isolation, this blueprint enforces consistency **across every layer simultaneously** — contracts that are both generated and validated, tests that cover the real Cassandra stack via Testcontainers, and security that is wired into the gateway rather than bolted on per-service. The deliberate architectural split between raw-driver (Products) and Spring Data (Orders) is preserved and governed, not flattened. BMAD + Claude provide the meta-layer: PRDs, Epics, Stories, and test plans are generated and kept traceable end-to-end, ensuring the engineering workflow itself is as governed as the code it produces.

The core insight is that **fear of change is an engineering problem, not a people problem** — and it is solved by making correctness verifiable at every layer, automatically.

### Project Classification

| Field | Value |
|-------|-------|
| **Project Type** | API Backend (multi-service REST microservices) |
| **Domain** | General / Developer Infrastructure |
| **Complexity** | High |
| **Project Context** | Brownfield |
| **Phase 1 Pillars** | API & Contract Enhancements, Testing Enhancements, Security Enhancements |
| **Deferred to Phase 2+** | K8s/Observability, CI/CD, Data/Cassandra, DX, BMAD Integration, Advanced (Kafka, Istio) |

## Success Criteria

### User Success

A developer working on this codebase after Phase 1 can:
- Modify any endpoint, run the full test suite, and get a trustworthy green/red signal — **no manual verification required**
- Read the OpenAPI 3.1 spec for either service and trust it reflects what the service actually does in production
- Onboard to the codebase and make a confident first contribution within **one working day**
- Write a new BDD scenario in Cucumber/Gherkin and have it execute against a real Cassandra data snapshot via Testcontainers — without standing up a cluster

**"Aha!" moment:** A developer changes a response field, the contract test catches a consumer break, and they fix it before it ever reaches a PR review. The system caught a breaking change automatically.

### Business / Team Success

**3–4 Month Targets:**
- Zero regressions introduced via PRs that had a full green test suite at merge time
- All REST endpoints across both services documented in versioned OpenAPI 3.1 specs with generated Swagger UI and ReDoc
- BDD AFT suite (Cucumber) running on every PR with Cassandra virtualization — no live cluster dependency in CI
- JWT authentication enforced at the gateway; no service-level auth duplication
- OWASP/Snyk dependency scan passing with zero critical CVEs in the build pipeline
- New team members self-onboarding using the runbook + API docs alone — no Slack escalations needed

### Technical Success

- OpenAPI 3.1 specs generated (not hand-authored) and validated against live service responses
- Test pyramid in place: unit (`@WebMvcTest`, `@ExtendWith(MockitoExtension.class)`) → integration (Testcontainers + pre-seeded Cassandra snapshot) → AFT (Cucumber/Gherkin scenarios) → contract (Spring Cloud Contract)
- Global `Problem+JSON` error model returned consistently across all error paths on both services
- API versioning (`/v1/`, `/v2/`) enforced with backward-compatibility rules documented
- JWT validation at Spring Cloud Gateway — services trust the gateway-propagated identity
- RBAC enforced at the service level for write operations
- Rate limiting configured at the gateway per route
- Dependency scanning (OWASP Dependency Check or Snyk) integrated into the Maven build

### Measurable Outcomes

| Outcome | Target |
|---------|--------|
| OpenAPI spec coverage | 100% of endpoints on both services |
| Test pyramid layers | 4 (unit, integration, AFT, contract) |
| AFT execution time (CI) | < 5 minutes with Testcontainers snapshot |
| Auth enforcement | 100% of write endpoints require valid JWT |
| Critical CVEs in build | 0 at merge time |
| Developer onboarding time | ≤ 1 working day |
| Contract test regressions caught pre-merge | Target: all breaking changes caught before PR merge |

## User Journeys

### Journey 1: Feature Developer — Happy Path

**Meet Priya.** She's a mid-level Java engineer tasked with adding a `lastModifiedBy` field to the Products API. On the old codebase, this would have meant manually updating Postman collections, hoping the Swagger doc was accurate, and texting the QA team to re-run their scripts.

**Opening scene:** Priya pulls the task from the sprint board. She opens the OpenAPI 3.1 spec — it's generated from annotations, not handwritten, so she trusts it. She sees the current Products contract in `/v1/products`.

**Rising action:** She adds the field to the `Product` entity and controller, updates the `@Schema` annotation, and runs `mvn test`. The unit tests pass. The Testcontainers integration test spins up a pre-seeded Cassandra snapshot and validates the DAO writes the new field correctly. The Spring Cloud Contract test detects she's added a new field — non-breaking — and passes.

**Climax:** She opens a PR. The CI pipeline runs the full pyramid: unit, integration, AFT (Cucumber scenarios execute against the gateway), contract tests. All green. The OpenAPI diff in the PR shows exactly what changed in the contract — one new optional field.

**Resolution:** The PR merges in 40 minutes. No manual QA cycle, no "did you update Swagger?", no production surprise. Priya's new reality: changes are safe because the system tells her immediately when they aren't.

**Requirements revealed:** OpenAPI diff in CI, contract test on new/changed fields, Testcontainers pre-seeded snapshot, Cucumber AFT on gateway routes.

---

### Journey 2: New Team Member — Onboarding

**Meet Tariq.** He joins the team on Monday. Previously, onboarding meant a week of pairing, tribal knowledge sessions, and "just ask someone" as the primary documentation strategy.

**Opening scene:** Tariq clones the repo. He reads the `README` and follows the runbook to start the local Docker Compose stack. Within 20 minutes he has Cassandra and both services running.

**Rising action:** He navigates to Swagger UI on the Products service — every endpoint is documented with request/response schemas, error codes, and example payloads. He hits ReDoc for a cleaner read. He runs the full test suite: it passes. He reads three Cucumber feature files and understands the expected API behavior without reading any Java code.

**Climax:** By afternoon he's modified a unit test to understand the `@WebMvcTest` pattern, run it, and seen it fail then pass. He hasn't broken anything because the test harness told him immediately.

**Resolution:** By end of day one Tariq has made a small, safe, verified change. No Slack pings needed. The documentation was executable — it didn't lie.

**Requirements revealed:** Swagger UI + ReDoc on both services, Cucumber feature files as living documentation, runbook, Docker Compose local stack.

---

### Journey 3: QA / Test Engineer — AFT Authoring & Failure Investigation

**Meet Amara.** She owns test strategy. Her frustration: tests that pass locally but fail in CI because they depend on a live Cassandra cluster that isn't always available.

**Opening scene:** Amara is writing a new Cucumber scenario for the Orders service — "Given an order exists, when I delete a product from the order, then the order total updates." She opens the feature file, writes the Gherkin, implements the step definition using REST Assured against the gateway.

**Rising action:** The step definition boots Testcontainers with the pre-seeded Cassandra snapshot. The scenario runs in isolation — no shared cluster, no flakiness from concurrent test runs. She runs the full AFT suite locally in under 4 minutes.

**Climax:** A developer's PR breaks one of her existing scenarios — the error response format changed from a plain string to `Problem+JSON`. The CI pipeline catches it. Amara gets a clear failure message pointing to the exact scenario and step. She files a comment on the PR with the broken scenario name.

**Resolution:** The developer fixes the response format, the scenario passes, the PR merges. Amara's suite is now the safety net that enforces the global error model — not a post-deploy manual test.

**Requirements revealed:** Testcontainers + pre-seeded snapshot for AFT isolation, Cucumber/Gherkin feature files, REST Assured step definitions, `Problem+JSON` global error model, CI AFT execution on every PR.

---

### Journey 4: Security / Platform Engineer — Auth & Dependency Posture Review

**Meet Daniel.** He's responsible for the platform security posture. His concern: services that do their own auth logic inconsistently, and dependencies that carry known CVEs quietly for months.

**Opening scene:** A new compliance review requires Daniel to demonstrate that all write operations require a valid JWT and that no critical CVEs exist in the dependency tree.

**Rising action:** He reviews the gateway configuration — JWT validation is wired at the gateway level via Spring Security. Services receive a verified identity header; they don't re-validate tokens. He checks the RBAC configuration: `POST`, `PUT`, `DELETE` routes on both services require the `ROLE_WRITER` claim. He runs `mvn verify` — the OWASP Dependency Check plugin generates a report. Zero critical CVEs.

**Climax:** Daniel opens the security review document. He can point to: gateway-enforced JWT, RBAC on write endpoints, rate limiting per route, and a passing dependency scan. All verifiable from the build output — not from memory or manual audit.

**Resolution:** The compliance review passes in one session. Daniel's new reality: security posture is a build artifact, not a conversation.

**Requirements revealed:** JWT validation at gateway, RBAC on write endpoints, rate limiting, OWASP/Snyk dependency scan in Maven build, trace-id/correlation-id headers for audit trail.

---

### Journey 5: API Consumer (Client Developer) — Integration Against Versioned Contract

**Meet Sofia.** She's building a mobile backend that calls the Products service via the gateway. Her fear: the API changes under her without warning and her integration breaks in production.

**Opening scene:** Sofia discovers the OpenAPI 3.1 spec published by the gateway at `/v1/api-docs`. She imports it into her HTTP client generator and gets typed models for free.

**Rising action:** Three weeks later, the Products team ships a change. Because the team uses Spring Cloud Contract, Sofia's consumer contract test runs in their CI pipeline. The new version introduces a breaking field rename — the contract test catches it before it merges.

**Climax:** Sofia gets a notification that a contract test failed on a PR in the Products repo. She reviews the diff, confirms the rename affects her, and the Products team adds a `/v2/` route preserving the old field. The `/v1/` contract is honoured.

**Resolution:** Sofia's integration never breaks in production. API versioning and consumer-driven contracts are the guarantee — not verbal agreements or changelog emails.

**Requirements revealed:** OpenAPI 3.1 spec published at gateway, Spring Cloud Contract consumer tests run in producer CI, API versioning (`/v1/`, `/v2/`), backward-compatibility rules enforced.

---

### Journey Requirements Summary

| Capability | Revealed By |
|-----------|------------|
| OpenAPI 3.1 spec generation + diff in CI | Journey 1, 5 |
| Swagger UI + ReDoc on both services | Journey 1, 2 |
| Testcontainers + pre-seeded Cassandra snapshot | Journey 1, 3 |
| Cucumber/Gherkin AFT suite + REST Assured | Journey 3 |
| Spring Cloud Contract (consumer-driven) | Journey 1, 5 |
| API versioning `/v1/` + `/v2/` | Journey 5 |
| `Problem+JSON` global error model | Journey 3 |
| JWT at gateway + RBAC on write endpoints | Journey 4 |
| Rate limiting per route | Journey 4 |
| OWASP/Snyk dependency scan in Maven build | Journey 4 |
| `trace-id` / `correlation-id` header propagation | Journey 4 |
| Docker Compose local stack + runbook | Journey 2 |
| Cucumber feature files as living documentation | Journey 2 |

## Domain-Specific Requirements

### Security Standards (OWASP API Security Top 10)

The following OWASP API Security Top 10 controls are explicitly in scope — each is both architecturally enforceable and testable via the AFT suite:

| Control | Mitigation Approach |
|---------|-------------------|
| **API1 — BOLA** (Broken Object Level Authorization) | Resource-level ownership checks enforced at service layer; RBAC claims validated per request |
| **API3 — Excessive Data Exposure** | Response schema enforcement via OpenAPI; no raw entity pass-through; explicit field projection on all read endpoints |
| **API5 — BFLA** (Broken Function Level Authorization) | Method-level RBAC (`ROLE_WRITER` for POST/PUT/DELETE); enforced at gateway and validated at service |
| **API6 — Mass Assignment** | `@RequestBody` deserialization locked to explicit DTO fields; no direct entity binding from request body |
| **API7 — Security Misconfiguration** | Dependency scanning (OWASP/Snyk) in Maven build; actuator endpoints restricted to management port; no debug endpoints in production profile |
| **API9 — Improper Assets Management** | API versioning (`/v1/`, `/v2/`) enforced; deprecation policy documented in OpenAPI spec; old versions sunset with explicit timeline |

All six controls must have corresponding Cucumber/Gherkin scenarios validating enforcement.

### JWT / OAuth2 Configuration

Provider-agnostic Spring Security OAuth2 Resource Server via configurable `JWKS_URI`, `issuer-uri`, and `audience`. Full detail in [Authentication & Authorization Model](#authentication--authorization-model).

### API Contract & Governance Standards

RFC 7807 Problem+JSON error model, Spectral linting, dual-mode URI + header versioning. Full detail in [API Backend Specific Requirements](#api-backend-specific-requirements).

### Testcontainers & Test Data Strategy

- **Context:** Commercial/enterprise team — Testcontainers standard OSS library (not Desktop/Cloud)
- **Seeding approach:** CQL init scripts (`init.cql`) executed on container startup — schema creation + seed data
- Test data versioned alongside test code in `src/test/resources/cassandra/`
- No Docker images with baked-in data — reproducibility guaranteed by CQL scripts
- Snapshot strategy: deterministic seed data set covering all test scenarios; scripts idempotent (`IF NOT EXISTS`, `INSERT IF NOT EXISTS`)

### Spring Boot Upgrade

- **Recommendation: Include upgrade in Phase 1** — target Spring Boot **2.7.x** as the safe minimum (no Jakarta migration required, unlocks SpringDoc 2.x, Spring Security 5.8, better Testcontainers integration)
- If timeline risk materialises: maintain 2.3.x but architect all enhancements as forward-compatible (no Spring Boot 2.3-specific APIs in new code)
- Spring Boot 3.x (Jakarta EE migration) deferred to Phase 2 unless team accepts the package rename scope
- Upgrade decision gate: assess actual migration effort in Sprint 1 technical spike; outcome determines path

### AFT Execution — Two-Tier Model

| Tier | Target | Trigger | Purpose |
|------|--------|---------|---------|
| **Tier 1 (Phase 1)** | Docker Compose (gateway + services + Cassandra) | Local dev + CI on every PR | Fast feedback, no cluster dependency |
| **Tier 2 (Phase 2)** | Minikube profile | Pre-prod / release branch | Production-parity validation |

- Same Cucumber feature files and step definitions run in both tiers — only the base URL and Testcontainers profile differ
- Docker Compose is the CI execution target for Phase 1; Minikube profile is a configuration flag, not a code change

### Risk Mitigations

| Risk | Mitigation |
|------|-----------|
| Spring Boot upgrade breaks existing functionality | Technical spike in Sprint 1; full test suite as regression gate |
| Spectral rules block PRs on false positives | Custom rules reviewed and approved before CI enforcement; override mechanism documented |
| Testcontainers CQL init scripts drift from production schema | Single source of truth: init scripts generated from or validated against the same schema files used in production |
| Provider lock-in creeping into JWT implementation | Architecture review gate: any JWT code that references a provider-specific class is a build violation |

## API Backend Specific Requirements

### Project-Type Overview

The OpenAPI 3.x specification is the primary governed artifact — generated from code, linted via Spectral, and used as the contract between producers and consumers. All functional and security requirements flow from the API contract, not from ad-hoc implementation decisions.

### API Contract & SDK Strategy

- **API-first:** OpenAPI 3.x spec is generated automatically from service annotations (SpringDoc), not hand-authored
- **Contract validation:** Spectral linting enforced in CI — naming conventions, pagination shape, error schema, versioning paths
- **Consumer SDKs:** Not published in Phase 1; consumers generate their own clients from the OpenAPI spec — reduces lifecycle overhead and avoids version-coupling
- **Spec publication:** OpenAPI specs exposed at `/api/v1/api-docs` on each service and aggregated at the gateway
- **Contract diff:** OpenAPI diff generated on every PR — breaking changes flagged automatically

### DTO Layer & Data Exposure Controls

Phase 1 introduces a strict DTO boundary across both services — no direct entity-to-response binding:

| Layer | Role |
|-------|------|
| **Request DTO** | Defines accepted input fields; validation annotations at boundary; prevents mass assignment (API6) |
| **Response DTO** | Defines public output fields only; no internal fields leaked; prevents excessive data exposure (API3) |
| **Domain Entity** | Internal persistence model; never exposed directly via API |

- All `@RequestBody` parameters bind to explicit DTO classes — no `@RequestBody Product` entity binding
- All `ResponseEntity<?>` return types use DTO classes — no raw entity serialization
- DTO validation via `@Valid` + Bean Validation (`@NotNull`, `@Size`, `@Pattern`) at controller entry point
- OpenAPI `@Schema` annotations on DTO fields — contract generation derives from DTOs, not entities

### Authentication & Authorization Model

- **JWT validation:** Spring Security OAuth2 Resource Server at gateway — validates token via `JWKS_URI`, `issuer-uri`, `audience`
- **Provider-agnostic:** Zero provider-specific code; Cognito/Auth0/Okta selected via Spring profile
- **Identity propagation:** Gateway forwards `X-User-Id` and `X-Roles` headers to downstream services after token validation
- **RBAC enforcement:**
  - Read operations (`GET`): no role requirement beyond valid JWT
  - Write operations (`POST`, `PUT`, `DELETE`): require `ROLE_WRITER` claim
  - Admin operations (if introduced): require `ROLE_ADMIN` claim
- **Service trust model:** Downstream services trust gateway-propagated headers; no re-validation of JWT at service level

### Data Schemas & Validation

- Request/response schemas derived exclusively from DTO classes via SpringDoc annotations
- Schema validation at DTO boundary — invalid requests rejected at controller entry with `400 Bad Request` + Problem+JSON body
- Consistent field naming: `camelCase` in JSON payloads, `snake_case` in Cassandra column names (mapped via `@Column`)
- Pagination shape (where applicable): `{ content: [], page: { size, number, totalElements, totalPages } }`
- Nullable vs required fields explicitly declared in OpenAPI schema via `@Schema(required = true/false, nullable = true/false)`

### Error Codes & Error Catalog

**RFC 7807 Problem+JSON — strict compliance:**

```json
{
  "type": "https://api.example.com/errors/ERR-PRODUCT-001",
  "title": "Product Not Found",
  "status": 404,
  "detail": "No product found with name 'mobile' and id '123e4567...'",
  "instance": "/api/v1/products/search/mobile/123e4567...",
  "traceId": "abc123def456"
}
```

**Centralized error code catalog (Phase 1 — initial set):**

| Code | HTTP Status | Description |
|------|------------|-------------|
| `ERR-PRODUCT-001` | 404 | Product not found |
| `ERR-PRODUCT-002` | 409 | Product already exists |
| `ERR-PRODUCT-003` | 400 | Invalid product payload |
| `ERR-ORDER-001` | 404 | Order not found |
| `ERR-ORDER-002` | 400 | Invalid order payload |
| `ERR-AUTH-001` | 401 | Token missing or expired |
| `ERR-AUTH-002` | 403 | Insufficient role |
| `ERR-AUTH-003` | 429 | Rate limit exceeded |
| `ERR-VALIDATION-001` | 400 | Request validation failure |
| `ERR-INTERNAL-001` | 500 | Unexpected internal error |

- Error codes referenced in `type` URI or extension field within Problem+JSON
- Catalog versioned alongside the codebase in `src/main/resources/error-catalog.yml`
- Cucumber AFT scenarios assert specific error codes on all negative test paths

### Rate Limits

- Rate limiting enforced at Spring Cloud Gateway per route
- Default limits (configurable via ConfigMap):
  - Read endpoints: 100 req/min per authenticated user
  - Write endpoints: 20 req/min per authenticated user
  - Unauthenticated: 0 (all routes require JWT)
- Rate limit exceeded returns `429 Too Many Requests` + `ERR-AUTH-003` Problem+JSON
- `Retry-After` header included in 429 responses

### API Documentation

- **Swagger UI:** Deployed on both services at `/swagger-ui.html`
- **ReDoc:** Deployed on both services at `/api/docs`
- **OpenAPI JSON:** Published at `/api/v1/api-docs`
- Both services upgraded to SpringDoc OpenAPI (Springfox removed from Products service)
- Documentation auto-refreshes from annotations — no manual doc maintenance

### Implementation Considerations

- DTOs introduced as a new `dto` package within each service's domain package (e.g., `com.datastax.examples.product.dto`)
- Mapper layer (manual or MapStruct) converts between DTO ↔ entity — no direct coupling
- Global `@ControllerAdvice` exception handler handles all error paths and returns Problem+JSON — replaces current silent 500s
- Spring Boot upgrade to 2.7.x confirmed as Phase 1 prerequisite — unlocks SpringDoc 2.x, Spring Security 5.8

## Project Scoping & Phased Development

### MVP Strategy & Philosophy

**MVP Approach:** Platform MVP — the minimum that makes the codebase *safe to change and verifiably governed*. A developer must be able to: hit a versioned, documented API; run a full green test suite; and deploy behind JWT-protected routes. Without these three capabilities, the blueprint claim doesn't hold.

**Resource Requirements:** 1–2 Java backend engineers, 1 QA/test engineer, access to a CI pipeline (GitHub Actions or equivalent). Spring Boot upgrade spike should be Sprint 1's first task.

### MVP Feature Set (Phase 1)

**Core User Journeys Supported:** Feature Developer (Journey 1), New Team Member (Journey 2), Security Engineer (Journey 4)

**Must-Have Capabilities:**

| Capability | Rationale |
|-----------|-----------|
| Spring Boot upgrade to 2.7.x | Unlocks SpringDoc 2.x, Spring Security 5.8 — all other Phase 1 work depends on this |
| OpenAPI 3.1 spec generation (SpringDoc) | Primary governed artifact; Springfox removed from Products service |
| Swagger UI + ReDoc on both services | Executable documentation — Journey 2 onboarding depends on this |
| DTO layer (request + response) on both services | OWASP API3 + API6 compliance; stable contract evolution |
| Global `@ControllerAdvice` + Problem+JSON error model | Replaces silent 500s; RFC 7807 strict; error code catalog |
| URI versioning `/api/v1/` on all routes | Contract stability for consumers (Journey 5) |
| Spectral linting in CI | Contract governance gate — fail build on violations |
| Unit tests: `@WebMvcTest` + `@ExtendWith(MockitoExtension.class)` | Full controller + service layer coverage |
| Integration tests: Testcontainers + CQL init scripts | Real Cassandra, reproducible, CI-safe |
| JWT at Spring Cloud Gateway (OAuth2 Resource Server) | OWASP API1 + API5; provider-agnostic |
| RBAC: `ROLE_WRITER` on write endpoints | Method-level authorization |
| Rate limiting per route at gateway | OWASP API9 + DoS mitigation |
| OWASP Dependency Check in Maven build | Zero critical CVEs gate |

### Post-MVP Features

**Phase 2 (Growth):**
- Cucumber + Gherkin BDD AFT suite (REST Assured step definitions)
- Testcontainers with pre-seeded CQL snapshot for AFT isolation
- Spring Cloud Contract consumer-driven contract tests
- `trace-id` / `correlation-id` header propagation end-to-end
- `X-Deprecated-Version` + sunset date in OpenAPI spec
- RBAC for `ROLE_ADMIN` operations
- Header-based versioning (documented + optional)

**Phase 3 (Vision):**
- Chaos + resilience tests (latency injection, pod kill, retry/backoff)
- Secrets management (External Secrets Operator + Vault / AWS Secrets Manager)
- Spring Boot 3.x / Jakarta EE migration
- Full BMAD + Claude traceability (PRDs → Epics → Stories → test plans auto-generated)
- Minikube AFT execution profile (Tier 2)

### Risk Mitigation Strategy

| Risk | Mitigation |
|------|-----------|
| **Spring Boot upgrade scope creep** | Time-boxed Sprint 1 spike (2 days max); if blocked, freeze at 2.3.x and design forward-compatible |
| **Spectral false positives blocking CI** | Dry-run mode first; rules reviewed before enforcement; documented override process |
| **Testcontainers flakiness in CI** | Fixed container versions; idempotent CQL scripts; retry policy on container startup |
| **DTO layer introduces breaking changes** | API versioning (`/v1/`) absorbs the break; old entity-bound endpoints removed in same PR as DTO introduction |
| **JWT provider selection delays Phase 1** | Use Keycloak locally (Docker Compose) as the dev/CI provider; swap via profile for production |

## Functional Requirements

### 1. API Contract Management

- **FR01:** API consumers can discover all available endpoints, schemas, and examples via a machine-readable OpenAPI 3.1 specification
- **FR02:** API consumers can interactively explore and test all endpoints via a browser-based Swagger UI
- **FR03:** API consumers can read structured, human-friendly API documentation via a ReDoc interface
- **FR04:** API producers can validate their OpenAPI spec against naming, pagination, error schema, and versioning rules via automated linting on every build
- **FR05:** API consumers can access all endpoints under a stable, versioned URI path (`/api/v1/`)
- **FR06:** API teams can detect breaking and non-breaking contract changes automatically on every pull request
- **FR07:** API consumers can rely on a documented deprecation policy with sunset dates for versioned endpoints
- **FR08:** API consumers can access all endpoints under an optional header-based versioning scheme (documented, Phase 1 optional)

### 2. Testing & Quality Assurance

- **FR09:** Developers can run unit tests for all controllers in complete isolation — no running Cassandra or external dependency required
- **FR10:** Developers can run unit tests for all service-layer components with mocked DAO/repository dependencies
- **FR11:** Developers can run integration tests against a real, containerized Cassandra instance using reproducible seed data
- **FR12:** QA engineers can author BDD test scenarios in plain-language Gherkin feature files without writing Java code *(Phase 2)*
- **FR13:** QA engineers can execute BDD scenarios against a pre-seeded, containerized Cassandra dataset without a live cluster *(Phase 2)*
- **FR14:** API consumers can verify their expected consumer contracts against producer API changes via automated contract testing *(Phase 2)*
- **FR15:** The build pipeline can enforce a zero-critical-CVE gate and fail the build when known vulnerabilities are detected in dependencies

### 3. Security & Authorization

- **FR16:** Authenticated users can access API endpoints using a JWT issued by any RFC 7519-compliant OAuth2 provider — no provider lock-in
- **FR17:** The system can validate JWT authenticity, issuer, and audience without code changes when switching identity providers
- **FR18:** Users holding the `ROLE_WRITER` claim can execute write operations (`POST`, `PUT`, `DELETE`) on both services
- **FR19:** Users without `ROLE_WRITER` are denied write operations and receive a machine-readable error response
- **FR20:** All API routes are protected against excessive request volume on a per-authenticated-user, per-route basis
- **FR21:** Clients that exceed rate limits receive a machine-readable error response including retry guidance
- **FR22:** The system can prevent unauthorized cross-user resource access at the service layer (OWASP API1 — BOLA)
- **FR23:** The system can prevent users from invoking functions beyond their role (OWASP API5 — BFLA)

### 4. Error Handling & Observability

- **FR24:** API consumers receive a standardized, RFC 7807-compliant `Problem+JSON` response for all error scenarios (4xx and 5xx)
- **FR25:** API consumers can identify the specific error type via a stable, versioned machine-readable error code (e.g., `ERR-PRODUCT-001`)
- **FR26:** Operations teams can correlate errors and requests across services using a propagated trace identifier (`traceId`)
- **FR27:** API consumers can determine appropriate retry behaviour from rate-limit error responses (`Retry-After` header)
- **FR28:** The error catalog is versioned and co-located with the codebase — new error codes are added via code, not manual documentation

### 5. Data Access & Domain Integrity

- **FR29:** API consumers can submit requests containing only the fields explicitly defined in the public API contract — no internal field binding
- **FR30:** API responses expose only the fields explicitly defined in the public API contract — no internal model leakage
- **FR31:** The system rejects requests with invalid field values at the API boundary and returns a descriptive, machine-readable error (OWASP API6)
- **FR32:** Developers can evolve internal domain models and Cassandra schema without changing the public API contract
- **FR33:** The system prevents exposure of internal entity fields in API responses (OWASP API3)
- **FR34:** The system can prevent undocumented or deprecated API endpoints from being silently accessible (OWASP API9)

### 6. Developer Experience & Onboarding

- **FR35:** New developers can start all services locally using a single Docker Compose command — no manual Cassandra setup
- **FR36:** New developers can understand expected API behaviour by reading Cucumber feature files — no Java code required *(Phase 2)*
- **FR37:** Operations teams can perform standard operational tasks by following a runbook — no tribal knowledge required
- **FR38:** API consumers can generate typed client libraries in any language from the published OpenAPI specification without SDK involvement from the producing team

## Non-Functional Requirements

_The following quality attributes constrain HOW WELL the system must perform. Each is specific and measurable — vague quality claims are not accepted._

### Performance

- **NFR-P01:** API read endpoints (`GET`) return responses within **200ms** at p95 under normal load (single-node local Cassandra or Docker Compose)
- **NFR-P02:** API write endpoints (`POST`, `PUT`, `DELETE`) complete within **500ms** at p95 under normal load
- **NFR-P03:** The full unit + integration test suite completes within **3 minutes** in CI — slow tests are a build-breaking concern
- **NFR-P04:** AFT suite (Cucumber + Testcontainers, Phase 2) completes within **5 minutes** in CI — Testcontainers startup included
- **NFR-P05:** OpenAPI spec generation adds no more than **2 seconds** to application startup time

### Security

- **NFR-S01:** All API traffic is JWT-authenticated — unauthenticated requests are rejected at the gateway with `401` before reaching downstream services
- **NFR-S02:** JWT validation uses asymmetric signing (RS256 minimum) — symmetric (HS256) is not permitted
- **NFR-S03:** All credentials (`DB_USERNAME`, `DB_PASSWORD`, `JWKS_URI`) are injected via environment variables or K8s secrets — zero secrets in source code or committed config files
- **NFR-S04:** The Maven build fails if any dependency carries a **critical (CVSS ≥ 9.0) CVE** — high CVEs generate a warning report but do not fail the build
- **NFR-S05:** Actuator endpoints are accessible only on the management port — never exposed on the public API port
- **NFR-S06:** All HTTP responses include security headers: `X-Content-Type-Options: nosniff`, `X-Frame-Options: DENY`, `Cache-Control: no-store` on authenticated routes
- **NFR-S07:** Rate limiting is enforced in-memory at the gateway — no shared state required for Phase 1 (Redis-backed rate limiting deferred to Phase 2+)

### Scalability

- **NFR-SC01:** The system architecture supports horizontal scaling of each service independently — no shared in-process state between service instances
- **NFR-SC02:** Cassandra connection pooling is configured to handle at least **10 concurrent requests per service instance** without connection exhaustion
- **NFR-SC03:** Spring Boot upgrade path to 3.x is not blocked by new Phase 1 code — no Spring Boot 2.3-specific APIs introduced

### Reliability

- **NFR-R01:** Testcontainers-based tests are deterministic and idempotent — the same CQL init scripts produce identical results on every run
- **NFR-R02:** No test has a dependency on test execution order — each test sets up and tears down its own state
- **NFR-R03:** The build pipeline is reproducible — given the same source commit, the build produces an identical result on any CI runner
- **NFR-R04:** Spectral linting rules are pinned to a fixed ruleset version — no unexpected rule additions from upstream auto-updates
- **NFR-R05:** Application startup fails fast with a clear error message if required environment variables (`JWKS_URI`, `DB_USERNAME`, `DB_PASSWORD`) are missing

### Integration

- **NFR-I01:** The OAuth2 Resource Server configuration supports any JWKS-endpoint-compatible provider — switching providers requires only environment variable changes, no code changes
- **NFR-I02:** Testcontainers uses a **pinned Cassandra image version** — no `latest` tag in test configuration
- **NFR-I03:** The Docker Compose stack starts all services in the correct dependency order (Cassandra before application services) without manual intervention
- **NFR-I04:** Spectral CLI is executable in CI without a network call to an external service — rules are bundled in the repository
