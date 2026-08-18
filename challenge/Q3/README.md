# Course Sharing: capped share links overfill, seats vanish, and restricted links leak

## Overview
CourseGen lets the creator of a course hand it out with a share link. A link is `PUBLIC`
(anyone with the URL), `PRIVATE` (only an explicit allow-list of users), or `DIRECT_INVITE`
(generated when the creator invites named people), and it can carry an expiry date and a
maximum-enrollment seat cap. A learner opens the link, sees a preview of the course, and joins;
joining creates an enrollment and consumes one of the link's seats.

Since the last release, support has collected a cluster of complaints about this flow from both
sides of it. Creators say the seat cap does not mean what it says — some cohorts came out one
learner too big, while other links reported themselves full and refused people while the roster
was nearly empty. Learners say a link that worked a minute ago suddenly reports itself invalid,
and at least one creator has reported that a link they restricted to three named colleagues was
readable by someone who was never on the list. Nothing in this area has any test coverage today.

## Reported Symptoms
* A creator capped a cohort link at 25 seats and ended up with 26 enrolled learners. A 1-seat
  link let a second learner in as well.
* The "N of M enrolled" counter a creator sees for a link climbs faster than the roster does. One
  creator watched a 2-seat workshop link report itself full while only one person had joined.
* A learner who opens the same join link a second time (refresh, or coming back to the course
  later from their invites list) is sometimes told the link is no longer valid — and worse, this
  can be what burns the seat that the *next* learner needed.
* A creator restricted a link to three named colleagues. A fourth person who was forwarded the
  URL could open the link and read back the course title, description, module/lesson counts and
  the usernames of the three invited colleagues. Pressing "Join" did then fail for them.
* A signed-out visitor can retrieve the preview for a restricted link instead of being told to
  sign in first.
* People who were sent a direct invite are told "You are not allowed to access this private share
  link" when they open the link that was mailed to them, even though the invite is valid.

## Steps to Reproduce
All steps use `$T` for a learner's bearer token and `$C` for a course id owned by the creator.

**1. Seat cap admits one learner too many**
1. As the creator: `POST /api/courses/$C/share/generate` with `{"linkType":"PUBLIC","maxEnrollments":2}`.
   Note `data.shareToken`.
2. As learner A: `POST /api/join/<token>/enroll` → succeeds.
3. As learner B: `POST /api/join/<token>/enroll` → succeeds. The link is now full.
4. As learner C: `POST /api/join/<token>/enroll` → **succeeds**, and
   `GET /api/courses/$C/share/links` reports `currentEnrollments: 3` against `maxEnrollments: 2`.
   Expected: step 4 is refused and the counter stays at 2.

**2. Seats are consumed by learners who are already enrolled**
1. As the creator: generate a `PUBLIC` link with `{"maxEnrollments":2}`.
2. As learner A: `POST /api/join/<token>/enroll` three times in a row (all three return 200 and
   the same enrollment id).
3. `GET /api/courses/$C/share/links` → `currentEnrollments` is **3**, not 1.
4. As learner B: `POST /api/join/<token>/enroll` → refused, "Share link is no longer valid",
   although only one learner has actually joined.

**3. A restricted link previews for people who are not on its allow-list**
1. As the creator: `POST /api/courses/$C/share/generate` with
   `{"linkType":"PRIVATE","allowedUsers":["alice","bob"]}`.
2. As `carol` (not listed): `GET /api/join/<token>` → **200** with the course preview and
   `allowedUsers: ["alice","bob"]`. Expected: 401.
3. With no `Authorization` header at all: `GET /api/join/<token>` → **200**. Expected: 401,
   "Login required to access this private share link".
4. As `carol`: `POST /api/join/<token>/enroll` → 400, refused. The two endpoints disagree.

**4. A direct-invite link cannot be opened by its recipient**
1. As the creator: `POST /api/courses/$C/share/invite` with `{"emails":["dave"]}`, then take the
   `DIRECT_INVITE` link's token from `GET /api/courses/$C/share/links`.
2. As `dave`: `GET /api/join/<token>` → 401 "You are not allowed to access this private share
   link". Expected: 200 with the course preview.

## Expected Behavior
* **Seat cap semantics.** `maxEnrollments: N` means the link may bring at most N learners into
  the course. The Nth learner is admitted; the (N+1)th is refused. `currentEnrollments` never
  exceeds `maxEnrollments`, and a link with no cap admits everyone.
* **The counter counts learners, not requests.** `currentEnrollments` equals the number of
  distinct learners that link brought into the course. One learner opening the link repeatedly
  consumes exactly one seat and never squeezes another learner out.
* **Re-joining is idempotent.** A learner who is already actively enrolled and joins again gets
  200 and their existing enrollment back: same enrollment id, still `ACTIVE`, no new record, no
  extra seat, progress untouched. A learner whose enrollment was dropped or suspended is revived
  to `ACTIVE`/`ACCEPTED` by joining again.
