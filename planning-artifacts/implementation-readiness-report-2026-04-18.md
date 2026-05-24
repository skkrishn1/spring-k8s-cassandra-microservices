---
stepsCompleted: ['step-01-document-discovery', 'step-02-prd-analysis', 'step-03-epic-coverage-validation', 'step-04-ux-alignment', 'step-05-epic-quality-review', 'step-06-final-assessment']
date: '2026-04-18'
project: 'Spring Microservices with Apache Cassandra and Kubernetes'
documents:
  prd: 'planning-artifacts/prd.md'
  architecture: null
  epics: null
  ux: null
---

# Implementation Readiness Assessment Report

**Date:** 2026-04-18
**Project:** Spring Microservices with Apache Cassandra and Kubernetes

## Document Inventory

| Document Type | File | Status |
|---|---|---|
| PRD | planning-artifacts/prd.md | Complete (12 steps, polished) |
| Architecture | — | Not yet created |
| Epics & Stories | — | Not yet created |
| UX Design | — | Not applicable (API-only project) |
| Project Context | planning-artifacts/project-context.md | Complete (42 rules) |

## PRD Analysis

### Functional Requirements (38 total)

#### 1. API Contract Management (FR01–FR08)

| ID | Requirement | Phase |
|----|-------------|-------|
| FR01 | API consumers can discover all endpoints/schemas/examples via machine-readable OpenAPI 3.1 spec | 1 |
| FR02 | API consumers can interactively explore and test all endpoints via Swagger UI | 1 |
| FR03 | API consumers can read structured, human-friendly docs via ReDoc interface | 1 |
| FR04 | API producers can validate OpenAPI spec against naming, pagination, error schema, and versioning rules via automated linting on every build | 1 |
| FR05 | API consumers can access all endpoints under a stable, versioned URI path (`/api/v1/`) | 1 |
| FR06 | API teams can detect breaking and non-breaking contract changes automatically on every PR | 1 |
| FR07 | API consumers can rely on a documented deprecation policy with sunset dates for versioned endpoints | 1 |
| FR08 | API consumers can access endpoints under an optional header-based versioning scheme (documented, Phase 1 optional) | 1 |

#### 2. Testing & Quality Assurance (FR09–FR15)

| ID | Requirement | Phase |
|----|-------------|-------|
| FR09 | Developers can run unit tests for all controllers with no running Cassandra or external dependency | 1 |
| FR10 | Developers can run unit tests for all service-layer components with mocked DAO/repository dependencies | 1 |
| FR11 | Developers can run integration tests against a real, containerized Cassandra instance using reproducible seed data | 1 |
| FR12 | QA engineers can author BDD test scenarios in plain-language Gherkin without writing Java code | **2** |
| FR13 | QA engineers can execute BDD scenarios against a pre-seeded, containerized Cassandra dataset without a live cluster | **2** |
| FR14 | API consumers can verify consumer contracts against producer API changes via automated contract testing | **2** |
| FR15 | Build pipeline can enforce a zero-critical-CVE gate and fail the build on known critical vulnerabilities | 1 |

#### 3. Security & Authorization (FR16–FR23)

| ID | Requirement | Phase |
|----|-------------|-------|
| FR16 | Authenticated users can access API endpoints using a JWT issued by any RFC 7519-compliant OAuth2 provider | 1 |
| FR17 | The system validates JWT authenticity, issuer, and audience without code changes when switching providers | 1 |
| FR18 | Users holding `ROLE_WRITER` claim can execute write operations (`POST`, `PUT`, `DELETE`) on both services | 1 |
| FR19 | Users without `ROLE_WRITER` are denied write operations and receive a machine-readable error response | 1 |
| FR20 | All API routes are protected against excessive request volume on a per-user, per-route basis | 1 |
| FR21 | Clients exceeding rate limits receive a machine-readable error response including retry guidance | 1 |
| FR22 | System prevents unauthorized cross-user resource access at service layer (OWASP API1 — BOLA) | 1 |
| FR23 | System prevents users from invoking functions beyond their role (OWASP API5 — BFLA) | 1 |

#### 4. Error Handling & Observability (FR24–FR28)

