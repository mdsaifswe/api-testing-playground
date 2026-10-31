import React, { useState, useMemo } from 'react';
import { Search, X } from 'lucide-react';
import { formatJson } from '../../utils/formatters';

interface JsonViewerProps {
  content: string;
}

export const JsonViewer: React.FC<JsonViewerProps> = ({ content }) => {
  const [searchQuery, setSearchQuery] = useState('');

  const prettyJson = useMemo(() => {
    return formatJson(content);
  }, [content]);

  const lines = useMemo(() => {
    return prettyJson.split('\n');
  }, [prettyJson]);

  // Syntax highlight simple JSON tokens
  const renderHighlightedLine = (line: string) => {
    if (!line) return <span>&nbsp;</span>;

    // Check if line matches search query
    const hasSearchMatch = searchQuery && line.toLowerCase().includes(searchQuery.toLowerCase());

    // Basic regex highlights: keys, strings, numbers, booleans, null
    const highlighted = line.replace(
      /("(\\u[a-zA-Z0-9]{4}|\\[^u]|[^\\"])*"(\s*:)?|\b(true|false|null)\b|-?\d+(?:\.\d*)?(?:[eE][+-]?\d+)?)/g,
      (match) => {
        let cls = 'text-amber-300'; // number
        if (/^"/.test(match)) {
          if (/:$/.test(match)) {
            cls = 'text-sky-300 font-semibold'; // key
          } else {
            cls = 'text-emerald-300'; // string
          }
        } else if (/true|false/.test(match)) {
          cls = 'text-purple-400 font-bold'; // boolean
        } else if (/null/.test(match)) {
          cls = 'text-slate-400 italic'; // null
        }
        return `<span class="${cls}">${match}</span>`;
      }
    );

    return (
      <span
        className={hasSearchMatch ? 'bg-brand-500/30 rounded px-1' : ''}
        dangerouslySetInnerHTML={{ __html: highlighted }}
      />
    );
  };

  return (
    <div className="flex flex-col h-full bg-dark-950">
      {/* Search Bar */}
      <div className="flex items-center justify-between px-3 py-2 border-b border-dark-800/80 bg-dark-900/40 text-xs">
        <div className="relative flex items-center max-w-xs w-full">
          <Search className="w-3.5 h-3.5 text-slate-500 absolute left-2.5" />
          <input
            type="text"
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            placeholder="Search in response JSON..."
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
          {lines.length} {lines.length === 1 ? 'line' : 'lines'}
        </div>
      </div>

      {/* JSON Content Area */}
      <div className="overflow-auto p-3 font-mono text-xs leading-relaxed max-h-[500px]">
        <table className="w-full border-collapse">
          <tbody>
            {lines.map((line, idx) => (
              <tr key={idx} className="hover:bg-dark-900/50">
                <td className="w-10 text-right pr-4 text-slate-600 select-none text-[10px] align-top">
                  {idx + 1}
                </td>
                <td className="text-slate-200 whitespace-pre font-mono">
                  {renderHighlightedLine(line)}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
};
