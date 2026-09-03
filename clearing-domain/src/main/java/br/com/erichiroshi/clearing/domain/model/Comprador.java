package br.com.erichiroshi.clearing.domain.model;

import br.com.erichiroshi.clearing.domain.exception.SaldoInsuficienteException;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Participante comprador de um trade. Mantém o saldo/garantia disponível
 * que a clearing usa para validar se ele pode honrar a ponta financeira
 * da operação (pagamento).
 */
public class Comprador {

    private final String id;
    private BigDecimal saldoDisponivel;

    public Comprador(String id, BigDecimal saldoDisponivel) {
        this.id = Objects.requireNonNull(id, "id do comprador não pode ser nulo");
        if (saldoDisponivel == null || saldoDisponivel.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("saldoDisponivel não pode ser nulo ou negativo");
        }
        this.saldoDisponivel = saldoDisponivel;
    }

    public boolean possuiSaldoSuficiente(BigDecimal valor) {
        return saldoDisponivel.compareTo(valor) >= 0;
    }

    /**
     * Debita o valor do saldo disponível. Só deve ser chamado depois que
     * {@link #possuiSaldoSuficiente(BigDecimal)} já confirmou que a operação
     * é possível — ver {@link Trade#validar()} para o fluxo de validação
     * atômica (Delivery versus Payment).
     */
    public void debitar(BigDecimal valor) {
        if (!possuiSaldoSuficiente(valor)) {
            throw new SaldoInsuficienteException(id, valor, saldoDisponivel);
        }
        this.saldoDisponivel = this.saldoDisponivel.subtract(valor);
    }

    public String getId() {
        return id;
    }

    public BigDecimal getSaldoDisponivel() {
        return saldoDisponivel;
    }
}
