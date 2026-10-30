package ma.adria.transaction;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
@SpringBootApplication
@org.springframework.cloud.openfeign.EnableFeignClients
public class TransactionApplication {
    public static void main(String[] args) { SpringApplication.run(TransactionApplication.class, args); }
}
