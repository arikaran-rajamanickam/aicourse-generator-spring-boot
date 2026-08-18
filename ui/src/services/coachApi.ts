import { apiFetch } from "./apiClient";
import { CoachRequest, CoachResponse } from "../types/coach";
import { executeMcpTool } from "./mcpApi";
import { USE_MCP_CLIENT } from "../constants";

export async function getCoachResponse(payload: CoachRequest): Promise<CoachResponse> {
  if (USE_MCP_CLIENT) {
    const mcpResponse = await executeMcpTool<CoachResponse>({
      tool: "coach.respond",
      input: payload,
    });

    if (!mcpResponse?.success || !mcpResponse.data) {
      throw new Error(mcpResponse?.error || "MCP coach.respond failed");
    }
    return mcpResponse.data;
  }

  const response = await apiFetch("/api/coach/respond", {
    method: "POST",
    body: JSON.stringify(payload),
  });

  if (response?.data) {
    return response.data as CoachResponse;
  }

  return response as CoachResponse;
}

/**
 * Stream AI Coach response using SSE
 */
export async function streamCoachResponse(
  payload: CoachRequest,
  onToken: (token: string) => void,
  onComplete: () => void,
  onError: (error: any) => void
) {
  const token = localStorage.getItem('token');
  const url = "/api/coach/respond/stream";
  
  if (USE_MCP_CLIENT) {
    // We don't have a streaming implementation for MCP yet.
    // Throw an error to trigger the fallback to getCoachResponse.
    throw new Error("Streaming not supported with MCP client, falling back to standard response.");
  }

  try {
    const controller = new AbortController();
    const timeoutId = setTimeout(() => controller.abort(), 30000); // 30s timeout to allow for model thinking

    const response = await fetch(url, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        "ngrok-skip-browser-warning": "true",
        ...(token && { Authorization: `Bearer ${token}` }),
      },
      body: JSON.stringify(payload),
      signal: controller.signal,
    });

    clearTimeout(timeoutId);

    if (!response.ok) {
      throw new Error(`SSE failed: ${response.status}`);
    }

    const reader = response.body?.getReader();
    if (!reader) throw new Error("ReadableStream not supported");

    const decoder = new TextDecoder();
    
    // Add a heartbeat/watchdog to ensure we keep receiving data
    let lastDataTime = Date.now();
    let receivedAnyData = false;
    
    let buffer = "";
    
    while (true) {
      const { done, value } = await reader.read();
      if (done) break;

      lastDataTime = Date.now();
      const chunk = decoder.decode(value, { stream: true });
      buffer += chunk;
      
      const lines = buffer.split("\n");
      // The last element is either an empty string (if chunk ended with \n) 
      // or a partial line. Keep it for the next round.
      buffer = lines.pop() || "";

      for (const line of lines) {
        const trimmedLine = line.trim();
        if (trimmedLine.startsWith("data:")) {
          let content = trimmedLine.substring(5);
          if (content.startsWith(" ")) {
            content = content.substring(1);
          }
          
          if (content || trimmedLine.length > 5) {
            receivedAnyData = true;
            onToken(content);
          }
        }
      }
    }
    
    // Process any remaining data in the buffer after stream ends
    if (buffer.trim().startsWith("data:")) {
      let content = buffer.trim().substring(5);
      if (content.startsWith(" ")) content = content.substring(1);
      onToken(content);
      receivedAnyData = true;
    }
    
    if (!receivedAnyData) {
      throw new Error("Stream closed without sending any data. Falling back.");
    }
    
    onComplete();
  } catch (err) {
    onError(err);
  }
}
