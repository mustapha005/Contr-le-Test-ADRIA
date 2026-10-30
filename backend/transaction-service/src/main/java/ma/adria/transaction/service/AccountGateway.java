package ma.adria.transaction.service;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import java.util.UUID;
@Service
public class AccountGateway {
    private final AccountClient client;
    private final ObjectMapper mapper;
    public AccountGateway(AccountClient client, ObjectMapper mapper) { this.client = client; this.mapper = mapper; }
    @CircuitBreaker(name="accounts", fallbackMethod="readFailure")
    public AccountClient.AccountView get(UUID id) { return client.get(id); }
    @CircuitBreaker(name="accounts", fallbackMethod="debitFailure")
    public void debit(UUID id, AccountClient.Debit input) { client.debit(id, input); }
    @CircuitBreaker(name="accounts", fallbackMethod="transferFailure")
    public void transfer(TransactionRequests.Transfer input) { client.transfer(input); }
    private AccountClient.AccountView readFailure(UUID id, Throwable error) { throw translate(error); }
    private void debitFailure(UUID id, AccountClient.Debit input, Throwable error) { throw translate(error); }
    private void transferFailure(TransactionRequests.Transfer input, Throwable error) { throw translate(error); }
    private BusinessException translate(Throwable error) {
        if (error instanceof FeignException e && e.status() >= 400 && e.status() < 500) {
            String message = "Opération refusée par le service des comptes.";
            try { message = mapper.readTree(e.contentUTF8()).path("message").asText(message); } catch (Exception ignored) { }
            return new BusinessException(HttpStatus.valueOf(e.status()), message);
        }
        // Ne jamais inventer un solde dans un fallback ni rejouer automatiquement un débit.
        return new BusinessException(HttpStatus.SERVICE_UNAVAILABLE,
            "Service des comptes indisponible. Vérifier le solde avant de réessayer une opération interrompue.");
    }
}
