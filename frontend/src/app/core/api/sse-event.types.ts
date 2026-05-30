/**
 * Événements SSE émis par le backend Spring (cf. StreamEventType côté Java).
 * Sealed union pour exhaustivité dans les switch côté consommateur.
 */
export type SseEvent =
  | SourceEvent
  | ToolStartEvent
  | ToolEndEvent
  | TokenEvent
  | FinalEvent
  | StreamErrorEvent;

export interface SourceEvent {
  event: 'source';
  data: {
    index: number;
    documentId: number;
    documentName: string;
    page: number | '';
    score: number;
    snippet: string;
  };
}

export interface ToolStartEvent {
  event: 'tool_start';
  data: {
    iteration: number;
    name: string;
    arguments: Record<string, unknown>;
  };
}

export interface ToolEndEvent {
  event: 'tool_end';
  data: {
    iteration: number;
    name: string;
    success: boolean;
    summary: string;
  };
}

export interface TokenEvent {
  event: 'token';
  data: { text: string };
}

export interface FinalEvent {
  event: 'final';
  data: {
    messageId: number;
    userMessageId?: number;
    replayed?: boolean;
  };
}

export interface StreamErrorEvent {
  event: 'error';
  data: { message: string };
}

/** Parse un bloc SSE "event:foo\ndata:..." vers un événement typé, ou null. */
export function parseSseBlock(block: string): SseEvent | null {
  let event = '';
  let dataStr = '';
  for (const line of block.split('\n')) {
    if (line.startsWith('event:')) event = line.slice(6).trim();
    else if (line.startsWith('data:')) dataStr += line.slice(5).trim();
  }
  if (!event || !dataStr) return null;
  try {
    const data = JSON.parse(dataStr);
    return { event, data } as SseEvent;
  } catch {
    return null;
  }
}
