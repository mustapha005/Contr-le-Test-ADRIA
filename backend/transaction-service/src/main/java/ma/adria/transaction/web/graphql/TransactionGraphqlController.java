package ma.adria.transaction.web.graphql;
import ma.adria.transaction.domain.BankTransaction;
import ma.adria.transaction.service.*;
import org.springframework.graphql.data.method.annotation.*;
import org.springframework.stereotype.Controller;
import java.util.*;
@Controller
public class TransactionGraphqlController {
    private final TransactionService service;
    public TransactionGraphqlController(TransactionService service) { this.service = service; }
    @QueryMapping public List<BankTransaction> transactions(@Argument UUID accountId) { return service.list(accountId); }
    @QueryMapping public BankTransaction transaction(@Argument UUID id) { return service.get(id); }
    @MutationMapping public BankTransaction createTransaction(@Argument TransactionRequests.Create input) { return service.create(input); }
    @MutationMapping public BankTransaction transfer(@Argument TransactionRequests.Transfer input) { return service.transfer(input); }
}
