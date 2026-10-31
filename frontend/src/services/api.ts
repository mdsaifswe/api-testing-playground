import axios from 'axios';
import {
  Collection,
  Environment,
  ExecuteRequestPayload,
  ExecutionResponse,
  RequestHistoryItem,
  SavedRequest,
} from '../types';

const api = axios.create({
  baseURL: '/api/v1',
  headers: {
    'Content-Type': 'application/json',
  },
});

export const playgroundApi = {
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
