package ma.adria.account;

import ma.adria.account.domain.*;
import ma.adria.account.repository.AccountRepository;
import ma.adria.account.service.*;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import java.math.BigDecimal;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest(properties = {
    "spring.cloud.config.enabled=false", "eureka.client.enabled=false",
    "spring.ai.mcp.server.enabled=false", "bank.demo-data=false"
})
class AccountServiceTest {
    @Autowired AccountService service;
    @Autowired AccountRepository repository;
    private Account source;
    private Account target;

    @BeforeEach void setup() {
        repository.deleteAll();
        source = create("SOURCE", "MAD", "500.00");
        target = create("TARGET", "MAD", "100.00");
    }
    private Account create(String name, String currency, String balance) {
        return service.create(new AccountRequests.Create(name, currency, new BigDecimal(balance)));
    }
    private AccountRequests.Transfer transfer(String amount) {
        return new AccountRequests.Transfer(source.getId(), target.getId(), new BigDecimal(amount), "MAD");
    }
    @Test void debitUpdatesBalanceExactly() {
        service.debit(source.getId(), new AccountRequests.Debit(new BigDecimal("0.10"), "MAD"));
        assertThat(service.get(source.getId()).getBalance()).isEqualByComparingTo("499.90");
    }
    @Test void transferPreservesTotalBalance() {
        service.transfer(transfer("125.50"));
        assertThat(service.get(source.getId()).getBalance()).isEqualByComparingTo("374.50");
        assertThat(service.get(target.getId()).getBalance()).isEqualByComparingTo("225.50");
    }
    @Test void insufficientFundsLeaveBothAccountsUnchanged() {
        assertThatThrownBy(() -> service.transfer(transfer("501"))).isInstanceOf(BusinessException.class).hasMessageContaining("insuffisant");
        assertThat(service.get(source.getId()).getBalance()).isEqualByComparingTo("500");
        assertThat(service.get(target.getId()).getBalance()).isEqualByComparingTo("100");
    }
    @Test void blockedTargetDoesNotDebitSource() {
        service.changeStatus(target.getId(), AccountStatus.BLOCKED);
        assertThatThrownBy(() -> service.transfer(transfer("10"))).hasMessageContaining("bloqué");
        assertThat(service.get(source.getId()).getBalance()).isEqualByComparingTo("500");
    }
    @Test void blockedSourceCannotPay() {
        service.changeStatus(source.getId(), AccountStatus.BLOCKED);
        assertThatThrownBy(() -> service.debit(source.getId(), new AccountRequests.Debit(BigDecimal.ONE, "MAD"))).hasMessageContaining("bloqué");
    }
    @Test void rejectsDifferentCurrency() {
        assertThatThrownBy(() -> service.debit(source.getId(), new AccountRequests.Debit(BigDecimal.ONE, "EUR"))).hasMessageContaining("devise");
    }
    @Test void rejectsLimitExceeded() {
        assertThatThrownBy(() -> service.debit(source.getId(), new AccountRequests.Debit(new BigDecimal("1000.01"), "MAD"))).hasMessageContaining("plafond");
    }
    @Test void rejectsZeroAndNegativeAmounts() {
        for (String amount : new String[]{"0", "-1"}) {
            assertThatThrownBy(() -> service.debit(source.getId(), new AccountRequests.Debit(new BigDecimal(amount), "MAD"))).isInstanceOf(ConstraintViolationException.class);
        }
    }
    @Test void rejectsFractionBeyondCents() {
        assertThatThrownBy(() -> service.debit(source.getId(), new AccountRequests.Debit(new BigDecimal("0.001"), "MAD"))).isInstanceOf(ConstraintViolationException.class);
    }
    @Test void rejectsSameAccount() {
        assertThatThrownBy(() -> service.transfer(new AccountRequests.Transfer(source.getId(), source.getId(), BigDecimal.ONE, "MAD"))).hasMessageContaining("différents");
    }
    @Test void missingAccountIsReported() {
        assertThatThrownBy(() -> service.get(UUID.randomUUID())).isInstanceOf(BusinessException.class).hasMessageContaining("introuvable");
    }
}
