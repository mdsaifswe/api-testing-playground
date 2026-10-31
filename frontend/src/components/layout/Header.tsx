import React from 'react';
import { Zap, BookOpen, ExternalLink } from 'lucide-react';
import { Environment } from '../../types';
import { EnvironmentSelector } from '../environments/EnvironmentSelector';

interface HeaderProps {
  environments: Environment[];
  activeEnvironment: Environment | null;
  onSelectEnvironment: (id: number | null) => void;
  onOpenEnvironmentManager: () => void;
  backendHealthy: boolean;
}

export const Header: React.FC<HeaderProps> = ({
  environments,
  activeEnvironment,
  onSelectEnvironment,
  onOpenEnvironmentManager,
  backendHealthy,
}) => {
  return (
    <header className="h-14 bg-dark-900 border-b border-dark-800 px-4 flex items-center justify-between z-20 shrink-0">
      {/* Brand Logo & Name */}
      <div className="flex items-center gap-3">
        <div className="w-8 h-8 rounded-lg bg-gradient-to-tr from-brand-600 to-indigo-400 flex items-center justify-center shadow-lg shadow-brand-500/20">
          <Zap className="w-4 h-4 text-white fill-white" />
        </div>
        <div>
          <div className="flex items-center gap-2">
            <h1 className="text-sm font-bold text-white tracking-tight">API Testing Playground</h1>
            <span className="text-[10px] uppercase font-mono px-1.5 py-0.5 rounded bg-brand-500/20 text-brand-300 border border-brand-500/30">
              v1.0
            </span>
          </div>
          <p className="text-[10px] text-slate-400 hidden sm:block">
            Developer REST Client & Request Persistence Engine
          </p>
        </div>
      </div>

      {/* Right Controls: Health Badge, Environment Selector, Swagger Link */}
      <div className="flex items-center gap-3">
        {/* Backend Health Badge */}
        <div
          className={`flex items-center gap-1.5 px-2.5 py-1 rounded-full text-[11px] font-medium border ${
            backendHealthy
              ? 'bg-emerald-500/10 text-emerald-400 border-emerald-500/20'
              : 'bg-rose-500/10 text-rose-400 border-rose-500/20'
          }`}
          title={backendHealthy ? 'Backend & MySQL Persistence Online' : 'Connecting to backend...'}
        >
          <span
            className={`w-2 h-2 rounded-full ${
              backendHealthy ? 'bg-emerald-400 animate-pulse' : 'bg-rose-500'
            }`}
          />
          <span className="hidden md:inline font-mono">
            {backendHealthy ? 'Backend Online' : 'Connecting...'}
          </span>
        </div>

        {/* Environment Selector */}
        <EnvironmentSelector
          environments={environments}
          activeEnvironment={activeEnvironment}
          onSelectEnvironment={onSelectEnvironment}
          onOpenManager={onOpenEnvironmentManager}
        />

        {/* OpenAPI / Swagger UI Link */}
        <a
          href="/swagger-ui.html"
          target="_blank"
          rel="noopener noreferrer"
          className="flex items-center gap-1 px-2.5 py-1.5 rounded-md bg-dark-800 hover:bg-dark-700 text-slate-300 hover:text-white text-xs border border-dark-700 transition-colors"
          title="Open Swagger OpenAPI Documentation"
        >
          <BookOpen className="w-3.5 h-3.5 text-brand-400" />
          <span className="hidden lg:inline">Swagger API Docs</span>
          <ExternalLink className="w-3 h-3 text-slate-500" />
        </a>
      </div>
    </header>
  );
};
