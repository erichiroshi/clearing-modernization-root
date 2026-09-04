package br.com.erichiroshi.clearing.ingestion.infrastructure.outbox;

import br.com.erichiroshi.clearing.contracts.event.TradeExecutedEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class TradeEventCodecTest {

    private final TradeEventCodec codec = new TradeEventCodec(new ObjectMapper().registerModule(new JavaTimeModule()));

    private final TradeEventPayload payloadExemplo = new TradeEventPayload(
            "trade-1", "comp-1", "vend-1", "PETR4",
            new BigDecimal("10"), new BigDecimal("30.00"), new BigDecimal("300.00"),
            Instant.parse("2026-01-01T12:00:00Z"), null);

    @Test
    void escreverELerDevemSerSimetricos() {
        String json = codec.escrever(payloadExemplo);
        TradeEventPayload lido = codec.ler(json);

        assertThat(lido).isEqualTo(payloadExemplo);
    }

    @Test
    void paraAvroDeveMapearTodosOsCampos() {
        TradeExecutedEvent evento = codec.paraAvro(payloadExemplo);

        assertThat(evento.getTradeId()).isEqualTo("trade-1");
        assertThat(evento.getBuyerId()).isEqualTo("comp-1");
        assertThat(evento.getSellerId()).isEqualTo("vend-1");
        assertThat(evento.getAssetSymbol()).isEqualTo("PETR4");
        assertThat(evento.getQuantity()).isEqualByComparingTo("10");
        assertThat(evento.getPrice()).isEqualByComparingTo("30.00");
        assertThat(evento.getTotalAmount()).isEqualByComparingTo("300.00");
        assertThat(evento.getTraceId()).isNull();
    }
}
