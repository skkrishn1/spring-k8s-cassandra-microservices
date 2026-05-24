---
project_name: 'Spring Microservices with Apache Cassandra and Kubernetes'
user_name: 'SK'
date: '2026-04-18'
sections_completed: ['technology_stack', 'language_rules', 'framework_rules', 'testing_rules', 'quality_rules', 'workflow_rules', 'anti_patterns']
status: 'complete'
rule_count: 42
optimized_for_llm: true
---

# Project Context for AI Agents

_This file contains critical rules and patterns that AI agents must follow when implementing code in this project. Focus on unobvious details that agents might otherwise miss._

---

## Technology Stack & Versions

| Component | Technology | Version |
|-----------|-----------|---------|
| Language | Java | 11 |
| Build | Maven (multi-module) | 3.x |
| Framework | Spring Boot | 2.3.0.RELEASE |
| Gateway | Spring Cloud Gateway | 2.2.3.RELEASE |
| K8s Config | Spring Cloud Kubernetes | 1.1.3.RELEASE |
| Data (orders) | Spring Data Cassandra | 3.0.0.RELEASE |
| Data (products) | Cassandra Java Driver (OSS) | 4.8.0 |
| API Docs (products) | Springfox Swagger | 2.9.2 |
| API Docs (orders) | SpringDoc OpenAPI | 1.3.9 |
| Logging | Logback | 1.4.14 |
| JSON | Jackson | 2.15.4 |
| Testing | JUnit 5 + Mockito + MockMvc | (Spring Boot managed) |
| Containers | Docker + Minikube | - |

**Note:** Two deliberately different data access approaches are used as a teaching/demonstration pattern — do NOT unify them.

---

## Critical Implementation Rules

### Java-Specific Rules

- Target Java 11 — do NOT use Java 14+ features (records, sealed classes, text blocks).
- Use `java.util.UUID` for all Cassandra UUID fields; never use String for IDs.
- Use `java.time.Instant` for all Cassandra timestamp fields — not `java.util.Date` or `LocalDateTime`.
- Use `java.math.BigDecimal` for price/decimal fields in the Products service; `Float` is used in Orders — match the existing type per service, do not normalize.
- `@Value("${property:default}")` is the injection pattern for config — do not use `@ConfigurationProperties` beans unless already present.
- Credentials (`DB_USERNAME`, `DB_PASSWORD`) are injected as env vars from K8s secrets — never hardcode credentials; the default fallback `cassandra`/`cassandra` is for local dev only.
- All Cassandra CQL statements in `ProductDao` **must** be `PreparedStatement` fields prepared in the constructor — never prepare inside a query method.

### Framework-Specific Rules

#### Spring Boot Service (Products — `microservice-spring-boot`)
- Data access goes through `ProductDao` (not a repository interface) — injected as a `@Bean` from `SpringBootCassandraConfiguration`.
- `ProductDao` constructor creates the keyspace + table schema on startup — do NOT add a separate schema migration step.
- Astra path is gated by `astra.secure-connect-bundle != "none"` — both `sessionBuilderCustomizer` and `driverConfigLoaderBuilderCustomizer` beans must preserve this dual-path logic.
- Contact-point config uses custom `cassandra.*` properties (not `spring.data.cassandra.*`) — keep them separate.
- Swagger UI via Springfox 2.9.2; config lives in `SwaggerConfig` — do not introduce SpringDoc here.

#### Spring Data Service (Orders — `microservice-spring-data`)
- Extends `AbstractCassandraConfiguration` — do NOT use `spring.data.cassandra.*` auto-config beans that conflict with the overridden methods.
- Keyspace is created in `getKeyspaceCreations()` (skipped for Astra); table schema loaded from `orders-schema.cql` via `keyspacePopulator()` — do NOT create tables programmatically in this service.
- `SchemaAction.CREATE_IF_NOT_EXISTS` is set — do not change to `RECREATE` or `NONE`.
- `OrderRepository` uses `@RepositoryRestResource` — Spring Data REST auto-exposes CRUD; only suppress endpoints with `@RestResource(exported = false)`.
- `OrderPrimaryKey` is a composite primary key (`@PrimaryKeyClass`) with `orderId` + `productId` — always pass both when doing key-based operations.
- API docs via SpringDoc 1.3.9 — do not introduce Springfox here.

#### Gateway Service
- Routes are loaded from a K8s ConfigMap (`gateway-configmap.yml`) — do NOT hardcode routes in `application.yml` for Kubernetes deployment.
- Uses full K8s DNS for upstream URLs: `http://<service>.<namespace>.svc.cluster.local:80`.
- Management port is 8085, app port is 8080 — do not collapse them.

### Testing Rules

#### Controller Tests (`@WebMvcTest`)
- Use `@WebMvcTest(ControllerClass.class)` for isolated controller testing — do NOT load full application context.
- Mock the service layer with `@MockBean` — never mock `ProductDao` or `OrderRepository` directly in controller tests.
- Use `MockMvc` for all HTTP assertions; inject via `@Autowired`.
- Use `ObjectMapper` (injected via `@Autowired`) for JSON serialization in request bodies — do not manually build JSON strings.
- Test method naming convention: `should<ExpectedBehavior>When<Condition>` (camelCase, descriptive).
- Structure each test with Given/When/Then comments.
- Always `verify()` service method calls with exact argument matchers after assertions.

#### DAO Tests (`ProductDaoTest`)
- DAO tests require a real `CqlSession` — do NOT mock Cassandra at the DAO level.
- Must use an embedded or running Cassandra instance; do not introduce Testcontainers unless explicitly asked.

