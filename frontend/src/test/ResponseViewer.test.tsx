import React from 'react';
import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import { ResponseViewer } from '../components/response/ResponseViewer';
import { ExecutionResponse } from '../types';

describe('ResponseViewer Component', () => {
  it('renders EmptyState when execution state is IDLE', () => {
    render(<ResponseViewer state="IDLE" response={null} />);
    expect(screen.getByText('No Request Executed')).toBeInTheDocument();
  });

  it('renders LoadingState when execution state is LOADING', () => {
    render(<ResponseViewer state="LOADING" response={null} />);
    expect(screen.getByText('Executing request...')).toBeInTheDocument();
  });

  it('renders ResponseStatus and JsonViewer on successful response', () => {
    const mockResponse: ExecutionResponse = {
      statusCode: 200,
      statusText: 'OK',
      durationMs: 42,
      sizeBytes: 128,
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ message: 'Success' }),
      contentType: 'application/json',
      errorCategory: 'SUCCESS',
    };

    render(<ResponseViewer state="SUCCESS" response={mockResponse} />);
    expect(screen.getByText('200')).toBeInTheDocument();
    expect(screen.getByText('OK')).toBeInTheDocument();
    expect(screen.getByText('42 ms')).toBeInTheDocument();
    expect(screen.getByText('Pretty JSON')).toBeInTheDocument();
  });

  it('renders HTTP error responses (e.g. 404 Not Found) with full inspectability', () => {
    const mockResponse: ExecutionResponse = {
      statusCode: 404,
      statusText: 'Not Found',
      durationMs: 35,
      sizeBytes: 64,
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ error: 'Endpoint missing' }),
      contentType: 'application/json',
      errorCategory: 'HTTP_ERROR',
    };

    render(<ResponseViewer state="HTTP_ERROR" response={mockResponse} />);
    expect(screen.getByText('404')).toBeInTheDocument();
    expect(screen.getByText('Not Found')).toBeInTheDocument();
  });

  it('renders ErrorState on network failure', () => {
    render(<ResponseViewer state="NETWORK_ERROR" response={null} />);
    expect(screen.getByText('Network / Connection Failure')).toBeInTheDocument();
  });

  it('renders ErrorState on timeout', () => {
    render(<ResponseViewer state="TIMEOUT" response={null} />);
    expect(screen.getByText('Request Timed Out')).toBeInTheDocument();
  });
});
