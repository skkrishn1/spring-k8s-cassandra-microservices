# BMAD Project — Copilot Instructions

This project uses the **BMAD method** with a **Ralph Loop** for autonomous implementation.

## Agent Map

```
epics.md ──► ralph-story-prep ──► story file (ready-for-dev)
                                        │
                                   ralph-loop ◄──────────────┐
                                        │                     │
                              ┌─────────┴──────────┐         │
                         code+tests            test fails     │
                              │                    │          │
                         tests pass         ralph-fix-agent   │
                              │                    │          │
                         TRACE written        fix applied ────┘
                              │
                    all tasks [x] done
                              │
                   bmad-governance-approval
                              │
                    APPROVED → PR ready
```

## Prompt Skills

### Starting point: you have stories

| Step | Prompt | Command |
|---|---|---|
| 1. Prep story | `.github/prompts/ralph-story-prep.prompt.md` | "prepare story 1.1 from epics" |
| 2. Run Ralph | `.github/prompts/ralph-loop.prompt.md` | "run the ralph loop on planning-artifacts/stories/1-1-<slug>.md" |
| 3. Fix failures | `.github/prompts/ralph-fix-agent.prompt.md` | called automatically by Ralph |
| 4. Review | `.github/prompts/bmad-governance-approval.prompt.md` | "review planning-artifacts/stories/1-1-<slug>.md" |

### Full suite

| Prompt File | Purpose |
|---|---|
| `.github/prompts/ralph-story-prep.prompt.md` | Epic → dev-ready story file |
| `.github/prompts/ralph-loop.prompt.md` | **Main loop**: pick task → RED → GREEN → FIX → TRACE → repeat |
| `.github/prompts/ralph-fix-agent.prompt.md` | Diagnose test failure → minimal fix → verify |
| `.github/prompts/bmad-governance-approval.prompt.md` | Adversarial review → APPROVED / BLOCKED |
| `.github/prompts/bmad-test-agent.prompt.md` | Generate missing tests for existing code |
| `.github/prompts/bmad-planner-orchestrator.prompt.md` | Epics → sprint-status.yaml |
| `.github/prompts/bmad-amelia-dev.prompt.md` | Amelia persona (single-story TDD implementation) |
| `.github/prompts/bmad-create-story.prompt.md` | Simple story file creator |

## Demo flow (you have stories already)

```
# Step 1 — prep one story from your epics
#file:.github/prompts/ralph-story-prep.prompt.md
prepare story 1.1 (Spring Boot upgrade spike) from planning-artifacts/epics.md

# Step 2 — run the loop
#file:.github/prompts/ralph-loop.prompt.md
run the ralph loop on planning-artifacts/stories/1-1-spring-boot-upgrade.md

# Step 3 — approve
#file:.github/prompts/bmad-governance-approval.prompt.md
review planning-artifacts/stories/1-1-spring-boot-upgrade.md
```

## Project context

- Products service: `microservice-spring-boot/` (port 8083) — raw CqlSession + PreparedStatement
- Orders service: `microservice-spring-data/` (port 8081) — Spring Data CassandraRepository
- Gateway: `gateway-service/` (port 8080)
- Stories: `planning-artifacts/stories/`
- Epics: `planning-artifacts/epics.md`
- Test command: `mvn test -pl microservice-spring-boot`
- Cassandra keyspace: `betterbotz`, DC: `dc1`
