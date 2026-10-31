import React from 'react';
import { Send, Loader2, BookmarkPlus, Terminal, XCircle } from 'lucide-react';
import { ExecutionState } from '../../types';

interface SendButtonProps {
  state: ExecutionState;
  onSend: () => void;
  onCancel?: () => void;
  onSave: () => void;
  onCopyCurl: () => void;
  disabled?: boolean;
}

export const SendButton: React.FC<SendButtonProps> = ({
  state,
  onSend,
  onCancel,
  onSave,
  onCopyCurl,
  disabled,
}) => {
  const isLoading = state === 'LOADING';

  return (
    <div className="flex items-center gap-2">
      {isLoading && onCancel ? (
        <button
          type="button"
          onClick={onCancel}
          className="flex items-center gap-1.5 px-4 py-2 rounded-r-md bg-rose-600 hover:bg-rose-500 text-white font-semibold text-xs transition-colors shadow-sm"
        >
          <XCircle className="w-4 h-4" />
          <span>Cancel</span>
        </button>
      ) : (
        <button
          id="send-request-button"
          type="button"
          onClick={onSend}
          disabled={disabled || isLoading}
          className="flex items-center gap-2 px-5 py-2 rounded-r-md bg-brand-600 hover:bg-brand-500 active:bg-brand-700 text-white font-semibold text-xs tracking-wide transition-all shadow-md hover:shadow-brand-500/20 disabled:opacity-50 disabled:pointer-events-none"
        >
          {isLoading ? (
            <>
              <Loader2 className="w-4 h-4 animate-spin" />
              <span>Sending...</span>
            </>
          ) : (
            <>
              <Send className="w-4 h-4" />
              <span>Send</span>
            </>
          )}
        </button>
      )}

      <button
        id="save-request-button"
        type="button"
        onClick={onSave}
        className="flex items-center gap-1.5 px-3 py-2 rounded-md bg-dark-900 hover:bg-dark-800 border border-dark-700 text-slate-300 hover:text-white font-medium text-xs transition-colors"
        title="Save request to collection"
      >
        <BookmarkPlus className="w-4 h-4 text-brand-400" />
        <span className="hidden sm:inline">Save</span>
      </button>

      <button
        id="copy-curl-button"
        type="button"
        onClick={onCopyCurl}
        className="flex items-center gap-1.5 px-3 py-2 rounded-md bg-dark-900 hover:bg-dark-800 border border-dark-700 text-slate-300 hover:text-white font-medium text-xs transition-colors"
        title="Copy request as cURL command"
      >
        <Terminal className="w-4 h-4 text-slate-400" />
        <span className="hidden sm:inline">cURL</span>
      </button>
    </div>
  );
};
