package br.com.erichiroshi.clearing.ingestion.infrastructure.outbox;

import br.com.erichiroshi.clearing.contracts.event.TradeExecutedEvent;
import br.com.erichiroshi.clearing.domain.repository.TradeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

/**
 * Processa UM registro do outbox por vez, numa transação curta — em vez de
 * uma transação gigante para o lote inteiro, que ficaria segurando conexão
 * de banco durante chamadas de rede (Kafka) de vários eventos.
 * <p>
 * Trade-off consciente: o envio ao Kafka ({@code .get(...)}, bloqueante)
 * ainda acontece dentro dessa transação curta. Numa versão mais "produção",
 * isso separaria o envio (fora da tx) da atualização de status (tx rápida
 * de UPDATE), mas para o escopo deste projeto o ganho não compensa a
 * complexidade extra (precisaria de idempotência mais cuidadosa).
 */
@Component
public class OutboxEventProcessor {

    private static final Logger log = LoggerFactory.getLogger(OutboxEventProcessor.class);
    private static final int MAX_TENTATIVAS = 5;
    private static final int TIMEOUT_SEGUNDOS = 5;

    private final OutboxEventJpaRepository outboxRepository;
    private final TradeRepository tradeRepository;
    private final TradeEventCodec codec;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final String topic;

    public OutboxEventProcessor(OutboxEventJpaRepository outboxRepository,
                                TradeRepository tradeRepository,
                                TradeEventCodec codec,
                                KafkaTemplate<String, Object> kafkaTemplate,
                                @Value("${clearing.kafka.topic.trade-executed}") String topic) {
        this.outboxRepository = outboxRepository;
        this.tradeRepository = tradeRepository;
        this.codec = codec;
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    @Transactional
    public void processar(OutboxEventEntity evento) {
        try {
            TradeEventPayload payload = codec.ler(evento.getPayload());
            TradeExecutedEvent avroEvent = codec.paraAvro(payload);

            kafkaTemplate.send(topic, payload.tradeId(), avroEvent)
                    .get(TIMEOUT_SEGUNDOS, TimeUnit.SECONDS);

            evento.marcarComoPublicado(Instant.now());
            outboxRepository.save(evento);

            // liquidação só acontece depois da confirmação do Kafka — é o
            // fechamento do ciclo PENDENTE -> VALIDADO -> LIQUIDADO do Trade
            tradeRepository.buscarPorId(evento.getAggregateId()).ifPresent(trade -> {
                trade.liquidar();
                tradeRepository.salvar(trade);
            });

        } catch (InterruptedException _) {
            // Restore interrupted status
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            log.warn("Falha ao publicar evento do outbox (aggregateId={}, tentativa={})",
                    evento.getAggregateId(), evento.getTentativas() + 1, e);
            evento.registrarFalha(Instant.now(), resumirErro(e), MAX_TENTATIVAS);
            outboxRepository.save(evento);
        }
    }

    private String resumirErro(Exception e) {
        String mensagem = e.getMessage();
        if (mensagem == null) {
            mensagem = e.getClass().getSimpleName();
        }
        return mensagem.length() > 500 ? mensagem.substring(0, 500) : mensagem;
    }
}
