package br.com.erichiroshi.clearing.ingestion.infrastructure.outbox;

import br.com.erichiroshi.clearing.contracts.event.TradeExecutedEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

@Component
public class TradeEventCodec {

    private final ObjectMapper objectMapper;

    public TradeEventCodec(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String escrever(TradeEventPayload payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Falha ao serializar payload do outbox", e);
        }
    }

    public TradeEventPayload ler(String json) {
        try {
            return objectMapper.readValue(json, TradeEventPayload.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Falha ao desserializar payload do outbox", e);
        }
    }

    /**
     * Monta o objeto Avro gerado a partir do .avsc (Task 1.2). Se o build
     * falhar aqui reclamando de tipo em setExecutedAt, é porque o
     * gradle-avro-plugin gerou o campo timestamp-millis como {@code Long}
     * em vez de {@code Instant} nessa versão — troque
     * {@code payload.executedAt()} por
     * {@code payload.executedAt().toEpochMilli()} nesse caso.
     */
    public TradeExecutedEvent paraAvro(TradeEventPayload payload) {
        return TradeExecutedEvent.newBuilder()
                .setTradeId(payload.tradeId())
                .setBuyerId(payload.buyerId())
                .setSellerId(payload.sellerId())
                .setAssetSymbol(payload.assetSymbol())
                .setQuantity(payload.quantity())
                .setPrice(payload.price())
                .setTotalAmount(payload.totalAmount())
                .setExecutedAt(payload.executedAt())
                .setTraceId(payload.traceId())
                .build();
    }
}