| ID | Requirement | Phase |
|----|-------------|-------|
| FR24 | API consumers receive standardized RFC 7807-compliant `Problem+JSON` for all error scenarios (4xx and 5xx) | 1 |
| FR25 | API consumers can identify the specific error type via stable, versioned machine-readable error code (e.g., `ERR-PRODUCT-001`) | 1 |
| FR26 | Operations teams can correlate errors and requests across services using a propagated `traceId` | 1 |
| FR27 | API consumers can determine appropriate retry behaviour from rate-limit error responses (`Retry-After` header) | 1 |
| FR28 | Error catalog is versioned and co-located with the codebase — new codes added via code, not manual documentation | 1 |

#### 5. Data Access & Domain Integrity (FR29–FR34)

| ID | Requirement | Phase |
|----|-------------|-------|
| FR29 | API consumers can submit requests containing only explicitly defined public API contract fields — no internal field binding | 1 |
| FR30 | API responses expose only fields explicitly defined in the public API contract — no internal model leakage | 1 |
| FR31 | System rejects requests with invalid field values at the API boundary with descriptive, machine-readable error (OWASP API6) | 1 |
| FR32 | Developers can evolve internal domain models and Cassandra schema without changing the public API contract | 1 |
| FR33 | System prevents exposure of internal entity fields in API responses (OWASP API3) | 1 |
| FR34 | System prevents undocumented or deprecated API endpoints from being silently accessible (OWASP API9) | 1 |

#### 6. Developer Experience & Onboarding (FR35–FR38)

| ID | Requirement | Phase |
|----|-------------|-------|
| FR35 | New developers can start all services locally using a single Docker Compose command — no manual Cassandra setup | 1 |
| FR36 | New developers can understand expected API behaviour by reading Cucumber feature files — no Java code required | **2** |
| FR37 | Operations teams can perform standard operational tasks by following a runbook — no tribal knowledge required | 1 |
| FR38 | API consumers can generate typed client libraries from the published OpenAPI specification without SDK involvement | 1 |

**Phase 1 FRs: 34 | Phase 2 FRs: 4 (FR12, FR13, FR14, FR36)**

---

### Non-Functional Requirements (24 total)

#### Performance (NFR-P01–P05)

| ID | Requirement |
|----|-------------|
| NFR-P01 | Read endpoints return responses within **200ms** at p95 under normal load |
| NFR-P02 | Write endpoints complete within **500ms** at p95 under normal load |
| NFR-P03 | Full unit + integration test suite completes within **3 minutes** in CI |
| NFR-P04 | AFT suite (Phase 2) completes within **5 minutes** in CI including Testcontainers startup |
| NFR-P05 | OpenAPI spec generation adds no more than **2 seconds** to application startup time |

#### Security (NFR-S01–S07)

| ID | Requirement |
|----|-------------|
| NFR-S01 | All API traffic is JWT-authenticated — unauthenticated requests rejected at gateway with `401` |
| NFR-S02 | JWT validation uses asymmetric signing (RS256 minimum) — HS256 not permitted |
| NFR-S03 | All credentials injected via env vars or K8s secrets — zero secrets in source code or committed config |
| NFR-S04 | Maven build fails on critical CVSS ≥ 9.0 CVE — high CVEs generate warning but do not fail |
| NFR-S05 | Actuator endpoints accessible only on management port — never on public API port |
| NFR-S06 | All HTTP responses include security headers: `X-Content-Type-Options`, `X-Frame-Options`, `Cache-Control` on authenticated routes |
| NFR-S07 | Rate limiting enforced in-memory at gateway — no shared state (Redis deferred to Phase 2+) |

#### Scalability (NFR-SC01–SC03)

| ID | Requirement |
|----|-------------|
| NFR-SC01 | Architecture supports horizontal scaling of each service independently — no shared in-process state |
| NFR-SC02 | Cassandra connection pooling handles at least **10 concurrent requests** per service instance without exhaustion |
| NFR-SC03 | Spring Boot upgrade path to 3.x is not blocked by new Phase 1 code — no 2.3-specific APIs introduced |

