# bmad-create-ralph-prd-json — Workflow

**Goal:** Take BMAD planning artifacts (`planning-artifacts/epics.md` and friends)
and emit a Ralph-consumable `prd.json` for ONE epic at a time. This closes the
documented gap between BMAD planning output and the Ralph autonomous loop input
(see `.github/RALPH-LOOP-RUNBOOK.md` row 18 — "Convert to `prd.json`").

**Your role:** You are a technical writer and product engineer working with the
user to translate epics into the smallest, most verifiable, most strictly-ordered
unit of work Ralph can execute autonomously. You enforce the contract; you do not
negotiate it.

---

## Contract — the prd.json schema (LOCKED)

This schema is mandated by the upstream Ralph skill
(`snarktank/ralph/skills/ralph/SKILL.md`). Do not deviate. Do not add fields.
Do not omit fields.

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
      "acceptanceCriteria": ["<verifiable statement>", "..."],
      "priority": <integer>,
      "passes": false,
      "notes": ""
    }
  ]
}
```

**Hard rules from upstream Ralph:**

1. **One-iteration sizing.** Each story must be completable in one Ralph context
   window. If a story names >10 new files or implies >500 LOC of changes, it's
   too large — split it before generating prd.json.
2. **Dependency ordering by `priority`.** Database/schema work precedes backend
   logic, which precedes UI. Never reverse. Lower priority number = runs first.
3. **Every AC must be verifiable.** No "works well", no "is fast", no "looks
   good". An AC that can't be programmatically checked is a defect in the AC.
4. **Every story must include `Typecheck passes` and a build-passes AC** as the
   final two acceptance criteria. This is the safety floor.
5. **`passes` always starts `false`.** Ralph toggles it to `true` after a clean
   commit. Never seed `true` values.
6. **`notes` always starts empty.** Ralph appends learnings as it runs.

---

## Inputs

| Input | Required? | Source |
|---|---|---|
| Epic number/title to convert | Yes | User argument (e.g., "epic 2") |
| `planning-artifacts/epics.md` | Yes | BMAD output of `bmad-create-epics-and-stories` |
| `planning-artifacts/prd.md` | Optional | For epic-level context if epics.md is terse |
| `planning-artifacts/architecture.md` | Optional | For technical guardrails — informs AC wording |
| `planning-artifacts/project-context.md` | Optional | For project-specific conventions |
| Root build file (`pom.xml`, `build.gradle`, `package.json`) | Yes | For `project` field + build-passes AC command |

---

## Workflow

Run each step in order. Do not skip. Do not optimize.

### Step 1 — Validate prerequisites

1. Check `planning-artifacts/epics.md` exists. If not:
   `⛔ HALT: planning-artifacts/epics.md not found — run bmad-create-epics-and-stories first.`
2. Detect the build tool by checking for `pom.xml` (Maven), `build.gradle[.kts]`
   (Gradle), `package.json` (npm/pnpm/yarn), `Cargo.toml` (Rust),
   `pyproject.toml` (Python). Record the build command:
   - Maven: `./mvnw clean package -DskipTests` (or `mvn` if no wrapper)
   - Gradle: `./gradlew build -x test`
   - npm: `npm run build`
   - pnpm: `pnpm build`
   - yarn: `yarn build`
   - Cargo: `cargo build`
   - Python (poetry): `poetry build`
   - If unknown: `⛔ HALT: no recognized build file at repo root — cannot derive build-passes AC.`
3. Determine `project` value:
   - Maven: root `pom.xml` `<artifactId>` (fall back to `<name>`)
   - Gradle: `rootProject.name` in `settings.gradle[.kts]`
   - npm/pnpm/yarn: `name` in root `package.json`
   - Otherwise: current directory basename

### Step 2 — Resolve target epic

1. Parse `planning-artifacts/epics.md`. Find epic headers matching the regex
   `^##\s+Epic\s+(\d+):` (or `^###\s+Epic\s+(\d+):` if epics are nested).
2. If the user provided no epic argument, print the available epics and HALT:
   ```
   Available epics in epics.md:
     1. <Epic 1 title>  (<N> stories)
     2. <Epic 2 title>  (<N> stories)
     ...
   Re-invoke with the epic number, e.g., "create prd.json for epic 2".
   ```
3. If the requested epic doesn't exist:
   `⛔ HALT: epic <N> not in epics.md — available: <list>.`
