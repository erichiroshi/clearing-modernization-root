package br.com.erichiroshi.clearing.ingestion.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Entity
@Table(name = "compradores")
public class CompradorEntity {

    @Id
    private String id;

    @Column(name = "saldo_disponivel", nullable = false, precision = 18, scale = 4)
    private BigDecimal saldoDisponivel;

    protected CompradorEntity() {
        // exigido pelo JPA
    }

    public CompradorEntity(String id, BigDecimal saldoDisponivel) {
        this.id = id;
        this.saldoDisponivel = saldoDisponivel;
    }

    public String getId() {
        return id;
    }

    public BigDecimal getSaldoDisponivel() {
        return saldoDisponivel;
    }

    public void setSaldoDisponivel(BigDecimal saldoDisponivel) {
        this.saldoDisponivel = saldoDisponivel;
    }
}
