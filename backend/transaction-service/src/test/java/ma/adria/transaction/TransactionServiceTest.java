package ma.adria.transaction;

import ma.adria.transaction.domain.BankTransaction;
import ma.adria.transaction.event.TransactionCreated;
import ma.adria.transaction.repository.TransactionRepository;
import ma.adria.transaction.service.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import java.math.BigDecimal;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {
    @Mock TransactionRepository repository;
    @Mock AccountGateway accounts;
    @Mock ApplicationEventPublisher events;
    TransactionService service;
    UUID accountId = UUID.randomUUID();

    @BeforeEach void setup() { service = new TransactionService(repository, accounts, events, new BigDecimal("1000")); }
    private TransactionRequests.Create request(String amount) {
        return new TransactionRequests.Create(accountId, new BigDecimal(amount), "MAD", "SHOPPING", "MA");
    }
    private AccountClient.AccountView account(String status, String currency, String balance) {
        return new AccountClient.AccountView(accountId, "CLIENT", status, currency, new BigDecimal(balance));
    }
    @Test void persistsDebitsAndCreatesEvent() {
        when(accounts.get(accountId)).thenReturn(account("ACTIVE", "MAD", "500"));
        BankTransaction result = service.create(request("250"));
        InOrder order = inOrder(accounts, repository, events);
        order.verify(accounts).get(accountId);
        order.verify(repository).saveAndFlush(result);
        order.verify(accounts).debit(eq(accountId), any());
        ArgumentCaptor<TransactionCreated> event = ArgumentCaptor.forClass(TransactionCreated.class);
        order.verify(events).publishEvent(event.capture());
        assertThat(event.getValue().accountId()).isEqualTo(accountId);
        assertThat(event.getValue().transactionId()).isEqualTo(result.getId());
        assertThat(event.getValue().eventType()).isEqualTo("TransactionCreated");
    }
    @Test void rejectsAmountAboveConfiguredLimitBeforeRemoteCall() {
        assertThatThrownBy(() -> service.create(request("1000.01"))).hasMessageContaining("plafond");
        verifyNoInteractions(accounts, repository, events);
    }
    @Test void acceptsExactLimit() {
        when(accounts.get(accountId)).thenReturn(account("ACTIVE", "MAD", "2000"));
        assertThat(service.create(request("1000")).getAmount()).isEqualByComparingTo("1000");
    }
    @Test void blockedAccountIsRejected() {
        when(accounts.get(accountId)).thenReturn(account("BLOCKED", "MAD", "500"));
        assertThatThrownBy(() -> service.create(request("1"))).hasMessageContaining("bloqué");
        verifyNoInteractions(repository, events);
    }
    @Test void currencyMismatchIsRejected() {
        when(accounts.get(accountId)).thenReturn(account("ACTIVE", "EUR", "500"));
        assertThatThrownBy(() -> service.create(request("1"))).hasMessageContaining("devise");
        verifyNoInteractions(repository, events);
    }
    @Test void insufficientBalanceIsRejected() {
        when(accounts.get(accountId)).thenReturn(account("ACTIVE", "MAD", "10"));
        assertThatThrownBy(() -> service.create(request("11"))).hasMessageContaining("insuffisant");
        verifyNoInteractions(repository, events);
    }
    @Test void unavailableAccountsDoNotCreateTransaction() {
        when(accounts.get(accountId)).thenThrow(new BusinessException(HttpStatus.SERVICE_UNAVAILABLE, "indisponible"));
        assertThatThrownBy(() -> service.create(request("1"))).hasMessageContaining("indisponible");
        verifyNoInteractions(repository, events);
    }
    @Test void failedDatabaseWriteDoesNotDebit() {
        when(accounts.get(accountId)).thenReturn(account("ACTIVE", "MAD", "500"));
        when(repository.saveAndFlush(any())).thenThrow(new IllegalStateException("database unavailable"));
        assertThatThrownBy(() -> service.create(request("1"))).isInstanceOf(IllegalStateException.class);
        verify(accounts, never()).debit(any(), any());
        verifyNoInteractions(events);
    }
    @Test void failedDebitDoesNotPublishEvent() {
        when(accounts.get(accountId)).thenReturn(account("ACTIVE", "MAD", "500"));
        doThrow(new BusinessException("Solde insuffisant.")).when(accounts).debit(any(), any());
        assertThatThrownBy(() -> service.create(request("1"))).isInstanceOf(BusinessException.class);
        verifyNoInteractions(events);
    }
    @Test void transferRecordsBeneficiaryAndUsesAtomicAccountEndpoint() {
        UUID targetId = UUID.randomUUID();
        when(accounts.get(accountId)).thenReturn(account("ACTIVE", "MAD", "500"));
        when(accounts.get(targetId)).thenReturn(new AccountClient.AccountView(targetId, "TARGET", "ACTIVE", "MAD", BigDecimal.ZERO));
        var input = new TransactionRequests.Transfer(accountId, targetId, new BigDecimal("25"), "MAD");
        BankTransaction result = service.transfer(input);
        assertThat(result.getTargetAccountId()).isEqualTo(targetId);
        assertThat(result.getMerchantCategory()).isEqualTo("TRANSFER");
        verify(accounts).transfer(input);
        verify(accounts, never()).debit(any(), any());
    }
}
