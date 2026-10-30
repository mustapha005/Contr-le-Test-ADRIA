import Keycloak from 'keycloak-js';

// Sans VITE_KEYCLOAK_URL (LANCER.ps1), l'application reste sans connexion comme avant.
const url = import.meta.env.VITE_KEYCLOAK_URL;
const keycloak = url ? new Keycloak({
  url,
  realm: import.meta.env.VITE_KEYCLOAK_REALM || 'adria-bank',
  clientId: import.meta.env.VITE_KEYCLOAK_CLIENT || 'adria-frontend',
}) : null;

export const authEnabled = Boolean(keycloak);

export async function initAuth() {
  if (keycloak) await keycloak.init({ onLoad: 'login-required', pkceMethod: 'S256', checkLoginIframe: false });
}

export async function authHeaders() {
  if (!keycloak) return {};
  try { await keycloak.updateToken(30); }
  catch { keycloak.login(); throw new Error('Session expirée. Reconnexion…'); }
  return { Authorization: `Bearer ${keycloak.token}` };
}

export function currentUser() {
  const token = keycloak?.tokenParsed;
  return token ? [token.given_name, token.family_name].filter(Boolean).join(' ') || token.preferred_username : null;
}

export function logout() {
  keycloak?.logout({ redirectUri: window.location.origin });
}
