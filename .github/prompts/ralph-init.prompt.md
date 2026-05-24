---
mode: agent
description: "Ralph Init — scan the workspace and generate all Ralph Loop prompt files customised to this project. Run once when setting up a new project."
tools:
  - codebase
  - editFiles
  - runCommands
---

# Ralph Init

You are setting up the Ralph Loop system for this project from scratch.
Scan the workspace, discover everything about the project, then generate or
overwrite all Ralph Loop configuration files tailored to what you find.

Do NOT ask questions. Discover everything from the code.

---

## STEP 1 — Discover the project

Run each scan in order. Record every finding — you will use all of it in Step 2.

### 1.1 Build system and modules
- Read `pom.xml` (root) or `build.gradle` at the root
- List every sub-module with its `artifactId` and directory
- Identify the build tool: Maven (`mvn`) or Gradle (`./gradlew`)
- Record the test command per module:
  - Maven: `mvn test -pl <module>`
  - Gradle: `./gradlew :<module>:test`

### 1.2 Tech stack
- Read all `pom.xml` / `build.gradle` files in sub-modules
- Identify: Java version, Spring Boot version, database driver, ORM/data layer
- List key dependencies (test frameworks, security, serialisation, API docs)

### 1.3 Test framework
- Search `src/test/java/**` for annotations used: `@Test`, `@SpringBootTest`, `@WebMvcTest`, `@DataJpaTest`, etc.
- Identify mock library: Mockito, EasyMock, WireMock, etc.
- Find any base test classes (e.g. `Abstract*Test`, `Base*Test`) — read their content
- Find how Testcontainers is used if present (look for `@Testcontainers`, `@Container`)
- Record the exact import packages used in test files

### 1.4 Naming conventions
- Scan `src/main/java/**` for:
  - Controller classes — what suffix? (`*Controller`, `*Resource`, `*Api`?)
  - Service classes — suffix? (`*Service`, `*ServiceImpl`?)
  - Repository classes — suffix? (`*Repository`, `*Dao`?)
  - DTO classes — suffix? (`*Dto`, `*Request`, `*Response`, `*Model`?)
  - Exception classes — naming pattern?
- Read 2-3 example files of each type to understand the pattern

### 1.5 API layer
- Find port configuration in `application.yml` / `application.properties` per module
- Find management/actuator port if configured
- Find API base path (e.g. `/api/v1/`, `/api/`)
- Find API documentation tool: SpringDoc, Springfox, none

### 1.6 Database / persistence layer
- Identify database type and driver
- Find connection config in `application.yml`
- Find keyspace/schema/database name
- Find any schema init files (`.sql`, `.cql`, `schema.sql`)
- Find any custom session/connection configuration classes

### 1.7 Error handling
- Search for `@ControllerAdvice` or `@RestControllerAdvice` classes
- Find error/problem response class name used in responses
- Find error catalog if it exists (`.yml`, `.json`, `.properties`)

### 1.8 Security
- Check for Spring Security, JWT, OAuth2 in dependencies
- Find `SecurityConfig` or equivalent class
- Note what headers or tokens are propagated between services

### 1.9 Anti-patterns (forbidden patterns in this codebase)
- Read `CLAUDE.md` if it exists — extract anti-patterns section
- Read `README.md` if it exists — extract any coding rules
- Read any `CONTRIBUTING.md` or `ARCHITECTURE.md`
- Look for comments with `// DO NOT`, `// NEVER`, `// FORBIDDEN` in source files

### 1.10 Existing story/planning artifacts
- Check if `planning-artifacts/` exists
- List any existing files: `epics.md`, `prd.md`, `architecture.md`, `project-context.md`
- Check if `planning-artifacts/stories/` directory exists

---

## STEP 2 — Generate all Ralph Loop files

Using ONLY what you discovered in Step 1, generate or overwrite these files.
Replace every `<placeholder>` with the actual discovered value.
Do not invent values — if something was not found, write `# TODO: not discovered — fill in manually`.

### 2.1 Generate `.github/copilot-instructions.md`

```markdown
# <project-name> — Copilot Instructions

This project uses the **BMAD method** with a **Ralph Loop** for autonomous implementation.

## Tech Stack
- Language: <java-version>
- Framework: <spring-boot-version>
- Database: <database-type> — <connection-detail>
- Build: <maven-or-gradle>
- API docs: <springdoc-or-springfox-or-none>

## Modules
| Module | Directory | Port | Purpose |
|---|---|---|---|
<one row per discovered module>

## Test Command
```
<exact test command per module>
```

## Agent Map
```
epics.md ──► ralph-story-prep ──► story file (ready-for-dev)
                                        │
                                   ralph-loop ◄──────────┐
                                        │                 │
                              code+tests  fail            │
                                        │                 │
                                   tests pass    ralph-fix-agent
                                        │
                            bmad-governance-approval
