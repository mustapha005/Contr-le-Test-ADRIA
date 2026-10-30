package ma.adria.account.web.graphql;
import ma.adria.account.domain.*;
import ma.adria.account.service.*;
import org.springframework.graphql.data.method.annotation.*;
import org.springframework.stereotype.Controller;
import java.util.*;
@Controller
public class AccountGraphqlController {
    private final AccountService service;
    public AccountGraphqlController(AccountService service) { this.service = service; }
    @QueryMapping public List<Account> accounts() { return service.list(); }
    @QueryMapping public Account account(@Argument UUID id) { return service.get(id); }
    @MutationMapping public Account createAccount(@Argument AccountRequests.Create input) { return service.create(input); }
    @MutationMapping public Account changeAccountStatus(@Argument UUID id, @Argument AccountStatus status) { return service.changeStatus(id, status); }
}
