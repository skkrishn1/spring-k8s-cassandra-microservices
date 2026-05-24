---
stepsCompleted: ['step-01-document-discovery', 'step-02-prd-analysis', 'step-03-epic-coverage-validation', 'step-04-ux-alignment', 'step-05-epic-quality-review', 'step-06-final-assessment']
date: '2026-04-18'
project: 'Spring Microservices with Apache Cassandra and Kubernetes'
documents:
  prd: 'planning-artifacts/prd.md'
  architecture: 'planning-artifacts/architecture.md'
  epics: 'planning-artifacts/epics.md'
  ux: null
---

# Implementation Readiness Assessment Report

**Date:** 2026-04-18
**Project:** Spring Microservices with Apache Cassandra and Kubernetes

## Document Inventory

| Document Type | File | Status |
|---|---|---|
| PRD | planning-artifacts/prd.md | Complete |
| Architecture | planning-artifacts/architecture.md | Complete |
| Epics & Stories | planning-artifacts/epics.md | Complete (23 stories, 5 epics) |
| UX Design | — | Not applicable (API-only project) |

## PRD Analysis

### Functional Requirements (38 total — 34 Phase 1, 4 Phase 2)

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

**Total Phase 1 FRs: 34 | Phase 2 FRs: 4 (FR12, FR13, FR14, FR36)**

### Non-Functional Requirements (24 total)

NFR-P01: API read endpoints return responses within 200ms at p95 under normal load
NFR-P02: API write endpoints complete within 500ms at p95 under normal load
NFR-P03: The full unit + integration test suite completes within 3 minutes in CI
NFR-P04: AFT suite (Phase 2) completes within 5 minutes in CI — Testcontainers startup included
NFR-P05: OpenAPI spec generation adds no more than 2 seconds to application startup time
NFR-S01: All API traffic is JWT-authenticated — unauthenticated requests rejected at gateway with 401
NFR-S02: JWT validation uses asymmetric signing (RS256 minimum) — HS256 not permitted
NFR-S03: All credentials injected via environment variables or K8s secrets — zero secrets in source code
NFR-S04: Maven build fails if any dependency carries a critical (CVSS ≥ 9.0) CVE
NFR-S05: Actuator endpoints accessible only on management port — never on public API port
NFR-S06: All HTTP responses include security headers on authenticated routes
NFR-S07: Rate limiting enforced in-memory at gateway — no shared state required for Phase 1
NFR-SC01: System architecture supports horizontal scaling independently — no shared in-process state
NFR-SC02: Cassandra connection pooling handles at least 10 concurrent requests per service instance
NFR-SC03: Spring Boot upgrade path to 3.x not blocked — no Spring Boot 2.3-specific APIs introduced
NFR-R01: Testcontainers-based tests are deterministic and idempotent
NFR-R02: No test has a dependency on test execution order
NFR-R03: Build pipeline is reproducible — same source commit produces identical result on any CI runner
NFR-R04: Spectral linting rules pinned to fixed ruleset version — no upstream auto-updates
NFR-R05: Application startup fails fast with clear error if required environment variables are missing
NFR-I01: OAuth2 Resource Server supports any JWKS-endpoint-compatible provider — env var changes only
NFR-I02: Testcontainers uses a pinned Cassandra image version — no `latest` tag
NFR-I03: Docker Compose stack starts all services in correct dependency order without manual intervention
NFR-I04: Spectral CLI executable in CI without network call to external service — rules bundled in repo

### Additional Requirements (from PRD constraints)

- Spring Boot upgrade from 2.3.0 → 2.7.x is a Phase 1 prerequisite gate (2-day spike in Sprint 1)
- Keycloak as local/CI identity provider — swap via Spring profile for production; zero provider-specific code
- DTO layer: Request + Response DTOs on both services; manual mapper pattern (no MapStruct in Phase 1)
- Global `@ControllerAdvice` + Problem+JSON replaces current silent 500s on both services
- OWASP API Security Top 10 controls in scope: API1 (BOLA), API3, API5 (BFLA), API6, API7, API9
- Testcontainers seeding via CQL init scripts — no Docker images with baked-in data

