import { apiFetch } from "./apiClient";
import { executeMcpTool } from "./mcpApi";
import { USE_MCP_CLIENT } from "../constants";

export type ProviderType = "GEMINI" | "GROQ";
export type WorkloadType = "COURSE_GENERATION" | "LESSON_GENERATION" | "AI_COACH";

export interface LlmProvider {
  id: number;
  code: string;
  displayName: string;
  providerType: ProviderType;
  modelName: string;
  baseUrl?: string | null;
  keyCooldownHours: number;
  enabled: boolean;
  keyCount: number;
  activeKeyCount: number;
  coolingDownKeyCount: number;
  lastError?: string | null;
  lastErrorAt?: string | null;
  lastSuccessAt?: string | null;
  maskedKeys: string[];
}

export interface LlmRoute {
  workload: WorkloadType;
  providerCode: string;
  providerDisplayName: string;
}

export interface LlmProviderHealth {
  providerCode: string;
  activeKeyCount: number;
  coolingDownKeyCount: number;
  lastError?: string | null;
  lastErrorAt?: string | null;
  lastSuccessAt?: string | null;
}

export interface McpAuditLogItem {
  id: number;
  requestId: string;
  tool: string;
  userId?: number | null;
  userRole?: string | null;
  inputSize?: number | null;
  status: string;
  latencyMs?: number | null;
  errorMessage?: string | null;
  responseBody?: string | null;
  createdAt: string;
}

export interface McpAuditLogPage {
  items: McpAuditLogItem[];
  page: number;
  size: number;
  totalItems: number;
  totalPages: number;
}

export interface McpAuditFilterOptions {
  tools: string[];
  statuses: string[];
}

export interface LlmProviderPayload {
  code: string;
  displayName: string;
  providerType: ProviderType;
  modelName: string;
  baseUrl?: string;
  keyCooldownHours: number;
  enabled: boolean;
  apiKeys?: string[];
}

export async function fetchLlmProviders(): Promise<LlmProvider[]> {
  if (USE_MCP_CLIENT) {
    const response = await executeMcpTool<LlmProvider[]>({
      tool: "llm.providers.list",
      input: {},
    });
    if (!response.success) {
      throw new Error(response.error || "MCP llm.providers.list failed");
    }
    return Array.isArray(response.data) ? response.data : [];
  }

  return apiFetch("/api/admin/llm/providers");
}

export async function upsertLlmProvider(payload: LlmProviderPayload): Promise<LlmProvider> {
  return apiFetch(`/api/admin/llm/providers/${encodeURIComponent(payload.code)}`, {
    method: "PUT",
    body: JSON.stringify(payload),
  });
}

export async function fetchLlmRoutes(): Promise<LlmRoute[]> {
  if (USE_MCP_CLIENT) {
    const response = await executeMcpTool<LlmRoute[]>({
      tool: "llm.routes.list",
      input: {},
    });
    if (!response.success) {
      throw new Error(response.error || "MCP llm.routes.list failed");
    }
    return Array.isArray(response.data) ? response.data : [];
  }

  return apiFetch("/api/admin/llm/routes");
}

export async function fetchLlmProviderHealth(): Promise<LlmProviderHealth[]> {
  if (USE_MCP_CLIENT) {
    const response = await executeMcpTool<LlmProviderHealth[]>({
      tool: "llm.providers.health",
      input: {},
    });
    if (!response.success) {
      throw new Error(response.error || "MCP llm.providers.health failed");
    }
    return Array.isArray(response.data) ? response.data : [];
  }

  return apiFetch("/api/admin/llm/health");
}

export async function upsertLlmRoute(workload: WorkloadType, providerCode: string): Promise<LlmRoute> {
  if (USE_MCP_CLIENT) {
    const response = await executeMcpTool<LlmRoute>({
      tool: "llm.routes.upsert",
      input: { workload, providerCode },
    });
    if (!response.success || !response.data) {
      throw new Error(response.error || "MCP llm.routes.upsert failed");
    }
    return response.data;
  }

  return apiFetch("/api/admin/llm/routes", {
    method: "PUT",
    body: JSON.stringify({ workload, providerCode }),
  });
}

export interface FetchMcpAuditLogsParams {
  tool?: string;
  status?: string;
  from?: string;
  to?: string;
  page?: number;
  size?: number;
}

export async function fetchMcpAuditLogs(params: FetchMcpAuditLogsParams = {}): Promise<McpAuditLogPage> {
  const query = new URLSearchParams();
  if (params.tool) query.set("tool", params.tool);
  if (params.status) query.set("status", params.status);
  if (params.from) query.set("from", params.from);
  if (params.to) query.set("to", params.to);
  query.set("page", String(params.page ?? 0));
  query.set("size", String(params.size ?? 20));

  const suffix = query.toString();
  return apiFetch(`/api/admin/mcp/audit-logs${suffix ? `?${suffix}` : ""}`);
}

export async function fetchMcpAuditFilterOptions(): Promise<McpAuditFilterOptions> {
  return apiFetch("/api/admin/mcp/audit-logs/filters");
}



