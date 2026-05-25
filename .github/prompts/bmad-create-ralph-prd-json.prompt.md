---
mode: agent
description: "BMAD → Ralph handoff: convert planning-artifacts/epics.md into prd.json for ONE epic at a time. Closes the documented gap in RALPH-LOOP-RUNBOOK.md row 18."
tools:
  - codebase
  - editFiles
---

# bmad-create-ralph-prd-json (Copilot Agent Mode)

Take BMAD planning artifacts and emit a Ralph-consumable `prd.json` for ONE epic
at a time. This is the bridge between BMAD planning output and the Ralph
autonomous implementation loop.

> This prompt is the GitHub Copilot Agent Mode variant of the
> `bmad-create-ralph-prd-json` Claude Code skill at
> `.claude/skills/bmad-create-ralph-prd-json/workflow.md`. Both produce
> identical output. Use whichever your team's runtime supports.

---

## Contract — prd.json schema (LOCKED, from snarktank/ralph)

```json
{
  "project": "<string>",
  "branchName": "ralph/<kebab-case>",
  "description": "<string>",
  "userStories": [
    {
      "id": "US-NNN",
      "title": "<string>",
      "description": "As a <persona>, I want <goal>, so that <benefit>.",
      "acceptanceCriteria": ["<verifiable>", "..."],
      "priority": <integer>,
      "passes": false,
      "notes": ""
    }
  ]
}
```

**Hard rules:**
1. **One-iteration sizing** — each story fits in one Ralph context window.
2. **Dependency ordering** by `priority` — schema → data → service → controller → UI.
3. **Every AC must be verifiable** — no "works well", no "is robust".
4. **Every story ends with two ACs:** the project build-passes AC, then `"Typecheck passes"`.
5. **`passes` always `false`**, **`notes` always `""`** — Ralph populates them.

---

## Inputs

| Input | Required | Source |
|---|---|---|
| Epic number/title | Yes | User argument |
| `planning-artifacts/epics.md` | Yes | BMAD output |
| `planning-artifacts/prd.md` | Optional | For epic-level context |
| `planning-artifacts/architecture.md` | Optional | For technical guardrails |
| `planning-artifacts/project-context.md` | Optional | For naming conventions |
| Root build file | Yes | For `project` field + build-passes AC |

---

## Workflow

### STEP 1 — Validate prerequisites

1. Read `planning-artifacts/epics.md`. If missing:
   `⛔ HALT: planning-artifacts/epics.md not found — run bmad-create-epics-and-stories first.`
2. Detect build tool by checking the repo root:
   - `pom.xml` → Maven, build cmd `./mvnw clean package -DskipTests`
   - `build.gradle` or `build.gradle.kts` → Gradle, `./gradlew build -x test`
   - `package.json` → derive from `scripts.build`, fall back to `npm run build`
   - `Cargo.toml` → `cargo build`
   - `pyproject.toml` → `poetry build` (or `python -m build`)
   - Unknown → `⛔ HALT: no recognized build file at repo root.`
3. Derive `project` string:
   - Maven: root `pom.xml` `<artifactId>` (fall back to `<name>`)
   - Gradle: `rootProject.name`
   - npm/pnpm/yarn: `name` in root `package.json`
   - Otherwise: current directory basename

### STEP 2 — Resolve target epic

1. Enumerate epics from `epics.md` (look for `^##\s+Epic\s+\d+:` or `^###\s+Epic\s+\d+:`).
2. If no epic argument provided, print the list and HALT:
   ```
   Available epics:
     1. <title>  (<N> stories)
     2. <title>  (<N> stories)
   Re-invoke with the epic number.
   ```
3. If epic doesn't exist: `⛔ HALT: epic <N> not in epics.md — available: <list>.`
4. Extract epic title, one-line description, and ordered story list.
5. If epic has zero stories: `⛔ HALT: epic <N> has no stories defined.`

### STEP 3 — Archive existing prd.json (Ralph archive protocol)

1. Compute new `branchName`: `ralph/epic-<N>-<kebab-slug-of-epic-title>` (max 60 chars).
2. If `prd.json` exists:
   - Read its current `branchName`.
   - If same as new → no archive (regenerating same epic).
   - If different →
     - Create `archive/<YYYY-MM-DD>-<old-branchname-slug>/`
     - Move existing `prd.json` and `progress.txt` (if present) there
     - Print `📦 Archived previous prd.json to <path>`
3. If archive fails: `⛔ HALT: cannot archive existing prd.json — manual cleanup needed.`

### STEP 4 — Build each story entry

For each story in the epic, construct:

#### 4a. `id`
Sequential, zero-padded: `US-001`, `US-002`, … Always restart at `US-001` per epic.

#### 4b. `title`
Story heading text, stripped of markdown. ≤80 chars.

#### 4c. `description` — MUST be "As a / I want / so that"
- If story already uses this format → copy verbatim.
- Otherwise reshape it. Persona defaults to "developer".
- Validate: starts with "As a " or "As an ", contains "I want", contains "so that".
- If unshapeable: `⛔ HALT: story <id> description not reshapeable — rewrite in user-story format.`

