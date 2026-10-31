import React, { useState } from 'react';
import { History, Trash2, Search, Clock, ArrowRight } from 'lucide-react';
import { RequestHistoryItem } from '../../types';
import { formatDuration, formatTimestamp } from '../../utils/formatters';

interface HistoryPanelProps {
  history: RequestHistoryItem[];
  onSelectRequest: (item: RequestHistoryItem) => void;
  onDeleteItem: (id: number) => void;
  onClearAll: () => void;
  isLoading?: boolean;
}

export const HistoryPanel: React.FC<HistoryPanelProps> = ({
  history,
  onSelectRequest,
  onDeleteItem,
  onClearAll,
  isLoading,
}) => {
  const [filterQuery, setFilterQuery] = useState('');
  const [confirmClear, setConfirmClear] = useState(false);

  const filteredHistory = history.filter(
    (item) =>
      item.url.toLowerCase().includes(filterQuery.toLowerCase()) ||
      item.method.toLowerCase().includes(filterQuery.toLowerCase()) ||
      (item.responseStatus && item.responseStatus.toString().includes(filterQuery))
  );

  const getMethodBadge = (m: string) => {
    const map: Record<string, string> = {
      GET: 'text-emerald-400 bg-emerald-500/10 border-emerald-500/20',
      POST: 'text-blue-400 bg-blue-500/10 border-blue-500/20',
      PUT: 'text-amber-400 bg-amber-500/10 border-amber-500/20',
      PATCH: 'text-purple-400 bg-purple-500/10 border-purple-500/20',
      DELETE: 'text-rose-400 bg-rose-500/10 border-rose-500/20',
    };
    return map[m.toUpperCase()] || 'text-slate-400 bg-slate-500/10 border-slate-500/20';
  };

  const getStatusBadge = (code?: number) => {
    if (!code) return 'text-slate-500';
    if (code >= 200 && code < 300) return 'text-emerald-400';
    if (code >= 400 && code < 500) return 'text-amber-400';
    return 'text-rose-400';
  };

  return (
    <div className="flex flex-col h-full bg-dark-950 border-r border-dark-800 w-80 shrink-0">
      {/* Header */}
      <div className="p-3 border-b border-dark-800 flex items-center justify-between">
        <div className="flex items-center gap-2">
          <History className="w-4 h-4 text-brand-400" />
          <h2 className="text-xs font-bold uppercase tracking-wider text-slate-200">History</h2>
          <span className="text-[10px] px-1.5 py-0.5 rounded-full bg-dark-800 text-slate-400 font-mono">
            {history.length}
          </span>
        </div>

        {history.length > 0 && (
          <div>
            {confirmClear ? (
              <div className="flex items-center gap-1.5">
                <button
                  type="button"
                  onClick={() => {
                    onClearAll();
                    setConfirmClear(false);
                  }}
                  className="text-[10px] font-semibold text-rose-400 hover:text-rose-300 px-1.5 py-0.5 rounded bg-rose-950/60 border border-rose-800"
                >
                  Confirm Clear
                </button>
                <button
                  type="button"
                  onClick={() => setConfirmClear(false)}
                  className="text-[10px] text-slate-400 hover:text-slate-200"
                >
                  Cancel
                </button>
              </div>
            ) : (
              <button
                type="button"
                onClick={() => setConfirmClear(true)}
                className="text-slate-500 hover:text-rose-400 p-1 rounded transition-colors"
                title="Clear all history"
              >
                <Trash2 className="w-3.5 h-3.5" />
              </button>
            )}
          </div>
        )}
      </div>

      {/* Filter Input */}
      <div className="p-2 border-b border-dark-800">
        <div className="relative flex items-center">
          <Search className="w-3.5 h-3.5 text-slate-500 absolute left-2.5" />
          <input
            type="text"
            value={filterQuery}
            onChange={(e) => setFilterQuery(e.target.value)}
            placeholder="Search history..."
            className="w-full bg-dark-900 border border-dark-700 rounded pl-8 pr-3 py-1 text-xs text-slate-200 placeholder-slate-500 focus:outline-none focus:ring-1 focus:ring-brand-500"
          />
        </div>
      </div>

      {/* History List */}
      <div className="flex-1 overflow-auto divide-y divide-dark-800/40">
        {isLoading ? (
          <div className="p-8 text-center text-xs text-slate-500">Loading history...</div>
        ) : filteredHistory.length === 0 ? (
          <div className="p-8 text-center text-xs text-slate-500 flex flex-col items-center">
            <Clock className="w-8 h-8 text-slate-600 mb-2" />
            <span>No request history found.</span>
            <span className="text-[10px] text-slate-600 mt-1">
              Executed requests persist automatically in MySQL.
            </span>
          </div>
        ) : (
          filteredHistory.map((item) => (
            <div
              key={item.id}
              className="p-2.5 hover:bg-dark-900/80 transition-colors group relative cursor-pointer"
              onClick={() => onSelectRequest(item)}
            >
              <div className="flex items-center justify-between gap-1 mb-1">
                <div className="flex items-center gap-1.5">
                  <span
                    className={`font-mono font-bold text-[10px] px-1.5 py-0.5 rounded border uppercase ${getMethodBadge(
                      item.method
                    )}`}
                  >
                    {item.method}
                  </span>
                  <span className={`font-mono font-semibold text-xs ${getStatusBadge(item.responseStatus)}`}>
                    {item.responseStatus || 'ERR'}
                  </span>
                </div>

                <div className="flex items-center gap-2 text-[10px] text-slate-500 font-mono">
                  {item.durationMs !== undefined && (
                    <span>{formatDuration(item.durationMs)}</span>
                  )}
                  <span>{formatTimestamp(item.executedAt)}</span>
                  <button
                    type="button"
                    onClick={(e) => {
                      e.stopPropagation();
                      onDeleteItem(item.id);
                    }}
                    className="opacity-0 group-hover:opacity-100 text-slate-500 hover:text-rose-400 p-0.5 rounded transition-all"
                    title="Delete item"
                  >
                    <Trash2 className="w-3 h-3" />
                  </button>
                </div>
              </div>

              <div className="text-xs text-slate-300 font-mono truncate" title={item.url}>
                {item.url}
              </div>

              <div className="opacity-0 group-hover:opacity-100 flex items-center gap-1 text-[10px] text-brand-400 mt-1 transition-opacity">
                <span>Restore to builder</span>
                <ArrowRight className="w-2.5 h-2.5" />
              </div>
            </div>
          ))
        )}
      </div>
    </div>
  );
};
