import React from 'react';
import { Plus, Trash2 } from 'lucide-react';
import { KeyValue } from '../../types';

interface QueryParamsEditorProps {
  queryParams: KeyValue[];
  onChange: (params: KeyValue[]) => void;
  disabled?: boolean;
}

export const QueryParamsEditor: React.FC<QueryParamsEditorProps> = ({
  queryParams,
  onChange,
  disabled,
}) => {
  const handleAdd = () => {
    onChange([...queryParams, { key: '', value: '', enabled: true }]);
  };

  const handleUpdate = (index: number, field: keyof KeyValue, value: any) => {
    const updated = [...queryParams];
    updated[index] = { ...updated[index], [field]: value };
    onChange(updated);
  };

  const handleDelete = (index: number) => {
    const updated = queryParams.filter((_, i) => i !== index);
    onChange(updated);
  };

  return (
    <div className="space-y-3">
      <div className="flex items-center justify-between text-xs text-slate-400 font-medium">
        <span>Query Parameters ({queryParams.filter((p) => p.enabled && p.key).length} active)</span>
        <button
          type="button"
          onClick={handleAdd}
          disabled={disabled}
          className="flex items-center gap-1 text-brand-400 hover:text-brand-300 font-semibold px-2 py-1 rounded hover:bg-dark-800 transition-colors"
        >
          <Plus className="w-3.5 h-3.5" />
          <span>Add Parameter</span>
        </button>
      </div>

      <div className="border border-dark-800 rounded-lg overflow-hidden bg-dark-900/60">
        <table className="w-full text-left text-xs">
          <thead>
            <tr className="border-b border-dark-800 bg-dark-900/80 text-slate-400">
              <th className="w-10 px-3 py-2 text-center">Active</th>
              <th className="px-3 py-2 w-1/2">Key</th>
              <th className="px-3 py-2 w-1/2">Value</th>
              <th className="w-10 px-2 py-2 text-center"></th>
            </tr>
          </thead>
          <tbody className="divide-y divide-dark-800/60 font-mono">
            {queryParams.length === 0 ? (
              <tr>
                <td colSpan={4} className="px-4 py-6 text-center text-slate-500 font-sans text-xs">
                  No query parameters configured. Click "Add Parameter" to append parameters to the URL.
                </td>
              </tr>
            ) : (
              queryParams.map((param, index) => (
                <tr key={index} className="hover:bg-dark-800/40 transition-colors">
                  <td className="px-3 py-1.5 text-center">
                    <input
                      type="checkbox"
                      checked={param.enabled}
                      onChange={(e) => handleUpdate(index, 'enabled', e.target.checked)}
                      disabled={disabled}
                      className="rounded bg-dark-800 border-dark-700 text-brand-500 focus:ring-0 focus:ring-offset-0 cursor-pointer"
                    />
                  </td>
                  <td className="px-2 py-1.5">
                    <input
                      type="text"
                      value={param.key}
                      onChange={(e) => handleUpdate(index, 'key', e.target.value)}
                      placeholder="e.g. limit"
                      disabled={disabled}
                      className="w-full bg-transparent border-0 px-2 py-1 text-slate-200 placeholder-slate-600 focus:outline-none focus:bg-dark-800 rounded transition-colors text-xs"
                    />
                  </td>
                  <td className="px-2 py-1.5">
                    <input
                      type="text"
                      value={param.value}
                      onChange={(e) => handleUpdate(index, 'value', e.target.value)}
                      placeholder="e.g. 50, {{limit}}"
                      disabled={disabled}
                      className="w-full bg-transparent border-0 px-2 py-1 text-slate-200 placeholder-slate-600 focus:outline-none focus:bg-dark-800 rounded transition-colors text-xs"
                    />
                  </td>
                  <td className="px-2 py-1.5 text-center">
                    <button
                      type="button"
                      onClick={() => handleDelete(index)}
                      disabled={disabled}
                      className="text-slate-500 hover:text-rose-400 p-1 rounded transition-colors"
                      title="Delete row"
                    >
                      <Trash2 className="w-3.5 h-3.5" />
                    </button>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
};
