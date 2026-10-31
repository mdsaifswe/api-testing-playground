import { describe, it, expect } from 'vitest';
import { substituteVariables, extractVariableNames } from '../utils/environment';
import { EnvironmentVariable } from '../types';

describe('Environment Variable Utilities', () => {
  it('extracts variable names from template strings', () => {
    const vars = extractVariableNames('{{baseUrl}}/api/v1/users/{{userId}}?key={{apiKey}}');
    expect(vars).toEqual(['baseUrl', 'userId', 'apiKey']);
  });

  it('substitutes active variables properly', () => {
    const variables: EnvironmentVariable[] = [
      { key: 'baseUrl', value: 'http://localhost:8080', isSecret: false, isEnabled: true },
      { key: 'apiKey', value: 'secret123', isSecret: true, isEnabled: true },
      { key: 'disabledVar', value: 'ignored', isSecret: false, isEnabled: false },
    ];

    const result = substituteVariables('{{baseUrl}}/echo?token={{apiKey}}&status={{disabledVar}}', variables);
    expect(result).toBe('http://localhost:8080/echo?token=secret123&status={{disabledVar}}');
  });

  it('preserves unknown variables unchanged', () => {
    const variables: EnvironmentVariable[] = [
      { key: 'host', value: 'api.io', isSecret: false, isEnabled: true },
    ];

    const result = substituteVariables('https://{{host}}/{{unknownPath}}', variables);
    expect(result).toBe('https://api.io/{{unknownPath}}');
  });
});
