import React from 'react';
import { extractVariableNames } from '../../utils/environment';

interface UrlInputProps {
  url: string;
  onChange: (url: string) => void;
  onSubmit: () => void;
  disabled?: boolean;
}

export const UrlInput: React.FC<UrlInputProps> = ({ url, onChange, onSubmit, disabled }) => {
  const detectedVars = extractVariableNames(url);

  const handleKeyDown = (e: React.KeyboardEvent<HTMLInputElement>) => {
    if (e.key === 'Enter') {
      e.preventDefault();
      onSubmit();
    }
  };

  return (
    <div className="flex-1 relative flex items-center">
      <input
        id="request-url-input"
        type="text"
        value={url}
        onChange={(e) => onChange(e.target.value)}
        onKeyDown={handleKeyDown}
        placeholder="Enter request URL (e.g. {{baseUrl}}/get, http://localhost:8080/api/v1/echo/get)"
        disabled={disabled}
        className="w-full bg-dark-900 border-y border-r border-dark-700 text-slate-100 placeholder-slate-500 font-mono text-xs px-3.5 py-2 focus:outline-none focus:ring-1 focus:ring-brand-500 focus:border-brand-500 transition-all"
      />
      {detectedVars.length > 0 && (
        <div className="absolute right-2.5 flex items-center gap-1 pointer-events-none">
          {detectedVars.slice(0, 2).map((v) => (
            <span
              key={v}
              className="text-[10px] font-mono bg-brand-500/20 text-brand-300 border border-brand-500/30 px-1.5 py-0.5 rounded"
            >
              &#123;&#123;{v}&#125;&#125;
            </span>
          ))}
          {detectedVars.length > 2 && (
            <span className="text-[10px] font-mono text-slate-400">+{detectedVars.length - 2}</span>
          )}
        </div>
      )}
    </div>
  );
};
