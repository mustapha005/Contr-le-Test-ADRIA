package ma.adria.account.mcp;
import ma.adria.account.domain.Account;
import ma.adria.account.service.*;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.*;
import java.util.*;
@Configuration(proxyBeanMethods = false)
public class AccountTools {
    private final AccountService service;
    public AccountTools(AccountService service) { this.service = service; }
    @Tool(description="Lister les comptes bancaires et leurs soldes.")
    public List<Account> listAccounts() { return service.list(); }
    @Tool(description="Consulter le solde, la devise et le statut d'un compte par son UUID.")
    public Account getAccount(String accountId) { return service.get(UUID.fromString(accountId)); }
    @Bean ToolCallbackProvider accountToolCallbacks() {
        return MethodToolCallbackProvider.builder().toolObjects(this).build();
    }
}
