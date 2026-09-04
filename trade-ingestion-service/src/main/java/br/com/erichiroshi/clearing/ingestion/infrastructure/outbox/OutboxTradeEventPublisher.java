package br.com.erichiroshi.clearing.ingestion.infrastructure.outbox;

import br.com.erichiroshi.clearing.domain.model.Trade;
import br.com.erichiroshi.clearing.ingestion.application.TradeEventPublisher;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class OutboxTradeEventPublisher implements TradeEventPublisher {

    private static final String TIPO_EVENTO_TRADE_VALIDADO = "TradeValidado";

    private final OutboxEventJpaRepository outboxRepository;
    private final TradeEventCodec codec;

    public OutboxTradeEventPublisher(OutboxEventJpaRepository outboxRepository, TradeEventCodec codec) {
        this.outboxRepository = outboxRepository;
        this.codec = codec;
    }

    /**
     * Só grava o registro na tabela de outbox — dentro da mesma transação
     * do ExecutarTradeUseCase. A publicação de verdade no Kafka acontece
     * depois, de forma assíncrona, via {@link OutboxPoller}.
     */
    @Override
    public void registrarEventoPendente(Trade trade) {
        TradeEventPayload payload = new TradeEventPayload(
                trade.getId(),
                trade.getComprador().getId(),
                trade.getVendedor().getId(),
                trade.getAtivo().getTicker(),
                trade.getQuantidade(),
                trade.getPreco(),
                trade.getValorTotal(),
                trade.getRegistradoEm(),
                null // traceId: propagado via OpenTelemetry na Task 3.3
        );

        String payloadJson = codec.escrever(payload);
        OutboxEventEntity evento = new OutboxEventEntity(
                trade.getId(), TIPO_EVENTO_TRADE_VALIDADO, payloadJson, Instant.now());

        outboxRepository.save(evento);
    }
}
