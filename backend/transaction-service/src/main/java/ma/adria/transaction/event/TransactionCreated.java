package ma.adria.transaction.event;
import ma.adria.transaction.domain.BankTransaction;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
public record TransactionCreated(UUID eventId, String eventType, UUID transactionId, UUID accountId,
                                 BigDecimal amount, String currency, Instant occurredAt) {
    public static TransactionCreated from(BankTransaction tx) {
        return new TransactionCreated(UUID.randomUUID(), "TransactionCreated", tx.getId(), tx.getAccountId(), tx.getAmount(), tx.getCurrency(), tx.getCreatedAt());
    }
}
