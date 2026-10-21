const DEFAULT_API_BASE = import.meta.env?.VITE_NABTA_API_BASE || 'http://localhost:8080/api';
const TOKEN_KEY = 'nabta_auth_token';
const USER_KEY = 'nabta_auth_user';

export function getApiBase() {
  // API destination is selected by the developer/build environment, not by a customer setting.
  return String(DEFAULT_API_BASE).trim().replace(/\/+$/, '');
}
export function getToken() { return localStorage.getItem(TOKEN_KEY) || ''; }
export function getUser() {
  try { return JSON.parse(localStorage.getItem(USER_KEY) || 'null'); } catch { return null; }
}
export function setSession(data) {
  if (!data?.token || !data?.user) throw new Error('The server response did not contain a login token and user.');
  localStorage.setItem(TOKEN_KEY, data.token);
  localStorage.setItem(USER_KEY, JSON.stringify(data.user));
}
export function clearSession() { localStorage.removeItem(TOKEN_KEY); localStorage.removeItem(USER_KEY); }

export async function api(path, options = {}) {
  const headers = new Headers(options.headers || {});
  const token = getToken();
  if (token) headers.set('Authorization', `Bearer ${token}`);
  if (options.body !== undefined && !headers.has('Content-Type')) headers.set('Content-Type', 'application/json');
  let response;
  try {
    response = await fetch(`${getApiBase()}${path.startsWith('/') ? path : `/${path}`}`, {
      ...options,
      headers,
      body: options.body === undefined || typeof options.body === 'string' ? options.body : JSON.stringify(options.body)
    });
  } catch (error) {
    throw new Error(`Cannot reach the Nabta API at ${getApiBase()}. Check that the backend is running and the API address is correct.`);
  }
  if (response.status === 401) {
    const detail = await parseResponse(response);
    if (getToken()) {
      clearSession();
      window.dispatchEvent(new CustomEvent('nabta:session-expired'));
    }
    throw new Error(detail.message || 'Please sign in to continue.');
  }
  if (response.status === 204) return null;
  const data = await parseResponse(response);
  if (!response.ok) throw new Error(data?.message || data?.error || `Request failed (${response.status}).`);
  return data;
}
async function parseResponse(response) {
  const text = await response.text();
  if (!text) return null;
  try { return JSON.parse(text); } catch { return { message: text }; }
}
export async function loadPage(path) {
  const data = await api(path);
  if (Array.isArray(data)) return data;
  if (Array.isArray(data?.content)) return data.content;
  return [];
}
