import React from 'react';
import { Send, LucideIcon } from 'lucide-react';

interface EmptyStateProps {
  icon?: LucideIcon;
  title?: string;
  description?: string;
  actionButton?: React.ReactNode;
}

export const EmptyState: React.FC<EmptyStateProps> = ({
  icon: Icon = Send,
  title = 'No Request Executed',
  description = 'Configure an endpoint URL, parameters, and headers above, then click Send to view the live response.',
  actionButton,
}) => {
  return (
    <div className="flex flex-col items-center justify-center p-12 text-center h-full min-h-[300px]">
      <div className="w-16 h-16 rounded-2xl bg-dark-900 border border-dark-800 flex items-center justify-center mb-4 text-slate-500 shadow-inner">
        <Icon className="w-8 h-8" />
      </div>
      <h3 className="text-base font-semibold text-slate-300">{title}</h3>
      <p className="text-xs text-slate-400 mt-1 max-w-sm leading-relaxed">{description}</p>
      {actionButton && <div className="mt-4">{actionButton}</div>}
    </div>
  );
};
