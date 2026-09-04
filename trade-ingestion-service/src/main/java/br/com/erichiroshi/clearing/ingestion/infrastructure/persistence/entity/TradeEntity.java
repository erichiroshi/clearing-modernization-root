package br.com.erichiroshi.clearing.ingestion.infrastructure.persistence.entity;

import br.com.erichiroshi.clearing.domain.model.StatusTrade;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "trades")
public class TradeEntity {

    @Id
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "comprador_id", nullable = false)
    private CompradorEntity comprador;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendedor_id", nullable = false)
    private VendedorEntity vendedor;

    @Column(name = "ativo_ticker", nullable = false, length = 20)
    private String ativoTicker;

    @Column(name = "ativo_nome", nullable = false, length = 120)
    private String ativoNome;

    @Column(nullable = false, precision = 18, scale = 8)
    private BigDecimal quantidade;

    @Column(nullable = false, precision = 18, scale = 4)
    private BigDecimal preco;

    @Column(name = "valor_total", nullable = false, precision = 18, scale = 4)
    private BigDecimal valorTotal;

    // Reaproveita o enum do domínio direto no mapeamento — é uma simplificação
    // consciente para este projeto; em um sistema maior, a infra normalmente
    // teria seu próprio enum de persistência para não acoplar o schema ao
    // vocabulário do domínio.
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusTrade status;

    @Column(name = "registrado_em", nullable = false)
    private Instant registradoEm;

    @Column(name = "liquidado_em")
    private Instant liquidadoEm;

    protected TradeEntity() {
        // exigido pelo JPA
    }

    public TradeEntity(String id, CompradorEntity comprador, VendedorEntity vendedor,
                        String ativoTicker, String ativoNome, BigDecimal quantidade, BigDecimal preco,
                        BigDecimal valorTotal, StatusTrade status, Instant registradoEm, Instant liquidadoEm) {
        this.id = id;
        this.comprador = comprador;
        this.vendedor = vendedor;
        this.ativoTicker = ativoTicker;
        this.ativoNome = ativoNome;
        this.quantidade = quantidade;
        this.preco = preco;
        this.valorTotal = valorTotal;
        this.status = status;
        this.registradoEm = registradoEm;
        this.liquidadoEm = liquidadoEm;
    }

    public String getId() {
        return id;
    }

    public CompradorEntity getComprador() {
        return comprador;
    }

    public VendedorEntity getVendedor() {
        return vendedor;
    }

    public String getAtivoTicker() {
        return ativoTicker;
    }

    public String getAtivoNome() {
        return ativoNome;
    }

    public BigDecimal getQuantidade() {
        return quantidade;
    }

    public BigDecimal getPreco() {
        return preco;
    }

    public BigDecimal getValorTotal() {
        return valorTotal;
    }

    public StatusTrade getStatus() {
        return status;
    }

    public void setStatus(StatusTrade status) {
        this.status = status;
    }

    public Instant getRegistradoEm() {
        return registradoEm;
    }

    public Instant getLiquidadoEm() {
        return liquidadoEm;
    }

    public void setLiquidadoEm(Instant liquidadoEm) {
        this.liquidadoEm = liquidadoEm;
    }
}
