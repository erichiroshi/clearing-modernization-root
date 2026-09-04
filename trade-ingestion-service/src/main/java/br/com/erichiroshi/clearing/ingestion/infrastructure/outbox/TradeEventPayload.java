package br.com.erichiroshi.clearing.ingestion.infrastructure.outbox;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Representação intermediária do evento, guardada como JSON no outbox.
 * Guardar isso (em vez dos bytes Avro já serializados) permite que o
 * {@link OutboxEventProcessor} monte o objeto Avro na hora de publicar,
 * deixando o {@code KafkaAvroSerializer} lidar com o Schema Registry — e
 * mantém o payload no banco legível/depurável.
 */
public record TradeEventPayload(
        String tradeId,
        String buyerId,
        String sellerId,
        String assetSymbol,
        BigDecimal quantity,
        BigDecimal price,
        BigDecimal totalAmount,
        Instant executedAt,
        String traceId
) {
}
