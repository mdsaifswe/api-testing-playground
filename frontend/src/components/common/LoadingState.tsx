import React from 'react';
import { Loader2 } from 'lucide-react';

interface LoadingStateProps {
  message?: string;
  subMessage?: string;
}

export const LoadingState: React.FC<LoadingStateProps> = ({
  message = 'Executing request...',
  subMessage = 'Connecting to target server and awaiting response...',
}) => {
  return (
    <div className="flex flex-col items-center justify-center p-12 text-center h-full min-h-[300px]">
      <div className="relative mb-4">
        <div className="w-12 h-12 rounded-full border-2 border-brand-500/20 animate-ping absolute inset-0" />
        <Loader2 className="w-12 h-12 text-brand-500 animate-spin relative" />
      </div>
      <h3 className="text-base font-medium text-slate-200">{message}</h3>
      <p className="text-xs text-slate-400 mt-1 max-w-sm">{subMessage}</p>
    </div>
  );
};
