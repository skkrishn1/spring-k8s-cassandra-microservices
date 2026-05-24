# Ralph Loop Runbook
## BMAD Method + Copilot Agent Mode — Governed Implementation System

> **Scope:** This runbook covers the Ralph Loop implementation pipeline.
> The loop reads stories one by one from each epic, and for each story executes an
> ordered cycle: pick task → write failing tests → implement code → self-correct on
> failure → trace results → advance to the next task — until all tasks in the story
> are complete. Governance review and human approval follow each completed story.
> GitHub Copilot Agent Mode is the execution engine.

---

## 0. Ralph Diagram → This Implementation

| **Diagram Step** | **This Project** | **File** |
|---|---|---|
| You write a PRD | `planning-artifacts/prd.md` | Already exists |
| Convert to `prd.json` | `prd.json` — stories with `passes: false/true` | `prd.json` |
| Run `ralph.sh` | `#file:.github/prompts/ralph-loop.prompt.md` in Copilot | `ralph-loop.prompt.md` |
| AI picks a story (`passes: false`) | Ralph reads lowest `priority` where `passes: false` | Built into prompt |
| Implements it — writes code, runs tests | Copilot edits files, runs `mvn test` | Built into prompt |
| Updates `prd.json` (`passes: true`) | Sets `passes: true` in `prd.json` after commit | Built into prompt |
| Commits changes (if tests pass) | `git commit -m "feat: [US-001] - <title>"` | Built into prompt |
| Logs to `progress.txt` | Appends story summary + learnings | `progress.txt` |
| Updates `AGENTS.md` with patterns | Codebase Patterns section in `progress.txt` | `progress.txt` |
| More stories? Loop / Done | `<promise>COMPLETE</promise>` when all `passes: true` | Built into prompt |

**One difference from original Ralph:** `ralph.sh` spawns a fresh AI instance per story (bash loop).
Copilot Agent Mode runs in one session — user says "continue" between stories for the demo.
With Claude Code, this becomes fully autonomous.

---

## 1. Agent / Skill Reference

| **Agent / Skill** | **Primary Role** | **Critical Inputs** | **Critical Outputs** | **Prompt File** |
|---|---|---|---|---|
| **Story Parser** | Parse BMAD epics into structured, dev-ready story files | `planning-artifacts/epics.md`; `architecture.md`; `project-context.md` | `planning-artifacts/stories/<n>-<n>-<slug>.md` with ACs, tasks, Dev Notes | `ralph-story-prep.prompt.md` |
| **Task Generator** | Emit ordered task/subtask list with acceptance criteria mapped to test expectations | Parsed story file; architecture refs; existing source patterns | Tasks/Subtasks section in story file; test run command | `ralph-story-prep.prompt.md` |
| **Planner Orchestrator** | Order all stories across epics, compute dependencies, emit sprint tracking | `epics.md`; existing story files in `planning-artifacts/stories/` | `planning-artifacts/sprint-status.yaml` with ordered status entries | `bmad-planner-orchestrator.prompt.md` |
| **Codegen Worker** | Execute code edits using Copilot Agent Mode for each task in the loop | Task description; target file paths from Dev Notes; repo workspace | Modified source files; test files; `[x]` marked tasks in story | `ralph-loop.prompt.md` |
| **Test Agent** | Generate and run tests mapped to every AC in the story | AC list from story file; production class under test | New/updated test files under `src/test/`; `mvn test` pass confirmation | `bmad-test-agent.prompt.md` |
| **Self-Correction Agent** | Diagnose test failures and iterate with targeted fixes until pass or attempt limit | Test failure output; failing source file; error message + line | Patched source file; test re-run result; attempt count | `ralph-fix-agent.prompt.md` |
| **Governance Agent** | Adversarial three-layer review enforcing spec compliance, edge cases, blind review | Git diff; story AC list; `project-context.md` | Findings triage (HIGH/MEDIUM/LOW); AC compliance matrix | `bmad-governance-approval.prompt.md` |
| **Trace Recorder** | Produce canonical execution record in the story file after each task | Codegen output; test results; file list | Dev Agent Record updated; File List updated; Change Log entry | Built into `ralph-loop.prompt.md` |
| **Approval Agent** | Render final APPROVED / CHANGES REQUESTED / BLOCKED decision and gate next steps | Governance findings; AC compliance matrix; test results | Decision record in story; review findings written back for fix cycle | `bmad-governance-approval.prompt.md` |

---

## 2. Artifact Reference

