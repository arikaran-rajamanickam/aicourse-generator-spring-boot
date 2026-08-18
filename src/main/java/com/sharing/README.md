# 📚 com.sharing Module - Complete Documentation

**Version:** 1.1
**Last Updated:** March 22, 2026

---

## 🎯 What is com.sharing?

The **com.sharing** module is a comprehensive course sharing and collaboration system that allows instructors to share
courses with students through multiple channels and track student progress through shared courses.

### Package Purpose & Features

✅ **Token-Based Sharing** - Create unique, expirable, transferable share links  
✅ **Direct Invitations** - Send personalized invites to specific users  
✅ **Progress Tracking** - Monitor lesson completion and calculate course progress %  
✅ **Enrollment Management** - Track who's enrolled, their status, and performance  
✅ **Leaderboard Ready** - Generate ranking and achievement data  
✅ **Security First** - Secure token generation, authorization checks

### Key Statistics

- **23 Java Classes** - Complete implementation
- **22 REST Endpoints** - All documented with examples
- **3 Database Tables** - Schema ready
- **5 Enums** - Type-safe definitions
- **4 DTOs** - Clean API responses

---

## 🏗️ Architecture Overview

### Module Structure

```
com.sharing/ (23 Java classes)
├── controller/          → 3 REST controllers (22 endpoints)
├── service/            → 4 services with business logic
├── model/              → 5 JPA entities
├── repo/               → 3 repositories with queries
├── dto/                → 4 response objects
└── enums/              → 5 type definitions
```

---

## 📡 API ENDPOINTS (Sharing & Progress)

### SHARE MANAGEMENT (Share Links + Direct Invites)

#### 1. Create Share Link ⭐

**Endpoint:** `POST /api/courses/{courseId}/share/generate`

**Auth:** ✅ Required | **Authorization:** ✅ Course creator only  
**Purpose:** Create a new shareable link for a course

**Request:**

```json
{
  "linkType": "PUBLIC",
  "expiresAt": "2026-04-22T11:14:17+05:30",
  "maxEnrollments": 50
}
```

**✅ SUCCESS (201 Created):**

```json
{
  "success": true,
  "message": "Share link generated successfully",
  "data": {
    "id": 1,
    "shareToken": "K_L4mPqRsT-uVwXyZ1a2b3c4d5e6f7g8h",
    "courseId": 123,
    "linkType": "PUBLIC",
    "createdAt": "2026-03-22T11:14:17+05:30",
    "expiresAt": "2026-04-22T11:14:17+05:30",
    "isActive": true,
    "currentEnrollments": 0,
    "maxEnrollments": 50,
    "shareUrl": "/join/K_L4mPqRsT-uVwXyZ1a2b3c4d5e6f7g8h"
  }
}
```

**❌ FAILURES:**

- 401: Missing/invalid JWT token
- 403: Not course creator
- 404: Course not found
- 400: Invalid link type

---

#### 2. Get Share Links

**Endpoint:** `GET /api/courses/{courseId}/share/links`

**Auth:** ✅ Required | **Authorization:** ✅ Course creator only

**✅ SUCCESS (200 OK):**

```json
{
  "success": true,
  "message": "Share links fetched successfully",
  "data": [
    {
      "id": 1,
      "shareToken": "K_L4mPqRsT-uVwXyZ1a2b3c4d5e6f7g8h",
      "courseId": 123,
      "linkType": "PUBLIC",
      "createdAt": "2026-03-22T11:14:17+05:30",
      "expiresAt": "2026-04-22T11:14:17+05:30",
      "isActive": true,
      "currentEnrollments": 5,
      "maxEnrollments": 50,
      "shareUrl": "/join/K_L4mPqRsT-uVwXyZ1a2b3c4d5e6f7g8h"
    }
  ]
}
```

---

#### 3. Update Share Link

**Endpoint:** `PUT /api/courses/{courseId}/share/links/{shareLinkId}`

**Request:**

```json
{
  "expiresAt": "2026-05-22T11:14:17+05:30",
  "maxEnrollments": 100
}
```

**✅ SUCCESS (200 OK):** Updated link object

---

#### 4. Deactivate Link

**Endpoint:** `PUT /api/courses/{courseId}/share/links/{shareLinkId}/deactivate`

**Purpose:** Temporarily disable (can be reactivated later)  
**✅ SUCCESS (200 OK):** `{ "success": true, "message": "Share link deactivated successfully" }`