#### Reliability (NFR-R01–R05)

| ID | Requirement |
|----|-------------|
| NFR-R01 | Testcontainers-based tests are deterministic and idempotent — same CQL scripts produce identical results every run |
| NFR-R02 | No test has dependency on execution order — each test sets up and tears down its own state |
| NFR-R03 | Build pipeline is reproducible — same source commit produces identical result on any CI runner |
| NFR-R04 | Spectral linting rules are pinned to a fixed ruleset version — no unexpected upstream rule additions |
| NFR-R05 | Application startup fails fast with clear error if required env vars (`JWKS_URI`, `DB_USERNAME`, `DB_PASSWORD`) are missing |

#### Integration (NFR-I01–I04)

| ID | Requirement |
|----|-------------|
| NFR-I01 | OAuth2 Resource Server supports any JWKS-endpoint-compatible provider — switching requires only env var changes |
| NFR-I02 | Testcontainers uses a pinned Cassandra image version — no `latest` tag in test configuration |
| NFR-I03 | Docker Compose stack starts all services in correct dependency order (Cassandra before app services) without manual intervention |
| NFR-I04 | Spectral CLI is executable in CI without network call to external service — rules bundled in repository |

### Additional Requirements & Constraints

- **Spring Boot upgrade to 2.7.x** is a Phase 1 prerequisite gate — Sprint 1 technical spike determines feasibility
- **OWASP API Security Top 10:** API1 (BOLA), API3, API5 (BFLA), API6, API7, API9 — all six must have corresponding Cucumber AFT scenarios (Phase 2 for Cucumber, Phase 1 for RBAC enforcement)
- **Error catalog** `src/main/resources/error-catalog.yml` — 10 initial codes defined in PRD
- **Rate limits:** 100 req/min read / 20 req/min write per authenticated user — configurable via ConfigMap
- **DTO mapper layer** (manual or MapStruct) — explicit architecture decision deferred to implementation
- **Keycloak (Docker Compose)** recommended as dev/CI JWT provider — swap via Spring profile for production
- **Two-tier AFT:** Tier 1 Docker Compose (Phase 1), Tier 2 Minikube (Phase 2) — same Cucumber files, different base URL

### PRD Completeness Assessment

**Strengths:**
- 38 FRs with clear phase assignment (Phase 1 vs Phase 2)
- 24 NFRs with specific measurable targets (p95 latencies, time bounds, CVSS thresholds)
- Error catalog pre-defined with 10 initial codes
- Risk mitigations documented for each major risk
- OWASP controls explicitly mapped to implementation approach and test coverage
- User journeys trace to specific requirements
- Technology constraints clearly inherited from brownfield project-context.md (42 rules)

**Gaps / Areas for Architecture to Resolve:**
- No architecture document yet — traceability to implementation components pending
- Mapper layer strategy (manual vs MapStruct) left as implementation decision
- `trace-id` propagation mechanism (MDC/Sleuth/manual header) not specified
- OpenAPI diff tooling choice not specified (openapi-diff, oasdiff, or custom Spectral rule)
- Spectral ruleset scope not defined (which core rules + which custom rules)
- BOLA (API1) enforcement pattern at service layer not specified (no multi-tenant model exists currently)

## Epic Coverage Validation

**No epics document found** — this is expected; epics have not been created yet. The PRD is the current terminal planning artifact.

### Coverage Matrix

