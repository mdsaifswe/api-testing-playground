import React, { useState } from 'react';
import { Search, X } from 'lucide-react';

interface HeadersViewerProps {
  headers: Record<string, string>;
}

export const HeadersViewer: React.FC<HeadersViewerProps> = ({ headers }) => {
  const [searchQuery, setSearchQuery] = useState('');

  const entries = Object.entries(headers || {});
  const filtered = entries.filter(
    ([k, v]) =>
      k.toLowerCase().includes(searchQuery.toLowerCase()) ||
      v.toLowerCase().includes(searchQuery.toLowerCase())
  );

  return (
    <div className="flex flex-col h-full bg-dark-950">
      <div className="flex items-center justify-between px-3 py-2 border-b border-dark-800/80 bg-dark-900/40 text-xs">
        <div className="relative flex items-center max-w-xs w-full">
          <Search className="w-3.5 h-3.5 text-slate-500 absolute left-2.5" />
          <input
            type="text"
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            placeholder="Filter headers..."
            className="w-full bg-dark-900 border border-dark-700 rounded-md pl-8 pr-7 py-1 text-xs text-slate-200 placeholder-slate-500 focus:outline-none focus:ring-1 focus:ring-brand-500 font-mono"
          />
          {searchQuery && (
            <button
              onClick={() => setSearchQuery('')}
              className="absolute right-2 text-slate-400 hover:text-white"
            >
              <X className="w-3.5 h-3.5" />
            </button>
          )}
        </div>
        <div className="text-[11px] text-slate-500 font-mono">
          {filtered.length} of {entries.length} headers
        </div>
      </div>

      <div className="overflow-auto p-3 max-h-[500px]">
        {filtered.length === 0 ? (
          <div className="text-center py-8 text-slate-500 text-xs">No matching headers found.</div>
        ) : (
          <table className="w-full text-left text-xs font-mono">
            <thead>
              <tr className="border-b border-dark-800 text-slate-400 font-sans">
                <th className="py-2 px-3 w-1/3">Header Name</th>
                <th className="py-2 px-3 w-2/3">Header Value</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-dark-800/40">
              {filtered.map(([key, val], idx) => (
                <tr key={idx} className="hover:bg-dark-900/50">
                  <td className="py-2 px-3 text-sky-400 font-semibold break-all">{key}</td>
                  <td className="py-2 px-3 text-slate-300 break-all select-all">{val}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </div>
  );
};
