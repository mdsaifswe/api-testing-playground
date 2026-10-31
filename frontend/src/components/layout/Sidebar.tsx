import React from 'react';
import { Folder, History, ChevronLeft, ChevronRight } from 'lucide-react';
import { Collection, RequestHistoryItem, SavedRequest } from '../../types';
import { CollectionsPanel } from '../collections/CollectionsPanel';
import { HistoryPanel } from '../history/HistoryPanel';

export type SidebarTab = 'collections' | 'history';

interface SidebarProps {
  activeTab: SidebarTab;
  onTabChange: (tab: SidebarTab) => void;
  isCollapsed: boolean;
  onToggleCollapse: () => void;
  collections: Collection[];
  history: RequestHistoryItem[];
  onSelectSavedRequest: (request: SavedRequest) => void;
  onSelectHistoryRequest: (historyItem: RequestHistoryItem) => void;
  onCreateCollection: (name: string, description?: string) => void;
  onDeleteCollection: (id: number) => void;
  onDuplicateSavedRequest: (id: number) => void;
  onDeleteSavedRequest: (id: number) => void;
  onDeleteHistoryItem: (id: number) => void;
  onClearHistory: () => void;
  isLoadingCollections?: boolean;
  isLoadingHistory?: boolean;
}

export const Sidebar: React.FC<SidebarProps> = ({
  activeTab,
  onTabChange,
  isCollapsed,
  onToggleCollapse,
  collections,
  history,
  onSelectSavedRequest,
  onSelectHistoryRequest,
  onCreateCollection,
  onDeleteCollection,
  onDuplicateSavedRequest,
  onDeleteSavedRequest,
  onDeleteHistoryItem,
  onClearHistory,
  isLoadingCollections,
  isLoadingHistory,
}) => {
  return (
    <div className="flex h-full bg-dark-950 border-r border-dark-800 z-10 shrink-0 select-none">
      {/* Mini Icon Bar */}
      <div className="w-12 bg-dark-900 border-r border-dark-800 flex flex-col items-center py-3 justify-between">
        <div className="flex flex-col items-center space-y-2 w-full">
          <button
            type="button"
            id="sidebar-tab-collections"
            onClick={() => {
              onTabChange('collections');
              if (isCollapsed) onToggleCollapse();
            }}
            className={`w-9 h-9 rounded-lg flex items-center justify-center transition-all ${
              !isCollapsed && activeTab === 'collections'
                ? 'bg-brand-600 text-white shadow-md shadow-brand-500/20'
                : 'text-slate-400 hover:text-slate-200 hover:bg-dark-800'
            }`}
            title="Collections"
          >
            <Folder className="w-5 h-5" />
          </button>

          <button
            type="button"
            id="sidebar-tab-history"
            onClick={() => {
              onTabChange('history');
              if (isCollapsed) onToggleCollapse();
            }}
            className={`w-9 h-9 rounded-lg flex items-center justify-center transition-all ${
              !isCollapsed && activeTab === 'history'
                ? 'bg-brand-600 text-white shadow-md shadow-brand-500/20'
                : 'text-slate-400 hover:text-slate-200 hover:bg-dark-800'
            }`}
            title="Request History"
          >
            <History className="w-5 h-5" />
          </button>
        </div>

        <button
          type="button"
          onClick={onToggleCollapse}
          className="w-8 h-8 rounded flex items-center justify-center text-slate-500 hover:text-slate-200 hover:bg-dark-800 transition-colors"
          title={isCollapsed ? 'Expand sidebar' : 'Collapse sidebar'}
        >
          {isCollapsed ? <ChevronRight className="w-4 h-4" /> : <ChevronLeft className="w-4 h-4" />}
        </button>
      </div>

      {/* Expanded Panel (Collections or History) */}
      {!isCollapsed && (
        <div className="h-full">
          {activeTab === 'collections' ? (
            <CollectionsPanel
              collections={collections}
              onSelectRequest={onSelectSavedRequest}
              onCreateCollection={onCreateCollection}
              onDeleteCollection={onDeleteCollection}
              onDuplicateRequest={onDuplicateSavedRequest}
              onDeleteRequest={onDeleteSavedRequest}
              isLoading={isLoadingCollections}
            />
          ) : (
            <HistoryPanel
              history={history}
              onSelectRequest={onSelectHistoryRequest}
              onDeleteItem={onDeleteHistoryItem}
              onClearAll={onClearHistory}
              isLoading={isLoadingHistory}
            />
          )}
        </div>
      )}
    </div>
  );
};