#### Service Tests (`ProductServiceTest`)
- Mock `ProductDao` with Mockito `@Mock` / `@InjectMocks` — no Spring context needed (`@ExtendWith(MockitoExtension.class)`).

#### General
- JUnit 5 (`@Test` from `org.junit.jupiter.api`) — do NOT use JUnit 4 annotations.
- No coverage threshold is configured — do not add JaCoCo or enforce minimums unless asked.
- Tests live in `src/test/java` mirroring the production package structure.

### Code Quality & Style Rules

#### Naming Conventions
- Classes: PascalCase (`ProductDao`, `OrderPrimaryKey`, `SpringBootCassandraConfiguration`)
- Methods/variables: camelCase (`findByName`, `addedToOrderTimestamp`)
- Constants: UPPER_SNAKE_CASE (`productsTableName`)
- Cassandra table/column names: snake_case (`product_name`, `last_updated`, `added_to_order_at`)
- Java field names do NOT need to match column names — use `@Column("column_name")` to map explicitly (Spring Data service)

#### Code Organization
- Package per service: `com.datastax.examples` root, sub-package per domain (`product`, `order`)
- Configuration classes stay at root package level, not inside domain packages
- One controller, one service, one DAO per domain — do not split into sub-classes
- No interface abstractions for DAO or Service unless already present

#### Comments & Documentation
- Minimal inline comments — only explain WHY (non-obvious driver/framework behavior)
- Existing block comments on config classes explain technique choices — preserve them, do not expand
- No Javadoc required on controllers, DAOs, or services

#### No Linting/Formatting Tooling Configured
- No Checkstyle, PMD, SpotBugs, or formatter plugins in the POM — do not add them
- Follow existing indentation (tabs in XML/YAML, 4-space indent in Java)

#### Lombok
- `@Data` is used on `Order` — acceptable for Spring Data entities
- Do NOT add Lombok to `microservice-spring-boot`; that service uses explicit getters/setters

### Development Workflow Rules

#### Build
- Always build from root with `mvn package` to ensure module consistency
- Use `-DskipTests` during iterative Docker image builds — tests are not part of the standard build gate yet
- Docker images built via `docker build` directly (not via `dockerfile:build` Maven goal) for Kubernetes/minikube workflow
- Image pull policy is `Never` in K8s manifests — images MUST be built against minikube's Docker daemon (`eval \`minikube docker-env\``) before deploying

#### Kubernetes Deployment
- Update Docker Hub username in `deploy/*/spring-*-deployment.yml` before applying — it is not templated
- Secrets (`db-secret`) must exist in the correct namespace before deploying — deploying without secrets causes CrashLoopBackOff
- Each service has its own namespace: `spring-boot-service`, `spring-data-service`, `gateway-service`
- Management (actuator) ports are separate from app ports — liveness/readiness probes hit actuator port with `initialDelaySeconds: 120`; do not reduce this without verifying startup time

#### Spring Profiles
- `default` → localhost Cassandra
- `docker` → `host.docker.internal` Cassandra
- `kubernetes` → ConfigMap-driven via Spring Cloud Kubernetes
- Profile is set via `SPRING_PROFILES_ACTIVE` env var in K8s deployments — do not bake it into `application.yml`

#### Astra Support
- Enabled by setting `astra.secure-connect-bundle` to a path (not `"none"`) in the ConfigMap
- Requires a separate `astracreds` K8s secret with the bundle file
- When Astra is active: skip contact-point config AND skip keyspace creation in both services

### Critical Don't-Miss Rules

#### Cassandra Data Modeling
- `products` table: partition key = `name`, clustering key = `id` — queries WITHOUT `name` are not supported (no full-table scans)
- `orders` table: composite primary key = `orderId` + `productId` — both are ALWAYS required for point lookups and deletes
- Do NOT add secondary indexes or ALLOW FILTERING — design queries around the existing key structure
- `toTimestamp(now())` is used server-side for `last_updated` in products INSERT — do not pass a client timestamp for this field

#### Two Services Must Stay Architecturally Distinct
- `microservice-spring-boot` = raw driver (`CqlSession`, `PreparedStatement`, manual mapping) — do NOT introduce Spring Data here
- `microservice-spring-data` = Spring Data Cassandra (`CassandraRepository`, entity annotations) — do NOT introduce raw `CqlSession` here
- This split is intentional as a reference/demo architecture — merging or abstracting across them breaks the educational purpose

#### Schema Management
- Products schema is created by `ProductDao` constructor — changing startup order can cause table-not-found errors
- Orders schema is loaded from `orders-schema.cql` classpath resource — schema changes go in this file, not in Java code
- Keyspace creation is skipped for Astra in BOTH services — the `astraSecureConnectBundle.equals("none")` check is the gate

#### API Surface
- Products controller returns `ResponseEntity` directly — no global exception handler exists; NullPointerExceptions from Cassandra `row.one()` will 500 silently
- Orders service exposes both Spring Data REST auto-endpoints AND manual `OrderController` endpoints — do not duplicate endpoints
- Gateway routes to both services; adding a new service requires updating `gateway-configmap.yml`

#### Security
- `DB_USERNAME` / `DB_PASSWORD` come from K8s secrets mounted as env vars — never read credentials from `application.yml` in production config
- No authentication/authorization on REST endpoints — do not assume any security layer exists

---

## Usage Guidelines

**For AI Agents:**
- Read this file before implementing any code in this project
- Follow ALL rules exactly as documented — especially the service separation rules
- When in doubt about which data access pattern to use, check which service you are in
- Update this file if new patterns emerge during implementation

**For Humans:**
- Keep this file lean and focused on agent needs
- Update when technology stack or deployment patterns change
- Remove rules that become obvious over time

_Last Updated: 2026-04-18_