### PRD Completeness Assessment

The PRD is thorough and production-quality: 38 FRs clearly numbered, 24 measurable NFRs, 5 detailed user journeys, explicit OWASP controls, phased scoping (Phase 1 vs Phase 2), and risk mitigations. No gaps identified.

## Epic Coverage Validation

### Coverage Matrix

| FR | PRD Requirement (summary) | Epic Coverage | Status |
|---|---|---|---|
| FR01 | OpenAPI 3.1 machine-readable spec | Epic 1 — Story 1.5 | ✅ Covered |
| FR02 | Swagger UI on both services | Epic 1 — Story 1.5 | ✅ Covered |
| FR03 | ReDoc interface on both services | Epic 1 — Story 1.5 | ✅ Covered |
| FR04 | Spectral linting on every build | Epic 2 — Story 2.4 | ✅ Covered |
| FR05 | URI versioning `/api/v1/` | Epic 1 — Story 1.4 | ✅ Covered |
| FR06 | Contract diff on every PR | Epic 2 — Story 2.5 | ✅ Covered |
| FR07 | Deprecation policy with sunset dates | Epic 1 — Story 1.6 | ✅ Covered |
| FR08 | Header-based versioning (documented, optional) | Epic 1 — Story 1.6 | ✅ Covered |
| FR09 | Controller unit tests — no Cassandra | Epic 3 — Story 3.1 | ✅ Covered |
| FR10 | Service unit tests — mocked DAO/repo | Epic 3 — Stories 3.2 + 3.3 | ✅ Covered |
| FR11 | Integration tests — Testcontainers + seed data | Epic 3 — Story 3.4 | ✅ Covered |
| FR12 | BDD Gherkin feature file authoring | Phase 2 — Explicitly deferred | ⏭️ Deferred |
| FR13 | BDD scenarios against Testcontainers snapshot | Phase 2 — Explicitly deferred | ⏭️ Deferred |
| FR14 | Consumer contract testing | Phase 2 — Explicitly deferred | ⏭️ Deferred |
| FR15 | Zero critical CVE build gate (OWASP Dep Check) | Epic 3 — Story 3.4 | ✅ Covered |
| FR16 | JWT via any RFC 7519-compliant provider | Epic 4 — Story 4.1 | ✅ Covered |
| FR17 | Provider-agnostic JWKS config | Epic 4 — Story 4.1 | ✅ Covered |
| FR18 | `ROLE_WRITER` grants write access | Epic 4 — Story 4.2 | ✅ Covered |
| FR19 | `ROLE_WRITER` denial + error response | Epic 4 — Story 4.2 | ✅ Covered |
| FR20 | Per-route, per-user rate limiting | Epic 4 — Story 4.3 | ✅ Covered |
| FR21 | 429 + machine-readable error with retry guidance | Epic 4 — Story 4.3 | ✅ Covered |
| FR22 | BOLA — cross-user resource access prevention | Epic 4 — Story 4.4 | ✅ Covered |
| FR23 | BFLA — function-level role enforcement | Epic 4 — Story 4.5 | ✅ Covered |
| FR24 | RFC 7807 Problem+JSON on all error paths | Epic 2 — Story 2.1 | ✅ Covered |
| FR25 | Stable error codes (ERR-PRODUCT-001 etc.) | Epic 2 — Story 2.2 | ✅ Covered |
| FR26 | traceId propagation across services | Epic 2 — Story 2.3 | ✅ Covered |
| FR27 | `Retry-After` header on rate-limit responses | Epic 4 — Story 4.3 | ✅ Covered |
| FR28 | Error catalog versioned in codebase | Epic 2 — Story 2.2 | ✅ Covered |
| FR29 | Request DTO — no internal field binding | Epic 1 — Stories 1.2 + 1.3 | ✅ Covered |
| FR30 | Response DTO — no internal model leakage | Epic 1 — Stories 1.2 + 1.3 | ✅ Covered |
| FR31 | Bean Validation at DTO boundary (400 + Problem+JSON) | Epic 1 — Stories 1.2 + 1.3 | ✅ Covered |
| FR32 | DTO decouples domain evolution from API contract | Epic 1 — Stories 1.2 + 1.3 | ✅ Covered |
| FR33 | No internal entity fields in responses (OWASP API3) | Epic 1 — Stories 1.2 + 1.3 | ✅ Covered |
| FR34 | API versioning prevents undocumented access (OWASP API9) | Epic 1 — Story 1.4 | ✅ Covered |
| FR35 | Docker Compose one-command local start | Epic 5 — Story 5.1 | ✅ Covered |
| FR36 | Cucumber feature files as living docs | Phase 2 — Explicitly deferred | ⏭️ Deferred |
| FR37 | Operations runbook | Epic 5 — Story 5.2 | ✅ Covered |
| FR38 | OpenAPI-driven client generation guide | Epic 5 — Story 5.3 | ✅ Covered |