4. Extract the epic's title, one-line description, and story list.
5. If the epic has zero stories:
   `⛔ HALT: epic <N> has no stories defined — add stories before converting.`

### Step 3 — Archive existing prd.json if branchName conflicts

Per upstream Ralph's archive protocol:

1. Compute the new `branchName`:
   `ralph/epic-<N>-<kebab-case-of-epic-title>` (truncate to 60 chars).
   Examples:
   - Epic 2 "Testing Enhancements" → `ralph/epic-2-testing-enhancements`
   - Epic 3 "Security & Auth" → `ralph/epic-3-security-and-auth`
2. If `prd.json` exists at repo root:
   a. Read its current `branchName`.
   b. If it equals the new branchName → continue (no archive needed; you're
      regenerating for the same epic).
   c. If it differs:
      - Create `archive/<YYYY-MM-DD>-<old-branchname-slug>/`
      - Move existing `prd.json` and `progress.txt` (if it exists) there
      - Print confirmation: `📦 Archived previous prd.json to <archive-path>`
3. If archive creation fails:
   `⛔ HALT: cannot archive existing prd.json — manual cleanup needed.`

### Step 4 — Extract stories into the schema

For each story in the epic (in document order), build a `userStories[]` entry:

#### 4a. Assign `id`
- Sequential, zero-padded 3-digit: `US-001`, `US-002`, `US-003`, ...
- Always start at `US-001` per epic (do NOT continue numbering across epics).

#### 4b. Set `title`
- Copy the story's heading text, stripping any markdown prefix.
- Keep it under 80 characters; rewrite for brevity if longer.

#### 4c. Set `description` — MUST be "As a / I want / so that"
- If the story already uses this format → copy verbatim.
- If not → reshape it:
  - Extract the actor (persona) — fall back to "developer" if none is stated.
  - Extract the goal (the thing to be enabled).
  - Extract the benefit (why) — derive from the epic's purpose if not explicit.
- Validate: the final string MUST start with "As a " or "As an " and contain
  "I want" and "so that". If you can't construct it, HALT and request the user
  rewrite the story.

#### 4d. Build `acceptanceCriteria` (array of strings)
- Extract the AC bullet list from the story body. Look for headings like
  "Acceptance Criteria", "ACs", "Done When", or a bullet list immediately
  after the story description.
- For each extracted AC:
  - REJECT vague ACs. Defect indicators: "works", "is fast", "looks good",
    "is robust", "handles edge cases" without specifics.
  - On rejection: `⛔ HALT: story <id> has vague AC: "<text>" — rewrite as
    a verifiable statement.`
  - Convert to active, verifiable form. Examples:
    - ❌ "Errors are handled properly"
    - ✅ "Invalid request returns 400 with field-level Problem+JSON detail"
- ALWAYS append, as the final two items:
  - The build-passes AC, e.g.:
    `"Build passes: ./mvnw clean package -DskipTests on all modules"`
  - `"Typecheck passes"` (compiler / linter / TypeScript check)

#### 4e. Assign `priority`
- Sequential integer 1, 2, 3, ... in the order stories appear in the epic.
- **DO NOT use the in-document order blindly.** Apply dependency ordering:
  - Schema/migration stories first
  - Data/persistence layer stories next
  - Service layer stories next
  - Controller / API layer stories
  - UI / client integration stories last
- If you re-order against the document, leave a comment in the printed report
  explaining why.

#### 4f. Set `passes: false`
- Always `false`. Ralph toggles to `true` itself.

#### 4g. Set `notes: ""`
- Always empty string. Ralph appends learnings as it runs.

### Step 5 — Story sizing sanity check

For each story, estimate the implementation surface from its ACs:
- Count distinct files implied (look for class names, file paths in ACs)
- Count distinct concerns (DAO + service + controller + test = 4 concerns)
- Estimate LOC from concern count × ~120 LOC each as a rough heuristic

If any story:
- Names >10 distinct files in its ACs, OR
- Touches >3 layers (DAO + service + controller + UI), OR
- Estimates >500 LOC

→ `⛔ HALT: story <id> exceeds one-iteration sizing — split into smaller
stories before regenerating prd.json. Suggested split: <propose 2–3 stories>.`

This rule comes from upstream Ralph: "spawns fresh instances with no memory,
so oversized stories result in incomplete code."

### Step 6 — Assemble top-level fields

- **`project`**: from Step 1.3
- **`branchName`**: from Step 3.1
- **`description`**: format as
  `"Epic <N> — <epic title>: <one-line epic summary from epics.md>"`

