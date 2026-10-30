import { useState } from 'react';
import { api, money } from '../api';
import AccountSelect from './AccountSelect';

export default function Transfer({ accounts, maxAmount, busy, execute }) {
  const [form, setForm] = useState({ sourceAccountId: '', targetAccountId: '', amount: '' });
  const source = accounts.find((a) => a.id === form.sourceAccountId);
  const target = accounts.find((a) => a.id === form.targetAccountId);
  const targets = accounts.filter((a) => !source || a.currency === source.currency);

  async function submit(event) {
    event.preventDefault();
    if (!source || !target) return;
    if (await execute(() => api.transfer({ ...form, currency: source.currency }), 'Le virement a été effectué.')) setForm({ ...form, amount: '' });
  }

  return <div className="split transfer-layout">
    <section className="panel">
      <h2>Effectuer un virement</h2>
      <p className="muted">Transférer un montant entre deux comptes actifs de même devise.</p>
      <form onSubmit={submit}><fieldset disabled={busy}>
        <AccountSelect label="Compte source" accounts={accounts} value={form.sourceAccountId} onChange={(id) => setForm({ ...form, sourceAccountId: id, targetAccountId: '' })} />
        <AccountSelect label="Compte bénéficiaire" accounts={targets} exclude={form.sourceAccountId} value={form.targetAccountId} onChange={(id) => setForm({ ...form, targetAccountId: id })} disabled={!source} />
        <label>Montant {source && `(${source.currency})`}<input required type="number" min="0.01" max={maxAmount} step="0.01" value={form.amount} onChange={(e) => setForm({ ...form, amount: e.target.value })} /></label>
        <small className="hint">Plafond : {maxAmount}. Aucun frais dans cette démonstration.</small>
        <button type="submit" disabled={!source || !target}>Valider le virement</button>
      </fieldset></form>
    </section>
    <aside className="panel transfer-summary">
      <h2>Récapitulatif</h2>
      <p className="muted">De</p><strong>{source?.customerReference || 'Compte source à sélectionner'}</strong>
      <p className="arrow" aria-hidden="true">↓</p>
      <p className="muted">Vers</p><strong>{target?.customerReference || 'Bénéficiaire à sélectionner'}</strong>
      <hr />
      <p className="muted">Montant du virement</p>
      <p className="large-amount">{source ? money(form.amount || 0, source.currency) : '—'}</p>
    </aside>
  </div>;
}
