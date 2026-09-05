package br.com.erichiroshi.clearing.query.application;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Representação interna do evento já consumido, usada da borda de mensageria
 * pra dentro. Existe pra não vazar o tipo Avro gerado (TradeExecutedEvent)
 * pra além do listener — o mesmo padrão usado no trade-ingestion-service
 * (TradeEventPayload) para a direção oposta.
 */
public record TradeProjecaoComando(
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
