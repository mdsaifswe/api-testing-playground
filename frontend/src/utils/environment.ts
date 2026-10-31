import { EnvironmentVariable } from '../types';

export const substituteVariables = (
  input: string,
  variables: EnvironmentVariable[]
): string => {
  if (!input || !variables || variables.length === 0) return input;

  const varMap: Record<string, string> = {};
  variables.forEach((v) => {
    if (v.isEnabled && v.key) {
      varMap[v.key] = v.value || '';
    }
  });

  return input.replace(/\{\{([a-zA-Z0-9_-]+)\}\}/g, (match, varName) => {
    return varMap[varName] !== undefined ? varMap[varName] : match;
  });
};

export const extractVariableNames = (input: string): string[] => {
  if (!input) return [];
  const matches = input.match(/\{\{([a-zA-Z0-9_-]+)\}\}/g);
  if (!matches) return [];
  return Array.from(new Set(matches.map((m) => m.replace(/\{\{|\}\}/g, ''))));
};