* **Who may come through a restricted link.** A `PRIVATE` link is usable only by a signed-in user
  on its allow-list; everyone else — signed out, or signed in but unlisted — is refused, and is
  refused *before* the link discloses anything about the course. `PUBLIC` links are readable by
  anyone, including signed-out visitors. `DIRECT_INVITE` links carry no allow-list and are usable
  by the signed-in recipient who was sent them.
* **Both endpoints apply the same rule.** Previewing a link and joining with it are separate
  endpoints that reach the same decision about the same link and the same caller. If one refuses,
  the other refuses; a caller must never be able to learn something from one that the other would
  have denied them.
* **An unsuccessful join changes nothing.** A refused caller gets no enrollment record and burns
  no seat.
* **Expiry and deactivation still win.** An expired, deactivated or deleted link, or a
  deactivated course, is refused regardless of how many seats remain.
* Enrolled-learner progress is unaffected by all of the above: completing *k* of *n* lessons
  leaves the learner's enrollment at `k * 100 / n` percent, and the learner's own progress view
  reports the same number.

## Expected API Behavior

All responses use the standard envelope. Success:

```json
{
  "success": true,
  "message": "human readable",
  "data": { }            // shape depends on the endpoint; omitted when there is nothing to return
}
```

Failure (the `data` key is omitted entirely):

```json
{ "success": false, "message": "why it failed" }
```

### POST /api/courses/{courseId}/share/generate
Creates a share link. Creator only.

Headers: `Authorization: Bearer <token>`, `Content-Type: application/json`

```json
{
  "linkType": "PUBLIC",          // PUBLIC | PRIVATE | DIRECT_INVITE, defaults to PUBLIC
  "expiresAt": "2026-09-01T00:00:00Z",   // optional ISO-8601 offset date-time, null = never
  "maxEnrollments": 25,          // optional seat cap, null = uncapped
  "allowedUsers": ["alice", "bob"]       // required for PRIVATE, ignored otherwise
}
```

`200 OK`

```json
{
  "success": true,
  "message": "Share link generated successfully",
  "data": {
    "id": "7213458901234567",           // ids serialize as strings
    "shareToken": "u9k1r...",           // opaque, url-safe
    "courseId": "6501234567890123",
    "linkType": "PUBLIC",
    "createdAt": "2026-08-18T09:12:44Z",
    "expiresAt": null,
    "isActive": true,
    "currentEnrollments": 0,            // seats used so far
    "maxEnrollments": 25,               // null when uncapped
    "shareUrl": "/join/u9k1r...",
    "allowedUsers": null,               // usernames, only populated for PRIVATE links
    "courseName": "Distributed Systems 101",
    "courseDescription": "Consensus, replication and failure modes",
    "inviterUsername": "creator",
    "moduleCount": 6,
    "lessonCount": 24
  }
}
```

Errors — all `400 Bad Request` with `"Error generating share link: <reason>"`:
`Course not found`; `User is not authorized to share this course`;
`At least one valid user is required for a PRIVATE share link`;
`Unknown users for PRIVATE link: <names>`.

### GET /api/join/{token}
Resolves a token to a course preview. This is the only endpoint in the flow that an unauthenticated
caller is allowed to reach; a bearer token is optional here and identifies the visitor when present.

`200 OK` — `data` is the same `ShareLinkResponse` shape as above.

Errors:

| status | when | body `message` |
|--------|------|----------------|
| `401 Unauthorized` | restricted link, caller not signed in | `Login required to access this private share link` |
| `401 Unauthorized` | restricted link, caller not on the allow-list | `You are not allowed to access this private share link` |
| `400 Bad Request` | unknown token | `Invalid or expired share link: Invalid or expired share link` |
| `400 Bad Request` | expired, deactivated, or seats exhausted | `Invalid or expired share link: Share link is no longer valid` |
| `400 Bad Request` | course deactivated by its owner | `Invalid or expired share link: This course has been deactivated and is no longer available` |

A refusal must not include any course or allow-list detail in `data`.

### POST /api/join/{token}/enroll
Enrolls the calling learner through the link. Requires a signed-in caller (a request with no
bearer token is answered `400`, not `401`). No request body.

`200 OK`

```json
{
  "success": true,
  "message": "User enrolled successfully",
  "data": {
    "id": "7300011122233344",        // enrollment id
    "courseId": "6501234567890123",
    "userId": "6600011122233344",
    "status": "ACTIVE",              // enrollment state: ACTIVE | COMPLETED | SUSPENDED | DROPPED
    "enrolledAt": "2026-08-18T09:20:01Z",
    "progressPercentage": 0.0,
    "courseName": "Distributed Systems 101",
    "courseDescription": "Consensus, replication and failure modes",
    "isRead": false,
    "inviteStatus": "ACCEPTED",      // invite workflow: PENDING | ACCEPTED | DECLINED
    "invitedBy": null,
    "invitedByName": null,
    "moduleCount": 6,
    "lessonCount": 24,
    "userName": "alice",
    "userHandle": "alice"
  }
}
```

