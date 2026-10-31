import React, { useState } from 'react';
import { ExecutionResponse, ExecutionState } from '../../types';
import { ResponseStatus } from './ResponseStatus';
import { JsonViewer } from './JsonViewer';
import { HeadersViewer } from './HeadersViewer';
import { LoadingState } from '../common/LoadingState';
import { ErrorState } from '../common/ErrorState';
import { EmptyState } from '../common/EmptyState';

interface ResponseViewerProps {
  state: ExecutionState;
  response: ExecutionResponse | null;
  onRetry?: () => void;
}

type ResponseTab = 'pretty' | 'raw' | 'headers';

export const ResponseViewer: React.FC<ResponseViewerProps> = ({ state, response, onRetry }) => {
  const [activeTab, setActiveTab] = useState<ResponseTab>('pretty');

  if (state === 'LOADING' || state === 'VALIDATING') {
    return <LoadingState />;
  }

  if (
    state === 'NETWORK_ERROR' ||
    state === 'TIMEOUT' ||
    (response && (response.errorCategory === 'SECURITY_VIOLATION' || response.errorCategory === 'TIMEOUT' || response.errorCategory === 'NETWORK_ERROR'))
  ) {
    const category = response?.errorCategory || state;
    const msg = response?.errorMessage || response?.body || 'Network or configuration failure.';
    return <ErrorState category={category} message={msg} onRetry={onRetry} />;
  }

  if (!response || state === 'IDLE') {
    return <EmptyState />;
  }

  const headersCount = response.headers ? Object.keys(response.headers).length : 0;

  return (
    <div className="flex flex-col h-full bg-dark-950 overflow-hidden">
      {/* Response Status Bar */}
      <ResponseStatus
        statusCode={response.statusCode}
        statusText={response.statusText}
        durationMs={response.durationMs}
        sizeBytes={response.sizeBytes}
        responseBody={response.body}
      />

      {/* Response View Sub-tabs */}
      <div className="flex items-center justify-between px-3 border-b border-dark-800 bg-dark-900/60">
        <div className="flex items-center gap-1">
          <button
            type="button"
            id="response-tab-pretty"
            onClick={() => setActiveTab('pretty')}
            className={`px-3 py-2 text-xs font-semibold border-b-2 transition-all ${
              activeTab === 'pretty'
                ? 'border-brand-500 text-brand-400 bg-brand-500/5'
                : 'border-transparent text-slate-400 hover:text-slate-200'
            }`}
          >
            Pretty JSON
          </button>

          <button
            type="button"
            id="response-tab-raw"
            onClick={() => setActiveTab('raw')}
            className={`px-3 py-2 text-xs font-semibold border-b-2 transition-all ${
              activeTab === 'raw'
                ? 'border-brand-500 text-brand-400 bg-brand-500/5'
                : 'border-transparent text-slate-400 hover:text-slate-200'
            }`}
          >
            Raw Response
          </button>

          <button
            type="button"
            id="response-tab-headers"
            onClick={() => setActiveTab('headers')}
            className={`px-3 py-2 text-xs font-semibold border-b-2 transition-all flex items-center gap-1.5 ${
              activeTab === 'headers'
                ? 'border-brand-500 text-brand-400 bg-brand-500/5'
                : 'border-transparent text-slate-400 hover:text-slate-200'
            }`}
          >
            <span>Headers</span>
            {headersCount > 0 && (
              <span className="w-4 h-4 rounded-full bg-dark-800 text-slate-400 text-[10px] flex items-center justify-center font-mono">
                {headersCount}
              </span>
            )}
          </button>
        </div>

        <div className="text-[11px] font-mono text-slate-500">
          Content-Type: {response.contentType || 'unknown'}
        </div>
      </div>

      {/* Tab Panels */}
      <div className="flex-1 overflow-auto">
        {activeTab === 'pretty' && <JsonViewer content={response.body} />}

        {activeTab === 'raw' && (
          <div className="p-4 bg-dark-950 font-mono text-xs text-slate-200 whitespace-pre-wrap break-all select-all leading-relaxed max-h-[500px] overflow-auto">
            {response.body || <span className="text-slate-500 italic">[Empty response body]</span>}
          </div>
        )}

        {activeTab === 'headers' && <HeadersViewer headers={response.headers || {}} />}
      </div>
    </div>
  );
};
