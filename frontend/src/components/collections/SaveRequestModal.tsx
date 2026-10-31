import React, { useState } from 'react';
import { X, BookmarkPlus } from 'lucide-react';
import { Collection } from '../../types';

interface SaveRequestModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSave: (name: string, collectionId?: number) => void;
  collections: Collection[];
  defaultName?: string;
  defaultCollectionId?: number;
}

export const SaveRequestModal: React.FC<SaveRequestModalProps> = ({
  isOpen,
  onClose,
  onSave,
  collections,
  defaultName = '',
  defaultCollectionId,
}) => {
  const [name, setName] = useState(defaultName || 'New Request');
  const [collectionId, setCollectionId] = useState<string>(
    defaultCollectionId ? String(defaultCollectionId) : ''
  );

  if (!isOpen) return null;

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!name.trim()) return;
    onSave(name.trim(), collectionId ? Number(collectionId) : undefined);
    onClose();
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/70 backdrop-blur-sm p-4">
      <div className="bg-dark-900 border border-dark-750 rounded-xl shadow-2xl max-w-md w-full overflow-hidden animate-scale-in">
        {/* Modal Header */}
        <div className="flex items-center justify-between p-4 border-b border-dark-800">
          <div className="flex items-center gap-2">
            <BookmarkPlus className="w-5 h-5 text-brand-400" />
            <h3 className="text-sm font-semibold text-slate-100">Save Request</h3>
          </div>
          <button
            type="button"
            onClick={onClose}
            className="text-slate-400 hover:text-white p-1 rounded transition-colors"
          >
            <X className="w-4 h-4" />
          </button>
        </div>

        {/* Modal Form */}
        <form onSubmit={handleSubmit} className="p-4 space-y-4">
          <div>
            <label className="block text-xs font-medium text-slate-300 mb-1">Request Name</label>
            <input
              type="text"
              value={name}
              onChange={(e) => setName(e.target.value)}
              placeholder="e.g. Get User Profile"
              required
              autoFocus
              className="w-full bg-dark-950 border border-dark-700 rounded-md px-3 py-2 text-xs text-slate-100 focus:outline-none focus:ring-1 focus:ring-brand-500"
            />
          </div>

          <div>
            <label className="block text-xs font-medium text-slate-300 mb-1">Collection</label>
            <select
              value={collectionId}
              onChange={(e) => setCollectionId(e.target.value)}
              className="w-full bg-dark-950 border border-dark-700 rounded-md px-3 py-2 text-xs text-slate-200 focus:outline-none focus:ring-1 focus:ring-brand-500 cursor-pointer"
            >
              <option value="">No Collection (Unassigned)</option>
              {collections.map((c) => (
                <option key={c.id} value={c.id}>
                  {c.name} ({c.requestCount || 0} requests)
                </option>
              ))}
            </select>
          </div>

          <div className="flex items-center justify-end gap-2 pt-2 border-t border-dark-800">
            <button
              type="button"
              onClick={onClose}
              className="px-3 py-1.5 rounded bg-dark-800 hover:bg-dark-700 text-slate-300 text-xs font-medium transition-colors"
            >
              Cancel
            </button>
            <button
              type="submit"
              className="px-4 py-1.5 rounded bg-brand-600 hover:bg-brand-500 text-white text-xs font-semibold shadow-md transition-colors"
            >
              Save Request
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};
