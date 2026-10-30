package ma.adria.transaction.repository;
import ma.adria.transaction.domain.BankTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface TransactionRepository extends JpaRepository<BankTransaction, UUID> {
    List<BankTransaction> findAllByOrderByCreatedAtDesc();
    List<BankTransaction> findByAccountIdOrTargetAccountIdOrderByCreatedAtDesc(UUID accountId, UUID targetAccountId);
}
