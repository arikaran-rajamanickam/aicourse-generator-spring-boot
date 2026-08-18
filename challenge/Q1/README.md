# Search & Discovery: result pages come back short and the top hit is often the wrong one

## Overview
CourseGen's search subsystem powers the global search box in the app header and the "see all
results" list behind it. It serves two endpoints — a paged, ranked search over courses and users,
and a fast autocomplete that returns prefix suggestions plus the best matching items — from an
in-memory index that is built when the backend starts. Since the last release, support has been
getting two complaints: pages of results are coming back with fewer items than were requested (and
some items seem to disappear entirely when you page through them), and for multi-word queries the
item everybody expects at the top is being listed below a weaker match.

## Steps to Reproduce
1. Sign in and make sure the catalogue contains at least 12 active courses whose titles all contain
   the word `Kubernetes` (e.g. `Kubernetes Guide 01` … `Kubernetes Guide 12`).
2. Call `GET /api/search?q=kubernetes&offset=0&limit=10`.
   The response body reports `"total": 12`, but the `results` array contains only 9 items.
3. Call `GET /api/search?q=kubernetes&offset=10&limit=10` to fetch the second page and put the two
   pages side by side. Between them they cover only 11 of the 12 matching courses — one course
   (the one that should have been the last entry of page 1) is never shown on any page.
4. Repeat with a small page size, `GET /api/search?q=kubernetes&limit=4`: 3 results come back.
5. Type `kubernetes` into the header search box (it asks the backend for a handful of top results
   via `GET /api/search/autocomplete?q=kubernetes&limit=5`). The dropdown shows 4 items even though
   12 courses match.
6. Now the ranking complaint. With two active courses in the catalogue:
   - `Java Programming: Testing And Debugging`, description
     `A hands on course about writing reliable software`
   - `Java Basics`, no description

   call `GET /api/search?q=java%20testing`. Both courses are returned, but `Java Basics` — which
   matches only the word `java` — is listed *first*, above the course that matches both `java` and
   `testing`. Adding more words to a course's description makes it sink further down the list, even
   when the query words all match it.

## Expected Behavior
- A page contains exactly as many results as the caller asked for whenever that many matches exist;
  it is shorter only when the result set runs out.
- Paging with `offset` walks the ranked result set without gaps or repeats: `offset=0&limit=10`
  followed by `offset=10&limit=10` over 12 matches yields all 12 items.
- `total` always describes the whole result set, independent of the requested page.
- Autocomplete's `topResults` follows the same rule: ask for 5 and get 5 when 12 items match.
- Ranking is driven by how much of the *query* an item matches. An item that matches every word of
  the query ranks above an item that matches only some of the words, all else being equal. The
  length of an item's title or description is not, by itself, a ranking penalty.
- Ties are broken deterministically by label (case-insensitive, ascending), so repeated identical
  requests return results in the same order.

## Expected API Behavior

### GET /api/search
**Purpose** Ranked, paged search across courses and users, served from the in-memory index.

**Query Parameters**

| Name         | Type    | Required | Default | Notes                                                                 |
|--------------|---------|----------|---------|-----------------------------------------------------------------------|
| `q`          | string  | yes      | —       | Free-text query. Blank/whitespace-only → empty result set, `total` 0. |
| `types`      | string  | no       | (all)   | Comma-separated `COURSE`, `USER`. Unknown values are ignored.         |
| `offset`     | int     | no       | `0`     | Zero-based index into the ranked result set; negatives clamp to `0`.   |
| `limit`      | int     | no       | `10`    | Page size, clamped to `1..50`.                                        |
| `excludeIds` | string  | no       | —       | Comma-separated user ids to omit (used by the share dialog).           |

**Success Response (200)**

```jsonc
{
  "results": [                 // one page of the ranked result set, best first
    {
      "id": "112",             // course id or user id, serialized as a STRING
      "type": "COURSE",        // COURSE | USER
      "label": "Kubernetes Guide 12",  // course title, or the user's display name
      "description": "Cluster operations handbook", // course description, or "@handle" for a user
      "score": 1.27,           // relevance score; higher is better, ordering is by this value
      "handle": null           // username for USER results, null for COURSE results
    }
    // ... exactly `limit` entries when at least `offset + limit` items match
  ],
  "total": 12                  // size of the WHOLE matching set, not of this page
}
```

