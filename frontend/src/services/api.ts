import axios from 'axios';
import {
  Collection,
  Environment,
  ExecuteRequestPayload,
  ExecutionResponse,
  RequestHistoryItem,
  SavedRequest,
} from '../types';

export const WORKSPACE_STORAGE_KEY = 'api_testing_playground_workspace_key';

export const getStoredWorkspaceKey = (): string | null => {
  try {
    return localStorage.getItem(WORKSPACE_STORAGE_KEY);
  } catch {
    return null;
  }
};

export const setStoredWorkspaceKey = (key: string): void => {
  try {
    localStorage.setItem(WORKSPACE_STORAGE_KEY, key);
  } catch {
    // localStorage may be unavailable
  }
};

export const clearStoredWorkspaceKey = (): void => {
  try {
    localStorage.removeItem(WORKSPACE_STORAGE_KEY);
  } catch {
    // ignore
  }
};

export const getApiBaseUrl = (rawUrl?: string): string => {
  if (!rawUrl || !rawUrl.trim()) {
    return '/api/v1';
  }
  // Trim whitespace, strip trailing slashes, and avoid appending /api/v1 twice
  const cleanUrl = rawUrl.trim().replace(/\/+$/, '').replace(/\/api\/v1\/?$/, '');
  return `${cleanUrl}/api/v1`;
};

const api = axios.create({
  baseURL: getApiBaseUrl(import.meta.env.VITE_API_URL),
  headers: {
    'Content-Type': 'application/json',
  },
});

api.interceptors.request.use((config) => {
  const workspaceKey = getStoredWorkspaceKey();
  if (workspaceKey && config.headers) {
    config.headers['X-Workspace-Key'] = workspaceKey;
  }
  return config;
});

export const playgroundApi = {
  // Workspaces
  createWorkspace: async (name?: string): Promise<{ id: number; name: string; workspaceKey: string }> => {
    const response = await api.post('/workspaces', { name: name || 'Default Workspace' });
    if (response.data?.workspaceKey) {
      setStoredWorkspaceKey(response.data.workspaceKey);
    }
    return response.data;
  },

  getCurrentWorkspace: async (): Promise<{ id: number; name: string; createdAt: string }> => {
    const response = await api.get('/workspaces/current');
    return response.data;
  },

  initWorkspace: async (): Promise<string> => {
    let key = getStoredWorkspaceKey();
    if (key) {
      try {
        await api.get('/workspaces/current');
        return key;
      } catch (err: any) {
        if (err.response?.status === 401) {
          clearStoredWorkspaceKey();
          key = null;
        }
      }
    }
    if (!key) {
      const created = await playgroundApi.createWorkspace('Default Workspace');
      key = created.workspaceKey;
    }
    return key;
  },
  executeRequest: async (payload: ExecuteRequestPayload, signal?: AbortSignal): Promise<ExecutionResponse> => {
    const response = await api.post<ExecutionResponse>('/execute', payload, { signal });
    return response.data;
  },

  // Collections
  getCollections: async (): Promise<Collection[]> => {
    const response = await api.get<Collection[]>('/collections');
    return response.data;
  },

  createCollection: async (name: string, description?: string): Promise<Collection> => {
    const response = await api.post<Collection>('/collections', { name, description });
    return response.data;
  },

  updateCollection: async (id: number, name: string, description?: string): Promise<Collection> => {
    const response = await api.put<Collection>(`/collections/${id}`, { name, description });
    return response.data;
  },

  deleteCollection: async (id: number): Promise<void> => {
    await api.delete(`/collections/${id}`);
  },

  // Saved Requests
  getSavedRequests: async (collectionId?: number): Promise<SavedRequest[]> => {
    const params = collectionId ? { collectionId } : {};
    const response = await api.get<SavedRequest[]>('/requests', { params });
    return response.data;
  },

  saveRequest: async (payload: import('../types').SaveRequestPayload): Promise<SavedRequest> => {
    const response = await api.post<SavedRequest>('/requests', payload);
    return response.data;
  },

  updateSavedRequest: async (id: number, payload: Partial<SavedRequest>): Promise<SavedRequest> => {
    const response = await api.put<SavedRequest>(`/requests/${id}`, payload);
    return response.data;
  },

  duplicateSavedRequest: async (id: number): Promise<SavedRequest> => {
    const response = await api.post<SavedRequest>(`/requests/${id}/duplicate`);
    return response.data;
  },

  deleteSavedRequest: async (id: number): Promise<void> => {
    await api.delete(`/requests/${id}`);
  },

  // Environments
  getEnvironments: async (): Promise<Environment[]> => {
    const response = await api.get<Environment[]>('/environments');
    return response.data;
  },

  getActiveEnvironment: async (): Promise<Environment | null> => {
    const response = await api.get<Environment>('/environments/active');
    return response.status === 204 ? null : response.data;
  },

  createEnvironment: async (payload: { name: string; isActive?: boolean; variables: any[] }): Promise<Environment> => {
    const response = await api.post<Environment>('/environments', payload);
    return response.data;
  },

  updateEnvironment: async (id: number, payload: { name: string; isActive?: boolean; variables: any[] }): Promise<Environment> => {
    const response = await api.put<Environment>(`/environments/${id}`, payload);
    return response.data;
  },

  activateEnvironment: async (id: number): Promise<Environment> => {
    const response = await api.patch<Environment>(`/environments/${id}/activate`);
    return response.data;
  },

  deleteEnvironment: async (id: number): Promise<void> => {
    await api.delete(`/environments/${id}`);
  },

  // History
  getHistory: async (page?: number, size: number = 50): Promise<RequestHistoryItem[]> => {
    const params = page !== undefined ? { page, size } : {};
    const response = await api.get<any>('/history', { params });
    return Array.isArray(response.data) ? response.data : response.data.content || [];
  },

  deleteHistoryItem: async (id: number): Promise<void> => {
    await api.delete(`/history/${id}`);
  },

  clearHistory: async (): Promise<void> => {
    await api.delete('/history');
  },

  // Health
  getHealth: async (): Promise<any> => {
    const response = await api.get('/health');
    return response.data;
  },
};
