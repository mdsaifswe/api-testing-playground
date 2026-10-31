import { ExecuteRequestPayload } from '../types';

export const generateCurlCommand = (request: ExecuteRequestPayload): string => {
  const parts: string[] = ['curl -X', request.method];

  // URL + Query Params
  let fullUrl = request.url;
  const activeParams = request.queryParams.filter((p) => p.enabled && p.key);
  if (activeParams.length > 0) {
    const separator = fullUrl.includes('?') ? '&' : '?';
    const queryString = activeParams
      .map((p) => `${encodeURIComponent(p.key)}=${encodeURIComponent(p.value)}`)
      .join('&');
    fullUrl += separator + queryString;
  }

  // Auth via query parameter
  if (request.auth?.type === 'API_KEY' && request.auth.placement === 'QUERY' && request.auth.keyName) {
    const separator = fullUrl.includes('?') ? '&' : '?';
    fullUrl += `${separator}${encodeURIComponent(request.auth.keyName)}=${encodeURIComponent(request.auth.keyValue)}`;
  }

  parts.push(`"${fullUrl}"`);

  // Headers
  request.headers
    .filter((h) => h.enabled && h.key)
    .forEach((h) => {
      parts.push(`-H "${h.key}: ${h.value}"`);
    });

  // Auth via header
  if (request.auth?.type === 'API_KEY' && request.auth.placement === 'HEADER' && request.auth.keyName) {
    parts.push(`-H "${request.auth.keyName}: ${request.auth.keyValue}"`);
  }

  // Body
  if (request.body && ['POST', 'PUT', 'PATCH', 'DELETE'].includes(request.method)) {
    // If not already in headers, add Content-Type
    const hasContentType = request.headers.some(
      (h) => h.enabled && h.key.toLowerCase() === 'content-type'
    );
    if (!hasContentType) {
      parts.push('-H "Content-Type: application/json"');
    }
    const escapedBody = request.body.replace(/"/g, '\\"');
    parts.push(`-d "${escapedBody}"`);
  }

  return parts.join(' ');
};
