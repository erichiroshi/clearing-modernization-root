package br.com.erichiroshi.clearing.query.infrastructure.messaging;

import br.com.erichiroshi.clearing.contracts.event.TradeExecutedEvent;
import br.com.erichiroshi.clearing.query.application.TradeProjecaoComando;
import br.com.erichiroshi.clearing.query.application.TradeProjectionHandler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.Acknowledgment;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TradeExecutedEventListenerTest {

    @Mock
    private TradeProjectionHandler handler;
    @Mock
    private Acknowledgment ack;

    @Test
    void deveMapearCamposEConfirmarAckQuandoHandlerProcessaComSucesso() {
        // Arrange
        TradeExecutedEventListener listenerComMock = new TradeExecutedEventListener(handler);
        TradeExecutedEvent evento = eventoExemplo();

        // Act
        listenerComMock.consumir(evento, ack);

        // Assert
        verify(handler).projetar(new TradeProjecaoComando(
                "trade-1", "comp-1", "vend-1", "PETR4",
                new BigDecimal("10"), new BigDecimal("30.00"), new BigDecimal("300.00"),
                evento.getExecutedAt(), null));
        verify(ack).acknowledge();
    }

    @Test
    void naoDeveConfirmarAckQuandoHandlerLancaExcecao() {
        // Arrange
        TradeExecutedEventListener listenerComMock = new TradeExecutedEventListener(handler);
        TradeExecutedEvent evento = eventoExemplo();
        doThrow(new RuntimeException("falha simulada na projeção"))
                .when(handler).projetar(org.mockito.ArgumentMatchers.any());

        // Act & Assert
        assertThatThrownBy(() -> listenerComMock.consumir(evento, ack))
                .isInstanceOf(RuntimeException.class);

        verify(ack, never()).acknowledge();
    }

    private TradeExecutedEvent eventoExemplo() {
        return TradeExecutedEvent.newBuilder()
                .setTradeId("trade-1")
                .setBuyerId("comp-1")
                .setSellerId("vend-1")
                .setAssetSymbol("PETR4")
                .setQuantity(new BigDecimal("10"))
                .setPrice(new BigDecimal("30.00"))
                .setTotalAmount(new BigDecimal("300.00"))
                .setExecutedAt(Instant.now())
                .setTraceId(null)
                .build();
    }
}
