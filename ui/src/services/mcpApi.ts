import { apiFetch } from "./apiClient";
import type { McpToolDescriptor, McpToolRequest, McpToolResponse } from "../types/mcp";

export async function listMcpTools(): Promise<McpToolDescriptor[]> {
  return apiFetch("/api/mcp/tools");
}

export async function executeMcpTool<T = unknown>(payload: McpToolRequest): Promise<McpToolResponse<T>> {
  return apiFetch("/api/mcp/execute", {
    method: "POST",
    body: JSON.stringify(payload),
  });
}

