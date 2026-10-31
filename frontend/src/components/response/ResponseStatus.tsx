import React, { useState } from 'react';
import { Copy, Check, Download, Clock, Database } from 'lucide-react';
import { formatBytes, formatDuration } from '../../utils/formatters';

interface ResponseStatusProps {
  statusCode: number;
  statusText: string;
  durationMs: number;
  sizeBytes: number;
  responseBody: string;
}

export const ResponseStatus: React.FC<ResponseStatusProps> = ({
  statusCode,
  statusText,
  durationMs,
  sizeBytes,
  responseBody,
}) => {
  const [copied, setCopied] = useState(false);

  const getStatusBadge = (code: number) => {
    if (code >= 200 && code < 300) {
      return 'bg-emerald-500/15 text-emerald-400 border-emerald-500/30';
    }
    if (code >= 300 && code < 400) {
      return 'bg-sky-500/15 text-sky-400 border-sky-500/30';
    }
    if (code >= 400 && code < 500) {
      return 'bg-amber-500/15 text-amber-400 border-amber-500/30';
    }
    return 'bg-rose-500/15 text-rose-400 border-rose-500/30';
  };

  const handleCopy = async () => {
    try {
      await navigator.clipboard.writeText(responseBody);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    } catch {
      // fallback
    }
  };

  const handleDownload = () => {
    const blob = new Blob([responseBody], { type: 'application/json' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `response-${statusCode}-${Date.now()}.json`;
    a.click();
    URL.revokeObjectURL(url);
  };

  return (
    <div className="flex flex-wrap items-center justify-between gap-3 p-3 bg-dark-900/80 border-b border-dark-800 text-xs">
      <div className="flex items-center gap-3">
        {/* Status Pill */}
        <div
          id="response-status-badge"
          className={`flex items-center gap-1.5 px-2.5 py-1 rounded-md font-mono font-bold text-xs border ${getStatusBadge(
            statusCode
          )}`}
        >
          <span>{statusCode}</span>
          <span>{statusText}</span>
        </div>

        {/* Duration */}
        <div className="flex items-center gap-1 text-slate-400 font-mono text-[11px]" title="Response duration">
          <Clock className="w-3.5 h-3.5 text-slate-500" />
          <span>{formatDuration(durationMs)}</span>
        </div>

        {/* Size */}
        <div className="flex items-center gap-1 text-slate-400 font-mono text-[11px]" title="Response body size">
          <Database className="w-3.5 h-3.5 text-slate-500" />
          <span>{formatBytes(sizeBytes)}</span>
        </div>
      </div>

      <div className="flex items-center gap-2">
        <button
          type="button"
          onClick={handleCopy}
          className="flex items-center gap-1 px-2.5 py-1 rounded bg-dark-800 hover:bg-dark-700 text-slate-300 hover:text-white border border-dark-700 text-[11px] transition-colors"
          title="Copy response body to clipboard"
        >
          {copied ? (
            <>
              <Check className="w-3.5 h-3.5 text-emerald-400" />
              <span className="text-emerald-400">Copied!</span>
            </>
          ) : (
            <>
              <Copy className="w-3.5 h-3.5" />
              <span>Copy</span>
            </>
          )}
        </button>

        <button
          type="button"
          onClick={handleDownload}
          className="flex items-center gap-1 px-2.5 py-1 rounded bg-dark-800 hover:bg-dark-700 text-slate-300 hover:text-white border border-dark-700 text-[11px] transition-colors"
          title="Download response JSON"
        >
          <Download className="w-3.5 h-3.5" />
          <span>Save</span>
        </button>
      </div>
    </div>
  );
};
