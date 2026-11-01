import React, { useState, useEffect, useCallback, useRef } from 'react';
import { AppShell } from './components/layout/AppShell';
import { RequestBuilder } from './components/request/RequestBuilder';
import { ResponseViewer } from './components/response/ResponseViewer';
import { SaveRequestModal } from './components/collections/SaveRequestModal';
import { EnvironmentManager } from './components/environments/EnvironmentManager';
import { ToastNotifications } from './components/common/ToastNotifications';
import { playgroundApi } from './services/api';
import { generateCurlCommand } from './utils/curl';
import {
  AuthConfig,
  Collection,
  Environment,
  EnvironmentVariable,
  ExecutionResponse,
  ExecutionState,
  HttpMethod,
  KeyValue,
  RequestHistoryItem,
  SavedRequest,
  ToastMessage,
} from './types';

export const App: React.FC = () => {
  const abortControllerRef = useRef<AbortController | null>(null);
  // Request Builder State
  const [method, setMethod] = useState<HttpMethod>('GET');
  const [url, setUrl] = useState<string>('http://localhost:8080/api/v1/echo/get?demo=true');
  const [queryParams, setQueryParams] = useState<KeyValue[]>([
    { key: 'demo', value: 'true', enabled: true },
  ]);
  const [headers, setHeaders] = useState<KeyValue[]>([
    { key: 'Accept', value: 'application/json', enabled: true },
  ]);
  const [auth, setAuth] = useState<AuthConfig>({
    type: 'NONE',
    keyName: 'X-API-Key',
    keyValue: '',
    placement: 'HEADER',
  });
  const [body, setBody] = useState<string>(
    '{\n  "message": "Hello from API Testing Playground",\n  "status": "ready"\n}'
  );

  // Execution & Response State
  const [executionState, setExecutionState] = useState<ExecutionState>('IDLE');
  const [response, setResponse] = useState<ExecutionResponse | null>(null);

  // Layout & Sidebar State
  const [sidebarTab, setSidebarTab] = useState<'collections' | 'history'>('collections');
  const [sidebarCollapsed, setSidebarCollapsed] = useState<boolean>(false);

  // Backend Data State
  const [collections, setCollections] = useState<Collection[]>([]);
  const [history, setHistory] = useState<RequestHistoryItem[]>([]);
  const [environments, setEnvironments] = useState<Environment[]>([]);
  const [activeEnvironment, setActiveEnvironment] = useState<Environment | null>(null);
  const [backendHealthy, setBackendHealthy] = useState<boolean>(false);

  // Modals & Notifications
  const [isSaveModalOpen, setIsSaveModalOpen] = useState(false);
  const [isEnvManagerOpen, setIsEnvManagerOpen] = useState(false);
  const [toasts, setToasts] = useState<ToastMessage[]>([]);

  const addToast = (type: 'success' | 'error' | 'info' | 'warning', message: string, title?: string) => {
    const id = `${Date.now()}-${Math.random()}`;
    setToasts((prev) => [...prev, { id, type, title, message }]);
    setTimeout(() => {
      setToasts((prev) => prev.filter((t) => t.id !== id));
    }, 4000);
  };

  const removeToast = (id: string) => {
    setToasts((prev) => prev.filter((t) => t.id !== id));
  };

  // Initial Data Fetch
  const loadInitialData = useCallback(async () => {
    try {
      const healthData = await playgroundApi.getHealth();
      setBackendHealthy(healthData.status === 'UP');
    } catch {
      setBackendHealthy(false);
    }

    try {
      await playgroundApi.initWorkspace();
      const [colData, histData, envData, activeEnvData] = await Promise.all([
        playgroundApi.getCollections(),
        playgroundApi.getHistory(),
        playgroundApi.getEnvironments(),
        playgroundApi.getActiveEnvironment(),
      ]);

      setCollections(colData || []);
      setHistory(histData || []);
      setEnvironments(envData || []);
      setActiveEnvironment(activeEnvData);
    } catch (err: any) {
      console.warn('Initial data load warning:', err.message);
    }
  }, []);

  useEffect(() => {
    loadInitialData();
  }, [loadInitialData]);

  // Request Execution & Cancellation
  const handleSend = async () => {
    if (!url.trim()) {
      addToast('warning', 'Please enter a target URL before sending.', 'URL Required');
      return;
    }

    if (abortControllerRef.current) {
      abortControllerRef.current.abort();
    }
    const controller = new AbortController();
    abortControllerRef.current = controller;

    setExecutionState('LOADING');

    try {
      const result = await playgroundApi.executeRequest(
        {
          method,
          url: url.trim(),
          queryParams,
          headers,
          auth,
          body,
          environmentId: activeEnvironment?.id,
        },
        controller.signal
      );

      setResponse(result);

      if (result.errorCategory === 'SUCCESS') {
        setExecutionState('SUCCESS');
        addToast('success', `Completed with status ${result.statusCode} ${result.statusText}`);
      } else if (result.errorCategory === 'HTTP_ERROR') {
        setExecutionState('HTTP_ERROR');
        addToast('warning', `Target returned HTTP ${result.statusCode} ${result.statusText}`, 'HTTP Error');
      } else if (result.errorCategory === 'SECURITY_VIOLATION') {
        setExecutionState('SECURITY_VIOLATION' as any);
        addToast('error', result.errorMessage || 'Blocked by SSRF security policy', 'Security Blocked');
      } else if (result.errorCategory === 'TIMEOUT') {
        setExecutionState('TIMEOUT');
        addToast('error', result.errorMessage || 'Target server did not respond in time', 'Timeout');
      } else {
        setExecutionState('NETWORK_ERROR');
        addToast('error', result.errorMessage || 'Could not connect to target host', 'Network Error');
      }

      // Refresh history from backend
      const updatedHistory = await playgroundApi.getHistory();
      setHistory(updatedHistory);
    } catch (err: any) {
      if (err.name === 'CanceledError' || err.name === 'AbortError' || err.code === 'ERR_CANCELED') {
        setExecutionState('IDLE');
        addToast('info', 'Request execution was cancelled');
        return;
      }

      const errorMsg = err.response?.data?.message || err.message || 'Execution error';
      setResponse({
        statusCode: err.response?.status || 500,
        statusText: 'Execution Error',
        durationMs: 0,
        sizeBytes: 0,
        headers: {},
        body: JSON.stringify(err.response?.data || { error: errorMsg }, null, 2),
        contentType: 'application/json',
        errorCategory: 'NETWORK_ERROR',
        errorMessage: errorMsg,
      });
      setExecutionState('NETWORK_ERROR');
      addToast('error', errorMsg, 'Execution Failed');
    } finally {
      if (abortControllerRef.current === controller) {
        abortControllerRef.current = null;
      }
    }
  };

  const handleCancel = () => {
    if (abortControllerRef.current) {
      abortControllerRef.current.abort();
      abortControllerRef.current = null;
    }
    setExecutionState('IDLE');
    addToast('info', 'Request execution cancelled');
  };

  // Reset Builder
  const handleReset = () => {
    setMethod('GET');
    setUrl('http://localhost:8080/api/v1/echo/get?demo=true');
    setQueryParams([{ key: 'demo', value: 'true', enabled: true }]);
    setHeaders([{ key: 'Accept', value: 'application/json', enabled: true }]);
    setAuth({ type: 'NONE', keyName: 'X-API-Key', keyValue: '', placement: 'HEADER' });
    setBody('{\n  "message": "Hello from API Testing Playground",\n  "status": "ready"\n}');
    setExecutionState('IDLE');
    setResponse(null);
    addToast('info', 'Request configuration reset to defaults');
  };

  // Copy as cURL
  const handleCopyCurl = async () => {
    try {
      const curlCmd = generateCurlCommand({
        method,
        url,
        queryParams,
        headers,
        auth,
        body,
      });
      await navigator.clipboard.writeText(curlCmd);
      addToast('success', 'cURL command copied to clipboard');
    } catch {
      addToast('error', 'Failed to copy to clipboard');
    }
  };

  // Restore Request from History
  const handleSelectHistoryRequest = (item: RequestHistoryItem) => {
    setMethod(item.method as HttpMethod);
    setUrl(item.url);

    // Parse headers if present
    if (item.requestHeaders) {
      try {
        const parsed = JSON.parse(item.requestHeaders);
        if (Array.isArray(parsed)) setHeaders(parsed);
      } catch {
        // ignore
      }
    }

    if (item.requestBody) {
      setBody(item.requestBody);
    }

    // Set response to history item's result
    if (item.responseStatus) {
      let respHeaders: Record<string, string> = {};
      try {
        if (item.responseHeaders) respHeaders = JSON.parse(item.responseHeaders);
      } catch {
        // ignore
      }

      setResponse({
        statusCode: item.responseStatus,
        statusText: item.responseStatusText || 'Response',
        durationMs: item.durationMs || 0,
        sizeBytes: item.responseSize || 0,
        headers: respHeaders,
        body: item.responseBody || '',
        contentType: respHeaders['Content-Type'] || respHeaders['content-type'] || 'text/plain',
        errorCategory: item.errorCategory || (item.responseStatus < 400 ? 'SUCCESS' : 'HTTP_ERROR'),
        historyId: item.id,
      });
      setExecutionState(item.responseStatus < 400 ? 'SUCCESS' : 'HTTP_ERROR');
    }

    addToast('info', `Restored previous ${item.method} request from history`);
  };

  // Restore Request from Saved Collection
  const handleSelectSavedRequest = (req: SavedRequest) => {
    setMethod(req.method);
    setUrl(req.url);
    if (req.queryParams) setQueryParams(req.queryParams);
    if (req.headers) setHeaders(req.headers);
    if (req.authConfig) {
      setAuth(req.authConfig);
    } else if (req.authType === 'API_KEY') {
      setAuth({ type: 'API_KEY', keyName: 'X-API-Key', keyValue: '', placement: 'HEADER' });
    } else {
      setAuth({ type: 'NONE', keyName: 'X-API-Key', keyValue: '', placement: 'HEADER' });
    }
    if (req.body) setBody(req.body);

    addToast('info', `Loaded "${req.name}" into workspace`);
  };

  // Save Current Request
  const handleSaveRequest = async (name: string, collectionId?: number) => {
    try {
      await playgroundApi.saveRequest({
        name,
        collectionId,
        method,
        url,
        queryParams,
        headers,
        auth,
        body,
      });

      const updatedCols = await playgroundApi.getCollections();
      setCollections(updatedCols);
      addToast('success', `Request "${name}" saved successfully`);
    } catch (err: any) {
      addToast('error', err.message || 'Failed to save request');
    }
  };

  // Collection Handlers
  const handleCreateCollection = async (name: string, description?: string) => {
    try {
      await playgroundApi.createCollection(name, description);
      const updated = await playgroundApi.getCollections();
      setCollections(updated);
      addToast('success', `Collection "${name}" created`);
    } catch (err: any) {
      addToast('error', err.message || 'Failed to create collection');
    }
  };

  const handleDeleteCollection = async (id: number) => {
    try {
      await playgroundApi.deleteCollection(id);
      const updated = await playgroundApi.getCollections();
      setCollections(updated);
      addToast('info', 'Collection deleted');
    } catch (err: any) {
      addToast('error', err.message || 'Failed to delete collection');
    }
  };

  const handleDuplicateSavedRequest = async (id: number) => {
    try {
      await playgroundApi.duplicateSavedRequest(id);
      const updated = await playgroundApi.getCollections();
      setCollections(updated);
      addToast('success', 'Request duplicated');
    } catch (err: any) {
      addToast('error', err.message || 'Failed to duplicate request');
    }
  };

  const handleDeleteSavedRequest = async (id: number) => {
    try {
      await playgroundApi.deleteSavedRequest(id);
      const updated = await playgroundApi.getCollections();
      setCollections(updated);
      addToast('info', 'Saved request deleted');
    } catch (err: any) {
      addToast('error', err.message || 'Failed to delete request');
    }
  };

  // History Handlers
  const handleDeleteHistoryItem = async (id: number) => {
    try {
      await playgroundApi.deleteHistoryItem(id);
      setHistory((prev) => prev.filter((item) => item.id !== id));
      addToast('info', 'History entry removed');
    } catch (err: any) {
      addToast('error', err.message || 'Failed to delete history item');
    }
  };

  const handleClearHistory = async () => {
    try {
      await playgroundApi.clearHistory();
      setHistory([]);
      addToast('info', 'All request history cleared');
    } catch (err: any) {
      addToast('error', err.message || 'Failed to clear history');
    }
  };

  // Environment Handlers
  const handleSelectEnvironment = async (envId: number | null) => {
    if (!envId) {
      setActiveEnvironment(null);
      addToast('info', 'Switched to No Environment');
      return;
    }

    try {
      const activated = await playgroundApi.activateEnvironment(envId);
      setActiveEnvironment(activated);
      const updatedEnvs = await playgroundApi.getEnvironments();
      setEnvironments(updatedEnvs);
      addToast('success', `Activated environment "${activated.name}"`);
    } catch (err: any) {
      addToast('error', err.message || 'Failed to switch environment');
    }
  };

  const handleCreateEnvironment = async (name: string, vars: EnvironmentVariable[]) => {
    try {
      await playgroundApi.createEnvironment({ name, variables: vars });
      const updated = await playgroundApi.getEnvironments();
      setEnvironments(updated);
      addToast('success', `Environment "${name}" created`);
    } catch (err: any) {
      addToast('error', err.message || 'Failed to create environment');
    }
  };

  const handleUpdateEnvironment = async (
    id: number,
    name: string,
    isActive: boolean,
    vars: EnvironmentVariable[]
  ) => {
    try {
      const updatedEnv = await playgroundApi.updateEnvironment(id, {
        name,
        isActive,
        variables: vars,
      });
      const updatedList = await playgroundApi.getEnvironments();
      setEnvironments(updatedList);
      if (isActive || activeEnvironment?.id === id) {
        setActiveEnvironment(updatedEnv);
      }
      addToast('success', `Environment "${name}" saved`);
    } catch (err: any) {
      addToast('error', err.message || 'Failed to update environment');
    }
  };

  const handleDeleteEnvironment = async (id: number) => {
    try {
      await playgroundApi.deleteEnvironment(id);
      const updated = await playgroundApi.getEnvironments();
      setEnvironments(updated);
      if (activeEnvironment?.id === id) {
        setActiveEnvironment(null);
      }
      addToast('info', 'Environment deleted');
    } catch (err: any) {
      addToast('error', err.message || 'Failed to delete environment');
    }
  };

  return (
    <AppShell
      environments={environments}
      activeEnvironment={activeEnvironment}
      onSelectEnvironment={handleSelectEnvironment}
      onOpenEnvironmentManager={() => setIsEnvManagerOpen(true)}
      backendHealthy={backendHealthy}
      sidebarTab={sidebarTab}
      onSidebarTabChange={setSidebarTab}
      sidebarCollapsed={sidebarCollapsed}
      onToggleSidebarCollapse={() => setSidebarCollapsed(!sidebarCollapsed)}
      collections={collections}
      history={history}
      onSelectSavedRequest={handleSelectSavedRequest}
      onSelectHistoryRequest={handleSelectHistoryRequest}
      onCreateCollection={handleCreateCollection}
      onDeleteCollection={handleDeleteCollection}
      onDuplicateSavedRequest={handleDuplicateSavedRequest}
      onDeleteSavedRequest={handleDeleteSavedRequest}
      onDeleteHistoryItem={handleDeleteHistoryItem}
      onClearHistory={handleClearHistory}
    >
      {/* Top Half: Request Builder */}
      <div className="shrink-0">
        <RequestBuilder
          method={method}
          setMethod={setMethod}
          url={url}
          setUrl={setUrl}
          queryParams={queryParams}
          setQueryParams={setQueryParams}
          headers={headers}
          setHeaders={setHeaders}
          auth={auth}
          setAuth={setAuth}
          body={body}
          setBody={setBody}
          executionState={executionState}
          onSend={handleSend}
          onCancel={handleCancel}
          onSave={() => setIsSaveModalOpen(true)}
          onCopyCurl={handleCopyCurl}
          onReset={handleReset}
        />
      </div>

      {/* Bottom Half: Response Viewer */}
      <div className="flex-1 overflow-hidden">
        <ResponseViewer
          state={executionState}
          response={response}
          onRetry={handleSend}
        />
      </div>

      {/* Save Request Modal */}
      <SaveRequestModal
        isOpen={isSaveModalOpen}
        onClose={() => setIsSaveModalOpen(false)}
        onSave={handleSaveRequest}
        collections={collections}
        defaultName={`${method} ${url.split('?')[0].split('/').pop() || 'Request'}`}
      />

      {/* Environment Management Modal */}
      <EnvironmentManager
        isOpen={isEnvManagerOpen}
        onClose={() => setIsEnvManagerOpen(false)}
        environments={environments}
        activeEnvironment={activeEnvironment}
        onCreateEnvironment={handleCreateEnvironment}
        onUpdateEnvironment={handleUpdateEnvironment}
        onActivateEnvironment={handleSelectEnvironment}
        onDeleteEnvironment={handleDeleteEnvironment}
      />

      {/* Toast Notifications */}
      <ToastNotifications toasts={toasts} onDismiss={removeToast} />
    </AppShell>
  );
};

export default App;
