import { describe, it, expect, beforeEach } from 'vitest';
import {
  getApiBaseUrl,
  WORKSPACE_STORAGE_KEY,
  getStoredWorkspaceKey,
  setStoredWorkspaceKey,
  clearStoredWorkspaceKey,
} from '../services/api';

describe('API Base URL Configuration (getApiBaseUrl)', () => {
  it('falls back to relative /api/v1 when VITE_API_URL is undefined', () => {
    expect(getApiBaseUrl(undefined)).toBe('/api/v1');
  });

  it('falls back to relative /api/v1 when VITE_API_URL is empty or whitespace', () => {
    expect(getApiBaseUrl('')).toBe('/api/v1');
    expect(getApiBaseUrl('   ')).toBe('/api/v1');
  });

  it('appends /api/v1 to configured backend base URL without trailing slash', () => {
    expect(getApiBaseUrl('http://localhost:8080')).toBe('http://localhost:8080/api/v1');
    expect(getApiBaseUrl('https://my-api.onrender.com')).toBe('https://my-api.onrender.com/api/v1');
  });

  it('strips single or multiple trailing slashes before appending /api/v1', () => {
    expect(getApiBaseUrl('http://localhost:8080/')).toBe('http://localhost:8080/api/v1');
    expect(getApiBaseUrl('https://my-api.onrender.com///')).toBe('https://my-api.onrender.com/api/v1');
  });

  it('does not append /api/v1 twice if already provided in the environment variable', () => {
    expect(getApiBaseUrl('http://localhost:8080/api/v1')).toBe('http://localhost:8080/api/v1');
    expect(getApiBaseUrl('https://my-api.onrender.com/api/v1/')).toBe('https://my-api.onrender.com/api/v1');
    expect(getApiBaseUrl('https://my-api.onrender.com/api/v1///')).toBe('https://my-api.onrender.com/api/v1');
  });
});

describe('Workspace Key Storage', () => {
  beforeEach(() => {
    localStorage.clear();
  });

  it('returns null when no workspace key is stored', () => {
    expect(getStoredWorkspaceKey()).toBeNull();
  });

  it('stores and retrieves workspace key from localStorage', () => {
    const testKey = 'wpk_abcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890';
    setStoredWorkspaceKey(testKey);
    expect(localStorage.getItem(WORKSPACE_STORAGE_KEY)).toBe(testKey);
    expect(getStoredWorkspaceKey()).toBe(testKey);
  });

  it('clears stored workspace key from localStorage', () => {
    const testKey = 'wpk_1111222233334444555566667777888899990000aaaabbbbccccddddeeeeffff';
    setStoredWorkspaceKey(testKey);
    expect(getStoredWorkspaceKey()).toBe(testKey);
    clearStoredWorkspaceKey();
    expect(getStoredWorkspaceKey()).toBeNull();
    expect(localStorage.getItem(WORKSPACE_STORAGE_KEY)).toBeNull();
  });
});
