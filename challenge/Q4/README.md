# Leaderboard: Weekly competition

**Level:** SDE II · **Time budget:** 2 – 3 hours · **Type:** feature development

## Overview

CourseGen already has a leaderboard, and it is a single **all-time** board.
`GET /api/leaderboard/global` ranks every user by their lifetime points total, and
`GET /api/leaderboard/me` tells the signed-in user where they sit on it. Points are awarded when a
lesson is completed, and each award adds to both a lifetime total and a per-week figure.

The problem is that the all-time board never turns over. A user who joined a year ago sits at the top
with tens of thousands of accumulated points, and nobody who joins this month can realistically
displace them. Engagement on the board is flat because the outcome is decided before a new learner
starts.

Product wants a **weekly** leaderboard: the same board, but ranked on points earned since the start
of the current week, and wiped clean every week so that each week is a fresh contest. Nothing about
the all-time board changes — the two boards sit side by side.

Half of the groundwork is already in the repository: the entity carries a weekly points figure
alongside the lifetime total and it is already kept up to date by the points writer, the repository
already has weekly-ordering and weekly-clearing queries that nothing calls, a Caffeine cache manager
is configured but wired to nothing, and there are placeholder classes whose names describe a weekly
reset job and a weekly reset service. The half that does not exist is everything that puts those
pieces together.

## What you are building

1. A **paged weekly leaderboard endpoint** that ranks users by their weekly points.
2. A **weekly reset**: a service that clears every user's weekly figure, plus the **scheduled
   trigger** that runs it once a week.
3. An **operational trigger** so an on-call engineer can run the reset on demand instead of waiting
   for the schedule. It must be restricted to the appropriate role.
4. **Read-through caching** of the weekly board through the cache manager that already exists in
   this package, and **invalidation** of that cache when the board is reset.

## The stubs

The classes you need already exist, with their **signatures declared and their bodies throwing
`UnsupportedOperationException`**. Filling them in is the job.

| File | What it needs |
|---|---|
| `com/leaderboard/service/impl/WeeklyLeaderboardService.java` | the weekly ranking + pagination read, and the score hook |
| `com/leaderboard/weeklyupdate/WeeklyLeaderboardResetService.java` | already declares the contract — read it |
| `com/leaderboard/weeklyupdate/impl/WeeklyLeaderboardResetServiceImpl.java` | the reset itself |
| `com/leaderboard/weeklyupdate/WeeklyLeaderboardJob.java` | the scheduled trigger |
| `com/leaderboard/controller/LeaderboardController.java` | the two new endpoint methods |

**Do not change these signatures.** Class names, method names, parameter lists, return types and
constructor parameter lists are the pinned contract, and the graded suite compiles against them.
Everything inside the method bodies, plus any annotations you need to add, is yours to design.

## Expected API Behavior

### `GET /api/leaderboard/weekly`

**Purpose** — return one page of the weekly leaderboard, ranked by points earned in the current
week, highest first.

**Authentication** — required. `/api/leaderboard/global` is the one endpoint on this controller that
is explicitly allow-listed as public; everything else under `/api/leaderboard` falls under the
default "authenticated" rule. The weekly board is a normal authenticated read, so you should not need
to touch the security configuration for it.

**Query Parameters**

| Name | Type | Default | Notes |
|---|---|---|---|
| `page` | int | `0` | zero-based page index |
| `size` | int | `10` | rows per page |

**Success Response (200)** — the same envelope the global endpoint already returns:

```json
{
  "data": [
    {
      "rank": 1,                     // absolute position on the whole board, 1-based
      "userId": 4242,
      "totalPoints": 120,            // the score this board is ranked by = the weekly figure
      "username": "Ada Lovelace",    // mirrors displayName
      "displayName": "Ada Lovelace", // account display name, falling back to the handle
      "handle": "ada",               // account username
      "courseCount": 2,              // courses the user has created
      "currentStreak": 7,            // consecutive-day streak
      "weeklyPoints": 120            // points earned this week
    }
  ],
  "page": 0,                         // the page that was requested, echoed back
  "size": 10,                        // the size that was requested, echoed back
  "totalElements": 137,              // rows on the whole board, not on this page
  "totalPages": 14                   // ceil(totalElements / size)
}
```

**Error Responses**

| Status | When | Body |
|---|---|---|
| `400` | `page < 0`, or `size < 1` | a short plain-text explanation, in the style of this controller |
| `401` | no authenticated caller | empty (handled by the security filter chain) |
| `500` | anything unexpected | a short plain-text message, as `/global` already does |

An empty board is **not** an error: it is `200` with `data: []`, `totalElements: 0`,
`totalPages: 0`.

### `POST /api/leaderboard/weekly/reset` — the operational reset trigger

**Purpose** — run the weekly reset immediately, so it can be triggered by an operator rather than
only by the schedule. It is a `POST`; wiping every user's weekly score is not a safe `GET`.

**Authentication / authorization** — an authenticated caller with the `ADMIN` role.

**Request** — no body, no parameters.

**Success Response (200)**

