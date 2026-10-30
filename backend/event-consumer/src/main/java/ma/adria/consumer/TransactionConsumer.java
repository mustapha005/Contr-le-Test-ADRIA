package ma.adria.consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.*;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
@Component
public class TransactionConsumer {
    private static final Logger log = LoggerFactory.getLogger(TransactionConsumer.class);
    @KafkaListener(topics="bank.transactions.created")
    public void receive(ConsumerRecord<String, String> record) {
        // Audit uniquement : aucune modification des comptes ni des transactions.
        log.info("Transaction reçue | accountId={} | événement={}", record.key(), record.value());
    }
}
