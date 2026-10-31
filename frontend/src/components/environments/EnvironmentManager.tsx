import React, { useState } from 'react';
import {
  X,
  Plus,
  Trash2,
  Check,
  Eye,
  EyeOff,
  Layers,
} from 'lucide-react';
import { Environment, EnvironmentVariable } from '../../types';

interface EnvironmentManagerProps {
  isOpen: boolean;
  onClose: () => void;
  environments: Environment[];
  activeEnvironment: Environment | null;
  onCreateEnvironment: (name: string, variables: EnvironmentVariable[]) => void;
  onUpdateEnvironment: (id: number, name: string, isActive: boolean, variables: EnvironmentVariable[]) => void;
  onActivateEnvironment: (id: number) => void;
  onDeleteEnvironment: (id: number) => void;
}

export const EnvironmentManager: React.FC<EnvironmentManagerProps> = ({
  isOpen,
  onClose,
  environments,
  activeEnvironment,
  onCreateEnvironment,
  onUpdateEnvironment,
  onActivateEnvironment,
  onDeleteEnvironment,
}) => {
  const [selectedEnvId, setSelectedEnvId] = useState<number | null>(
    activeEnvironment?.id || (environments[0]?.id ?? null)
  );
  const [isCreatingNew, setIsCreatingNew] = useState(false);

  // Form states
  const [envName, setEnvName] = useState('');
  const [variables, setVariables] = useState<EnvironmentVariable[]>([]);
  const [showSecretMap, setShowSecretMap] = useState<Record<number, boolean>>({});

  // Sync state when selected environment changes
  React.useEffect(() => {
    if (isCreatingNew) {
      setEnvName('New Environment');
      setVariables([
        { key: 'baseUrl', value: 'http://localhost:8080/api/v1/echo', isSecret: false, isEnabled: true },
        { key: 'apiKey', value: 'secret-key-token', isSecret: true, isEnabled: true },
      ]);
    } else {
      const current = environments.find((e) => e.id === selectedEnvId);
      if (current) {
        setEnvName(current.name);
        setVariables(current.variables ? JSON.parse(JSON.stringify(current.variables)) : []);
      }
    }
  }, [selectedEnvId, isCreatingNew, environments]);

  if (!isOpen) return null;

  const handleAddVariable = () => {
    setVariables([...variables, { key: '', value: '', isSecret: false, isEnabled: true }]);
  };

  const handleUpdateVariable = (index: number, field: keyof EnvironmentVariable, val: any) => {
    const updated = [...variables];
    updated[index] = { ...updated[index], [field]: val };
    setVariables(updated);
  };

  const handleDeleteVariable = (index: number) => {
    setVariables(variables.filter((_, i) => i !== index));
  };

  const toggleShowSecret = (index: number) => {
    setShowSecretMap((prev) => ({ ...prev, [index]: !prev[index] }));
  };

  const handleSave = () => {
    if (!envName.trim()) return;

    if (isCreatingNew) {
      onCreateEnvironment(envName.trim(), variables);
      setIsCreatingNew(false);
    } else if (selectedEnvId) {
      const current = environments.find((e) => e.id === selectedEnvId);
      onUpdateEnvironment(
        selectedEnvId,
        envName.trim(),
        Boolean(current?.isActive),
        variables
      );
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/75 backdrop-blur-sm p-4">
      <div className="bg-dark-900 border border-dark-750 rounded-xl shadow-2xl max-w-4xl w-full h-[600px] flex flex-col overflow-hidden animate-scale-in">
        {/* Modal Header */}
        <div className="flex items-center justify-between p-4 border-b border-dark-800 bg-dark-950">
          <div className="flex items-center gap-2">
            <Layers className="w-5 h-5 text-brand-400" />
            <h3 className="text-sm font-bold text-slate-100">Environment Management</h3>
            <span className="text-xs text-slate-400">
              — Define variables used in URLs and headers via &#123;&#123;variable&#125;&#125;
            </span>
          </div>
          <button
            type="button"
            onClick={onClose}
            className="text-slate-400 hover:text-white p-1 rounded transition-colors"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Modal Body: Split view */}
        <div className="flex-1 flex overflow-hidden">
          {/* Left List of Environments */}
          <div className="w-64 border-r border-dark-800 bg-dark-950/70 p-3 flex flex-col">
            <button
              type="button"
              onClick={() => {
                setIsCreatingNew(true);
                setSelectedEnvId(null);
              }}
              className="w-full flex items-center justify-center gap-2 py-2 px-3 rounded-lg bg-brand-600 hover:bg-brand-500 text-white text-xs font-semibold shadow-md transition-colors mb-3"
            >
              <Plus className="w-4 h-4" />
              <span>New Environment</span>
            </button>

            <div className="flex-1 overflow-auto space-y-1">
              {environments.map((env) => {
                const isSelected = !isCreatingNew && selectedEnvId === env.id;
                const isActive = activeEnvironment?.id === env.id;

                return (
                  <button
                    key={env.id}
                    type="button"
                    onClick={() => {
                      setIsCreatingNew(false);
                      setSelectedEnvId(env.id);
                    }}
                    className={`w-full text-left p-2.5 rounded-lg text-xs transition-colors flex items-center justify-between group ${
                      isSelected
                        ? 'bg-dark-800 text-brand-300 font-semibold border border-dark-700'
                        : 'text-slate-300 hover:bg-dark-900'
                    }`}
                  >
                    <div className="truncate pr-1">
                      <div className="truncate">{env.name}</div>
                      <div className="text-[10px] text-slate-500 font-mono">
                        {env.variables?.length || 0} variables
                      </div>
                    </div>
                    {isActive && (
                      <span className="w-2 h-2 rounded-full bg-emerald-400 shrink-0" title="Active" />
                    )}
                  </button>
                );
              })}
            </div>
          </div>

          {/* Right Editor for Selected Environment */}
          <div className="flex-1 p-5 flex flex-col bg-dark-900 overflow-hidden">
            <div className="flex items-center justify-between pb-3 border-b border-dark-800 mb-4">
              <div className="flex items-center gap-3 flex-1">
                <input
                  type="text"
                  value={envName}
                  onChange={(e) => setEnvName(e.target.value)}
                  placeholder="Environment name"
                  className="bg-dark-950 border border-dark-700 rounded px-3 py-1.5 text-sm font-semibold text-slate-100 max-w-xs w-full focus:outline-none focus:ring-1 focus:ring-brand-500"
                />
                {!isCreatingNew && selectedEnvId && (
                  <button
                    type="button"
                    onClick={() => onActivateEnvironment(selectedEnvId)}
                    disabled={activeEnvironment?.id === selectedEnvId}
                    className={`px-3 py-1.5 rounded text-xs font-semibold transition-all flex items-center gap-1.5 ${
                      activeEnvironment?.id === selectedEnvId
                        ? 'bg-emerald-500/20 text-emerald-300 border border-emerald-500/30'
                        : 'bg-dark-800 hover:bg-dark-700 text-slate-300 border border-dark-700'
                    }`}
                  >
                    <Check className="w-3.5 h-3.5" />
                    <span>{activeEnvironment?.id === selectedEnvId ? 'Active' : 'Set as Active'}</span>
                  </button>
                )}
              </div>

              {!isCreatingNew && selectedEnvId && (
                <button
                  type="button"
                  onClick={() => {
                    if (confirm(`Delete environment "${envName}"?`)) {
                      onDeleteEnvironment(selectedEnvId);
                      setSelectedEnvId(environments[0]?.id || null);
                    }
                  }}
                  className="text-slate-400 hover:text-rose-400 p-1.5 rounded transition-colors"
                  title="Delete this environment"
                >
                  <Trash2 className="w-4 h-4" />
                </button>
              )}
            </div>

            {/* Variables Table */}
            <div className="flex items-center justify-between text-xs text-slate-400 font-medium mb-2">
              <span>Environment Variables ({variables.filter((v) => v.isEnabled && v.key).length} active)</span>
              <button
                type="button"
                onClick={handleAddVariable}
                className="flex items-center gap-1 text-brand-400 hover:text-brand-300 font-semibold px-2 py-1 rounded hover:bg-dark-800 transition-colors"
              >
                <Plus className="w-3.5 h-3.5" />
                <span>Add Variable</span>
              </button>
            </div>

            <div className="flex-1 overflow-auto border border-dark-800 rounded-lg bg-dark-950/60">
              <table className="w-full text-left text-xs font-mono">
                <thead>
                  <tr className="border-b border-dark-800 bg-dark-900/80 text-slate-400 font-sans">
                    <th className="w-10 px-3 py-2 text-center">Active</th>
                    <th className="px-3 py-2 w-1/3">Variable Key</th>
                    <th className="px-3 py-2 w-1/2">Value</th>
                    <th className="w-16 px-2 py-2 text-center">Secret</th>
                    <th className="w-10 px-2 py-2 text-center"></th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-dark-800/60">
                  {variables.length === 0 ? (
                    <tr>
                      <td colSpan={5} className="px-4 py-8 text-center text-slate-500 font-sans text-xs">
                        No variables defined in this environment. Click "Add Variable" to add variables.
                      </td>
                    </tr>
                  ) : (
                    variables.map((v, idx) => {
                      const isSecret = Boolean(v.isSecret);
                      const isRevealed = Boolean(showSecretMap[idx]);

                      return (
                        <tr key={idx} className="hover:bg-dark-900/40">
                          <td className="px-3 py-2 text-center">
                            <input
                              type="checkbox"
                              checked={v.isEnabled}
                              onChange={(e) => handleUpdateVariable(idx, 'isEnabled', e.target.checked)}
                              className="rounded bg-dark-800 border-dark-700 text-brand-500 focus:ring-0 cursor-pointer"
                            />
                          </td>
                          <td className="px-2 py-2">
                            <input
                              type="text"
                              value={v.key}
                              onChange={(e) => handleUpdateVariable(idx, 'key', e.target.value)}
                              placeholder="e.g. baseUrl"
                              className="w-full bg-transparent px-2 py-1 text-slate-200 placeholder-slate-600 focus:outline-none focus:bg-dark-800 rounded"
                            />
                          </td>
                          <td className="px-2 py-2 relative">
                            <div className="relative flex items-center">
                              <input
                                type={isSecret && !isRevealed ? 'password' : 'text'}
                                value={v.value || ''}
                                onChange={(e) => handleUpdateVariable(idx, 'value', e.target.value)}
                                placeholder="Value"
                                className="w-full bg-transparent px-2 py-1 pr-7 text-slate-200 placeholder-slate-600 focus:outline-none focus:bg-dark-800 rounded"
                              />
                              {isSecret && (
                                <button
                                  type="button"
                                  onClick={() => toggleShowSecret(idx)}
                                  className="absolute right-1.5 text-slate-500 hover:text-slate-300 p-0.5"
                                  title={isRevealed ? 'Hide secret' : 'Reveal secret'}
                                >
                                  {isRevealed ? <EyeOff className="w-3.5 h-3.5" /> : <Eye className="w-3.5 h-3.5" />}
                                </button>
                              )}
                            </div>
                          </td>
                          <td className="px-2 py-2 text-center">
                            <input
                              type="checkbox"
                              checked={isSecret}
                              onChange={(e) => handleUpdateVariable(idx, 'isSecret', e.target.checked)}
                              className="rounded bg-dark-800 border-dark-700 text-amber-500 focus:ring-0 cursor-pointer"
                              title="Mask value as secret"
                            />
                          </td>
                          <td className="px-2 py-2 text-center">
                            <button
                              type="button"
                              onClick={() => handleDeleteVariable(idx)}
                              className="text-slate-500 hover:text-rose-400 p-1 rounded"
                              title="Delete variable"
                            >
                              <Trash2 className="w-3.5 h-3.5" />
                            </button>
                          </td>
                        </tr>
                      );
                    })
                  )}
                </tbody>
              </table>
            </div>

            <div className="flex items-center justify-end gap-3 pt-4 border-t border-dark-800 mt-4">
              <button
                type="button"
                onClick={onClose}
                className="px-4 py-2 rounded-md bg-dark-800 hover:bg-dark-700 text-slate-300 text-xs font-medium transition-colors"
              >
                Close
              </button>
              <button
                type="button"
                onClick={handleSave}
                className="px-5 py-2 rounded-md bg-brand-600 hover:bg-brand-500 text-white text-xs font-semibold shadow-md transition-colors"
              >
                Save Environment
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};