```json
{
  "message": "Weekly leaderboard reset",
  "usersReset": 137                  // number of user-stats rows whose weekly figure was cleared
}
```

`usersReset` is required and must carry the count reported by the reset service. Additional keys are
fine.

**Error Responses**

| Status | When | Body |
|---|---|---|
| `401` | `Authentication` is absent or not authenticated | empty, matching `/api/leaderboard/me` |
| `403` | authenticated but not `ADMIN` | `{"message": "Admin access required"}` |
| `500` | the reset fails | a JSON object with a `message` |

## Behavioural Requirements

This section is the specification of record.

**Ranking and pagination**

1. The weekly board is ordered by **weekly points, descending**.
2. Users on equal weekly points are ordered by **ascending `userId`**, so the board is
   deterministic. The existing weekly-ordering query does not define a tie-break, so the ordering has
   to be imposed by your code rather than assumed from the query.
3. `rank` is the row's **absolute** position on the whole board, 1-based. Page 1 of size 3 is
   ranked 4, 5, 6 — not 1, 2, 3.
4. `page` and `size` in the response echo what was requested, unclamped.
5. `totalElements` is the size of the whole board; `totalPages` is `ceil(totalElements / size)`,
   which is `0` for an empty board.
6. A page past the end of the board returns `data: []` and still reports the real `totalElements`
   and `totalPages`. It must not throw.
7. A `size` larger than the whole board returns every row on a single page.
8. `page < 0` or `size < 1` is a client error: `WeeklyLeaderboardService.getTopWeeklyUsers` throws
   `IllegalArgumentException`, and the endpoint answers `400` rather than `500`.
9. Each row carries the weekly figure in **both** `totalPoints` (the shared score slot of the
   response DTO — on this board it holds the score the board is ranked by) and `weeklyPoints`, plus
   the user's streak and created-course count.
10. `displayName` / `handle` / `username` are hydrated from the user account: the display name when
    it is present and not blank, otherwise the handle; `handle` is always the account username;
    `username` mirrors the resolved display name. A row whose account cannot be found is left with an
    unresolved name rather than failing the whole request.
11. The board is read through `UserStatsRepository.findAllOrderByWeeklyPoints()`, then ordered and
    paginated in memory, the way the global board already does it.
12. The global board is untouched: it still ranks by the lifetime total, still reports the lifetime
    total in `totalPoints`, and its response shape does not change.

**The reset**

13. The reset clears the **weekly figure only**. The lifetime points total, the current streak,
    `coursesCompleted`, `lessonsCompleted`, `totalCoursesCreated`, `totalProjectsCreated` and
    `lastActivityDate` must all survive it untouched.
14. The reset is observable at the entity level: it reads the stats rows through
    `UserStatsRepository.findAll()`, clears each one, and persists them with
    `UserStatsRepository.saveAll(...)`. The bulk `@Modifying` update on the repository bypasses the
    entity, cannot be observed without a database, and is **not** what is checked here.
15. `WeeklyLeaderboardResetService.resetWeeklyLeaderboard()` returns the number of rows it reset, and
    that count is what the endpoint reports.
16. The reset is **idempotent**: running it twice in a row is harmless, reports the same count both
    times, and does not erode any of the figures listed in (13).
17. Resetting an empty board reports `0` and does not fail.
18. The reset happens in one transaction.

**The schedule**

19. The reset runs automatically once a week, at the start of the week. Express it as a `@Scheduled`
    **cron** expression with all six fields (`second minute hour day-of-month month day-of-week`)
    whose day-of-week field selects Monday.
20. `WeeklyLeaderboardJob.runWeeklyReset()` **delegates** to the reset service; it does not
    reimplement the reset.
21. If the reset fails, `runWeeklyReset()` logs and returns rather than letting the exception escape —
    one bad week must not become an unhandled scheduler error.

**The cache**

22. The weekly board read is cached **read-through** under the cache name `weeklyLeaderboard`, served
    by the `CacheManager` bean that already exists in this package. That bean does **not** create
    caches on demand — it only serves the caches it has been configured with — so a request for
    `weeklyLeaderboard` has to be satisfiable by that bean, alongside the caches it already serves.
23. Each page is cached separately: the cache key includes both `page` and `size`. Page 0 must never
    be served for a request for page 3.
24. Resetting the weekly leaderboard evicts **all** entries of `weeklyLeaderboard`, at
    `WeeklyLeaderboardResetServiceImpl.resetWeeklyLeaderboard`. A reset that leaves a stale board in
    the cache is not a reset, and both reset paths — the schedule and the operator trigger — must
    invalidate it.
25. The existing `globalLeaderboard` cache must keep working.

**Authorization**

26. The role restriction on the operational trigger is enforced **consistently with how the existing
    admin endpoints in this repository authorize their callers**. Go and read one before you write
    this; the mechanism they use is the mechanism that actually works in this application, and it is
    checked by a plain unit test that calls the controller method directly.

**How the declarative parts are checked**

