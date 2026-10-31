import React, { useState } from 'react';
import { KeyRound, Eye, EyeOff, ShieldCheck } from 'lucide-react';
import { AuthConfig as AuthConfigType } from '../../types';

interface AuthConfigProps {
  auth: AuthConfigType;
  onChange: (auth: AuthConfigType) => void;
  disabled?: boolean;
}

export const AuthConfig: React.FC<AuthConfigProps> = ({ auth, onChange, disabled }) => {
  const [showSecret, setShowSecret] = useState(false);

  const handleTypeChange = (type: 'NONE' | 'API_KEY') => {
    onChange({
      ...auth,
      type,
      keyName: type === 'API_KEY' && !auth.keyName ? 'X-API-Key' : auth.keyName,
    });
  };

  const handleQuickPreset = (keyName: string, placement: 'HEADER' | 'QUERY') => {
    onChange({
      ...auth,
      type: 'API_KEY',
      keyName,
      placement,
    });
  };

  return (
    <div className="space-y-4 max-w-2xl">
      <div className="flex items-center gap-4">
        <label className="text-xs font-semibold text-slate-300">Authentication Type:</label>
        <div className="flex rounded-md bg-dark-900 border border-dark-800 p-0.5">
          <button
            type="button"
            onClick={() => handleTypeChange('NONE')}
            disabled={disabled}
            className={`px-3 py-1 text-xs font-medium rounded transition-all ${
              auth.type === 'NONE'
                ? 'bg-brand-600 text-white shadow-sm'
                : 'text-slate-400 hover:text-slate-200'
            }`}
          >
            No Auth
          </button>
          <button
            type="button"
            onClick={() => handleTypeChange('API_KEY')}
            disabled={disabled}
            className={`px-3 py-1 text-xs font-medium rounded transition-all flex items-center gap-1.5 ${
              auth.type === 'API_KEY'
                ? 'bg-brand-600 text-white shadow-sm'
                : 'text-slate-400 hover:text-slate-200'
            }`}
          >
            <KeyRound className="w-3.5 h-3.5" />
            <span>API Key</span>
          </button>
        </div>
      </div>

      {auth.type === 'API_KEY' ? (
        <div className="p-4 rounded-xl border border-dark-800 bg-dark-900/60 space-y-4">
          <div className="flex items-center justify-between pb-2 border-b border-dark-800">
            <span className="text-xs text-slate-400 font-medium flex items-center gap-1.5">
              <ShieldCheck className="w-4 h-4 text-emerald-400" />
              API Key Authentication Configuration
            </span>
            <div className="flex items-center gap-1.5 text-[11px] text-slate-400">
              <span>Quick Presets:</span>
              <button
                type="button"
                onClick={() => handleQuickPreset('X-API-Key', 'HEADER')}
                className="px-1.5 py-0.5 bg-dark-800 hover:bg-dark-700 text-slate-300 rounded border border-dark-700 text-[10px]"
              >
                X-API-Key (Header)
              </button>
              <button
                type="button"
                onClick={() => handleQuickPreset('apiKey', 'QUERY')}
                className="px-1.5 py-0.5 bg-dark-800 hover:bg-dark-700 text-slate-300 rounded border border-dark-700 text-[10px]"
              >
                apiKey (Query)
              </button>
            </div>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div>
              <label htmlFor="auth-key-name" className="block text-xs font-medium text-slate-300 mb-1">
                Key Name
              </label>
              <input
                id="auth-key-name"
                type="text"
                value={auth.keyName}
                onChange={(e) => onChange({ ...auth, keyName: e.target.value })}
                placeholder="e.g. X-API-Key, Authorization, apiKey"
                disabled={disabled}
                className="w-full bg-dark-950 border border-dark-700 rounded-md px-3 py-2 text-xs font-mono text-slate-200 focus:outline-none focus:ring-1 focus:ring-brand-500"
              />
            </div>

            <div>
              <label htmlFor="auth-placement-select" className="block text-xs font-medium text-slate-300 mb-1">
                Placement
              </label>
              <select
                id="auth-placement-select"
                value={auth.placement}
                onChange={(e) => onChange({ ...auth, placement: e.target.value as 'HEADER' | 'QUERY' })}
                disabled={disabled}
                className="w-full bg-dark-950 border border-dark-700 rounded-md px-3 py-2 text-xs text-slate-200 focus:outline-none focus:ring-1 focus:ring-brand-500 cursor-pointer"
              >
                <option value="HEADER">Header (e.g. X-API-Key: value)</option>
                <option value="QUERY">Query Parameter (e.g. ?apiKey=value)</option>
              </select>
            </div>
          </div>

          <div>
            <label htmlFor="auth-key-value" className="block text-xs font-medium text-slate-300 mb-1">
              Key Value / Token
              <span className="text-[10px] text-slate-500 ml-2 font-normal">
                (supports environment variables like <code className="text-brand-400 font-mono">&#123;&#123;apiKey&#125;&#125;</code>)
              </span>
            </label>
            <div className="relative">
              <input
                id="auth-key-value"
                type={showSecret ? 'text' : 'password'}
                value={auth.keyValue}
                onChange={(e) => onChange({ ...auth, keyValue: e.target.value })}
                placeholder="Enter secret key or {{apiKey}}"
                disabled={disabled}
                className="w-full bg-dark-950 border border-dark-700 rounded-md pl-3 pr-10 py-2 text-xs font-mono text-slate-200 focus:outline-none focus:ring-1 focus:ring-brand-500"
              />
              <button
                type="button"
                onClick={() => setShowSecret(!showSecret)}
                className="absolute right-2.5 top-1/2 -translate-y-1/2 text-slate-500 hover:text-slate-300 p-1"
                title={showSecret ? 'Mask secret' : 'Show secret'}
              >
                {showSecret ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
              </button>
            </div>
          </div>
        </div>
      ) : (
        <div className="p-6 rounded-xl border border-dark-800 bg-dark-900/30 text-center">
          <p className="text-xs text-slate-400">
            This request currently sends no authentication headers or query parameters. Select "API Key" to attach authenticated credentials.
          </p>
        </div>
      )}
    </div>
  );
};
