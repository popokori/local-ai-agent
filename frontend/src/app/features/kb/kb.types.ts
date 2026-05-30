export type Domain =
  | 'GENERIC'
  | 'SCIENCE'
  | 'BIOLOGY'
  | 'CHEMISTRY'
  | 'MATHEMATICS'
  | 'MEDICAL'
  | 'COMPUTER_SCIENCE';

export type DocumentStatus = 'UPLOADED' | 'PARSING' | 'INDEXED' | 'FAILED';

export interface KnowledgeBaseDto {
  id: number;
  name: string;
  slug: string;
  description: string | null;
  domain: Domain;
  embeddingModel: string;
  embeddingDim: number;
  createdAt: string;
}

export interface CreateKbRequest {
  name: string;
  description?: string;
  domain?: Domain;
}

export interface DocumentDto {
  id: number;
  kbId: number;
  fileName: string;
  mimeType: string | null;
  sizeBytes: number;
  status: DocumentStatus;
  error: string | null;
  pageCount: number | null;
  chunkCount: number | null;
  createdAt: string;
  indexedAt: string | null;
}
