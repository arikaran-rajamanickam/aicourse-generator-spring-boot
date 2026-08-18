# Authentication: signing out does not end the session — the old token keeps working

## Overview
CourseGen authenticates API calls with a bearer JWT: `POST /api/auth/login` returns a signed
token, and every protected endpoint expects it in an `Authorization: Bearer <token>` header.
Because the token itself is stateless, `POST /api/auth/logout` is what makes a token unusable —
the server is supposed to record the token as revoked and refuse it from that moment until it
would have expired on its own.

During a security review we found that revocation does not take effect. After a successful
logout the very same token is still accepted on authenticated endpoints, and it keeps being
accepted for the remainder of its lifetime. Support has also had reports from users on shared
machines: they sign out, the UI returns them to the login screen, but the token that the
previous session was using still opens the API if it is replayed.

## Steps to Reproduce

1. Register a user (or use an existing one) and log in to obtain a token:

   ```bash
   curl -s -X POST http://localhost:8080/api/auth/register \
     -H 'Content-Type: application/json' \
     -d '{"username":"revoke-demo","password":"Passw0rd!","displayName":"Revoke Demo"}'

   TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
     -H 'Content-Type: application/json' \
     -d '{"username":"revoke-demo","password":"Passw0rd!"}' | jq -r .token)
   ```

2. Confirm the token works:

   ```bash
   curl -s -o /dev/null -w '%{http_code}\n' http://localhost:8080/api/auth/me \
     -H "Authorization: Bearer $TOKEN"
   # 200
   ```

3. Log out with that token:

   ```bash
   curl -s -o /dev/null -w '%{http_code}\n' -X POST http://localhost:8080/api/auth/logout \
     -H "Authorization: Bearer $TOKEN"
   # 200
   ```

4. Replay the *same* token against an authenticated endpoint:

   ```bash
   curl -s -w '\n%{http_code}\n' http://localhost:8080/api/auth/me \
     -H "Authorization: Bearer $TOKEN"
   ```

   **Observed:** `200` with the full user payload. The token is still good, and repeating step 4
   minutes later still returns `200`.

   **Expected:** `401` with a JSON error body.

5. The same holds for the notification stream, which receives its token as a query parameter
   instead of a header:

   ```bash
   curl -s -o /dev/null -w '%{http_code}\n' \
     "http://localhost:8080/api/notifications/stream?token=$TOKEN"
   # observed 200, expected 401 after step 3
   ```

## Expected Behavior

* A token presented after a successful logout is rejected — the request is never treated as
  authenticated, and no authentication is placed in the security context for it.
* Rejection is immediate: the first request after logout already fails, and every later request
  with that token fails too, for as long as the token would otherwise have been valid.
* Revoking one session must not silently un-revoke another. If two tokens are logged out, both
  stay revoked.
* Revocation applies however the token reached the server on logout — the `Authorization` header
  or the `token` request parameter — and the token that gets revoked is exactly the token the
  client sent, byte for byte.
* Nothing about the normal path changes: login still issues a working token, a token that was
  never logged out still authenticates, and token lifetime is unchanged.

## Expected API Behavior

### POST /api/auth/login

Exchange credentials for a bearer token. Public endpoint.

Request:

```
Content-Type: application/json

{ "username": "revoke-demo", "password": "Passw0rd!" }
```

`200 OK`:

```jsonc
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",   // signed JWT; subject is the username
  "user": {
    "id": 7231847362819072,
    "handle": "revoke-demo",
    "username": "revoke-demo",          // same value as handle
    "displayName": "Revoke Demo",
    "role": "USER"
  }
}
```

Errors:

| Status | When |
|--------|------|
| `401 Unauthorized` | username unknown or password wrong |
| `400 Bad Request`  | malformed body |

### POST /api/auth/logout

Ends the session that the supplied token belongs to. The token is sent either as
`Authorization: Bearer <token>` or as a `token` request parameter (the browser client sends the
header).

