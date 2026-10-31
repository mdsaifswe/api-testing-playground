import React, { useState } from 'react';
import { MethodSelector } from './MethodSelector';
import { UrlInput } from './UrlInput';
import { SendButton } from './SendButton';
import { QueryParamsEditor } from './QueryParamsEditor';
import { HeadersEditor } from './HeadersEditor';
import { AuthConfig } from './AuthConfig';
import { BodyEditor } from './BodyEditor';
import {
  AuthConfig as AuthConfigType,
  ExecutionState,
  HttpMethod,
  KeyValue,
} from '../../types';
import { RotateCcw } from 'lucide-react';

interface RequestBuilderProps {
  method: HttpMethod;
  setMethod: (method: HttpMethod) => void;
  url: string;
  setUrl: (url: string) => void;
  queryParams: KeyValue[];
  setQueryParams: (params: KeyValue[]) => void;
  headers: KeyValue[];
  setHeaders: (headers: KeyValue[]) => void;
  auth: AuthConfigType;
  setAuth: (auth: AuthConfigType) => void;
  body: string;
  setBody: (body: string) => void;
  executionState: ExecutionState;
  onSend: () => void;
  onCancel?: () => void;
  onSave: () => void;
  onCopyCurl: () => void;
  onReset: () => void;
}

type TabType = 'params' | 'headers' | 'auth' | 'body';

export const RequestBuilder: React.FC<RequestBuilderProps> = ({
  method,
  setMethod,
  url,
  setUrl,
  queryParams,
  setQueryParams,
  headers,
  setHeaders,
  auth,
  setAuth,
  body,
  setBody,
  executionState,
  onSend,
  onCancel,
  onSave,
  onCopyCurl,
  onReset,
}) => {
  const [activeTab, setActiveTab] = useState<TabType>('params');

  const activeParamsCount = queryParams.filter((p) => p.enabled && p.key).length;
  const activeHeadersCount = headers.filter((h) => h.enabled && h.key).length;
  const hasAuth = auth.type === 'API_KEY';
  const hasBody = Boolean(body && body.trim());

  return (
    <div className="flex flex-col bg-dark-900/40 border-b border-dark-800 p-4 space-y-4">
      {/* Top Request Bar: Method Selector, URL Input, Send Button */}
      <div className="flex items-center gap-0 w-full shadow-lg rounded-md overflow-visible">
        <MethodSelector
          method={method}
          onChange={setMethod}
          disabled={executionState === 'LOADING'}
        />
        <UrlInput
          url={url}
          onChange={setUrl}
          onSubmit={onSend}
          disabled={executionState === 'LOADING'}
        />
        <SendButton
          state={executionState}
          onSend={onSend}
          onCancel={onCancel}
          onSave={onSave}
          onCopyCurl={onCopyCurl}
        />
      </div>

      {/* Tabs Header */}
      <div className="flex items-center justify-between border-b border-dark-800 pt-1">
        <div className="flex items-center gap-1">
          <button
            type="button"
            id="tab-params"
            onClick={() => setActiveTab('params')}
            className={`px-3.5 py-2 text-xs font-semibold border-b-2 transition-all flex items-center gap-1.5 ${
              activeTab === 'params'
                ? 'border-brand-500 text-brand-400 bg-brand-500/5'
                : 'border-transparent text-slate-400 hover:text-slate-200'
            }`}
          >
            <span>Params</span>
            {activeParamsCount > 0 && (
              <span className="w-4 h-4 rounded-full bg-brand-500/20 text-brand-300 text-[10px] flex items-center justify-center font-mono">
                {activeParamsCount}
              </span>
            )}
          </button>

          <button
            type="button"
            id="tab-headers"
            onClick={() => setActiveTab('headers')}
            className={`px-3.5 py-2 text-xs font-semibold border-b-2 transition-all flex items-center gap-1.5 ${
              activeTab === 'headers'
                ? 'border-brand-500 text-brand-400 bg-brand-500/5'
                : 'border-transparent text-slate-400 hover:text-slate-200'
            }`}
          >
            <span>Headers</span>
            {activeHeadersCount > 0 && (
              <span className="w-4 h-4 rounded-full bg-brand-500/20 text-brand-300 text-[10px] flex items-center justify-center font-mono">
                {activeHeadersCount}
              </span>
            )}
          </button>

          <button
            type="button"
            id="tab-auth"
            onClick={() => setActiveTab('auth')}
            className={`px-3.5 py-2 text-xs font-semibold border-b-2 transition-all flex items-center gap-1.5 ${
              activeTab === 'auth'
                ? 'border-brand-500 text-brand-400 bg-brand-500/5'
                : 'border-transparent text-slate-400 hover:text-slate-200'
            }`}
          >
            <span>Auth</span>
            {hasAuth && (
              <span className="w-2 h-2 rounded-full bg-emerald-400" title="API Key Enabled" />
            )}
          </button>

          <button
            type="button"
            id="tab-body"
            onClick={() => setActiveTab('body')}
            className={`px-3.5 py-2 text-xs font-semibold border-b-2 transition-all flex items-center gap-1.5 ${
              activeTab === 'body'
                ? 'border-brand-500 text-brand-400 bg-brand-500/5'
                : 'border-transparent text-slate-400 hover:text-slate-200'
            }`}
          >
            <span>Body</span>
            {hasBody && (
              <span className="w-2 h-2 rounded-full bg-blue-400" title="Body configured" />
            )}
          </button>
        </div>

        <button
          type="button"
          onClick={onReset}
          className="flex items-center gap-1 text-[11px] text-slate-400 hover:text-slate-200 px-2 py-1 rounded hover:bg-dark-800 transition-colors"
          title="Reset request parameters"
        >
          <RotateCcw className="w-3 h-3" />
          <span>Reset</span>
        </button>
      </div>

      {/* Tab Panels */}
      <div className="pt-1">
        {activeTab === 'params' && (
          <QueryParamsEditor queryParams={queryParams} onChange={setQueryParams} />
        )}
        {activeTab === 'headers' && (
          <HeadersEditor headers={headers} onChange={setHeaders} />
        )}
        {activeTab === 'auth' && (
          <AuthConfig auth={auth} onChange={setAuth} />
        )}
        {activeTab === 'body' && (
          <BodyEditor body={body} onChange={setBody} />
        )}
      </div>
    </div>
  );
};
