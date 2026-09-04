package br.com.erichiroshi.clearing.ingestion.infrastructure.persistence.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Entity
@Table(name = "vendedores")
public class VendedorEntity {

    @Id
    private String id;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "vendedor_posicoes", joinColumns = @JoinColumn(name = "vendedor_id"))
    @MapKeyColumn(name = "ticker")
    @Column(name = "quantidade", precision = 18, scale = 8, nullable = false)
    private Map<String, BigDecimal> posicoes = new HashMap<>();

    protected VendedorEntity() {
        // exigido pelo JPA
    }

    public VendedorEntity(String id, Map<String, BigDecimal> posicoes) {
        this.id = id;
        this.posicoes = new HashMap<>(posicoes);
    }

    public String getId() {
        return id;
    }

    public Map<String, BigDecimal> getPosicoes() {
        return posicoes;
    }

    public void setPosicoes(Map<String, BigDecimal> posicoes) {
        this.posicoes.clear();
        this.posicoes.putAll(posicoes);
    }
}