| FR | Requirement (summary) | Phase | Epic Coverage | Status |
|----|----------------------|-------|---------------|--------|
| FR01 | OpenAPI 3.1 spec generation | 1 | No epics exist | ⬜ PENDING |
| FR02 | Swagger UI on both services | 1 | No epics exist | ⬜ PENDING |
| FR03 | ReDoc interface on both services | 1 | No epics exist | ⬜ PENDING |
| FR04 | Spectral linting in CI | 1 | No epics exist | ⬜ PENDING |
| FR05 | URI versioning `/api/v1/` | 1 | No epics exist | ⬜ PENDING |
| FR06 | OpenAPI contract diff on PRs | 1 | No epics exist | ⬜ PENDING |
| FR07 | Deprecation policy in OpenAPI spec | 1 | No epics exist | ⬜ PENDING |
| FR08 | Header-based versioning (optional) | 1 | No epics exist | ⬜ PENDING |
| FR09 | Controller unit tests (`@WebMvcTest`) | 1 | No epics exist | ⬜ PENDING |
| FR10 | Service-layer unit tests (Mockito) | 1 | No epics exist | ⬜ PENDING |
| FR11 | Integration tests with Testcontainers | 1 | No epics exist | ⬜ PENDING |
| FR12 | Cucumber/Gherkin BDD authoring | **2** | No epics exist | ⬜ PENDING |
| FR13 | AFT execution against Cassandra snapshot | **2** | No epics exist | ⬜ PENDING |
| FR14 | Spring Cloud Contract consumer tests | **2** | No epics exist | ⬜ PENDING |
| FR15 | OWASP/Snyk dependency scan gate | 1 | No epics exist | ⬜ PENDING |
| FR16 | JWT auth via RFC 7519 OAuth2 provider | 1 | No epics exist | ⬜ PENDING |
| FR17 | Provider-agnostic JWT config | 1 | No epics exist | ⬜ PENDING |
| FR18 | RBAC `ROLE_WRITER` on write ops | 1 | No epics exist | ⬜ PENDING |
| FR19 | Deny write ops without `ROLE_WRITER` | 1 | No epics exist | ⬜ PENDING |
| FR20 | Rate limiting per user/route | 1 | No epics exist | ⬜ PENDING |
| FR21 | 429 response with retry guidance | 1 | No epics exist | ⬜ PENDING |
| FR22 | BOLA prevention (OWASP API1) | 1 | No epics exist | ⬜ PENDING |
| FR23 | BFLA prevention (OWASP API5) | 1 | No epics exist | ⬜ PENDING |
| FR24 | Global RFC 7807 Problem+JSON error model | 1 | No epics exist | ⬜ PENDING |
| FR25 | Stable error code per error type | 1 | No epics exist | ⬜ PENDING |
| FR26 | `traceId` propagation across services | 1 | No epics exist | ⬜ PENDING |
| FR27 | `Retry-After` header on 429 | 1 | No epics exist | ⬜ PENDING |
| FR28 | Error catalog versioned in codebase | 1 | No epics exist | ⬜ PENDING |
| FR29 | Request DTO — no internal field binding | 1 | No epics exist | ⬜ PENDING |
| FR30 | Response DTO — no entity leakage | 1 | No epics exist | ⬜ PENDING |
| FR31 | Request validation at API boundary | 1 | No epics exist | ⬜ PENDING |
| FR32 | Internal model evolution decoupled from API contract | 1 | No epics exist | ⬜ PENDING |
| FR33 | No internal entity fields in responses (API3) | 1 | No epics exist | ⬜ PENDING |
| FR34 | No silent deprecated/undocumented endpoints (API9) | 1 | No epics exist | ⬜ PENDING |
| FR35 | Docker Compose single-command local start | 1 | No epics exist | ⬜ PENDING |
| FR36 | Cucumber feature files as living docs | **2** | No epics exist | ⬜ PENDING |
| FR37 | Runbook for operational tasks | 1 | No epics exist | ⬜ PENDING |
| FR38 | Typed client generation from OpenAPI spec | 1 | No epics exist | ⬜ PENDING |

### Coverage Statistics

| Metric | Count |
|--------|-------|
| Total PRD FRs | 38 |
| Phase 1 FRs | 34 |
| Phase 2 FRs | 4 |
| FRs covered in epics | 0 |
| Coverage percentage | 0% — **epics not yet authored** |

> **Note:** 0% coverage is the correct and expected state at this point in the workflow. The next step is `bmad-create-architecture` followed by `bmad-create-epics`. These 38 FRs represent the complete input scope for epic decomposition.

## UX Alignment Assessment

### UX Document Status

**Not Found** — no UX documentation exists in `planning-artifacts/`.

### UX Applicability Assessment

