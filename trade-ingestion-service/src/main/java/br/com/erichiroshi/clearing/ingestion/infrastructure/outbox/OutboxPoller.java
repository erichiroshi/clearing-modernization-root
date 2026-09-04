package br.com.erichiroshi.clearing.ingestion.infrastructure.outbox;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "clearing.outbox.poller", name = "enabled", havingValue = "true", matchIfMissing = true)
public class OutboxPoller {

    private final OutboxEventJpaRepository outboxRepository;
    private final OutboxEventProcessor processor;

    public OutboxPoller(OutboxEventJpaRepository outboxRepository, OutboxEventProcessor processor) {
        this.outboxRepository = outboxRepository;
        this.processor = processor;
    }

    @Scheduled(fixedDelayString = "${clearing.outbox.poll-interval-ms:2000}")
    public void publicarEventosPendentes() {
        outboxRepository.findTop50ByStatusOrderByCriadoEmAsc(StatusOutbox.PENDENTE)
                .forEach(processor::processar);
    }
}
