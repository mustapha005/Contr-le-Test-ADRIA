package ma.adria.transaction.event;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.*;
import org.springframework.kafka.config.TopicBuilder;
@Configuration @ConditionalOnProperty(name="bank.kafka.enabled", havingValue="true")
public class KafkaConfig {
    @Bean NewTopic transactionsTopic() { return TopicBuilder.name(TransactionEventPublisher.TOPIC).partitions(3).replicas(1).build(); }
}
