package ma.adria.account.web.rest;
import ma.adria.account.domain.Account;
import ma.adria.account.service.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.*;
@RestController @RequestMapping("/api/accounts")
public class AccountController {
    private final AccountService service;
    public AccountController(AccountService service) { this.service = service; }
    @GetMapping public List<Account> list() { return service.list(); }
    @GetMapping("/{id}") public Account get(@PathVariable UUID id) { return service.get(id); }
    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public Account create(@Valid @RequestBody AccountRequests.Create input) { return service.create(input); }
    @PatchMapping("/{id}/status")
    public Account status(@PathVariable UUID id, @Valid @RequestBody AccountRequests.Status input) { return service.changeStatus(id, input.status()); }
}
