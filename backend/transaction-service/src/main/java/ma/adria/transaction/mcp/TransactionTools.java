package ma.adria.transaction.mcp;
import ma.adria.transaction.domain.BankTransaction;
import ma.adria.transaction.service.*;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.*;
import java.math.BigDecimal;
import java.util.*;
@Configuration(proxyBeanMethods = false)
public class TransactionTools {
    private final TransactionService service;
    public TransactionTools(TransactionService service) { this.service = service; }
    @Tool(description="Lister les transactions d'un compte bancaire identifié par son UUID.")
    public List<BankTransaction> listTransactions(String accountId) { return service.list(UUID.fromString(accountId)); }
    @Tool(description="Enregistrer un paiement et débiter le compte. Uniquement sur demande explicite de l'utilisateur avec montant, devise, catégorie et pays.")
    public BankTransaction createTransaction(String accountId, BigDecimal amount, String currency, String merchantCategory, String country) {
        return service.create(new TransactionRequests.Create(UUID.fromString(accountId), amount, currency, merchantCategory, country));
    }
    @Tool(description="Effectuer un virement entre deux comptes. Exige une demande explicite avec comptes source et bénéficiaire, montant et devise.")
    public BankTransaction transferMoney(String sourceAccountId, String targetAccountId, BigDecimal amount, String currency) {
        return service.transfer(new TransactionRequests.Transfer(UUID.fromString(sourceAccountId), UUID.fromString(targetAccountId), amount, currency));
    }
    @Bean ToolCallbackProvider transactionToolCallbacks() {
        return MethodToolCallbackProvider.builder().toolObjects(this).build();
    }
}