---

#### 5. Revoke Link

**Endpoint:** `DELETE /api/courses/{courseId}/share/links/{shareLinkId}`

**⚠️ Permanent deletion** | Existing enrollments NOT affected

---

#### 6. Send Direct Invites

**Endpoint:** `POST /api/courses/{courseId}/share/invite`

**Purpose:** Send personalized invitations to specific users

**Request:**

```json
{
  "emails": [
    "user1@example.com",
    "user2@example.com"
  ]
}
```

**✅ SUCCESS (200 OK):** `{ "success": true, "message": "Direct invites sent successfully" }`

**📧 Email includes:** Course name, Accept link, Decline link, Creator name

---

### JOIN/ENROLL (2 endpoints)

#### 1. Resolve Share Token (PUBLIC - NO AUTH!) 🌐

**Endpoint:** `GET /api/join/{token}`

**Auth:** ❌ NOT REQUIRED (Public endpoint!)  
**Purpose:** Preview course before enrollment (no login needed)

**✅ SUCCESS (200 OK):**

```json
{
  "success": true,
  "message": "Share token resolved successfully",
  "data": {
    "id": 1,
    "shareToken": "K_L4mPqRsT-uVwXyZ1a2b3c4d5e6f7g8h",
    "courseId": 123,
    "linkType": "PUBLIC",
    "isActive": true,
    "currentEnrollments": 5,
    "maxEnrollments": 50,
    "shareUrl": "/join/K_L4mPqRsT-uVwXyZ1a2b3c4d5e6f7g8h"
  }
}
```

**❌ FAILURES:**

- 400: Invalid/expired token
- 400: Link deactivated
- 400: Max enrollments reached

---

#### 2. Enroll via Link

**Endpoint:** `POST /api/join/{token}/enroll`

**Auth:** ✅ Required (User must be logged in)

**✅ SUCCESS (201 Created):**

```json
{
  "success": true,
  "message": "User enrolled successfully",
  "data": {
    "id": 456,
    "courseId": 123,
    "userId": 789,
    "status": "ACTIVE",
    "enrolledAt": "2026-03-22T11:14:17+05:30",
    "progressPercentage": 0.0,
    "courseName": "Advanced Spring Boot"
  }
}
```

**❌ 409 CONFLICT:** Already enrolled

---

### PROGRESS TRACKING (8 endpoints)

#### 1. Mark Lesson Complete

**Endpoint:** `PUT /api/progress/lessons/{lessonId}/complete?courseId={courseId}`

**✅ SUCCESS (200 OK):** Marks complete, updates progress %

---

#### 2. Mark Lesson Incomplete

**Endpoint:** `PUT /api/progress/lessons/{lessonId}/incomplete?courseId={courseId}`

**✅ SUCCESS (200 OK):** Reverts progress

---

#### 3. Get Course Progress

**Endpoint:** `GET /api/progress/courses/{courseId}`

**✅ SUCCESS (200 OK):**

```json
{
  "success": true,
  "data": {
    "courseId": 123,
    "courseName": "Advanced Spring Boot",
    "courseProgress": 30.5,
    "totalLessons": 10,
    "completedLessons": 3,
    "enrolledAt": "2026-03-22T11:14:17+05:30",
    "lastAccessedAt": "2026-03-22T12:14:17+05:30"
  }
}
```

---

#### 4. Get All User Progress ⭐

**Endpoint:** `GET /api/progress/my-progress`

**✅ SUCCESS (200 OK):** Array of all courses with progress

---

#### 5. Get Enrollment Status

**Endpoint:** `GET /api/progress/enrollments/{courseId}`

**✅ SUCCESS (200 OK):** Enrollment details

---

#### 6. Get All Enrollments (Creator Only) 👨‍🏫

**Endpoint:** `GET /api/progress/courses/{courseId}/enrollments`

**Auth:** ✅ Required | **Authorization:** ✅ Creator only

**✅ SUCCESS (200 OK):** List of all students enrolled

---

#### 7. Update Enrollment Status

**Endpoint:** `PUT /api/progress/enrollments/{courseId}/status`

**Valid Values:** ACTIVE, COMPLETED, SUSPENDED, DROPPED

**Request:**

```json
{
  "status": "COMPLETED"
}
```

**✅ SUCCESS (200 OK):** Status updated