```

## Prompt Skills
| Step | Prompt | Invoke |
|---|---|---|
| Init (first time) | `.github/prompts/ralph-init.prompt.md` | "run ralph init" |
| Prep story | `.github/prompts/ralph-story-prep.prompt.md` | "prepare story N.N from epics" |
| Run Ralph | `.github/prompts/ralph-loop.prompt.md` | "run ralph loop on <story-file>" |
| Fix failures | `.github/prompts/ralph-fix-agent.prompt.md` | called by Ralph automatically |
| Review | `.github/prompts/bmad-governance-approval.prompt.md` | "review <story-file>" |
```

### 2.2 Overwrite `.github/prompts/ralph-story-prep.prompt.md`

Replace the Dev Notes template section with project-specific content:

```markdown
### Test Approach
- Base test class: <discovered base class or "none">
- Annotations: <@ExtendWith(MockitoExtension.class) or @SpringBootTest etc>
- Mock library: <Mockito / WireMock / etc>
- Testcontainers: <yes — singleton via <BaseClass> / no>
- Run command: <exact mvn/gradle test command>

### Naming Conventions
- Controller suffix: <*Controller / *Resource>
- Service suffix: <*Service / *ServiceImpl>
- Repository suffix: <*Repository / *Dao>
- Request DTO: <*RequestDto / *Request / *Dto>
- Response DTO: <*ResponseDto / *Response>
- Exception class: <*Exception / *Error>

### Database Pattern
- Type: <Cassandra / PostgreSQL / MySQL / MongoDB>
- Keyspace/schema: <discovered name>
- Session/connection class: <discovered class>
- Schema init: <file path or "programmatic">

### Error Handling
- Problem/error response class: <discovered class name>
- Error catalog location: <file path or "none">
- ControllerAdvice class: <discovered class>

### API Pattern
- Base path: <discovered base path>
- Security: <JWT / Basic / None — detail>
- Propagated headers: <list or "none">

### Anti-patterns (NEVER do these)
<list every anti-pattern discovered from CLAUDE.md, README, comments>
```

### 2.3 Overwrite `.github/prompts/ralph-loop.prompt.md`

Update the test run command in the LOOP section:
```
Run: <exact test command for the module>
```

### 2.4 Overwrite `.github/prompts/bmad-test-agent.prompt.md`

Update the test output format section with the project's actual test class pattern:
```java
<actual import statements from discovered test files>
<actual base class if any>
<actual annotation pattern used in this project>
```

---

## STEP 3 — Create missing directories

If `planning-artifacts/stories/` does not exist:
- Create `planning-artifacts/stories/.gitkeep`

If `planning-artifacts/sprint-status.yaml` does not exist, create it:
```yaml
# Sprint Status — managed by Ralph Loop
# Run ralph-planner-orchestrator to populate from epics.md
last_updated: <today's date>
development_status: {}
```

---

## STEP 4 — Print the init report

```
╔══════════════════════════════════════════════════════════╗
║              RALPH INIT COMPLETE                         ║
╠══════════════════════════════════════════════════════════╣
║ Project      : <name>                                    ║
║ Modules      : <N> discovered                            ║
║ Tech stack   : <Java version> / <framework> / <database> ║
║ Test runner  : <command>                                 ║
║ Base class   : <base test class or none>                 ║
╠══════════════════════════════════════════════════════════╣
║ Files generated                                          ║
║  ✅ .github/copilot-instructions.md                      ║
║  ✅ .github/prompts/ralph-story-prep.prompt.md           ║
║  ✅ .github/prompts/ralph-loop.prompt.md                 ║
║  ✅ .github/prompts/bmad-test-agent.prompt.md            ║
╠══════════════════════════════════════════════════════════╣
║ TODOs (not auto-discovered — fill in manually)           ║
║  <list anything that returned # TODO>                    ║
╠══════════════════════════════════════════════════════════╣
║ Next step                                                ║
║  #file:.github/prompts/ralph-story-prep.prompt.md        ║
║  prepare story 1.1 from planning-artifacts/epics.md      ║
╚══════════════════════════════════════════════════════════╝
```

---

## HOW TO INVOKE

In Copilot Agent Mode (any project, first time setup):

```
#file:.github/prompts/ralph-init.prompt.md
run ralph init
```

Run this once per project. Re-run whenever the tech stack changes significantly.
