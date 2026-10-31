export type HttpMethod = 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE' | 'HEAD' | 'OPTIONS';

export interface KeyValue {
  key: string;
  value: string;
  enabled: boolean;
}

export type AuthPlacement = 'HEADER' | 'QUERY';

export interface AuthConfig {
  type: 'NONE' | 'API_KEY';
  keyName: string;
  keyValue: string;
  placement: AuthPlacement;
}

export type ExecutionState =
  | 'IDLE'
  | 'VALIDATING'
  | 'LOADING'
  | 'SUCCESS'
  | 'HTTP_ERROR'
  | 'NETWORK_ERROR'
  | 'TIMEOUT'
  | 'CANCELLED';

export interface ExecutionResponse {
  statusCode: number;
  statusText: string;
  durationMs: number;
  sizeBytes: number;
  headers: Record<string, string>;
  body: string;
  contentType: string;
  errorCategory: 'SUCCESS' | 'HTTP_ERROR' | 'NETWORK_ERROR' | 'TIMEOUT' | 'SECURITY_VIOLATION' | string;
  errorMessage?: string;
  historyId?: number;
}

export interface ExecuteRequestPayload {
  method: HttpMethod;
  url: string;
  queryParams: KeyValue[];
  headers: KeyValue[];
  auth?: AuthConfig;
  body?: string;
  timeoutMs?: number;
  environmentId?: number;
}

export interface SaveRequestPayload {
  collectionId?: number;
  name: string;
  method: string;
  url: string;
  queryParams?: KeyValue[];
  headers?: KeyValue[];
  auth?: AuthConfig;
  authConfig?: AuthConfig;
  authType?: string;
  body?: string;
}

export interface SavedRequest {
  id: number;
  collectionId?: number;
  name: string;
  method: HttpMethod;
  url: string;
  queryParams: KeyValue[];
  headers: KeyValue[];
  authType: string;
  authConfig?: AuthConfig;
  body?: string;
  createdAt: string;
  updatedAt: string;
}

export interface Collection {
  id: number;
  name: string;
  description?: string;
  requests: SavedRequest[];
  requestCount: number;
  createdAt: string;
  updatedAt: string;
}

export interface EnvironmentVariable {
  id?: number;
  key: string;
  value: string;
  isSecret: boolean;
  isEnabled: boolean;
}

export interface Environment {
  id: number;
  name: string;
  isActive: boolean;
  variables: EnvironmentVariable[];
  createdAt: string;
  updatedAt: string;
}

export interface RequestHistoryItem {
  id: number;
  method: string;
  url: string;
  requestHeaders?: string;
  requestBody?: string;
  responseStatus?: number;
  responseStatusText?: string;
  responseHeaders?: string;
  responseBody?: string;
  responseSize?: number;
  durationMs?: number;
  errorCategory?: string;
  executedAt: string;
}

export interface ToastMessage {
  id: string;
  type: 'success' | 'error' | 'info' | 'warning';
  title?: string;
  message: string;
  duration?: number;
}