| **Artifact** | **Location** | **Produced By** | **Consumed By** |
|---|---|---|---|
| Epics file | `planning-artifacts/epics.md` | BMAD planning phase | Story Parser, Planner Orchestrator |
| Architecture doc | `planning-artifacts/architecture.md` | BMAD planning phase | Story Parser (Dev Notes) |
| Project context | `planning-artifacts/project-context.md` | BMAD planning phase | All agents |
| Story file | `planning-artifacts/stories/<n>-<n>-<slug>.md` | Story Parser | Codegen Worker, Test Agent, Governance Agent |
| Sprint status | `planning-artifacts/sprint-status.yaml` | Planner Orchestrator | Ralph Loop (picks next story) |
| Dev Agent Record | Section inside story file | Trace Recorder | Governance Agent, Approval Agent |
| File List | Section inside story file | Trace Recorder | Governance Agent (diff scope) |
| Review findings | Section inside story file | Governance Agent | Self-Correction Agent (fix cycle) |

---

## 3. End-to-End Flow

```
┌─────────────────────────────────────────────────────────┐
│                    PHASE 0 — PLAN                        │
│                                                         │
│  epics.md ──► Planner Orchestrator ──► sprint-status    │
└────────────────────────┬────────────────────────────────┘
                         │
┌────────────────────────▼────────────────────────────────┐
│                    PHASE 1 — PREP                        │
│                                                         │
│  epics.md ──► Story Parser + Task Generator             │
│                    │                                    │
│                    ▼                                    │
│            story file (ready-for-dev)                   │
│            ├── Acceptance Criteria                      │
│            ├── Tasks/Subtasks                           │
│            └── Dev Notes (file paths, patterns)         │
└────────────────────────┬────────────────────────────────┘
                         │
┌────────────────────────▼────────────────────────────────┐
│                  PHASE 2 — RALPH LOOP                    │
│                                                         │
│  ┌──────────────────────────────────────────────────┐   │
│  │  PICK next unchecked task                        │   │
│  │       │                                          │   │
│  │       ▼                                          │   │
│  │  RED  ── write failing tests ── run mvn test     │   │
│  │       │                                          │   │
│  │       ▼                                          │   │
│  │  GREEN ── implement minimal code ── run tests    │   │
│  │       │                    │                     │   │
│  │       │               FAIL │                     │   │
│  │       │                    ▼                     │   │
│  │       │         Self-Correction Agent            │   │
│  │       │         fix → rerun (max 3×)             │   │
│  │       │                    │                     │   │
│  │       │         STILL FAIL │                     │   │
│  │       │                    ▼                     │   │
│  │       │              ⛔ HALT                     │   │
│  │       │                                          │   │
│  │    PASS ▼                                        │   │
│  │  REFACTOR ── clean up ── run full module tests   │   │
│  │       │                                          │   │
│  │       ▼                                          │   │
│  │  TRACE ── mark [x] ── update File List           │   │
│  │          ── update Dev Agent Record              │   │
│  │       │                                          │   │
│  │       ▼                                          │   │
│  │  more tasks? ──YES──► PICK (loop)                │   │
│  │       │                                          │   │
│  │      NO                                          │   │
│  └───────┼──────────────────────────────────────────┘   │
│          │                                              │
│          ▼                                              │
│     full regression ── set Status: review              │
└────────────────────────┬────────────────────────────────┘
                         │
┌────────────────────────▼────────────────────────────────┐
│                  PHASE 3 — GOVERNANCE                    │
│                                                         │
│  Governance Agent runs 3 review layers in parallel:     │
│  ├── Acceptance Auditor  (AC by AC compliance)          │
│  ├── Edge Case Hunter    (boundary + error paths)       │
│  └── Blind Hunter        (no-spec adversarial scan)     │
│                    │                                    │
│                    ▼                                    │
│           Findings triage                               │
│           HIGH ──► BLOCKED ──► fix cycle ──► re-review  │
│           MED  ──► CHANGES REQUESTED ──► optional fix   │
│           LOW  ──► noted only                           │
│                    │                                    │
│                    ▼                                    │
│             Approval Agent decision                     │
│             APPROVED ──► PR ready                       │
└─────────────────────────────────────────────────────────┘
```

---

## 4. Invocation Commands (Copilot Agent Mode)

### Phase 0 — Sprint Planning
```
#file:.github/prompts/bmad-planner-orchestrator.prompt.md
run sprint planning from planning-artifacts/epics.md
```

### Phase 1 — Prepare a Story
```
#file:.github/prompts/ralph-story-prep.prompt.md
prepare story 1.1 (Spring Boot upgrade) from planning-artifacts/epics.md
```

### Phase 2 — Run Ralph Loop
```
#file:.github/prompts/ralph-loop.prompt.md
run the ralph loop on planning-artifacts/stories/1-1-spring-boot-upgrade.md
```

