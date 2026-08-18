# CourseGen — AI-Assisted Coding Challenge

Welcome. This repository is the **AI Course Generation Platform** (Spring Boot 3.5 / Java 25
backend, Vite + React frontend, PostgreSQL). It is a real, working codebase; your job is to
work inside it the way an on-call engineer would.

You have an AI coding assistant available in this environment. Using it is expected. What is
being measured is **your** engineering judgement: whether you can navigate an unfamiliar
codebase, reproduce a defect, isolate a root cause, implement a minimal correct change, and
verify it — including verifying anything the assistant hands you.

---

## The four problems

| # | Problem | Level | Time budget | Run its tests with |
|---|---------|-------|-------------|--------------------|
| [Q1](challenge/Q1/README.md) | see `challenge/Q1/README.md` | SDE I | 60 – 90 min | `./challenge/run_graded.sh q1` |
| [Q2](challenge/Q2/README.md) | see `challenge/Q2/README.md` | SDE I | 60 – 90 min | `./challenge/run_graded.sh q2` |
| [Q3](challenge/Q3/README.md) | see `challenge/Q3/README.md` | SDE II | 2 – 3 hours | `./challenge/run_graded.sh q3` |
| [Q4](challenge/Q4/README.md) | see `challenge/Q4/README.md` | SDE II | 2 – 3 hours | `./challenge/run_graded.sh q4` |

Work them in any order. Each problem is scoped to one area of the product and none of them
depends on another being finished first.

---

## When is a problem solved?

**Every problem ships with its own graded test suite. A problem counts as solved only when
that suite is 100% green — every test, no skips — and `./mvnw test` is green overall.**
Partial passes do not count.

The verification loop for this challenge is the **test suite**, not the running application
(this environment has no PostgreSQL instance and no `src/main/resources/application.properties`,
so `spring-boot:run` will not start).

### Triggering a problem's tests

```bash
./challenge/run_graded.sh q1       # Q1's graded suite only
./challenge/run_graded.sh q2       # Q2
./challenge/run_graded.sh q3       # Q3
./challenge/run_graded.sh q4       # Q4
./challenge/run_graded.sh q1 q3    # a selection
./challenge/run_graded.sh          # all four
```

Each run prints `PASS` / `FAIL` per problem with the failing test names, and writes the full
Maven log to `challenge/.report/<q>.log` so you can read stack traces and assertion diffs.

The equivalent raw Maven invocation, if you prefer it:

```bash
./mvnw test -Dtest='com.challenge.q1.**' -DfailIfNoTests=false
```

### Baseline

Before you touch anything, the whole suite is green:

```bash
./mvnw test          # 33 tests, 0 failures
```

If a test *outside* `src/test/java/com/challenge/` starts failing, your change broke
something unrelated — that counts against you even if your problem's suite is green.

### Rules for the graded suites

* **Do not edit, delete, disable, or `@Disabled` any test under `src/test/java/com/challenge/`.**
  Make the production code satisfy them.
* Do not weaken an assertion, relax a bound, or special-case a test input to make it pass.
* Do not delete a graded test class and re-add it, and do not exclude it in `pom.xml`.
* Existing tests outside `com/challenge/` must stay green.
* Adding *your own* extra tests is welcome and counts in your favour — just put them
  somewhere other than `src/test/java/com/challenge/`.

---

## Ground rules

1. **Minimal, targeted diffs.** Fix the defect; do not reformat, rename, or refactor
   surrounding code. A 400-line diff for a one-line bug reads as a red flag.
2. **Root cause, not symptom.** Special-casing the input in the failing test, catching and
   swallowing an exception, or patching the frontend to hide a backend defect are all wrong
   answers even when the tests go green.
3. **Stay in scope.** Each problem names the behaviour it wants. Unrelated bugs you notice
   along the way — mention them in your notes; don't fix them.
4. **Explain yourself.** For each problem, be ready to state: the symptom, the root cause,
   why your fix is correct, and how you verified it.
5. The assistant is a tool, not an oracle. If it proposes a change, you are accountable for
   whether that change is right.

---

## Orienting yourself

```
src/main/java/com/           backend, grouped by domain
  aicourse/                    course + lesson generation, AI routing, MCP, config
  auth/                        registration, login, JWT, revocation
  sharing/                     share links, invites, enrollment, lesson progress
  leaderboard/                 points, streaks, ranking
  search/                      in-memory search index + autocomplete
  project/  features/  marketing/  about/  aicoach/
src/main/resources/db/       Liquibase changelogs
src/test/java/com/           existing test suite
src/test/java/com/challenge/ graded suites for this challenge
ui/src/                      React frontend (pages/, components/, services/)
challenge/Q1..Q4/README.md   problem statements
```

Useful starting points: `README.md` for the product tour, `docs/` for API notes,
`src/main/java/com/aicourse/config/Config.java` for the security/route wiring.

Note: some legacy tests are excluded from the build in `pom.xml` (they were written against
older signatures, or need a live database). That is pre-existing and not part of any problem.

Good luck.
