package ma.adria.transaction.service;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.UUID;
@FeignClient(name="account-service", url="${bank.account-service.url:}")
public interface AccountClient {
    record AccountView(UUID id, String customerReference, String status, String currency, BigDecimal balance) {}
    record Debit(BigDecimal amount, String currency) {}
    @GetMapping("/api/accounts/{id}") AccountView get(@PathVariable UUID id);
    @PostMapping("/internal/accounts/{id}/debit") AccountView debit(@PathVariable UUID id, @RequestBody Debit input);
    @PostMapping("/internal/accounts/transfer") void transfer(@RequestBody TransactionRequests.Transfer input);
}