---

---

## 🔔 INVITES & NOTIFICATIONS (CURRENT BEHAVIOR)

These endpoints power the **Notifications** / **Shared Courses** UI for the current implementation:

- "Courses shared *with* me" (incoming direct invites only)
- "Courses shared *by* me" (outgoing direct invites you sent)
- Notification badge count for **unread & pending** direct invites
- Accept / decline direct invites (also marks them read)
- Mark one or all invites as read without changing invite status

Underlying data lives in the `course_enrollments` table and is populated when a direct invite is created:

- `invite_type` (STRING) – always `"DIRECT"` for notifications (link enrollments stay as `"LINK"`)
- `invite_status` (STRING) – `"PENDING"` on creation, becomes `"ACCEPTED"` or `"DECLINED"`
- `status` (ENUM) – enrollment status (`SUSPENDED` for pending, `ACTIVE` after accept, `DROPPED` after decline)
- `invited_by` (LONG) – user ID of the inviter (course creator)
- `is_read` (BOOLEAN) – whether the invite notification has been read in the UI

> Note: Link-based enrollments (`invite_type = "LINK"`) are **not** treated as notifications.

### 1. Get Invites Shared With Me (Incoming)

**Endpoint:** `GET /api/sharing/invites/shared-with-me`

**Auth:** ✅ Required  
**Purpose:** List all courses that have been **directly shared** with the current user.

**✅ SUCCESS (200 OK):**

```json
{
  "success": true,
  "message": "Invites shared with me fetched successfully",
  "data": [
    {
      "id": 690903828077219840,
      "courseId": 688090884410970112,
      "userId": 690900585221722112,
      "status": "SUSPENDED",
      "enrolledAt": "2026-03-22T18:15:08.986456+05:30",
      "progressPercentage": 0.0,
      "courseName": "Intro to Spring Boot"
    }
  ]
}
```

**❌ ERROR (400 Bad Request):**

```json
{
  "success": false,
  "message": "Error fetching invites shared with me: <details>",
  "data": null
}
```

> Frontend: Use this to populate the **Notifications** list or "Shared with me" tab.

---

### 2. Get Invites Shared By Me (Outgoing)

**Endpoint:** `GET /api/sharing/invites/shared-by-me`

**Auth:** ✅ Required  
**Purpose:** As a course creator, see who you have invited to your courses.

**✅ SUCCESS (200 OK):**

```json
{
  "success": true,
  "message": "Invites shared by me fetched successfully",
  "data": [
    {
      "id": 690903828077219840,
      "courseId": 688090884410970112,
      "userId": 690900585221722112,
      "status": "SUSPENDED",
      "enrolledAt": "2026-03-22T18:15:08.986456+05:30",
      "progressPercentage": 0.0,
      "courseName": "Intro to Spring Boot"
    }
  ]
}
```

**❌ ERROR (400 Bad Request):**

```json
{
  "success": false,
  "message": "Error fetching invites shared by me: <details>",
  "data": null
}
```

> Frontend: Use this to implement a **"Shared by me"** tab that only lists courses and invites created by the logged-in
> user.

---

### 3. Get Invite Summary (Notification Badge Count)

**Endpoint:** `GET /api/sharing/invites/summary`

**Auth:** ✅ Required  
**Purpose:** Return the **pending invite count** for the current user. This is ideal for a small badge on the
Notifications tab.

The backend counts rows where:

- `user_id = currentUserId`
- `invite_type = "DIRECT"`
- `invite_status = "PENDING"`
- `is_read` is `false` or `null`

**✅ SUCCESS (200 OK):**

```json
{
  "success": true,
  "message": "Invite summary fetched successfully",
  "data": {
    "pendingInvitesCount": 3
  }
}
```

**❌ ERROR (400 Bad Request):**

```json
{
  "success": false,
  "message": "Error fetching invite summary: <details>",
  "data": null
}
```

> Frontend: Call this after login and after accept/decline/mark-all-read to keep the badge count in sync.

---

### 4. Mark a Single Invite As Read

**Endpoint:** `PUT /api/sharing/invites/{inviteId}/read`

**Auth:** ✅ Required \\
**Purpose:** Mark one direct invite as read without changing its accept/decline state.

**✅ SUCCESS (200 OK):**