27. The caching (22–24), the schedule (19) and the request mappings are **verified by inspecting your
    wiring** — the annotations you declare, and the cache-manager bean itself — because the
    application cannot be started in this environment. They are graded; they are not optional polish.

## Constraints

- Follow the conventions already in this package: the response envelope and DTO style, the service
  layering, how the existing global endpoint resolves the caller, and how it handles and logs errors.
- Reuse what is already there rather than duplicating it — the abstract leaderboard service and its
  rank-building helper, the entity's own mutators, the existing repository queries, the existing
  cache manager, and the placeholder classes that were clearly left for this work.
- Do not change the global leaderboard's behaviour or its response shape.
- Do not change the stub signatures.
- **Do not edit, delete, disable, `@Disabled`, or exclude via `pom.xml` anything under
  `src/test/java/com/challenge/`, and do not weaken an assertion to make it pass.** Make the
  production code satisfy the suite.
- Do not edit `src/main/java/com/aicourse/config/Config.java`, `pom.xml`, `CHALLENGE.md` or
  `challenge/run_graded.sh`.
- Stay inside `src/main/java/com/leaderboard/**`. No schema change is needed — the weekly-points
  column already exists.
- Additional tests of your own are welcome and count in your favour, as long as they live **outside**
  `src/test/java/com/challenge/` and need no database, Spring context, or network.

## Additional Information

- The leaderboard controller is rooted at `/api/leaderboard` and today exposes `/global` (paged) and
  `/me` (the caller's own rank).
- The security matcher list lives in `src/main/java/com/aicourse/config/Config.java`.
  `GET /api/leaderboard/global` is explicitly `permitAll()`; everything else falls through to
  `anyRequest().authenticated()`. **Do not edit this file** — anything new you add is authenticated by
  default, and any role restriction belongs in the method (see requirement 26).
- `com/leaderboard/service/impl/AbstractLeaderboardService.java` holds the shared rank-building helper
  (which takes an offset) and an abstract score hook; `GlobalLeaderboardService` supplies the lifetime
  total as the score and hydrates display names. Read its `paginate` before writing the weekly
  equivalent.
- `com/leaderboard/model/UserStats.java` is the entity. `totalPoints` is the **lifetime** total,
  `weeklyPoints` is the **current week's** figure; both are incremented together by `addPoints(...)`,
  and `resetWeeklyPoints()` clears the weekly figure only. There are no setters for the point fields —
  the entity's own mutators are the API. The other figures are `coursesCompleted`,
  `lessonsCompleted`, `currentStreak`, `totalCoursesCreated`, `totalProjectsCreated` and
  `lastActivityDate`.
- `com/leaderboard/repository/UserStatsRepository.java` already carries weekly-ordering queries and a
  bulk weekly-points update. None of them is called from anywhere yet; requirements 11 and 14 say
  which ones this feature uses.
- The cache bean is declared in `com/leaderboard/config/CacheConfig.java`. `@EnableCaching` is already
  on it and `@EnableScheduling` is already on `com/AiCourseGeneratorApplication.java`, so you do not
  need to enable either. There is currently no `@Cacheable` anywhere in the repository.
- **The application cannot be booted in this environment** — there is no PostgreSQL instance and no
  `src/main/resources/application.properties`, so `spring-boot:run` will not start. The test suite is
  the verification loop.
- Several other empty classes exist in this package (`points/**`, `LeaderboardEntry`,
  `enums/LeaderboardType`, `model/LeaderboardType`, `LeaderboardRepository`,
  `DefaultPointsCalculationService`, `LeaderboardServiceImpl`). They are pre-existing dead
  scaffolding, unrelated to this problem. Leave them alone.

## How this is graded

```bash
./challenge/run_graded.sh q4                                        # the graded suite for this problem
./mvnw test -Dtest='com.challenge.q4.**' -DfailIfNoTests=false      # the same suite, raw
./mvnw test                                                         # everything
```

Failure detail for the graded run lands in `challenge/.report/q4.log`.

**Definition of done — both of these, together:**

- **34 / 34 green** in `src/test/java/com/challenge/q4/` — four classes:
  `WeeklyLeaderboardRankingTest` (13), `WeeklyLeaderboardEndpointTest` (10),
  `WeeklyLeaderboardCachingAndScheduleTest` (6), `WeeklyLeaderboardResetTest` (5); and
- **`./mvnw test` green overall** — the 33 pre-existing tests must not regress.

**A partial pass does not count.** "Most of the suite is green" is not done.

The suite is JUnit 5 + Mockito + AssertJ with no database, no Spring context and no network. It
instantiates your classes directly and mocks the repositories. It is the Behavioural Requirements
above in executable form, and it is readable — when something fails, read the assertion message and
the requirement it maps to before you change anything.

A strong submission gets the observable contract right, resets exactly the one field it is supposed to
reset, invalidates the cache at the same moment, numbers ranks correctly across page boundaries, and
does all of it by extending the structures already in the package rather than bolting a parallel
implementation alongside them. Be ready to state: what you added and where, why each piece sits at
that layer, what you chose *not* to do, and how you convinced yourself it works given that the
application cannot be started here.