### Missing Requirements

None. All Phase 1 FRs have traceable story coverage.

### Coverage Statistics

- Total PRD FRs: 38
- Phase 1 FRs: 34
- Phase 2 FRs (explicitly deferred): 4 (FR12, FR13, FR14, FR36)
- Phase 1 FRs covered in epics: **34 / 34**
- Coverage percentage: **100%**

## UX Alignment Assessment

### UX Document Status

Not found — and correctly absent. The PRD explicitly classifies this project as **API Backend** with no user-facing UI layer. All five user journeys in the PRD describe interactions via API clients, curl commands, Swagger UI (provided by SpringDoc), and terminal tools. No web or mobile UI is implied or required.

### Alignment Issues

None.

### Warnings

None. The absence of a UX document is intentional and consistent across PRD, Architecture, and Epics. Swagger UI and ReDoc serve as the developer-facing "UI" and are fully covered by Epic 1 Stories 1.5.

## Epic Quality Review

### Epic Structure Validation

#### Epic 1: Governed API Contract & DTO Boundary
- **User value:** ✅ Consumer-centric — "trust the API contract" is a clear developer/consumer outcome
- **Independence:** ✅ Standalone — versioned DTOs + OpenAPI spec deliver value with no downstream dependency
- **Brownfield gate story (1.1):** ✅ Acceptable — Spring Boot upgrade is the integration point with the existing system, correctly placed as Story 1 of Epic 1

#### Epic 2: API Governance, Error Standards & Observability
- **User value:** ✅ "Every error path returns consistent Problem+JSON" — directly reduces debugging friction for consumers
- **Independence:** ✅ Builds on Epic 1 only — all cross-epic dependencies are on previous epics

#### Epic 3: Test Pyramid — Unit & Integration Coverage
- **User value:** ✅ "Run full suite in under 3 minutes with zero external dependency" — developer productivity outcome
- **Independence:** ✅ Tests the surface built in Epics 1+2; no dependency on Epics 4 or 5

#### Epic 4: Security, Authorization & Rate Protection
- **User value:** ✅ "Security posture is a build artifact" — platform engineer and consumer trust outcome
- **Independence:** ✅ Secures the tested API surface from Epics 1-3

#### Epic 5: Developer Experience & Operational Readiness
- **User value:** ✅ "Onboard in under one working day" — concrete, measurable new-developer outcome
- **Independence:** ✅ References prior epic outputs correctly; no future epic dependencies

### Story Dependency Analysis

| Story | Depends On | Forward Dep? |
|---|---|---|
| 1.1 | Nothing | ✅ None |
| 1.2–1.6 | Previous stories in Epic 1 | ✅ None |
| 2.1–2.5 | Epic 1 + previous stories | ✅ None |
| 3.1–3.4 | Epics 1+2 | ✅ None |
| 4.1–4.5 | Epic 1 + previous stories in Epic 4 | ✅ None |
| 5.1–5.3 | Previous epics | ✅ None |

**No forward dependencies found. ✅**

### Database/Entity Creation Timing
- Products table: Created by `ProductDao` on startup (existing behaviour, preserved) ✅
- Orders schema: Loaded from `orders-schema.cql` at startup (existing behaviour, preserved) ✅
- No upfront "create all tables" anti-pattern ✅

