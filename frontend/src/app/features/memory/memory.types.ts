export type MemoryKind = 'EPISODIC' | 'SEMANTIC';

export interface UserFactDto {
  id: number;
  factKey: string;
  factValue: string;
  confidence: number | null;
  source: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface UpsertFactRequest {
  factKey: string;
  factValue: string;
  confidence?: number;
}

export interface MemoryEntryDto {
  id: number;
  kind: MemoryKind;
  summary: string;
  sourceSessionId: number | null;
  importance: number | null;
  createdAt: string;
  lastAccessedAt: string | null;
}
