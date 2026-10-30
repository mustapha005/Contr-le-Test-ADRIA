package ma.adria.account.web.rest;
import ma.adria.account.domain.Account;
import ma.adria.account.service.*;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;
// Ces routes internes ne sont pas exposées par la gateway.
@RestController @RequestMapping("/internal/accounts")
public class InternalAccountController {
    private final AccountService service;
    public InternalAccountController(AccountService service) { this.service = service; }
    @PostMapping("/{id}/debit")
    public Account debit(@PathVariable UUID id, @Valid @RequestBody AccountRequests.Debit input) { return service.debit(id, input); }
    @PostMapping("/transfer")
    public void transfer(@Valid @RequestBody AccountRequests.Transfer input) { service.transfer(input); }
}