### Violations Found

#### 🔴 Critical Violations
None.

#### 🟠 Major Issues
None.

#### 🟡 Minor Concerns

**MC-01 — Incremental error response shape (by design):**
Stories 1.2/1.3 ACs specify HTTP 400 (correctly, without Problem+JSON shape — that's Story 2.1). Story 3.1 ACs specify "400 + Problem+JSON" which is valid because Epic 2 completes before Epic 3. The ordering is correct but implicit. Dev agents should implement epics in sequence: 1 → 2 → 3 → 4 → 5.
*Recommendation:* Document implementation order in Sprint Planning. No structural change needed.

**MC-02 — Story 3.4 bundles Testcontainers + OWASP:**
Pragmatic bundling of two Maven build-quality gates. Acceptable for a single dev session.
*Recommendation:* Split only if the story feels too large during Sprint Planning.

### Best Practices Compliance

| Epic | User Value | Independent | No Forward Deps | Clear ACs | FR Traceable |
|---|---|---|---|---|---|
| Epic 1 | ✅ | ✅ | ✅ | ✅ | ✅ |
| Epic 2 | ✅ | ✅ | ✅ | ✅ | ✅ |
| Epic 3 | ✅ | ✅ | ✅ | ✅ | ✅ |
| Epic 4 | ✅ | ✅ | ✅ | ✅ | ✅ |
| Epic 5 | ✅ | ✅ | ✅ | ✅ | ✅ |

## Summary and Recommendations

### Overall Readiness Status

## ✅ READY FOR IMPLEMENTATION

### Critical Issues Requiring Immediate Action

None. No critical violations, no major issues, and no missing FR coverage.

### Minor Items to Address Before or During Sprint 1

1. **MC-01 — Epic implementation order:** Document in Sprint Planning that epics must be implemented in sequence (1 → 2 → 3 → 4 → 5). Stories 1.2/1.3 deliver HTTP 400 responses; the Problem+JSON shape is added by Story 2.1. Dev agents must be aware of this incremental delivery pattern.

2. **MC-02 — Story 3.4 scope:** Monitor during Sprint Planning — if the Testcontainers + OWASP story feels too large for one dev session, split into 3.4a (Testcontainers) and 3.4b (OWASP Dependency Check).

### Recommended Next Steps

1. **Run Sprint Planning** (`bmad-sprint-planning` [SP]) — produce a sequenced sprint plan ordering all 23 stories across 5 epics, starting with Story 1.1 (Spring Boot upgrade spike, 2-day time-box)
2. **Create Story 1.1** (`bmad-create-story` [CS]) — first story in the dev cycle; gates all other Phase 1 work
3. **Begin Dev Story 1.1** (`bmad-dev-story` [DS]) — Spring Boot 2.7.x upgrade spike

### Assessment Summary

| Category | Finding | Severity |
|---|---|---|
| FR Coverage | 34/34 Phase 1 FRs covered | ✅ Pass |
| Phase 2 FRs | 4 FRs explicitly deferred (FR12, FR13, FR14, FR36) | ✅ By design |
| UX Alignment | No UX document — correctly absent for API project | ✅ Pass |
| Epic Structure | All 5 epics deliver user value, not technical milestones | ✅ Pass |
| Forward Dependencies | Zero forward dependencies across all 23 stories | ✅ Pass |
| Story Quality | All ACs use Given/When/Then, cover error conditions | ✅ Pass |
| Database Timing | No upfront table creation anti-pattern | ✅ Pass |
| Brownfield Handling | Story 1.1 handles Spring Boot upgrade correctly | ✅ Pass |
| Minor Concerns | MC-01 (epic ordering), MC-02 (Story 3.4 scope) | 🟡 Minor |

**Total issues: 2 minor concerns, 0 major, 0 critical.**

This project is cleared for Phase 4 implementation.

---
*Assessment conducted: 2026-04-18 | Project: Spring Microservices with Apache Cassandra and Kubernetes*
