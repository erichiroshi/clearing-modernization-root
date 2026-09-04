package br.com.erichiroshi.clearing.ingestion.infrastructure.outbox;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * Registro do Transactional Outbox — gravado na MESMA transação que o
 * Trade (ver ExecutarTradeUseCase), garantindo que "o trade foi validado"
 * e "o evento será publicado" nunca fiquem inconsistentes entre si.
 * <p>
 * Um processo separado ({@link OutboxPoller} + {@link OutboxEventProcessor})
 * lê os registros PENDENTE e publica no Kafka de forma assíncrona.
 */
@Entity
@Table(name = "trade_events_outbox")
public class OutboxEventEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "aggregate_id", nullable = false, length = 36)
    private String aggregateId;

    @Column(name = "tipo_evento", nullable = false, length = 50)
    private String tipoEvento;

    @Column(nullable = false, columnDefinition = "text")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusOutbox status;

    @Column(nullable = false)
    private int tentativas;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm;

    @Column(name = "publicado_em")
    private Instant publicadoEm;

    @Column(name = "ultima_tentativa_em")
    private Instant ultimaTentativaEm;

    @Column(name = "ultimo_erro", columnDefinition = "text")
    private String ultimoErro;

    protected OutboxEventEntity() {
        // exigido pelo JPA
    }

    public OutboxEventEntity(String aggregateId, String tipoEvento, String payload, Instant criadoEm) {
        this.aggregateId = aggregateId;
        this.tipoEvento = tipoEvento;
        this.payload = payload;
        this.status = StatusOutbox.PENDENTE;
        this.tentativas = 0;
        this.criadoEm = criadoEm;
    }

    public void marcarComoPublicado(Instant agora) {
        this.status = StatusOutbox.PUBLICADO;
        this.publicadoEm = agora;
        this.ultimaTentativaEm = agora;
        this.ultimoErro = null;
    }

    /**
     * Registra uma tentativa de publicação falha. Depois de
     * {@code maxTentativas}, o evento vai para {@link StatusOutbox#FALHA} —
     * estado terminal que exige intervenção manual (ou um alerta, se isso
     * fosse produção de verdade); o poller para de tentar sozinho.
     */
    public void registrarFalha(Instant agora, String erro, int maxTentativas) {
        this.tentativas++;
        this.ultimaTentativaEm = agora;
        this.ultimoErro = erro;
        if (this.tentativas >= maxTentativas) {
            this.status = StatusOutbox.FALHA;
        }
    }

    public Long getId() {
        return id;
    }

    public String getAggregateId() {
        return aggregateId;
    }

    public String getTipoEvento() {
        return tipoEvento;
    }

    public String getPayload() {
        return payload;
    }

    public StatusOutbox getStatus() {
        return status;
    }

    public int getTentativas() {
        return tentativas;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }

    public Instant getPublicadoEm() {
        return publicadoEm;
    }

    public Instant getUltimaTentativaEm() {
        return ultimaTentativaEm;
    }

    public String getUltimoErro() {
        return ultimoErro;
    }
}
