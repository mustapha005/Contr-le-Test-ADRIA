package ma.adria.account.service;
import ma.adria.account.domain.*;
import ma.adria.account.repository.AccountRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import java.math.BigDecimal;
import java.util.*;
@Service @Validated @Transactional
public class AccountService {
    private final AccountRepository repository;
    private final BigDecimal maxAmount;
    public AccountService(AccountRepository repository, @Value("${bank.transaction.max-amount:1000}") BigDecimal maxAmount) {
        this.repository = repository; this.maxAmount = maxAmount;
    }
    @Transactional(readOnly = true)
    public List<Account> list() { return repository.findAll(); }
    @Transactional(readOnly = true)
    public Account get(@NotNull UUID id) { return repository.findById(id).orElseThrow(() -> missing(id)); }
    public Account create(@NotNull @Valid AccountRequests.Create input) {
        return repository.save(new Account(input.customerReference().trim(), input.currency(), input.balance()));
    }
    public Account changeStatus(@NotNull UUID id, @NotNull AccountStatus status) {
        Account account = locked(id); account.setStatus(status); return account;
    }
    public Account debit(@NotNull UUID id, @NotNull @Valid AccountRequests.Debit input) {
        Account account = locked(id);
        check(account, input.amount(), input.currency(), true);
        account.setBalance(account.getBalance().subtract(input.amount()));
        return account;
    }
    public void transfer(@NotNull @Valid AccountRequests.Transfer input) {
        if (input.sourceAccountId().equals(input.targetAccountId())) throw new BusinessException("Choisir deux comptes différents.");
        // Ordre de verrouillage stable : évite les interblocages entre virements inverses.
        UUID first = input.sourceAccountId().compareTo(input.targetAccountId()) < 0 ? input.sourceAccountId() : input.targetAccountId();
        UUID second = first.equals(input.sourceAccountId()) ? input.targetAccountId() : input.sourceAccountId();
        Account a = locked(first); Account b = locked(second);
        Account source = a.getId().equals(input.sourceAccountId()) ? a : b;
        Account target = source == a ? b : a;
        check(source, input.amount(), input.currency(), true);
        check(target, input.amount(), input.currency(), false);
        source.setBalance(source.getBalance().subtract(input.amount()));
        target.setBalance(target.getBalance().add(input.amount()));
    }
    private void check(Account a, BigDecimal amount, String currency, boolean debit) {
        if (amount.compareTo(maxAmount) > 0) throw new BusinessException("Le montant dépasse le plafond de " + maxAmount + ".");
        if (a.getStatus() != AccountStatus.ACTIVE) throw new BusinessException("Le compte est bloqué.");
        if (!a.getCurrency().equals(currency)) throw new BusinessException("La devise ne correspond pas au compte.");
        if (debit && a.getBalance().compareTo(amount) < 0) throw new BusinessException("Solde insuffisant.");
    }
    private Account locked(UUID id) { return repository.findLockedById(id).orElseThrow(() -> missing(id)); }
    private BusinessException missing(UUID id) { return new BusinessException(HttpStatus.NOT_FOUND, "Compte introuvable : " + id); }
}
