import React from 'react';
import { HttpMethod } from '../../types';

interface MethodSelectorProps {
  method: HttpMethod;
  onChange: (method: HttpMethod) => void;
  disabled?: boolean;
}

const METHOD_COLORS: Record<HttpMethod, { bg: string; text: string; border: string }> = {
  GET: { bg: 'bg-emerald-500/10', text: 'text-emerald-400', border: 'border-emerald-500/30' },
  POST: { bg: 'bg-blue-500/10', text: 'text-blue-400', border: 'border-blue-500/30' },
  PUT: { bg: 'bg-amber-500/10', text: 'text-amber-400', border: 'border-amber-500/30' },
  PATCH: { bg: 'bg-purple-500/10', text: 'text-purple-400', border: 'border-purple-500/30' },
  DELETE: { bg: 'bg-rose-500/10', text: 'text-rose-400', border: 'border-rose-500/30' },
  HEAD: { bg: 'bg-cyan-500/10', text: 'text-cyan-400', border: 'border-cyan-500/30' },
  OPTIONS: { bg: 'bg-slate-500/10', text: 'text-slate-400', border: 'border-slate-500/30' },
};

export const MethodSelector: React.FC<MethodSelectorProps> = ({ method, onChange, disabled }) => {
  const currentStyle = METHOD_COLORS[method] || METHOD_COLORS.GET;

  return (
    <div className="relative inline-block">
      <select
        id="http-method-select"
        value={method}
        onChange={(e) => onChange(e.target.value as HttpMethod)}
        disabled={disabled}
        className={`appearance-none font-mono font-bold text-xs uppercase px-3 py-2 pr-7 rounded-l-md border focus:outline-none focus:ring-1 focus:ring-brand-500 transition-colors cursor-pointer ${currentStyle.bg} ${currentStyle.text} ${currentStyle.border}`}
      >
        <option value="GET" className="bg-dark-900 text-emerald-400 font-bold">GET</option>
        <option value="POST" className="bg-dark-900 text-blue-400 font-bold">POST</option>
        <option value="PUT" className="bg-dark-900 text-amber-400 font-bold">PUT</option>
        <option value="PATCH" className="bg-dark-900 text-purple-400 font-bold">PATCH</option>
        <option value="DELETE" className="bg-dark-900 text-rose-400 font-bold">DELETE</option>
        <option value="HEAD" className="bg-dark-900 text-cyan-400 font-bold">HEAD</option>
        <option value="OPTIONS" className="bg-dark-900 text-slate-400 font-bold">OPTIONS</option>
      </select>
      <div className="pointer-events-none absolute inset-y-0 right-0 flex items-center px-2 text-slate-400">
        <svg className="w-3.5 h-3.5 fill-current" viewBox="0 0 20 20">
          <path d="M5.293 7.293a1 1 0 011.414 0L10 10.586l3.293-3.293a1 1 0 111.414 1.414l-4 4a1 1 0 01-1.414 0l-4-4a1 1 0 010-1.414z" />
        </svg>
      </div>
    </div>
  );
};
