import { useState } from 'react';
import { api } from '../api';

export default function Chat({ onOperation }) {
  const [messages, setMessages] = useState([]);
  const [input, setInput] = useState('');
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  async function submit(event) {
    event.preventDefault();
    const message = input.trim();
    if (!message || busy) return;
    setBusy(true); setError('');
    setMessages((items) => [...items, { role: 'user', text: message }]);
    try {
      const reply = await api.chat(message);
      setMessages((items) => [...items, { role: 'assistant', text: reply.answer }]);
      setInput('');
      await onOperation();
    } catch (e) { setError(e.message); }
    finally { setBusy(false); }
  }
  return <section className="panel chat-panel">
    <h2>Assistant bancaire</h2>
    <p className="muted">Consultez vos comptes ou demandez une opération. Chaque demande doit contenir toutes les informations nécessaires.</p>
    <div className="chat-log" role="log" aria-live="polite">
      {!messages.length && <div className="empty"><p>Commencez par consulter vos comptes.</p><button className="secondary" onClick={() => setInput('Liste mes comptes avec leurs soldes et identifiants.')}>Liste mes comptes avec leurs soldes</button></div>}
      {messages.map((m, i) => <div className={`chat-message ${m.role}`} key={i}><strong>{m.role === 'user' ? 'Vous' : 'Assistant'}</strong><p>{m.text}</p></div>)}
      {busy && <p className="muted">L’assistant consulte les services…</p>}
    </div>
    {error && <p className="notice error" role="alert">{error}</p>}
    <form onSubmit={submit} className="chat-form">
      <label className="sr-only" htmlFor="chat-input">Votre demande</label>
      <input id="chat-input" required maxLength={2000} value={input} onChange={(e) => setInput(e.target.value)} placeholder="Ex. : Quel est le solde du compte CLIENT-001 ?" disabled={busy} />
      <button disabled={busy || !input.trim()}>{busy ? 'En cours…' : 'Envoyer'}</button>
    </form>
  </section>;
}
