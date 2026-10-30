package ma.adria.transaction.web.rest;
import ma.adria.transaction.domain.BankTransaction;
import ma.adria.transaction.service.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.*;
@RestController @RequestMapping("/api/transactions")
public class TransactionController {
    private final TransactionService service;
    public TransactionController(TransactionService service) { this.service = service; }
    @GetMapping public List<BankTransaction> list(@RequestParam(required=false) UUID accountId) { return service.list(accountId); }
    @GetMapping("/{id}") public BankTransaction get(@PathVariable UUID id) { return service.get(id); }
    @GetMapping("/config") public Map<String, BigDecimal> config() { return Map.of("maxAmount", service.maxAmount()); }
    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public BankTransaction create(@Valid @RequestBody TransactionRequests.Create input) { return service.create(input); }
    @PostMapping("/transfer") @ResponseStatus(HttpStatus.CREATED)
    public BankTransaction transfer(@Valid @RequestBody TransactionRequests.Transfer input) { return service.transfer(input); }
}
