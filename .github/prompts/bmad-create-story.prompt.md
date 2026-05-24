---
mode: agent
description: "BMAD Create Story — prepare a story file with full implementation context before handing to Amelia"
tools:
  - codebase
  - editFiles
---

# BMAD Create Story

Prepare a well-structured story file so Amelia (bmad-amelia-dev) can implement it without ambiguity.

## What to do

1. Read `planning-artifacts/epics.md` (or the epic file the user points to)
2. Identify the next story not yet created, or use the story title the user specifies
3. Read `planning-artifacts/project-context.md` if it exists
4. Create a story file at `planning-artifacts/stories/<epic-number>-<story-number>-<slug>.md`

## Story file format

```markdown
# Story <epic>.<story>: <Title>

**Status:** ready-for-dev

## Story
As a <persona>, I want <goal>, so that <benefit>.

## Acceptance Criteria
- [ ] AC1: <criterion>
- [ ] AC2: <criterion>

## Tasks/Subtasks
- [ ] Task 1: <description>
  - [ ] 1.1 <subtask>
  - [ ] 1.2 <subtask>
- [ ] Task 2: <description>

## Dev Notes
<!-- Architecture patterns, file targets, libraries, gotchas — everything Amelia needs -->
- Key files: <list relevant existing files>
- Pattern to follow: <existing example in codebase>
- Framework: <Spring Boot / Cassandra Driver / etc>
- Test approach: <JUnit 5, Mockito, etc>

## Dev Agent Record
### Implementation Notes
<!-- Amelia fills this in during implementation -->

### File List
<!-- Amelia fills this in — all changed files, relative to repo root -->

## Change Log
| Date | Change |
|------|--------|
```

## How to invoke

```
create a story for "add product search by description" from epic 1
```