### Phase 2 (manual) — Run Fix Agent
```
#file:.github/prompts/ralph-fix-agent.prompt.md
fix the test failures in microservice-spring-boot
```

### Phase 2 (manual) — Generate Missing Tests
```
#file:.github/prompts/bmad-test-agent.prompt.md
generate tests for planning-artifacts/stories/1-1-spring-boot-upgrade.md
```

### Phase 3 — Governance Review
```
#file:.github/prompts/bmad-governance-approval.prompt.md
review planning-artifacts/stories/1-1-spring-boot-upgrade.md
```

---

## 5. HALT Conditions Reference

| **Condition** | **Triggered By** | **Output** | **Resolution** |
|---|---|---|---|
| 3 consecutive test failures | Self-Correction Agent exhausted | `⛔ HALT: 3 fix attempts exhausted on Task N` | Manually fix + re-run Ralph Loop |
| Dependency not in story | Codegen Worker detects missing dep | `⛔ HALT: Dependency not approved — <name>` | Add dep to story Dev Notes, re-run |
| Required file not found | Codegen Worker reads Dev Notes | `⛔ HALT: Cannot find <file>` | Check file path in Dev Notes |
| Architecture conflict | Codegen Worker detects pattern violation | `⛔ HALT: Conflicts with architecture` | Check `architecture.md`, clarify story |
| Empty diff on review | Governance Agent | `⛔ HALT: Nothing to review` | Ensure story was implemented first |
| BLOCKED decision | Approval Agent finds HIGH finding | `BLOCKED: <reason>` | Fix Agent → re-run Governance |

---

## 6. Story File Lifecycle

```
not-started  ──►  ready-for-dev  ──►  in-progress  ──►  review  ──►  done
     │                  │                   │               │
  (epic only)    (Story Parser        (Ralph Loop       (Governance
                  created file)        started)          approved)
```

**Status values in `sprint-status.yaml`:**

| Status | Set By | Meaning |
|---|---|---|
| `not-started` | Planner Orchestrator | Story not yet created |
| `ready-for-dev` | Story Parser (after file created) | Ralph Loop can pick it |
| `in-progress` | Ralph Loop (on start) | Currently implementing |
| `review` | Ralph Loop (on completion) | Awaiting governance |
| `done` | Approval Agent (APPROVED) | Merged, complete |

---

## 7. Copilot Agent Mode — Capability Matrix

| **Capability** | **Supported** | **Notes** |
|---|---|---|
| Read story/epic files | ✅ Full | Via `#file:` or `@workspace` |
| Edit source + test files | ✅ Full | Agent mode native |
| Run `mvn test` | ✅ Full | Requires user "Allow" per run |
| Read terminal output | ✅ Full | Informs fix logic |
| Update story file checkboxes | ✅ Full | Edits markdown in place |
| True autonomous loop | ⚠️ Partial | User types "continue" between tasks |
| Spawn sub-agents natively | ❌ No | Fix logic is inline in Ralph Loop |
| Persist state across sessions | ❌ No | Each chat session is independent |
| Auto-update sprint-status.yaml | ⚠️ Manual | Prompt explicitly or update manually |

> **Upgrading to full autonomy:** Replace Copilot Agent Mode with Claude Code.
> All prompt files are forward-compatible — swap `#file:` invocation for native `/skill` invocation.

---

## 8. File Map

```
.github/
├── copilot-instructions.md          ← loaded automatically by Copilot
├── RALPH-LOOP-RUNBOOK.md            ← this file
└── prompts/
    ├── ralph-story-prep.prompt.md   ← Story Parser + Task Generator
    ├── ralph-loop.prompt.md         ← Codegen Worker + Trace Recorder (main loop)
    ├── ralph-fix-agent.prompt.md    ← Self-Correction Agent
    ├── bmad-test-agent.prompt.md    ← Test Agent
    ├── bmad-governance-approval.prompt.md  ← Governance Agent + Approval Agent
    ├── bmad-planner-orchestrator.prompt.md ← Planner Orchestrator
    ├── bmad-amelia-dev.prompt.md    ← Amelia (single-story TDD, no loop)
    └── bmad-create-story.prompt.md  ← Simple story creator (lightweight)

planning-artifacts/
├── epics.md                         ← source of truth for all stories
├── architecture.md                  ← consumed by Story Parser
├── project-context.md               ← consumed by all agents
├── prd.md                           ← consumed by Story Parser (FR/NFR mapping)
├── sprint-status.yaml               ← produced by Planner Orchestrator
└── stories/
    └── <n>-<n>-<slug>.md           ← produced by Story Parser, run by Ralph Loop
```
