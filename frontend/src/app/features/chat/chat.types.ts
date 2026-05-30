export type ChatMode = 'NORMAL' | 'EXPERT' | 'FACT_CHECK';
export type MessageRole = 'USER' | 'ASSISTANT' | 'SYSTEM' | 'TOOL';

export interface ChatSessionDto {
  id: number;
  title: string | null;
  mode: ChatMode;
  modelName: string | null;
  knowledgeBaseId: number | null;
  createdAt: string;
  updatedAt: string;
  lastMessageAt: string | null;
}

export interface CreateChatRequest {
  title?: string;
  mode?: ChatMode;
  modelName?: string;
  knowledgeBaseId?: number;
}

export interface UpdateChatRequest {
  title?: string;
  mode?: ChatMode;
  modelName?: string;
  knowledgeBaseId?: number;
}

export interface MessageDto {
  id: number;
  sessionId: number;
  role: MessageRole;
  content: string;
  tokensIn: number | null;
  tokensOut: number | null;
  latencyMs: number | null;
  clientRequestId: string | null;
  createdAt: string;
}

export interface SendMessageRequest {
  content: string;
  clientRequestId?: string;
}

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  last: boolean;
}
