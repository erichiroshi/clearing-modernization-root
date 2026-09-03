package br.com.erichiroshi.clearing.domain.model;

import br.com.erichiroshi.clearing.domain.exception.PosicaoInsuficienteException;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Participante vendedor de um trade. Mantém, por ticker, a quantidade do
 * ativo que possui em custódia — é essa posição que a clearing valida
 * para garantir que ele pode honrar a ponta de entrega do ativo.
 */
public class Vendedor {

    private final String id;
    private final Map<String, BigDecimal> posicoes;

    public Vendedor(String id, Map<String, BigDecimal> posicoes) {
        this.id = Objects.requireNonNull(id, "id do vendedor não pode ser nulo");
        this.posicoes = new HashMap<>(Objects.requireNonNull(posicoes, "posicoes não podem ser nulas"));
    }

    public boolean possuiPosicaoSuficiente(String ticker, BigDecimal quantidade) {
        return posicaoDe(ticker).compareTo(quantidade) >= 0;
    }

    /**
     * Reduz a posição em custódia. Só deve ser chamado depois que
     * {@link #possuiPosicaoSuficiente(String, BigDecimal)} já confirmou que
     * a operação é possível — ver {@link Trade#validar()} para o fluxo de
     * validação atômica (Delivery versus Payment).
     */
    public void reduzirPosicao(String ticker, BigDecimal quantidade) {
        if (!possuiPosicaoSuficiente(ticker, quantidade)) {
            throw new PosicaoInsuficienteException(id, ticker, quantidade, posicaoDe(ticker));
        }
        posicoes.merge(ticker, quantidade.negate(), BigDecimal::add);
    }

    public BigDecimal posicaoDe(String ticker) {
        return posicoes.getOrDefault(ticker, BigDecimal.ZERO);
    }

    /**
     * Retorna uma cópia imutável das posições — usada pelos adaptadores de
     * persistência (Task 2.2) para gravar o estado atual no banco.
     */
    public Map<String, BigDecimal> getPosicoes() {
        return Map.copyOf(posicoes);
    }

    public String getId() {
        return id;
    }
}
