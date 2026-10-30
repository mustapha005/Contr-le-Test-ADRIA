package ma.adria.transaction;

import ma.adria.transaction.event.*;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import static org.mockito.Mockito.*;

class TransactionEventPublisherTest {
    @Test void publishesToRequiredTopicUsingAccountAsKey() {
        @SuppressWarnings("unchecked") KafkaTemplate<String, Object> kafka = mock(KafkaTemplate.class);
        var event = new TransactionCreated(UUID.randomUUID(), "TransactionCreated", UUID.randomUUID(), UUID.randomUUID(), new BigDecimal("250.00"), "EUR", Instant.now());
        when(kafka.send(TransactionEventPublisher.TOPIC, event.accountId().toString(), event))
            .thenReturn(CompletableFuture.<SendResult<String, Object>>completedFuture(null));
        new TransactionEventPublisher(kafka, true).publish(event);
        verify(kafka).send("bank.transactions.created", event.accountId().toString(), event);
    }
}