Request:

```
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
```

`200 OK` — the response body is not relied on by the client; the status is what matters.

After this call, the supplied token must no longer authenticate any request.

| Status | When |
|--------|------|
| `200 OK` | always, including when no token was supplied (logout is idempotent) |

### GET /api/auth/me

Returns the currently authenticated user. Requires a valid, non-revoked token.

Request:

```
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
```

`200 OK`:

```jsonc
{
  "id": 7231847362819072,
  "handle": "revoke-demo",
  "username": "revoke-demo",
  "displayName": "Revoke Demo",
  "role": "USER"                        // USER | PREMIUM_USER | ADMIN
}
```

Errors:

```jsonc
// 401 Unauthorized — token missing, malformed, expired, or revoked
{
  "success": false,
  "message": "..."                      // short human-readable reason
}
```

| Status | When |
|--------|------|
| `401 Unauthorized` | no token, unparseable/badly signed token, expired token, **revoked token** |

## Additional Information

* The token is a bearer JWT signed with a symmetric key held by the backend. The username is
  the `sub` claim; there are no role or session claims. Tokens are issued with a fixed
  10-hour lifetime and are not refreshed or rotated.
* Because the token is stateless, expiry alone cannot end a session early. Revocation is
  **server-side**: the backend keeps its own record of tokens that have been logged out, keyed
  by the token, and consults it on every authenticated request. That record is in-memory and
  only needs to remember a token until the token's own expiry passes — after that the token is
  refused on its own merits and remembering it is pointless.
* Every authenticated request goes through the same path: the token is pulled off the request,
  checked against the revocation record, resolved to a user, validated, and only then is an
  authentication placed in the security context. A request whose token is refused gets a `401`
  and a small JSON body (`{"success": false, "message": "..."}`) and is not passed further down
  the chain.
* Two transports carry the token: normal REST calls use `Authorization: Bearer <token>`; the
  SSE endpoint `GET /api/notifications/stream` receives it as a `?token=<token>` query
  parameter, because `EventSource` cannot set headers. Logout accepts both forms.
* Logout is wired into the framework's logout processing for `POST /api/auth/logout`, and it
  responds `200` regardless of whether it found a token to revoke.
* There is no database and no `application.properties` in this environment — reproduce and
  verify through tests, not a running server. The reproduction steps above are what the
  security reviewer ran against a deployed instance.

## How this is graded

Run the graded suite for this problem:

```bash
./challenge/run_graded.sh q2
```

The raw equivalent, if you want Maven's output directly:

```bash
./mvnw test -Dtest='com.challenge.q2.**' -DfailIfNoTests=false
```

**Definition of done:** this problem counts as solved only when *every* test in the Q2 graded
suite passes — no failures, no errors, no skips — **and** `./mvnw test` is green overall. A
partial pass does not count.

Failures are summarised by the runner and written in full to `challenge/.report/q2.log`, which
holds the complete Maven log including assertion diffs and stack traces.

Rules:

* Do not edit, disable (`@Disabled`), delete, or exclude any test under
  `src/test/java/com/challenge/` — including via `pom.xml`. Make the production code satisfy
  them.
* Do not weaken an assertion to make it pass.
* Tests outside `com/challenge/` must stay green.
* Extra tests of your own are welcome, as long as they live outside `com/challenge/`.

A good fix:

* Makes revocation actually stick, so that a logged-out token is refused on the next request
  and on every request after that until it expires.
* Keeps the working path intact — a token that was never logged out still authenticates, an
  unknown token is still not treated as revoked, and the issued token's lifetime is unchanged.
* Is minimal and addresses the cause rather than the symptom: no new caches or maps, no
  special-casing in the request filter, no "revoke everything" shortcut, no widening of what
  counts as a revoked token, and no making the revocation record grow forever.
* Leaves the existing suite (`./mvnw test`) green.
