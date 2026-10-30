package ma.adria.transaction.service;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.UUID;
public final class TransactionRequests {
    private TransactionRequests() {}
    public record Create(@NotNull UUID accountId,
                         @NotNull @DecimalMin(value="0", inclusive=false) @Digits(integer=15, fraction=2) BigDecimal amount,
                         @NotNull @Pattern(regexp="[A-Z]{3}") String currency,
                         @NotBlank @Size(max=80) String merchantCategory,
                         @NotNull @Pattern(regexp="[A-Z]{2}") String country) {}
    public record Transfer(@NotNull UUID sourceAccountId, @NotNull UUID targetAccountId,
                           @NotNull @DecimalMin(value="0", inclusive=false) @Digits(integer=15, fraction=2) BigDecimal amount,
                           @NotNull @Pattern(regexp="[A-Z]{3}") String currency) {}
}
