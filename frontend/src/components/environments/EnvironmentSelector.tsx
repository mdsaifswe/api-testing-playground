import React, { useState, useRef, useEffect } from 'react';
import { Layers, Settings, ChevronDown, Check } from 'lucide-react';
import { Environment } from '../../types';

interface EnvironmentSelectorProps {
  environments: Environment[];
  activeEnvironment: Environment | null;
  onSelectEnvironment: (envId: number | null) => void;
  onOpenManager: () => void;
}

export const EnvironmentSelector: React.FC<EnvironmentSelectorProps> = ({
  environments,
  activeEnvironment,
  onSelectEnvironment,
  onOpenManager,
}) => {
  const [isOpen, setIsOpen] = useState(false);
  const dropdownRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const handleClickOutside = (e: MouseEvent) => {
      if (dropdownRef.current && !dropdownRef.current.contains(e.target as Node)) {
        setIsOpen(false);
      }
    };
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  return (
    <div className="relative" ref={dropdownRef}>
      <div className="flex items-center rounded-md bg-dark-900 border border-dark-700 p-0.5">
        <button
          type="button"
          id="active-environment-button"
          onClick={() => setIsOpen(!isOpen)}
          className="flex items-center gap-2 px-2.5 py-1 text-xs text-slate-200 hover:text-white rounded hover:bg-dark-800 transition-colors"
        >
          <Layers className="w-3.5 h-3.5 text-brand-400" />
          <span className="font-medium max-w-[140px] truncate">
            {activeEnvironment ? activeEnvironment.name : 'No Environment'}
          </span>
          {activeEnvironment && (
            <span className="w-2 h-2 rounded-full bg-emerald-400 shrink-0" title="Active Environment" />
          )}
          <ChevronDown className="w-3.5 h-3.5 text-slate-400 ml-1" />
        </button>

        <button
          type="button"
          id="manage-environments-button"
          onClick={onOpenManager}
          className="p-1.5 text-slate-400 hover:text-brand-300 hover:bg-dark-800 rounded transition-colors"
          title="Manage Environments"
        >
          <Settings className="w-3.5 h-3.5" />
        </button>
      </div>

      {isOpen && (
        <div className="absolute right-0 mt-1 w-56 rounded-lg bg-dark-900 border border-dark-700 shadow-xl py-1 z-50 text-xs">
          <div className="px-3 py-1.5 text-[10px] font-bold uppercase tracking-wider text-slate-400 border-b border-dark-800">
            Select Active Environment
          </div>

          <button
            type="button"
            onClick={() => {
              onSelectEnvironment(null);
              setIsOpen(false);
            }}
            className={`w-full flex items-center justify-between px-3 py-2 text-left hover:bg-dark-800 transition-colors ${
              !activeEnvironment ? 'text-brand-400 font-semibold bg-brand-500/10' : 'text-slate-300'
            }`}
          >
            <span>No Environment</span>
            {!activeEnvironment && <Check className="w-3.5 h-3.5 text-brand-400" />}
          </button>

          {environments.map((env) => {
            const isCurrent = activeEnvironment?.id === env.id;
            const varCount = env.variables?.filter((v) => v.isEnabled)?.length || 0;

            return (
              <button
                key={env.id}
                type="button"
                onClick={() => {
                  onSelectEnvironment(env.id);
                  setIsOpen(false);
                }}
                className={`w-full flex items-center justify-between px-3 py-2 text-left hover:bg-dark-800 transition-colors ${
                  isCurrent ? 'text-brand-400 font-semibold bg-brand-500/10' : 'text-slate-300'
                }`}
              >
                <div className="truncate pr-2">
                  <div className="truncate">{env.name}</div>
                  <div className="text-[10px] text-slate-500 font-mono">
                    {varCount} active variables
                  </div>
                </div>
                {isCurrent && <Check className="w-3.5 h-3.5 text-brand-400 shrink-0" />}
              </button>
            );
          })}

          <div className="p-1 border-t border-dark-800 mt-1">
            <button
              type="button"
              onClick={() => {
                setIsOpen(false);
                onOpenManager();
              }}
              className="w-full flex items-center justify-center gap-1.5 py-1.5 px-2 rounded bg-dark-800 hover:bg-dark-700 text-brand-400 text-xs font-medium transition-colors"
            >
              <Settings className="w-3.5 h-3.5" />
              <span>Configure Environments</span>
            </button>
          </div>
        </div>
      )}
    </div>
  );
};
