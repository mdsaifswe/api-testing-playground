import React, { useState } from 'react';
import {
  Folder,
  FolderOpen,
  FolderPlus,
  ChevronRight,
  ChevronDown,
  Trash2,
  Copy,
  Search,
} from 'lucide-react';
import { Collection, SavedRequest } from '../../types';

interface CollectionsPanelProps {
  collections: Collection[];
  onSelectRequest: (request: SavedRequest) => void;
  onCreateCollection: (name: string, description?: string) => void;
  onDeleteCollection: (id: number) => void;
  onDuplicateRequest: (id: number) => void;
  onDeleteRequest: (id: number) => void;
  isLoading?: boolean;
}

export const CollectionsPanel: React.FC<CollectionsPanelProps> = ({
  collections,
  onSelectRequest,
  onCreateCollection,
  onDeleteCollection,
  onDuplicateRequest,
  onDeleteRequest,
  isLoading,
}) => {
  const [openFolders, setOpenFolders] = useState<Record<number, boolean>>({ 1: true });
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [newCollectionName, setNewCollectionName] = useState('');
  const [filterQuery, setFilterQuery] = useState('');

  const toggleFolder = (id: number) => {
    setOpenFolders((prev) => ({ ...prev, [id]: !prev[id] }));
  };

  const handleCreateSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!newCollectionName.trim()) return;
    onCreateCollection(newCollectionName.trim());
    setNewCollectionName('');
    setShowCreateModal(false);
  };

  const getMethodBadge = (m: string) => {
    const map: Record<string, string> = {
      GET: 'text-emerald-400 bg-emerald-500/10',
      POST: 'text-blue-400 bg-blue-500/10',
      PUT: 'text-amber-400 bg-amber-500/10',
      PATCH: 'text-purple-400 bg-purple-500/10',
      DELETE: 'text-rose-400 bg-rose-500/10',
    };
    return map[m.toUpperCase()] || 'text-slate-400 bg-slate-500/10';
  };

  return (
    <div className="flex flex-col h-full bg-dark-950 border-r border-dark-800 w-80 shrink-0">
      {/* Header */}
      <div className="p-3 border-b border-dark-800 flex items-center justify-between">
        <div className="flex items-center gap-2">
          <Folder className="w-4 h-4 text-brand-400" />
          <h2 className="text-xs font-bold uppercase tracking-wider text-slate-200">Collections</h2>
          <span className="text-[10px] px-1.5 py-0.5 rounded-full bg-dark-800 text-slate-400 font-mono">
            {collections.length}
          </span>
        </div>

        <button
          type="button"
          onClick={() => setShowCreateModal(true)}
          className="flex items-center gap-1 text-[11px] font-semibold text-brand-400 hover:text-brand-300 px-2 py-1 rounded bg-dark-900 border border-dark-700 hover:border-brand-500/40 transition-colors"
          title="Create collection"
        >
          <FolderPlus className="w-3.5 h-3.5" />
          <span>New</span>
        </button>
      </div>

      {/* Search Bar */}
      <div className="p-2 border-b border-dark-800">
        <div className="relative flex items-center">
          <Search className="w-3.5 h-3.5 text-slate-500 absolute left-2.5" />
          <input
            type="text"
            value={filterQuery}
            onChange={(e) => setFilterQuery(e.target.value)}
            placeholder="Search collections & requests..."
            className="w-full bg-dark-900 border border-dark-700 rounded pl-8 pr-3 py-1 text-xs text-slate-200 placeholder-slate-500 focus:outline-none focus:ring-1 focus:ring-brand-500"
          />
        </div>
      </div>

      {/* Inline Create Collection Modal / Form */}
      {showCreateModal && (
        <form onSubmit={handleCreateSubmit} className="p-3 bg-dark-900 border-b border-dark-800 space-y-2">
          <div className="text-xs font-semibold text-slate-300">Create New Collection</div>
          <input
            type="text"
            value={newCollectionName}
            onChange={(e) => setNewCollectionName(e.target.value)}
            placeholder="Collection name (e.g. Auth APIs)"
            autoFocus
            className="w-full bg-dark-950 border border-dark-700 rounded px-2.5 py-1.5 text-xs text-slate-100 focus:outline-none focus:ring-1 focus:ring-brand-500"
          />
          <div className="flex justify-end gap-1.5 pt-1">
            <button
              type="button"
              onClick={() => setShowCreateModal(false)}
              className="text-[11px] px-2 py-1 text-slate-400 hover:text-white"
            >
              Cancel
            </button>
            <button
              type="submit"
              className="text-[11px] px-3 py-1 bg-brand-600 hover:bg-brand-500 text-white font-medium rounded shadow-sm"
            >
              Create
            </button>
          </div>
        </form>
      )}

      {/* Collections Tree */}
      <div className="flex-1 overflow-auto p-2 space-y-1">
        {isLoading ? (
          <div className="p-8 text-center text-xs text-slate-500">Loading collections...</div>
        ) : collections.length === 0 ? (
          <div className="p-8 text-center text-xs text-slate-500 flex flex-col items-center">
            <Folder className="w-8 h-8 text-slate-600 mb-2" />
            <span>No collections created yet.</span>
            <span className="text-[10px] text-slate-600 mt-1">
              Click "New" above to organize your saved requests.
            </span>
          </div>
        ) : (
          collections.map((col) => {
            const isOpen = Boolean(openFolders[col.id]);
            const requests = col.requests || [];
            const filteredRequests = requests.filter(
              (r) =>
                r.name.toLowerCase().includes(filterQuery.toLowerCase()) ||
                r.url.toLowerCase().includes(filterQuery.toLowerCase())
            );

            return (
              <div key={col.id} className="rounded-md overflow-hidden bg-dark-900/30 border border-dark-800/60">
                {/* Collection Header Row */}
                <div
                  className="flex items-center justify-between p-2 hover:bg-dark-900/80 cursor-pointer select-none group"
                  onClick={() => toggleFolder(col.id)}
                >
                  <div className="flex items-center gap-1.5 truncate">
                    {isOpen ? (
                      <ChevronDown className="w-3.5 h-3.5 text-slate-500 shrink-0" />
                    ) : (
                      <ChevronRight className="w-3.5 h-3.5 text-slate-500 shrink-0" />
                    )}
                    {isOpen ? (
                      <FolderOpen className="w-4 h-4 text-brand-400 shrink-0" />
                    ) : (
                      <Folder className="w-4 h-4 text-slate-400 shrink-0" />
                    )}
                    <span className="text-xs font-semibold text-slate-200 truncate">{col.name}</span>
                    <span className="text-[10px] text-slate-500 font-mono">({requests.length})</span>
                  </div>

                  <button
                    type="button"
                    onClick={(e) => {
                      e.stopPropagation();
                      if (confirm(`Delete collection "${col.name}" and its requests?`)) {
                        onDeleteCollection(col.id);
                      }
                    }}
                    className="opacity-0 group-hover:opacity-100 text-slate-500 hover:text-rose-400 p-1 rounded transition-opacity"
                    title="Delete collection"
                  >
                    <Trash2 className="w-3 h-3" />
                  </button>
                </div>

                {/* Collection Request Items */}
                {isOpen && (
                  <div className="pl-4 pr-1 py-1 space-y-0.5 border-t border-dark-800/40">
                    {filteredRequests.length === 0 ? (
                      <div className="py-2 text-[11px] text-slate-500 italic pl-3">
                        No requests in this collection.
                      </div>
                    ) : (
                      filteredRequests.map((req) => (
                        <div
                          key={req.id}
                          onClick={() => onSelectRequest(req)}
                          className="flex items-center justify-between p-1.5 rounded hover:bg-dark-800 cursor-pointer group/item transition-colors"
                        >
                          <div className="flex items-center gap-2 truncate">
                            <span
                              className={`text-[9px] font-mono font-bold px-1 rounded uppercase shrink-0 ${getMethodBadge(
                                req.method
                              )}`}
                            >
                              {req.method}
                            </span>
                            <span className="text-xs text-slate-300 truncate" title={req.name}>
                              {req.name}
                            </span>
                          </div>

                          <div className="flex items-center gap-1 opacity-0 group-hover/item:opacity-100 transition-opacity">
                            <button
                              type="button"
                              onClick={(e) => {
                                e.stopPropagation();
                                onDuplicateRequest(req.id);
                              }}
                              className="text-slate-400 hover:text-slate-200 p-1 rounded"
                              title="Duplicate request"
                            >
                              <Copy className="w-3 h-3" />
                            </button>
                            <button
                              type="button"
                              onClick={(e) => {
                                e.stopPropagation();
                                onDeleteRequest(req.id);
                              }}
                              className="text-slate-400 hover:text-rose-400 p-1 rounded"
                              title="Delete request"
                            >
                              <Trash2 className="w-3 h-3" />
                            </button>
                          </div>
                        </div>
                      ))
                    )}
                  </div>
                )}
              </div>
            );
          })
        )}
      </div>
    </div>
  );
};
