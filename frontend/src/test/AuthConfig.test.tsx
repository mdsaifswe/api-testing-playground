import React from 'react';
import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { AuthConfig } from '../components/request/AuthConfig';
import { AuthConfig as AuthConfigType } from '../types';

describe('AuthConfig Component', () => {
  it('toggles from No Auth to API Key', () => {
    const auth: AuthConfigType = { type: 'NONE', keyName: '', keyValue: '', placement: 'HEADER' };
    const handleChange = vi.fn();

    render(<AuthConfig auth={auth} onChange={handleChange} />);
    expect(screen.getByText(/This request currently sends no authentication headers/i)).toBeInTheDocument();

    const apiKeyButton = screen.getByText('API Key');
    fireEvent.click(apiKeyButton);

    expect(handleChange).toHaveBeenCalledWith(
      expect.objectContaining({
        type: 'API_KEY',
        keyName: 'X-API-Key',
      })
    );
  });

  it('allows configuring key name, value, and placement', () => {
    const auth: AuthConfigType = {
      type: 'API_KEY',
      keyName: 'X-Custom-Key',
      keyValue: 'my-secret-val',
      placement: 'HEADER',
    };
    const handleChange = vi.fn();

    render(<AuthConfig auth={auth} onChange={handleChange} />);

    const keyInput = screen.getByLabelText(/Key Name/i) as HTMLInputElement;
    expect(keyInput.value).toBe('X-Custom-Key');

    const placementSelect = screen.getByLabelText(/Placement/i) as HTMLSelectElement;
    expect(placementSelect.value).toBe('HEADER');

    fireEvent.change(placementSelect, { target: { value: 'QUERY' } });
    expect(handleChange).toHaveBeenCalledWith(
      expect.objectContaining({ placement: 'QUERY' })
    );
  });
});
