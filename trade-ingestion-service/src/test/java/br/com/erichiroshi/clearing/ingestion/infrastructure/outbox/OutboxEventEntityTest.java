package br.com.erichiroshi.clearing.ingestion.infrastructure.outbox;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class OutboxEventEntityTest {

    @Test
    void deveIniciarComoPendente() {
        OutboxEventEntity evento = new OutboxEventEntity("trade-1", "TradeValidado", "{}", Instant.now());

        assertThat(evento.getStatus()).isEqualTo(StatusOutbox.PENDENTE);
        assertThat(evento.getTentativas()).isZero();
    }

    @Test
    void marcarComoPublicadoDeveAtualizarStatusEDataEZerarUltimoErro() {
        OutboxEventEntity evento = new OutboxEventEntity("trade-1", "TradeValidado", "{}", Instant.now());
        Instant agora = Instant.now();

        evento.marcarComoPublicado(agora);

        assertThat(evento.getStatus()).isEqualTo(StatusOutbox.PUBLICADO);
        assertThat(evento.getPublicadoEm()).isEqualTo(agora);
        assertThat(evento.getUltimoErro()).isNull();
    }

    @Test
    void registrarFalhaDeveIncrementarTentativasESoMudarParaFalhaNoLimite() {
        OutboxEventEntity evento = new OutboxEventEntity("trade-1", "TradeValidado", "{}", Instant.now());

        evento.registrarFalha(Instant.now(), "timeout", 3);
        assertThat(evento.getTentativas()).isEqualTo(1);
        assertThat(evento.getStatus()).isEqualTo(StatusOutbox.PENDENTE);

        evento.registrarFalha(Instant.now(), "timeout", 3);
        assertThat(evento.getStatus()).isEqualTo(StatusOutbox.PENDENTE);

        evento.registrarFalha(Instant.now(), "timeout", 3);
        assertThat(evento.getTentativas()).isEqualTo(3);
        assertThat(evento.getStatus()).isEqualTo(StatusOutbox.FALHA);
    }
}