This is a **pure API backend project** (classification: `api_backend` in PRD frontmatter). There is no user-facing web or mobile UI — the "users" are developers and API consumers interacting via:
- Swagger UI (auto-generated from SpringDoc annotations)
- ReDoc (auto-generated from OpenAPI spec)
- REST clients / generated SDK clients

### Warnings

| Severity | Finding |
|----------|---------|
| INFO | No UX documentation — **not required** for this project type. API documentation surfaces (Swagger UI, ReDoc) are defined as FRs in the PRD and are sufficient. |
| INFO | Developer Experience (DX) — Journey 2 (Tariq, onboarding) and Journey 3 (Amara, test authoring) are the closest analog to UX concerns. Both are addressed via FR35 (Docker Compose), FR37 (runbook), FR02 (Swagger UI), FR03 (ReDoc), and FR36 (Cucumber feature files as living docs, Phase 2). |

**Assessment:** No UX gaps. The PRD fully accounts for all developer-facing documentation and tooling surfaces appropriate to an API backend project.

## Epic Quality Review

**No epics document found** — this step is a forward-looking quality readiness assessment. Rather than flagging violations (there are no epics to violate), this section documents the quality standards that MUST be applied when epics are authored in `bmad-create-epics`.

### Brownfield Context Requirements

This is a **brownfield project** — epics must include:
- Integration points with the existing codebase (ProductDao, OrderRepository, SpringBootCassandraConfiguration, AbstractCassandraConfiguration)
- Migration stories where patterns change (e.g., Springfox → SpringDoc, entity-bound controllers → DTO-bound controllers)
- No regressions allowed — existing API surface (ports, paths) must remain functional throughout each epic

### Pre-authoring Quality Standards (to enforce in bmad-create-epics)

#### 🔴 Critical Rules — Must Not Violate

| Rule | Applied to This Project |
|------|------------------------|
| Epics deliver **user value**, not technical milestones | "API Consumer can discover all endpoints via OpenAPI 3.1" ✓ vs "Migrate to SpringDoc" ✗ |
| Epic N cannot require Epic N+1 | DTO layer (Epic A) must not require Error Handling (Epic B) to be "done" first |
| Stories are independently completable | Each controller DTO migration completable per-controller, not as a batch |
| No forward dependencies within an epic | Story 1.2 "Add Swagger UI" must not reference Story 1.4 "Configure SpringDoc security" |

#### 🟠 Major Standards — Must Apply

| Standard | Applied to This Project |
|----------|------------------------|
| Stories create DB/schema artefacts only when first needed | CQL init scripts created in the story that first requires that test data — not all upfront |
| Acceptance criteria use Given/When/Then | All security stories must test both allowed and denied paths |
| Error conditions explicitly covered in ACs | Each DTO story must AC the 400 + Problem+JSON rejection path |
| Phase 2 FRs must NOT appear in Phase 1 epics | FR12, FR13, FR14, FR36 are explicitly Phase 2 — no Cucumber, no contract tests in Phase 1 epics |

#### 🟡 Minor Standards

| Standard | Applied to This Project |
|----------|------------------------|
| FR traceability maintained on each story | Tag each story with its FR(s): `Implements: FR01, FR05` |
| Brownfield stories call out files being modified | "Modify `ProductController.java` to add `ProductRequestDto`" not just "Add DTO" |
| Two-service architectural split preserved | Separate stories for Products service and Orders service — never one story modifying both |

### Recommended Epic Structure for Phase 1

Based on the 34 Phase 1 FRs across 3 pillars, the following epic groupings are recommended input for `bmad-create-epics`:

| Suggested Epic | FRs Covered | Pillar |
|---------------|-------------|--------|
| Epic 1: Spring Boot 2.7.x Upgrade + SpringDoc Migration | FR01, FR02, FR03, FR05, FR38 | API Contract |
| Epic 2: DTO Layer + Request/Response Boundary | FR06, FR29, FR30, FR31, FR32, FR33 | API Contract |
| Epic 3: Global Error Model + Error Catalog | FR24, FR25, FR26, FR27, FR28 | API Contract |
| Epic 4: API Versioning + Spectral Linting | FR04, FR05, FR07, FR08, FR34 | API Contract |
| Epic 5: Unit + Integration Test Pyramid | FR09, FR10, FR11, FR15 | Testing |
| Epic 6: JWT Auth + RBAC at Gateway | FR16, FR17, FR18, FR19, FR22, FR23 | Security |
| Epic 7: Rate Limiting + Security Headers | FR20, FR21 | Security |
| Epic 8: Developer Experience + Runbook | FR35, FR37 | DX |

