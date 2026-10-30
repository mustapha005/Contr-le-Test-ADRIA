import { money } from '../api';

export default function AccountSelect({ label, accounts, value, onChange, exclude, disabled }) {
  return <label>{label}
    <select required value={value} onChange={(event) => onChange(event.target.value)} disabled={disabled}>
      <option value="">Sélectionner un compte</option>
      {accounts.filter((a) => a.id !== exclude && a.status === 'ACTIVE').map((a) =>
        <option key={a.id} value={a.id}>{a.customerReference} · {money(a.balance, a.currency)} · {a.id.slice(0, 8)}</option>
      )}
    </select>
  </label>;
}