```json
{
  "success": true,
  "message": "Invite marked as read",
  "data": {
    "id": 690903828077219840,
    "courseId": 688090884410970112,
    "userId": 690900585221722112,
    "status": "SUSPENDED",
    "enrolledAt": "2026-03-22T18:15:08.986456+05:30",
    "progressPercentage": 0.0,
    "courseName": "Intro to Spring Boot"
  }
}
```

**❌ Possible Errors:**

- 404: Invite not found
- 403: Invite not owned by current user
- 400: Invite type is not `DIRECT`

> Frontend: Use this when a notification item is opened. Re-fetch `/api/sharing/invites/summary` after marking.

---

### 5. Accept a Direct Invite

**Endpoint:** `PUT /api/sharing/invites/{inviteId}/accept`

**Auth:** ✅ Required  
**Purpose:** Accept a **DIRECT** course invite that was sent to the current user.

**✅ SUCCESS (200 OK):**

```json
{
  "success": true,
  "message": "Invite accepted successfully",
  "data": {
    "id": 690903828077219840,
    "courseId": 688090884410970112,
    "userId": 690900585221722112,
    "status": "ACTIVE",
    "enrolledAt": "2026-03-22T18:15:08.986456+05:30",
    "progressPercentage": 0.0,
    "courseName": "Intro to Spring Boot"
  }
}
```

**🔁 Idempotent case (already accepted):**

```json
{
  "success": true,
  "message": "Invite already accepted",
  "data": {
    /* same EnrollmentResponse */
  }
}
```

**❌ Possible Errors:**

- Invite not found:

```json
{
  "success": false,
  "message": "Error accepting invite: Invite not found",
  "data": null
}
```

- Invite not owned by current user (403):

```json
{
  "success": false,
  "message": "You are not allowed to act on this invite",
  "data": null
}
```

- Wrong invite type (not DIRECT):

```json
{
  "success": false,
  "message": "Only DIRECT invites can be accepted via this endpoint",
  "data": null
}
```

> Backend side effect: sets `invite_status = "ACCEPTED"`, `enrollment_status = ACTIVE`, and `is_read = true` for that
> row.
> Frontend: After a successful accept, refresh `/api/sharing/invites/summary` to clear the badge.

---

### 6. Decline a Direct Invite

**Endpoint:** `PUT /api/sharing/invites/{inviteId}/decline`

**Auth:** ✅ Required  
**Purpose:** Decline a **DIRECT** course invite.

**✅ SUCCESS (200 OK):**

```json
{
  "success": true,
  "message": "Invite declined successfully",
  "data": {
    "id": 690903828077219840,
    "courseId": 688090884410970112,
    "userId": 690900585221722112,
    "status": "DROPPED",
    "enrolledAt": "2026-03-22T18:15:08.986456+05:30",
    "progressPercentage": 0.0,
    "courseName": "Intro to Spring Boot"
  }
}
```

**🔁 Idempotent case (already declined):**

```json
{
  "success": true,
  "message": "Invite already declined",
  "data": {
    /* same EnrollmentResponse */
  }
}
```

Error responses mirror the Accept API (invite not found, wrong user, wrong invite type).

> Backend side effect: sets `invite_status = "DECLINED"`, `enrollment_status = DROPPED`, and `is_read = true`.
> Frontend: After a successful decline, refresh `/api/sharing/invites/summary` to clear the badge.

---

### 7. Mark All Invites As Read

**Endpoint:** `PUT /api/sharing/invites/mark-all-read`

**Auth:** ✅ Required  
**Purpose:** Mark **all** direct invites for the current user as read in the UI without changing their acceptance
status.

**Backend behavior:**

- For the current user, update all rows where:
  - `invite_type = "DIRECT"`
  - `is_read = false`
- Set `is_read = true`.

**✅ SUCCESS (200 OK):**

```json
{
  "success": true,
  "message": "All invites marked as read",
  "data": {
    "updatedCount": 5
  }
}
```

**❌ ERROR (400 Bad Request):**

```json
{
  "success": false,
  "message": "Error marking invites as read: <details>",
  "data": null
}
```

> Frontend: After calling this, re-fetch `/api/sharing/invites/summary` to clear the notification badge.

---

## 🔑 Authentication

### Headers Required:

```
Authorization: Bearer {JWT_TOKEN}
Content-Type: application/json
```

### Public Endpoint (No Auth):

```
GET /api/join/{token}  ← Can be called without login!
```

---

