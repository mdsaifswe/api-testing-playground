import React from 'react';
import { AlertCircle, ShieldAlert, Clock, WifiOff } from 'lucide-react';

interface ErrorStateProps {
  category: 'NETWORK_ERROR' | 'TIMEOUT' | 'SECURITY_VIOLATION' | string;
  message?: string;
  onRetry?: () => void;
}

export const ErrorState: React.FC<ErrorStateProps> = ({ category, message, onRetry }) => {
  const isTimeout = category === 'TIMEOUT';
  const isSecurity = category === 'SECURITY_VIOLATION';
  const isNetwork = category === 'NETWORK_ERROR';

  const icon = isTimeout ? (
    <Clock className="w-10 h-10 text-amber-400" />
  ) : isSecurity ? (
    <ShieldAlert className="w-10 h-10 text-rose-500" />
  ) : isNetwork ? (
    <WifiOff className="w-10 h-10 text-rose-400" />
  ) : (
    <AlertCircle className="w-10 h-10 text-rose-400" />
  );

  const title = isTimeout
    ? 'Request Timed Out'
    : isSecurity
    ? 'Security Policy Blocked Request'
    : isNetwork
    ? 'Network / Connection Failure'
    : 'Execution Error';

  return (
    <div className="flex flex-col items-center justify-center p-8 text-center h-full min-h-[300px]">
      <div className="w-16 h-16 rounded-full bg-slate-800/60 border border-slate-700/80 flex items-center justify-center mb-4 shadow-lg">
        {icon}
      </div>
      <h3 className="text-base font-semibold text-slate-100">{title}</h3>
      <p className="text-sm text-slate-400 mt-1 max-w-md break-words">
        {message || 'Unable to complete request. Please verify the destination address and parameters.'}
      </p>
      {onRetry && (
        <button
          onClick={onRetry}
          className="mt-4 px-4 py-1.5 bg-slate-800 hover:bg-slate-700 border border-slate-600 text-xs font-medium text-slate-200 rounded-md transition-colors"
        >
          Try Again
        </button>
      )}
    </div>
  );
};
