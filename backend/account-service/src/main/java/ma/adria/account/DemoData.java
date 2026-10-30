package ma.adria.account;
import ma.adria.account.service.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import java.math.BigDecimal;
@Configuration
public class DemoData {
    @Bean @ConditionalOnProperty(name="bank.demo-data", havingValue="true", matchIfMissing=true)
    CommandLineRunner seed(AccountService service) {
        return args -> {
            if (service.list().isEmpty()) {
                service.create(new AccountRequests.Create("CLIENT-001", "MAD", new BigDecimal("5000.00")));
                service.create(new AccountRequests.Create("CLIENT-002", "MAD", new BigDecimal("2500.00")));
                service.create(new AccountRequests.Create("CLIENT-003", "EUR", new BigDecimal("1200.00")));
            }
        };
    }
}
