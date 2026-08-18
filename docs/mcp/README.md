# MCP Integration (Phase 1)

This project now includes an MCP-style tool transport at `/api/mcp` with feature-flagged rollout.

## Current tools

- `lesson.generate` - generates lesson content blocks for a lesson
- `coach.respond` - returns AI coach response for a user message
- `llm.providers.list` - lists provider metadata and key health (admin-only)
- `llm.providers.health` - lists provider health snapshots (admin-only)
- `llm.routes.list` - lists workload routing (admin-only)
- `llm.routes.upsert` - updates workload routing (admin-only)

## Endpoints

- `GET /api/mcp/tools` - list available tool descriptors
- `POST /api/mcp/execute` - execute a tool with JSON input

## Feature flag

Set in `src/main/resources/application.properties`:

```properties
mcp.enabled=false
```

Set `mcp.enabled=true` to enable MCP endpoint/controller flow.

Frontend toggle:

```properties
VITE_USE_MCP=true
```

When enabled, UI `lesson.generate` and `coach.respond` calls are routed through `/api/mcp/execute`.

## Sample execute payloads

### lesson.generate

```json
{
  "tool": "lesson.generate",
  "input": {
    "courseId": 1001,
    "moduleId": 2001,
    "lessonId": 3001
  }
}
```

### coach.respond

```json
{
  "tool": "coach.respond",
  "input": {
    "courseId": 1001,
    "lessonId": 3001,
    "message": "Give me a short quiz",
    "previousQuizQuestions": [],
    "chatHistory": []
  }
}
```

### llm.providers.list

```json
{
  "tool": "llm.providers.list",
  "input": {}
}
```

### llm.routes.upsert

```json
{
  "tool": "llm.routes.upsert",
  "input": {
    "workload": "AI_COACH",
    "providerCode": "gemini"
  }
}
```

## Audit logging

Each MCP execution now writes request audit logs with:

- request ID
- tool name
- user ID and role
- input size
- success/failure
- latency in milliseconds

These logs are emitted from `McpFacadeService` with `mcp.audit` prefixes.
Audit entries are also persisted in the `mcp_audit_logs` table.

## Frontend MCP routing

With `VITE_USE_MCP=true`, the UI routes these LLM admin calls through MCP:

- `fetchLlmProviders` -> `llm.providers.list`
- `upsertLlmRoute` -> `llm.routes.upsert`

## Notes

- Existing endpoints remain backward compatible.
- `LessonController` and `AiCoachController` now support MCP-backed execution when flag is enabled.
- Streaming coach endpoint remains on legacy path in this phase.


