import { authHeaders } from './auth';

export async function request(path, options = {}) {
  const response = await fetch(`/api${path}`, {
    ...options,
    headers: { 'Content-Type': 'application/json', ...(await authHeaders()), ...options.headers },
  });
  const text = await response.text();
  let data;
  try { data = text ? JSON.parse(text) : null; } catch { data = null; }
  if (!response.ok) {
    throw new Error(data?.message || `Service indisponible (HTTP ${response.status}). Vérifier que le backend est démarré.`);
  }
  return data;
}

export const api = {
  accounts: () => request('/accounts'),
  transactions: () => request('/transactions'),
  config: () => request('/transactions/config'),
  createAccount: (body) => request('/accounts', { method: 'POST', body: JSON.stringify(body) }),
  changeStatus: (id, status) => request(`/accounts/${id}/status`, { method: 'PATCH', body: JSON.stringify({ status }) }),
  pay: (body) => request('/transactions', { method: 'POST', body: JSON.stringify(body) }),
  transfer: (body) => request('/transactions/transfer', { method: 'POST', body: JSON.stringify(body) }),
  chat: (message) => request('/chat', { method: 'POST', body: JSON.stringify({ message }) }),
};

export function money(value, currency) {
  return new Intl.NumberFormat('fr-MA', { style: 'currency', currency }).format(value);
}
