import React from 'react';
import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { MethodSelector } from '../components/request/MethodSelector';

describe('MethodSelector Component', () => {
  it('renders with initial method and responds to changes', () => {
    const handleChange = vi.fn();
    render(<MethodSelector method="GET" onChange={handleChange} />);

    const select = screen.getByRole('combobox');
    expect(select).toHaveValue('GET');

    fireEvent.change(select, { target: { value: 'POST' } });
    expect(handleChange).toHaveBeenCalledWith('POST');

    fireEvent.change(select, { target: { value: 'DELETE' } });
    expect(handleChange).toHaveBeenCalledWith('DELETE');
  });

  it('renders all HTTP method options', () => {
    render(<MethodSelector method="GET" onChange={() => {}} />);
    expect(screen.getByText('GET')).toBeInTheDocument();
    expect(screen.getByText('POST')).toBeInTheDocument();
    expect(screen.getByText('PUT')).toBeInTheDocument();
    expect(screen.getByText('PATCH')).toBeInTheDocument();
    expect(screen.getByText('DELETE')).toBeInTheDocument();
  });
});
