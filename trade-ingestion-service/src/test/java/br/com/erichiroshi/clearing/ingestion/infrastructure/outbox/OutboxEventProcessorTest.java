package br.com.erichiroshi.clearing.ingestion.infrastructure.outbox;

import br.com.erichiroshi.clearing.contracts.event.TradeExecutedEvent;
import br.com.erichiroshi.clearing.domain.model.*;
import br.com.erichiroshi.clearing.domain.repository.TradeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OutboxEventProcessorTest {

    private static final String TOPICO = "market.trades.v1";

    @Mock
    private OutboxEventJpaRepository outboxRepository;
    @Mock
    private TradeRepository tradeRepository;
    @Mock
    private TradeEventCodec codec;
    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    private OutboxEventProcessor processor;

    @BeforeEach
    void setUp() {
        processor = new OutboxEventProcessor(outboxRepository, tradeRepository, codec, kafkaTemplate, TOPICO);
    }

    @Test
    void devePublicarMarcarComoPublicadoELiquidarOTradeQuandoKafkaConfirma() {
        // Arrange
        OutboxEventEntity evento = new OutboxEventEntity("trade-1", "TradeValidado", "{}", Instant.now());
        TradeEventPayload payload = payloadExemplo();
        TradeExecutedEvent avroEvent = mock(TradeExecutedEvent.class);

        when(codec.ler(evento.getPayload())).thenReturn(payload);
        when(codec.paraAvro(payload)).thenReturn(avroEvent);
        when(kafkaTemplate.send(TOPICO, "trade-1", avroEvent))
                .thenReturn(CompletableFuture.completedFuture(null));

        Trade tradeValidado = tradeValidadoExemplo();
        when(tradeRepository.buscarPorId("trade-1")).thenReturn(Optional.of(tradeValidado));

        // Act
        processor.processar(evento);

        // Assert
        assertThat(evento.getStatus()).isEqualTo(StatusOutbox.PUBLICADO);
        assertThat(evento.getPublicadoEm()).isNotNull();
        verify(outboxRepository).save(evento);
        verify(tradeRepository).salvar(org.mockito.ArgumentMatchers.argThat(
                t -> t != null && t.getStatus() == StatusTrade.LIQUIDADO));
    }

    @Test
    void deveRegistrarFalhaSemLiquidarQuandoKafkaFalha() {
        // Arrange
        OutboxEventEntity evento = new OutboxEventEntity("trade-1", "TradeValidado", "{}", Instant.now());
        TradeEventPayload payload = payloadExemplo();
        TradeExecutedEvent avroEvent = mock(TradeExecutedEvent.class);

        when(codec.ler(evento.getPayload())).thenReturn(payload);
        when(codec.paraAvro(payload)).thenReturn(avroEvent);
        when(kafkaTemplate.send(TOPICO, "trade-1", avroEvent))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("timeout simulado")));

        // Act
        processor.processar(evento);

        // Assert
        assertThat(evento.getStatus()).isEqualTo(StatusOutbox.PENDENTE);
        assertThat(evento.getTentativas()).isEqualTo(1);
        assertThat(evento.getUltimoErro()).isNotBlank();
        verify(outboxRepository).save(evento);
        verify(tradeRepository, never()).buscarPorId(any());
    }

    @Test
    void deveIrParaFalhaTerminalDepoisDoLimiteDeTentativas() {
        // Arrange
        OutboxEventEntity evento = new OutboxEventEntity("trade-1", "TradeValidado", "{}", Instant.now());
        TradeEventPayload payload = payloadExemplo();
        when(codec.ler(evento.getPayload())).thenReturn(payload);
        TradeExecutedEvent mock = mock(TradeExecutedEvent.class);
        when(codec.paraAvro(payload)).thenReturn(mock);
        when(kafkaTemplate.send(eq(TOPICO), eq("trade-1"), any()))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("timeout simulado")));

        // Act — simula 5 execuções do poller sobre o mesmo evento
        for (int i = 0; i < 5; i++) {
            processor.processar(evento);
        }

        // Assert
        assertThat(evento.getTentativas()).isEqualTo(5);
        assertThat(evento.getStatus()).isEqualTo(StatusOutbox.FALHA);
    }

    private TradeEventPayload payloadExemplo() {
        return new TradeEventPayload("trade-1", "comp-1", "vend-1", "PETR4",
                new BigDecimal("10"), new BigDecimal("30.00"), new BigDecimal("300.00"),
                Instant.now(), null);
    }

    private Trade tradeValidadoExemplo() {
        return Trade.reconstituir(
                "trade-1",
                new Comprador("comp-1", new BigDecimal("700.00")),
                new Vendedor("vend-1", Map.of("PETR4", new BigDecimal("90"))),
                new Ativo("PETR4", "Petrobras PN"),
                new BigDecimal("10"), new BigDecimal("30.00"), new BigDecimal("300.00"),
                StatusTrade.VALIDADO, Instant.now(), null);
    }
}
