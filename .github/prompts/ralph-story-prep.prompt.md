---
mode: agent
description: "Ralph Story Prep — given existing epics, create a dev-ready story file with all context Ralph needs to run without halting"
tools:
  - codebase
  - editFiles
---

# Ralph Story Prep

Prepares a story file so Ralph Loop can run it fully autonomously — no missing context, no ambiguity.

## Input

User specifies which story from `planning-artifacts/epics.md` to prepare.

## What to read before writing

1. `planning-artifacts/epics.md` — find the story and its AC list
2. `planning-artifacts/architecture.md` — identify relevant components
3. `planning-artifacts/project-context.md` — coding standards, patterns
4. `planning-artifacts/prd.md` — any relevant FR/NFR
5. Existing source files in the relevant module — understand current patterns to follow

## Story file output

Save to: `planning-artifacts/stories/<epic>-<story>-<slug>.md`

```markdown
# Story <epic>.<story>: <Title>

**Status:** ready-for-dev
**Epic:** <epic title>
**Module:** microservice-spring-boot | microservice-spring-data | gateway-service

## Story
As a <persona>, I want <goal>, so that <benefit>.

## Acceptance Criteria
- [ ] AC1: <specific, testable criterion>
- [ ] AC2: <specific, testable criterion>

## Tasks/Subtasks

- [ ] Task 1: <description>
  - [ ] 1.1 Write failing test for <behaviour>
  - [ ] 1.2 Implement <class/method>
  - [ ] 1.3 Verify test passes

- [ ] Task 2: <description>
  - [ ] 2.1 ...

## Dev Notes

### Architecture Context
- Pattern to follow: <cite existing file:line as example>
- Anti-patterns forbidden: <from project constraints>
- Key files to edit: <exact paths>

### Test Approach
- Test class location: `src/test/java/<package>/<ClassName>Test.java`
- Framework: JUnit 5 + Mockito (`@ExtendWith(MockitoExtension.class)`)
- For Cassandra DAOs: extend `AbstractCassandraIntegrationTest` (singleton Testcontainers)
- Run command: `mvn test -pl <module> -Dtest=<TestClass>`

### Cassandra Notes (if applicable)
- Keyspace: `betterbotz`
- Local DC: `dc1`
- Never use ALLOW FILTERING
- Never call session.execute() inside query methods — use PreparedStatements prepared in constructor

### DTO Pattern
- Request: `<Domain>RequestDto` with `toEntity()` method
- Response: `<Domain>ResponseDto` with static `fromEntity()` factory
- No entity classes in @RequestBody or ResponseEntity

### Error Handling
- Use `ProblemResponse` (not Spring's ProblemDetail)
- Error codes from `src/main/resources/error-catalog.yml`

## Dev Agent Record
### Implementation Notes
<!-- Ralph fills this in -->

### File List
<!-- Ralph fills this in — relative paths from repo root -->

## Change Log
| Date | Change |
|------|--------|
```

## Quality gate before saving

Confirm the story has:
- [ ] Every AC is testable (not vague)
- [ ] Every task maps to at least one AC
- [ ] Dev Notes has exact file paths Ralph will edit
- [ ] Test run command is correct for this module
- [ ] No missing context that would cause Ralph to HALT

## How to invoke

```
#file:.github/prompts/ralph-story-prep.prompt.md
prepare story 1.1 from the epics — Spring Boot upgrade spike
```
