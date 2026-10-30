import { useState } from 'react';
import { api, money } from '../api';

export default function Accounts({ accounts, busy, execute }) {
  const [form, setForm] = useState({ customerReference: '', currency: 'MAD', balance: '0' });
  const set = (key) => (event) => setForm({ ...form, [key]: event.target.value });

  async function submit(event) {
    event.preventDefault();
    if (await execute(() => api.createAccount(form), 'Le compte a été créé.')) {
      setForm({ customerReference: '', currency: 'MAD', balance: '0' });
    }
  }

  return <div className="split">
    <section className="panel">
      <h2>Nouveau compte</h2>
      <p className="muted">Créer un compte avec son solde initial.</p>
      <form onSubmit={submit}>
        <fieldset disabled={busy}>
          <label>Référence client<input required maxLength={100} value={form.customerReference} onChange={set('customerReference')} placeholder="CLIENT-004" /></label>
          <label>Devise<select value={form.currency} onChange={set('currency')}><option>MAD</option><option>EUR</option><option>USD</option></select></label>
          <label>Solde initial<input required type="number" min="0" step="0.01" max="999999999999999" value={form.balance} onChange={set('balance')} /></label>
          <button type="submit">Créer le compte</button>
        </fieldset>
      </form>
    </section>
    <section className="panel grow">
      <h2>Liste des comptes <span className="count">{accounts.length}</span></h2>
      <div className="table-wrap"><table>
        <thead><tr><th>Client / identifiant</th><th>Solde</th><th>Statut</th><th>Action</th></tr></thead>
        <tbody>{accounts.map((a) => <tr key={a.id}>
          <td><strong>{a.customerReference}</strong><small className="uuid">{a.id}</small></td>
          <td className="nowrap">{money(a.balance, a.currency)}</td>
          <td><span className={`badge ${a.status.toLowerCase()}`}>{a.status === 'ACTIVE' ? 'Actif' : 'Bloqué'}</span></td>
          <td><button className="secondary compact" disabled={busy} onClick={() => execute(() => api.changeStatus(a.id, a.status === 'ACTIVE' ? 'BLOCKED' : 'ACTIVE'), 'Le statut a été modifié.')}>{a.status === 'ACTIVE' ? 'Bloquer' : 'Activer'}</button></td>
        </tr>)}</tbody>
      </table></div>
      {!accounts.length && <p className="empty">Aucun compte disponible.</p>}
    </section>
  </div>;
}
