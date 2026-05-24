<!--
GitHub auto-loads this into the PR description when you open a PR.
Delete sections that don't apply. Keep what's useful for the reviewer.
-->

## Summary

<!-- 1-3 sentences. What does this PR do? Why? -->

## Stories / tickets

<!-- Map each commit or chunk of work back to a user story, ticket, or epic.
     For Ralph Loop PRs, link the prd.json story IDs. -->

- [ ] US-XXX — <story title>

## What's in this PR

<!-- One bullet per logical change. If the PR has multiple commits, group by commit. -->

-

## Test plan

<!-- Check off what you actually did. Reviewer should be able to reproduce. -->

- [ ] `./mvnw clean package -DskipTests` — all modules build
- [ ] `./mvnw test -pl microservice-spring-boot` — unit tests pass
- [ ] `./mvnw clean test` (no `-pl`) — every module's tests pass
- [ ] Ran the affected service locally (Docker Compose / minikube)
- [ ] Exercised the touched endpoints manually with curl / Postman
- [ ] OpenAPI spec still renders at `/v3/api-docs` and `/swagger-ui.html`

## Screenshots / payloads (if API surface changed)

<!-- Paste request/response examples or Swagger UI screenshots. -->

## Risk and rollout

<!-- What could break? What's the blast radius? Does this need a feature flag, dark launch,
     or staged ramp? Any backwards-compatibility concerns? -->

- **Blast radius:**
- **Rollback plan:**
- **Schema/config changes:**

## Follow-ups (not blocking merge)

<!-- File-and-forget items the reviewer should be aware of but shouldn't gate this PR on. -->

-

## Checklist

- [ ] Commit messages follow conventional commits (`feat:`, `fix:`, `chore:`, `docs:`, `test:`)
- [ ] `prd.json` story status updated if this PR closes user stories
- [ ] `progress.txt` updated with the iteration log entry
- [ ] No secrets committed (API tokens, credentials, real `.env` values)
- [ ] No `target/`, IDE files, or OS artifacts staged
- [ ] If new dependencies added: compatibility verified against Spring Boot 2.7 + Spring Cloud 2021.0.x + SpringDoc 1.7.x

<!--
Reviewer guide: see docs/EPIC-1-SUMMARY.md for the canonical example of what
a Ralph Loop epic PR looks like fully documented (stories, ACs, evidence,
bonus improvements, follow-ups).
-->
