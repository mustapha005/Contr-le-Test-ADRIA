package ma.adria.transaction.service;
import ma.adria.transaction.domain.BankTransaction;
import ma.adria.transaction.repository.TransactionRepository;
import ma.adria.transaction.event.TransactionCreated;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import java.math.BigDecimal;
import java.util.*;
@Service @Validated @Transactional
public class TransactionService {
    private final TransactionRepository repository;
    private final AccountGateway accounts;
    private final ApplicationEventPublisher events;
    private final BigDecimal maxAmount;
    public TransactionService(TransactionRepository repository, AccountGateway accounts,
            ApplicationEventPublisher events, @Value("${bank.transaction.max-amount:1000}") BigDecimal maxAmount) {
        this.repository = repository; this.accounts = accounts; this.events = events; this.maxAmount = maxAmount;
    }
    @Transactional(readOnly=true)
    public List<BankTransaction> list(UUID accountId) {
        return accountId == null ? repository.findAllByOrderByCreatedAtDesc()
            : repository.findByAccountIdOrTargetAccountIdOrderByCreatedAtDesc(accountId, accountId);
    }
    @Transactional(readOnly=true)
    public BankTransaction get(@NotNull UUID id) {
        return repository.findById(id).orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Transaction introuvable."));
    }
    public BankTransaction create(@NotNull @Valid TransactionRequests.Create input) {
        checkAmount(input.amount());
        checkSource(accounts.get(input.accountId()), input.amount(), input.currency());
        BankTransaction tx = new BankTransaction(input.accountId(), null, input.amount(), input.currency(), input.merchantCategory(), input.country());
        // Flush avant le débit distant : les erreurs SQL ordinaires ne débitent pas le compte.
        repository.saveAndFlush(tx);
        accounts.debit(input.accountId(), new AccountClient.Debit(input.amount(), input.currency()));
        events.publishEvent(TransactionCreated.from(tx));
        return tx;
    }
    public BankTransaction transfer(@NotNull @Valid TransactionRequests.Transfer input) {
        checkAmount(input.amount());
        if (input.sourceAccountId().equals(input.targetAccountId())) throw new BusinessException("Choisir deux comptes différents.");
        checkSource(accounts.get(input.sourceAccountId()), input.amount(), input.currency());
        AccountClient.AccountView target = accounts.get(input.targetAccountId());
        if (!"ACTIVE".equals(target.status())) throw new BusinessException("Le compte bénéficiaire est bloqué.");
        if (!input.currency().equals(target.currency())) throw new BusinessException("Les deux comptes doivent avoir la même devise.");
        BankTransaction tx = new BankTransaction(input.sourceAccountId(), input.targetAccountId(), input.amount(), input.currency(), "TRANSFER", "MA");
        repository.saveAndFlush(tx);
        // Les deux soldes sont modifiés dans une seule transaction de account-service.
        accounts.transfer(input);
        events.publishEvent(TransactionCreated.from(tx));
        return tx;
    }
    private void checkAmount(BigDecimal amount) {
        if (amount.compareTo(maxAmount) > 0) throw new BusinessException("Le montant dépasse le plafond de " + maxAmount + ".");
    }
    private void checkSource(AccountClient.AccountView account, BigDecimal amount, String currency) {
        if (!"ACTIVE".equals(account.status())) throw new BusinessException("Le compte est bloqué.");
        if (!currency.equals(account.currency())) throw new BusinessException("La devise ne correspond pas au compte.");
        if (account.balance().compareTo(amount) < 0) throw new BusinessException("Solde insuffisant.");
    }
    public BigDecimal maxAmount() { return maxAmount; }
}
