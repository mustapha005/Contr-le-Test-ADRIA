import { useEffect, useState } from 'react';
import { api } from './api';
import Accounts from './components/Accounts';
import Transactions from './components/Transactions';
import Transfer from './components/Transfer';
import Chat from './components/Chat';

const tabs = ['Comptes', 'Transactions', 'Virement', 'Assistant'];

export default function App() {
  const [tab, setTab] = useState('Comptes');
  const [accounts, setAccounts] = useState([]);
  const [transactions, setTransactions] = useState([]);
  const [maxAmount, setMaxAmount] = useState(1000);
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [notice, setNotice] = useState(null);

  async function refresh() {
    const [a, t, config] = await Promise.all([api.accounts(), api.transactions(), api.config()]);
    setAccounts(a); setTransactions(t); setMaxAmount(config.maxAmount);
  }
  async function reload() {
    setLoading(true); setNotice(null);
    try { await refresh(); } catch (e) { setNotice({ type: 'error', text: e.message }); }
    finally { setLoading(false); }
  }
  useEffect(() => { reload(); }, []);

  async function execute(operation, message) {
    setBusy(true); setNotice(null);
    try { await operation(); }
    catch (e) { setNotice({ type: 'error', text: e.message }); setBusy(false); return false; }
    setNotice({ type: 'success', text: message });
    try { await refresh(); }
    catch { setNotice({ type: 'error', text: `${message} L’actualisation a échoué ; cliquez sur Actualiser avant une nouvelle opération.` }); }
    finally { setBusy(false); }
    return true;
  }

  return <>
    <header className="topbar"><div className="brand"><span className="brand-mark">A</span><div>ADRIA <strong>Bank</strong><small>Gestion bancaire · TP</small></div></div><span className="demo-label">Environnement de démonstration</span></header>
    <main>
      <div className="page-heading"><div><p className="eyebrow">ESPACE BANCAIRE</p><h1>Comptes & opérations</h1><p className="muted">Consultez les soldes et gérez les opérations bancaires.</p></div><button className="secondary" onClick={reload} disabled={loading || busy}>{loading ? 'Chargement…' : 'Actualiser'}</button></div>
      <div className="stats"><div><span>Comptes</span><strong>{accounts.length}</strong></div><div><span>Comptes actifs</span><strong>{accounts.filter((a) => a.status === 'ACTIVE').length}</strong></div><div><span>Transactions</span><strong>{transactions.length}</strong></div><div><span>Plafond par opération</span><strong>{maxAmount.toLocaleString('fr-FR')} <small>/ devise</small></strong></div></div>
      <nav aria-label="Navigation principale">{tabs.map((name) => <button key={name} className={tab === name ? 'selected' : ''} aria-current={tab === name ? 'page' : undefined} onClick={() => { setTab(name); setNotice(null); }}>{name}</button>)}</nav>
      {notice && <div className={`notice ${notice.type}`} role={notice.type === 'error' ? 'alert' : 'status'}>{notice.text}</div>}
      {tab === 'Comptes' && <Accounts accounts={accounts} busy={busy || loading} execute={execute} />}
      {tab === 'Transactions' && <Transactions accounts={accounts} transactions={transactions} maxAmount={maxAmount} busy={busy || loading} execute={execute} />}
      {tab === 'Virement' && <Transfer accounts={accounts} maxAmount={maxAmount} busy={busy || loading} execute={execute} />}
      {tab === 'Assistant' && <Chat onOperation={async () => { try { await refresh(); } catch { setNotice({ type: 'error', text: 'Actualisation impossible. Vérifiez les comptes avant une nouvelle opération.' }); } }} />}
    </main>
    <footer>ADRIA Bank · Application de TP · Données de démonstration</footer>
  </>;
}
