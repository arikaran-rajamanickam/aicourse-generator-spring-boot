export interface McpToolRequest {
  tool: string;
  input?: unknown;
}

export interface McpToolResponse<T = unknown> {
  success: boolean;
  tool: string;
  data?: T;
  error?: string;
}

export interface McpToolDescriptor {
  name: string;
  description: string;
  inputSchema?: any;
}