> **Note:** This is a suggested grouping for architecture / epic planning — not a mandate. `bmad-create-epics` should validate and adjust based on architectural decisions.

---

## Summary and Recommendations

### Overall Readiness Status

> **PRD-READY / PROCEED TO ARCHITECTURE**

The PRD is complete, internally consistent, and fit for downstream planning. The project is at the correct workflow stage — no epics or architecture exist yet, which is the expected state. All gaps identified are forward-looking inputs for the next two workflows, not defects in the PRD.

### Findings Summary

| Category | Finding | Severity | Action Required |
|----------|---------|----------|----------------|
| Documents | Architecture not yet created | INFO | Next workflow: `bmad-create-architecture` |
| Documents | Epics not yet authored | INFO | After architecture: `bmad-create-epics` |
| Documents | UX not applicable (API-only project) | INFO | None |
| PRD | 34 Phase 1 FRs fully specified with measurable targets | PASS | None |
| PRD | 4 Phase 2 FRs explicitly deferred (FR12, FR13, FR14, FR36) | PASS | Ensure not included in Phase 1 epics |
| PRD | 24 NFRs with specific numeric targets | PASS | None |
| PRD | OWASP API1, 3, 5, 6, 7, 9 explicitly mapped to controls | PASS | None |
| PRD | 10 error codes pre-defined in error catalog | PASS | None |
| Gaps | Mapper strategy (manual vs MapStruct) unresolved | LOW | Architecture decision |
| Gaps | `traceId` propagation mechanism unspecified | LOW | Architecture decision |
| Gaps | OpenAPI diff tooling not selected | LOW | Architecture decision |
| Gaps | Spectral ruleset scope not defined | LOW | Architecture decision |
| Gaps | BOLA enforcement pattern undefined (no multi-tenant model) | MEDIUM | Architecture + epic design |
| Epic Quality | Phase 1/Phase 2 FR boundary must be respected in epics | MEDIUM | Enforce at `bmad-create-epics` time |
| Epic Quality | Two-service architectural split must be preserved in all stories | MEDIUM | Enforce at `bmad-create-epics` time |

### Critical Issues Requiring Immediate Action

**None.** The PRD is production-quality and ready for architecture planning.

### Recommended Next Steps

1. **Run `bmad-create-architecture`** — resolve the 5 open architectural decisions (mapper strategy, traceId mechanism, OpenAPI diff tooling, Spectral ruleset scope, BOLA enforcement pattern). The architecture document is the PRD's direct successor and will feed epics.

2. **Conduct Sprint 1 Technical Spike: Spring Boot 2.7.x upgrade assessment** — this is the PRD's explicit first-gate prerequisite. Time-box to 2 days. Outcome determines whether Phase 1 proceeds on 2.7.x or stays on 2.3.x with forward-compatible patterns.

3. **Run `bmad-create-epics`** after architecture is complete — use the 8-epic grouping from the Epic Quality Review section as the starting input. Enforce: Phase 2 FR exclusion, two-service story separation, brownfield integration story requirements.

4. **Pin the Keycloak Docker Compose configuration early** — the PRD designates Keycloak as the dev/CI JWT provider. Having this running locally unblocks security epic stories from Sprint 2 onwards.

### Final Note

This assessment identified **5 medium/low gaps** across 2 categories (PRD gaps and epic quality pre-standards). All 5 are forward-looking inputs for the architecture and epics phases — none require changes to the PRD. The PRD itself has **0 defects**. The assessment confirms: the project is ready to proceed to `bmad-create-architecture`.

---

**Assessment completed:** 2026-04-18
**Assessor:** Claude Code (bmad-check-implementation-readiness v6.3.0)
**Report location:** `planning-artifacts/implementation-readiness-report-2026-04-18.md`
