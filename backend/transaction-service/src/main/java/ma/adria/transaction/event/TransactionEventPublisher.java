package ma.adria.transaction.event;
import org.slf4j.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.*;
@Component
public class TransactionEventPublisher {
    public static final String TOPIC = "bank.transactions.created";
    private static final Logger log = LoggerFactory.getLogger(TransactionEventPublisher.class);
    private final KafkaTemplate<String, Object> kafka;
    private final boolean enabled;
    public TransactionEventPublisher(KafkaTemplate<String, Object> kafka, @Value("${bank.kafka.enabled:false}") boolean enabled) {
        this.kafka = kafka; this.enabled = enabled;
    }
    @TransactionalEventListener(phase=TransactionPhase.AFTER_COMMIT)
    public void publish(TransactionCreated event) {
        if (!enabled) { log.info("Kafka désactivé : transaction {} enregistrée", event.transactionId()); return; }
        try {
            kafka.send(TOPIC, event.accountId().toString(), event).whenComplete((result, error) -> {
                if (error == null) log.info("Événement {} publié, compte {}", event.eventId(), event.accountId());
                else log.error("Échec de publication de l'événement {}", event.eventId(), error);
            });
        } catch (Exception error) {
            // La transaction SQL est déjà validée : ne pas retourner une fausse erreur au client.
            log.error("Transaction enregistrée mais événement {} non publié", event.eventId(), error);
        }
    }
}