### Step 7 — Write prd.json

1. Pretty-print JSON with 2-space indent.
2. Write to repo root as `prd.json` (overwrite — Step 3 already archived).
3. Validate: re-read the file and `JSON.parse`/equivalent. If parse fails,
   `⛔ HALT: generated prd.json failed JSON validation — re-run.`
4. Validate the schema: every story has all 7 required fields, `passes` is
   `false`, `priority` values are unique sequential integers starting at 1.

### Step 8 — Print handoff report

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
║   then invoke the Ralph autonomous loop on prd.json      ║
║                                                          ║
║   Claude Code:  /ralph-loop                              ║
║   Copilot:      #file:.github/prompts/ralph-loop.prompt.md ║
║                 run the ralph loop                       ║
╚══════════════════════════════════════════════════════════╝
```

---

## HALT conditions (consolidated)

| Trigger | Output |
|---|---|
| `planning-artifacts/epics.md` missing | `⛔ HALT: planning-artifacts/epics.md not found — run bmad-create-epics-and-stories first.` |
| No build file recognized at repo root | `⛔ HALT: no recognized build file at repo root — cannot derive build-passes AC.` |
| No epic argument supplied | Print epic list, HALT for user input |
| Requested epic doesn't exist | `⛔ HALT: epic <N> not in epics.md — available: <list>.` |
| Epic has no stories | `⛔ HALT: epic <N> has no stories defined — add stories before converting.` |
| Story `description` can't be shaped as As/I want/so that | `⛔ HALT: story <id> description not reshapeable — rewrite in user-story format.` |
| Any AC is vague | `⛔ HALT: story <id> has vague AC: "<text>" — rewrite as a verifiable statement.` |
| Story exceeds one-iteration sizing | `⛔ HALT: story <id> exceeds one-iteration sizing — split into smaller stories.` |
| Existing prd.json archive fails | `⛔ HALT: cannot archive existing prd.json — manual cleanup needed.` |
| Generated prd.json fails JSON parse | `⛔ HALT: generated prd.json failed JSON validation — re-run.` |

---

## Quality gate — confirm before returning

- [ ] `prd.json` is valid JSON and re-readable.
- [ ] Every story has `id`, `title`, `description`, `acceptanceCriteria` (array
      of ≥3 strings), `priority` (integer), `passes: false`, `notes: ""`.
- [ ] Every `description` begins with "As a " or "As an " and contains both
      "I want" and "so that".
- [ ] Every story's `acceptanceCriteria` array ends with the build-passes AC
      and `"Typecheck passes"`.
- [ ] `priority` values are unique, sequential, starting at 1.
- [ ] `branchName` matches `^ralph/epic-\d+-[a-z0-9-]+$` and is ≤60 chars.
- [ ] No story was flagged as oversized.
- [ ] Existing prd.json from a different branch was archived (or this is the
      first run).

---

## How this fits the broader pipeline

```
                         ┌──────────────────────────────┐
                         │   BMAD PLANNING (per project) │
                         │                              │
                         │  bmad-create-prd ───► prd.md │
                         │  bmad-create-epics ─► epics.md │
                         └────────────┬─────────────────┘
                                      │
                                      │ (per epic)
                                      ▼
                ┌─────────────────────────────────────────┐
                │   bmad-create-ralph-prd-json (THIS SKILL)│
                │                                         │
                │   epics.md ──► prd.json                 │
                │   (one epic at a time, schema locked)   │
                └────────────┬────────────────────────────┘
                             │
                             ▼
                ┌─────────────────────────────────────────┐
                │   RALPH AUTONOMOUS LOOP                  │
                │                                         │
                │   prd.json ──► implement story by story │
                │            ──► toggle passes:true       │
                │            ──► commit per story         │
                │            ──► <promise>COMPLETE</promise> │
                └─────────────────────────────────────────┘
```

Run BMAD planning ONCE per project. Run this conversion skill ONCE per epic.
Run the Ralph loop ONCE per epic.

---

## Invocation

**Claude Code (BMAD + Claude Code workflow):**
```
/bmad-create-ralph-prd-json epic 2
```

**GitHub Copilot Agent Mode (BMAD + Copilot workflow):**
```
#file:.github/prompts/bmad-create-ralph-prd-json.prompt.md
convert epic 2 from epics.md to prd.json
```

Both paths produce the same `prd.json` from the same `epics.md` — choose
whichever runtime your team uses.