Repeating the call for a learner who is already `ACTIVE` returns `200` with the identical
enrollment id and no additional seat consumed.

Errors — all `400 Bad Request` with `"Error enrolling user: <reason>"`:
`Invalid or expired share link`; `Share link is no longer valid` (expired, deactivated, or full);
`You are not allowed to access this private share link`; `Share link does not match the course`;
`This course has been deactivated and is no longer available`; `Course not found`.

### GET /api/courses/{courseId}/share/links
Lists every link the calling creator has generated for the course — this is what backs the seat
counters on the share screen.

Headers: `Authorization: Bearer <token>`

`200 OK`

```json
{
  "success": true,
  "message": "Share links fetched successfully",
  "data": [
    {
      "id": "7213458901234567",
      "shareToken": "u9k1r...",
      "linkType": "PRIVATE",
      "isActive": true,
      "currentEnrollments": 2,
      "maxEnrollments": 25,
      "allowedUsers": ["alice", "bob"]
    }
  ]
}
```

Errors — `400 Bad Request`, `"Error fetching share links: <reason>"`: `Course not found`;
`User is not authorized to view share links for this course`.

## Additional Information
* **Response envelope.** Every endpoint in this area returns `{success, message, data}`; `data` is
  omitted on failure. Controllers translate exceptions into status codes, so the exception message
  is what surfaces in `message`.
* **Ids are `long` (snowflake) values serialized as JSON strings** — compare them as strings on the
  client, and expect string ids in request paths.
* **Two status fields live on one enrollment, and they are not the same thing.**
  `inviteStatus` is the invite workflow state (`PENDING` / `ACCEPTED` / `DECLINED`) — it answers
  "has this person accepted?". `status` is the enrollment state (`ACTIVE` / `COMPLETED` /
  `SUSPENDED` / `DROPPED`) — it answers "is this enrollment live?". They are stored in DB columns
  whose names are crossed over relative to the field names, so read the mapping before you trust
  either one. Only an `ACTIVE` or `COMPLETED` enrollment unlocks course content.
* **Which endpoint is public.** The whole `/api/join/**` path is outside the authenticated area of
  the security filter chain. That is deliberate for the resolve endpoint, which must work for a
  signed-out visitor following a public URL; the enroll endpoint identifies its caller itself.
  Because resolve is reachable anonymously, the access rules for restricted links are enforced in
  application code, not by the filter chain.
* **Resolve and enroll validate independently.** They are two endpoints with two code paths that
  happen to check overlapping conditions (token valid, link live, seats free, course active,
  caller permitted). Treat "these two agree" as part of the contract, not an implementation detail.
* **Content locking is a separate concern** from joining: once enrolled, a learner whose link was
  later deactivated or expired keeps their enrollment but has content locked, which answers `403`
  where everything else in this area answers `400`. You should not need to change that behaviour.
* **Progress percentage is computed in more than one place** (what is stored on the enrollment and
  what is reported to the learner). Both are expected to give the same answer.
* The frontend pages `ui/src/pages/ShareCourse.tsx` and `ui/src/pages/JoinCourse.tsx` and the
  services in `ui/src/services/` are useful for seeing how the endpoints are called, but the
  behaviour described above is backend behaviour and the graded suite exercises it directly.
* This area ships with **no tests**, so nothing existing will guide you to the affected behaviour;
  reproduce from the symptoms.

## How this is graded

Run the graded suite:

```bash
./challenge/run_graded.sh q3
```

or, equivalently, the raw Maven invocation:

```bash
./mvnw test -Dtest='com.challenge.q3.**' -DfailIfNoTests=false
```

**Definition of done:** the problem counts as solved only when **every** test in the Q3 graded
suite passes — no failures, no errors, no skips — **and** `./mvnw test` is green overall. Partial
passes do not count. This problem covers several reported symptoms, so making some of the suite
go green while the rest still fails is explicitly not a pass.

Where to read failures: `challenge/.report/q3.log` holds the full Maven output for the last graded
run, including assertion diffs and stack traces.

Rules:
* Tests under `src/test/java/com/challenge/` must not be edited, renamed, deleted, `@Disabled`,
  or excluded in `pom.xml`. Fix the production code instead.
* Do not weaken an assertion to make it pass.
* Tests outside `com/challenge/` must stay green. Your own additional tests are welcome as long as
  they live somewhere else.

What a good fix looks like: a small number of minimal, root-cause changes in the sharing code —
each one a line or two at the place where the rule is actually decided — with no test edits, no
new configuration, no broad refactor of the sharing services, and no special-casing of the inputs
the tests happen to use. Be ready to explain, for each symptom, what the wrong behaviour was, why
your change is the correct fix rather than a patch over it, and how you verified it.
