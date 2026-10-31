import React from 'react';
import { Header } from './Header';
import { Sidebar, SidebarTab } from './Sidebar';
import { Collection, Environment, RequestHistoryItem, SavedRequest } from '../../types';

interface AppShellProps {
  children: React.ReactNode;
  environments: Environment[];
  activeEnvironment: Environment | null;
  onSelectEnvironment: (id: number | null) => void;
  onOpenEnvironmentManager: () => void;
  backendHealthy: boolean;
  sidebarTab: SidebarTab;
  onSidebarTabChange: (tab: SidebarTab) => void;
  sidebarCollapsed: boolean;
  onToggleSidebarCollapse: () => void;
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

export const AppShell: React.FC<AppShellProps> = ({
  children,
  environments,
  activeEnvironment,
  onSelectEnvironment,
  onOpenEnvironmentManager,
  backendHealthy,
  sidebarTab,
  onSidebarTabChange,
  sidebarCollapsed,
  onToggleSidebarCollapse,
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
    <div className="flex flex-col h-screen w-screen overflow-hidden bg-dark-950 text-slate-100">
      {/* Header */}
      <Header
        environments={environments}
        activeEnvironment={activeEnvironment}
        onSelectEnvironment={onSelectEnvironment}
        onOpenEnvironmentManager={onOpenEnvironmentManager}
        backendHealthy={backendHealthy}
      />

      {/* Main Body */}
      <div className="flex flex-1 overflow-hidden">
        {/* Sidebar */}
        <Sidebar
          activeTab={sidebarTab}
          onTabChange={onSidebarTabChange}
          isCollapsed={sidebarCollapsed}
          onToggleCollapse={onToggleSidebarCollapse}
          collections={collections}
          history={history}
          onSelectSavedRequest={onSelectSavedRequest}
          onSelectHistoryRequest={onSelectHistoryRequest}
          onCreateCollection={onCreateCollection}
          onDeleteCollection={onDeleteCollection}
          onDuplicateSavedRequest={onDuplicateSavedRequest}
          onDeleteSavedRequest={onDeleteSavedRequest}
          onDeleteHistoryItem={onDeleteHistoryItem}
          onClearHistory={onClearHistory}
          isLoadingCollections={isLoadingCollections}
          isLoadingHistory={isLoadingHistory}
        />

        {/* Main Center Workspace */}
        <main className="flex-1 flex flex-col overflow-hidden bg-dark-900/30">
          {children}
        </main>
      </div>
    </div>
  );
};
