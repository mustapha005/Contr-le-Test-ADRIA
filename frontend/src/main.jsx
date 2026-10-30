import React from 'react';
import { createRoot } from 'react-dom/client';
import App from './App.jsx';
import { initAuth } from './auth';
import './styles.css';

const root = createRoot(document.getElementById('root'));
initAuth()
  .then(() => root.render(<React.StrictMode><App /></React.StrictMode>))
  .catch(() => root.render(<main><div className="notice error" role="alert">Service de connexion indisponible. Vérifier que Keycloak est démarré, puis recharger la page.</div></main>));