#### 4d. `acceptanceCriteria` (array)
- Extract from "Acceptance Criteria" / "ACs" / "Done When" section.
- For each AC:
  - Reject vague language ("works", "is fast", "is robust", "handles edge cases" without specifics).
  - On rejection: `⛔ HALT: story <id> has vague AC: "<text>" — rewrite as a verifiable statement.`
  - Reshape into active, verifiable form.
- **Always append two final ACs:**
  - Build-passes AC, e.g., `"Build passes: ./mvnw clean package -DskipTests on all modules"`
  - `"Typecheck passes"`

#### 4e. `priority`
- Sequential integer starting at 1, in DEPENDENCY ORDER:
  - Schema/migration → data/persistence → service → controller/API → UI
- If you re-order against the document, log the reason in the final report.

#### 4f. `passes`
Always `false`. Ralph toggles to `true` itself.

#### 4g. `notes`
Always empty string.

### STEP 5 — Story-sizing sanity check

For each story, estimate the implementation surface:
- Distinct files implied in ACs (count class names, file paths)
- Distinct layers touched (DAO + service + controller + test = 4)
- Rough LOC estimate (concerns × ~120)

If a story:
- Names >10 distinct files, OR
- Touches >3 layers, OR
- Estimates >500 LOC

→ `⛔ HALT: story <id> exceeds one-iteration sizing — split into smaller stories.
Suggested split: <propose 2–3 stories>.`

This rule is from upstream Ralph: spawning fresh instances per story means
oversized stories produce incomplete code.

### STEP 6 — Top-level fields

- `project`: from Step 1.3
- `branchName`: from Step 3.1
- `description`: `"Epic <N> — <epic title>: <one-line epic summary>"`

### STEP 7 — Write & validate prd.json

1. Pretty-print with 2-space indent.
2. Write to repo root as `prd.json` (overwrite — Step 3 archived).
3. Validate JSON parseability. If invalid: `⛔ HALT: generated prd.json failed JSON validation.`
4. Validate schema: every story has all 7 fields, `passes:false`, `priority` unique & sequential from 1.

### STEP 8 — Print handoff report

```
╔══════════════════════════════════════════════════════════╗
║              PRD → JSON CONVERSION COMPLETE              ║
╠══════════════════════════════════════════════════════════╣
║ Source       : planning-artifacts/epics.md (Epic <N>)    ║
║ Output       : prd.json                                  ║
║ Project      : <project>                                 ║
║ Branch       : <branchName>                              ║
║ Stories      : <N> converted, all passes:false           ║
║ Build cmd    : <build command embedded in ACs>           ║
║ Archived     : <archive path or "none — first run">      ║
╠══════════════════════════════════════════════════════════╣
║ Stories (in priority order)                              ║
║  1. US-001  <title>                                      ║
║  2. US-002  <title>                                      ║
║  ...                                                     ║
╠══════════════════════════════════════════════════════════╣
║ Reordering notes (if any)                                ║
║  <explain any deviations from document order>            ║
╠══════════════════════════════════════════════════════════╣
║ Next step                                                ║
║   git checkout -b <branchName>                           ║
║   #file:.github/prompts/ralph-loop.prompt.md             ║
║   run the ralph loop                                     ║
╚══════════════════════════════════════════════════════════╝
```

---

## HALT conditions (consolidated)

| Trigger | Output |
|---|---|
| `planning-artifacts/epics.md` missing | `⛔ HALT: ... run bmad-create-epics-and-stories first.` |
| No build file at root | `⛔ HALT: no recognized build file at repo root.` |
| No epic argument | Print epic list, HALT for user input |
| Epic doesn't exist | `⛔ HALT: epic <N> not in epics.md.` |
| Epic has no stories | `⛔ HALT: epic <N> has no stories defined.` |
| Description unshapeable | `⛔ HALT: story <id> description not reshapeable.` |
| Vague AC | `⛔ HALT: story <id> has vague AC: "<text>".` |
| Story too large | `⛔ HALT: story <id> exceeds one-iteration sizing.` |
| Archive failed | `⛔ HALT: cannot archive existing prd.json.` |
| JSON validation failed | `⛔ HALT: generated prd.json failed JSON validation.` |

---

## Quality gate before returning

- [ ] `prd.json` is valid JSON.
- [ ] Every story has all 7 required fields.
- [ ] Every `description` starts with "As a "/"As an " and contains "I want" + "so that".
- [ ] Every story's `acceptanceCriteria` ends with the build AC + `"Typecheck passes"`.
- [ ] `priority` values unique, sequential, from 1.
- [ ] `branchName` matches `^ralph/epic-\d+-[a-z0-9-]+$` and is ≤60 chars.
- [ ] No story flagged as oversized.
- [ ] Existing prd.json from a different branch was archived (or first run).

---

## How to invoke

```
#file:.github/prompts/bmad-create-ralph-prd-json.prompt.md
convert epic 2 from epics.md to prd.json
```

(Optionally name a specific epic title instead of number.)