## 📊 HTTP Status Codes

| Code | Meaning      | Example               |
|------|--------------|-----------------------|
| 200  | OK           | GET/PUT succeeded     |
| 201  | Created      | POST resource created |
| 400  | Bad Request  | Invalid input         |
| 401  | Unauthorized | Missing JWT           |
| 403  | Forbidden    | Not authorized        |
| 404  | Not Found    | Resource missing      |
| 409  | Conflict     | Already enrolled      |
| 500  | Server Error | Database error        |

---

## 🔐 Authorization Matrix

| Endpoint                   | Auth | Creator | User | Public |
|----------------------------|------|---------|------|--------|
| POST .../share/generate    | ✅    | ✅       | ✗    | ✗      |
| GET .../share/links        | ✅    | ✅       | ✗    | ✗      |
| PUT .../share/links/...    | ✅    | ✅       | ✗    | ✗      |
| DELETE .../share/links/... | ✅    | ✅       | ✗    | ✗      |
| POST .../share/invite      | ✅    | ✅       | ✗    | ✗      |
| **GET /api/join/{token}**  | ❌    | ✗       | ✗    | ✅      |
| POST .../enroll            | ✅    | ✗       | ✅    | ✗      |
| PUT .../complete           | ✅    | ✗       | ✅    | ✗      |
| GET .../progress           | ✅    | ✗       | ✅    | ✗      |
| GET .../enrollments        | ✅    | ✅       | ✗    | ✗      |

---

## 💡 Enhancement Recommendations

### 🔥 HIGH PRIORITY (2 weeks):

1. Email notifications
2. Unit tests (50%+ coverage)
3. Database migrations
4. Webhook system

### ⭐ MEDIUM PRIORITY (Weeks 3-4):

1. Leaderboard with rankings
2. Notification system
3. Analytics dashboard
4. QR code generation

### 🎯 MEDIUM-TERM (Weeks 5-8):

1. Gamification (badges, points, streaks)
2. Bulk enrollment import
3. Role-based access control
4. Adaptive learning paths

### 🚀 LONG-TERM (Months 2-3):

1. Social features (forums)
2. Calendar integration
3. Mobile app support
4. Enterprise SSO (LDAP, SAML)

---

## 📝 Important Notes

1. **Token Format** - Base64-URL safe (32 bytes = ~43 chars)
2. **Progress Formula** - (completed / total) × 100
3. **Only ONE Public Endpoint** - GET /api/join/{token}
4. **Unique Enrollment** - One per (courseId, userId)
5. **Timestamps** - OffsetDateTime with timezone
6. **Security** - SecureRandom tokens, authorization verified

---

## 🧪 Testing Examples

```bash
# Create link (Creator)
curl -X POST http://localhost:8080/api/courses/123/share/generate \
  -H "Authorization: Bearer {JWT}" \
  -H "Content-Type: application/json" \
  -d '{"linkType":"PUBLIC"}'

# Get preview (PUBLIC - no auth!)
curl -X GET http://localhost:8080/api/join/K_L4mPqRsT-uVwXyZ1a2b3c4d5e6f7g8h

# Enroll (User - requires JWT)
curl -X POST http://localhost:8080/api/join/K_L4mPqRsT.../enroll \
  -H "Authorization: Bearer {JWT}"

# Mark complete
curl -X PUT "http://localhost:8080/api/progress/lessons/456/complete?courseId=123" \
  -H "Authorization: Bearer {JWT}"

# Get my progress
curl -X GET http://localhost:8080/api/progress/my-progress \
  -H "Authorization: Bearer {JWT}"
```

---

## ✅ Response Format (All Endpoints)

**Success:**

```json
{
  "success": true,
  "message": "Descriptive message",
  "data": {
    /* response object */
  }
}
```

**Error:**

```json
{
  "success": false,
  "message": "Error description",
  "data": null
}
```

---

## 🔧 Troubleshooting

| Issue                 | Solution                                   |
|-----------------------|--------------------------------------------|
| Token expired         | Regenerate new link                        |
| Already enrolled      | Can't enroll twice (by design)             |
| Progress not updating | Verify lesson exists, check @Transactional |
| 401 Unauthorized      | JWT expired - login again                  |
| 403 Forbidden         | Must be course creator                     |

---

**Status:** ✅|**Version:** 1.1 | **Last Updated:** March 22, 2026
