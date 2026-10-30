package ma.adria.account.domain;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.util.UUID;
@Entity @Table(name = "accounts") @Getter @Setter @NoArgsConstructor
public class Account {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @Column(nullable = false) private String customerReference;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private AccountStatus status;
    @Column(nullable = false, length = 3) private String currency;
    // BigDecimal évite les erreurs d'arrondi de double sur les montants.
    @Column(nullable = false, precision = 19, scale = 2) private BigDecimal balance;
    @Version private Long version;
    public Account(String customerReference, String currency, BigDecimal balance) {
        this.customerReference = customerReference; this.currency = currency;
        this.balance = balance; this.status = AccountStatus.ACTIVE;
    }
}
