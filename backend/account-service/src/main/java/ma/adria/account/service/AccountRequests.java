package ma.adria.account.service;
import jakarta.validation.constraints.*;
import ma.adria.account.domain.AccountStatus;
import java.math.BigDecimal;
import java.util.UUID;
public final class AccountRequests {
    private AccountRequests() {}
    public record Create(@NotBlank @Size(max=100) String customerReference,
                         @NotNull @Pattern(regexp="[A-Z]{3}") String currency,
                         @NotNull @DecimalMin("0") @Digits(integer=15, fraction=2) BigDecimal balance) {}
    public record Status(@NotNull AccountStatus status) {}
    public record Debit(@NotNull @DecimalMin(value="0", inclusive=false) @Digits(integer=15, fraction=2) BigDecimal amount,
                        @NotNull @Pattern(regexp="[A-Z]{3}") String currency) {}
    public record Transfer(@NotNull UUID sourceAccountId, @NotNull UUID targetAccountId,
                           @NotNull @DecimalMin(value="0", inclusive=false) @Digits(integer=15, fraction=2) BigDecimal amount,
                           @NotNull @Pattern(regexp="[A-Z]{3}") String currency) {}
}
