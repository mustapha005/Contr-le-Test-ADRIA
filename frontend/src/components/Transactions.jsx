import { useState } from 'react';
import { api, money } from '../api';
import AccountSelect from './AccountSelect';

export default function Transactions({ accounts, transactions, maxAmount, busy, execute }) {
  const [form, setForm] = useState({ accountId: '', amount: '', merchantCategory: 'SHOPPING', country: 'MA' });
  const [filter, setFilter] = useState('');
  const account = accounts.find((a) => a.id === form.accountId);
  const set = (key) => (event) => setForm({ ...form, [key]: event.target.value });
  const label = (id) => accounts.find((a) => a.id === id)?.customerReference || id;
  const visible = transactions.filter((t) => !filter || t.accountId === filter || t.targetAccountId === filter);

  async function submit(event) {
    event.preventDefault();
    if (!account) return;
    if (await execute(() => api.pay({ ...form, currency: account.currency }), 'Le paiement a été enregistré.')) setForm({ ...form, amount: '' });
  }

  return <div className="split">
    <section className="panel">
      <h2>Nouveau paiement</h2>
      <p className="muted">Le montant sera débité du compte sélectionné.</p>
      <form onSubmit={submit}><fieldset disabled={busy}>
        <AccountSelect label="Compte à débiter" accounts={accounts} value={form.accountId} onChange={(id) => setForm({ ...form, accountId: id })} />
        <label>Montant {account && `(${account.currency})`}<input required type="number" min="0.01" max={maxAmount} step="0.01" value={form.amount} onChange={set('amount')} /></label>
        <label>Catégorie<select value={form.merchantCategory} onChange={set('merchantCategory')}><option value="SHOPPING">Achats</option><option value="FOOD">Alimentation</option><option value="TRANSPORT">Transport</option><option value="OTHER">Autre</option></select></label>
        <label>Pays<select value={form.country} onChange={set('country')}><option value="MA">Maroc</option><option value="FR">France</option><option value="US">États-Unis</option></select></label>
        <small className="hint">Plafond par opération : {maxAmount} dans la devise du compte.</small>
        <button type="submit" disabled={!account}>Enregistrer le paiement</button>
      </fieldset></form>
    </section>
    <section className="panel grow">
      <div className="panel-heading"><h2>Historique</h2>
        <select aria-label="Filtrer par compte" value={filter} onChange={(e) => setFilter(e.target.value)}>
          <option value="">Tous les comptes</option>{accounts.map((a) => <option key={a.id} value={a.id}>{a.customerReference} · {a.id.slice(0, 8)}</option>)}
        </select>
      </div>
      <div className="table-wrap"><table>
        <thead><tr><th>Date</th><th>Compte</th><th>Montant</th><th>Type</th></tr></thead>
        <tbody>{visible.map((t) => <tr key={t.id}>
          <td>{new Date(t.createdAt).toLocaleString('fr-FR')}<small className="uuid" title={t.id}>{t.id.slice(0, 8)}</small></td>
          <td>{label(t.accountId)}{t.targetAccountId && <small>Vers {label(t.targetAccountId)}</small>}</td>
          <td className="nowrap">{money(t.amount, t.currency)}</td>
          <td>{t.targetAccountId ? 'Virement' : t.merchantCategory}<small>{t.country}</small></td>
        </tr>)}</tbody>
      </table></div>
      {!visible.length && <p className="empty">Aucune transaction pour le moment.</p>}
    </section>
  </div>;
}