### GET /api/search/autocomplete
**Purpose** Type-ahead for the header search box: prefix-expanded term suggestions plus the best
matching items, in one round trip.

**Query Parameters**

| Name         | Type   | Required | Default | Notes                                                        |
|--------------|--------|----------|---------|--------------------------------------------------------------|
| `q`          | string | yes      | —       | Prefix the user has typed so far.                            |
| `types`      | string | no       | (all)   | Same syntax as `/api/search`.                                |
| `limit`      | int    | no       | `8`     | Upper bound on the items returned, clamped to `1..20`.       |
| `excludeIds` | string | no       | —       | Comma-separated user ids to omit.                            |

**Success Response (200)**

```jsonc
{
  "suggestions": [             // indexed terms that start with `q`, most frequent first
    "java",
    "javascript"
  ],
  "topResults": [              // best matching items, same shape as `results` above
    {
      "id": "501",
      "type": "COURSE",
      "label": "Java Basics",
      "description": "",
      "score": 1.18,
      "handle": null
    }
    // ... `limit` entries when at least that many items match
  ]
}
```

An unknown prefix returns `{"suggestions": [], "topResults": []}` with status 200.

## Additional Information
- The index lives in memory and is built once at application start-up from all **active** courses
  and all users; inactive courses are not searchable. It is rebuilt only by an explicit refresh.
- Documents are tokenized on lower-cased alphanumeric runs (`.` and `_` are kept inside a token);
  tokens shorter than two characters are dropped. Matching is token-exact, so `q=kubernetes` matches
  the token `kubernetes`, not the prefix `kube` — that is what autocomplete's suggestions are for.
- A course document is tokenized from its title *and* description; a user document from display
  name, username and numeric id. Users are additionally searchable by their handle.
- Relevance blends how much of the query matched, how recently the item was created, a per-type
  popularity weight, and a bonus when the query text appears verbatim in the title or handle.
  Courses carry a higher popularity weight than users, so a course and a user that match equally
  well are not expected to score identically.
- `id` is serialized as a JSON **string** on the wire (ids exceed the safe integer range in JS).
- `limit` is clamped to `1..50` on `/api/search` (default 10) and `1..20` on
  `/api/search/autocomplete` (default 8). `offset` below zero is treated as `0`.
- Requesting an `offset` beyond the end of the result set is not an error: it returns an empty
  `results` array with the correct `total`.
- The frontend header search asks for a handful of items at a time (`ui/src/services/searchApi.ts`,
  `ui/src/components/AppLayout.tsx`) and renders whatever the backend returns, in the order it
  arrives — it does no re-ranking of its own.

## How this is graded
- Run the graded suite with:

  ```bash
  ./challenge/run_graded.sh q1
  ```

  The raw equivalent, if you prefer Maven directly:

  ```bash
  ./mvnw test -Dtest='com.challenge.q1.**' -DfailIfNoTests=false
  ```

- **Definition of done:** the problem counts as solved only when **every** test in the Q1 graded
  suite passes — no failures, no skips — **and** `./mvnw test` is green overall. Partial passes do
  not count.
- Failure details, including assertion diffs and stack traces, are written to
  `challenge/.report/q1.log` (the full Maven log of the last graded run).
- Rules:
  - Graded tests under `src/test/java/com/challenge/` must not be edited, disabled, deleted, or
    excluded via `pom.xml` (or any other build configuration), and their assertions must not be
    weakened.
  - Tests outside `com/challenge/` must stay green.
  - Your own additional tests are welcome, as long as they live outside `src/test/java/com/challenge/`.
- A good fix is minimal and addresses the root cause in the production code: no reformatting, no
  rewriting of the scoring model or the index, no special-casing of the values used in the tests.
  Expect the change to be small — the endpoints' behaviour, not their shape, is what is wrong.
