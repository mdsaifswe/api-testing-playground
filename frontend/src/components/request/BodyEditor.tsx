import React, { useMemo } from 'react';
import { AlignLeft, CheckCircle2, AlertCircle, Eraser } from 'lucide-react';
import { formatJson, isValidJson } from '../../utils/formatters';

interface BodyEditorProps {
  body: string;
  onChange: (body: string) => void;
  disabled?: boolean;
}

export const BodyEditor: React.FC<BodyEditorProps> = ({ body, onChange, disabled }) => {
  const isJson = useMemo(() => isValidJson(body), [body]);

  const handleFormat = () => {
    if (!body || !body.trim()) return;
    const formatted = formatJson(body);
    onChange(formatted);
  };

  const handleClear = () => {
    onChange('');
  };

  const lineCount = (body.match(/\n/g) || []).length + 1;

  return (
    <div className="space-y-2">
      <div className="flex items-center justify-between text-xs text-slate-400">
        <div className="flex items-center gap-3">
          <span className="font-medium text-slate-300">Raw JSON Payload</span>
          {body.trim() && (
            <span
              className={`flex items-center gap-1 text-[11px] px-2 py-0.5 rounded-full border ${
                isJson
                  ? 'bg-emerald-500/10 text-emerald-400 border-emerald-500/30'
                  : 'bg-rose-500/10 text-rose-400 border-rose-500/30'
              }`}
            >
              {isJson ? (
                <>
                  <CheckCircle2 className="w-3 h-3" /> Valid JSON
                </>
              ) : (
                <>
                  <AlertCircle className="w-3 h-3" /> Invalid JSON Syntax
                </>
              )}
            </span>
          )}
        </div>

        <div className="flex items-center gap-2">
          {body && (
            <button
              type="button"
              onClick={handleClear}
              disabled={disabled}
              className="flex items-center gap-1 px-2 py-1 rounded text-slate-400 hover:text-slate-200 hover:bg-dark-800 transition-colors"
              title="Clear request body"
            >
              <Eraser className="w-3.5 h-3.5" />
              <span>Clear</span>
            </button>
          )}
          <button
            type="button"
            onClick={handleFormat}
            disabled={disabled || !body.trim() || !isJson}
            className="flex items-center gap-1 px-2.5 py-1 rounded bg-dark-800 hover:bg-dark-700 text-brand-300 hover:text-brand-200 border border-dark-700 transition-colors disabled:opacity-40 disabled:pointer-events-none"
            title="Prettify JSON indentation"
          >
            <AlignLeft className="w-3.5 h-3.5" />
            <span>Format JSON</span>
          </button>
        </div>
      </div>

      <div className="relative border border-dark-800 rounded-lg overflow-hidden bg-dark-900 focus-within:border-brand-500 transition-colors">
        <textarea
          id="request-body-input"
          value={body}
          onChange={(e) => onChange(e.target.value)}
          placeholder={`{\n  "message": "Hello World",\n  "userId": "{{userId}}"\n}`}
          disabled={disabled}
          rows={10}
          className="w-full bg-dark-900 text-slate-200 font-mono text-xs p-3 leading-relaxed focus:outline-none resize-y selection:bg-brand-500 selection:text-white"
          spellCheck={false}
        />
        <div className="bg-dark-950 px-3 py-1 border-t border-dark-800 flex items-center justify-between text-[10px] text-slate-500 font-mono">
          <span>JSON (application/json)</span>
          <span>
            {lineCount} {lineCount === 1 ? 'line' : 'lines'} • {body.length} chars
          </span>
        </div>
      </div>
    </div>
  );
};
