package ma.adria.transaction.domain;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
@Entity @Table(name="bank_transactions") @Getter @Setter @NoArgsConstructor
public class BankTransaction {
    @Id private UUID id;
    @Column(nullable=false) private UUID accountId;
    private UUID targetAccountId;
    @Column(nullable=false, precision=19, scale=2) private BigDecimal amount;
    @Column(nullable=false, length=3) private String currency;
    @Column(nullable=false) private String merchantCategory;
    @Column(nullable=false) private String country;
    @Column(nullable=false) private Instant createdAt;
    public BankTransaction(UUID accountId, UUID targetAccountId, BigDecimal amount, String currency, String merchantCategory, String country) {
        this.id = UUID.randomUUID(); this.accountId = accountId; this.targetAccountId = targetAccountId;
        this.amount = amount; this.currency = currency; this.merchantCategory = merchantCategory;
        this.country = country; this.createdAt = Instant.now();
    }
}
