package br.com.erichiroshi.clearing.domain.model;

import java.util.Objects;

/**
 * Value object representando o ativo negociado. Igualdade por ticker.
 */
public final class Ativo {

    private final String ticker;
    private final String nome;

    public Ativo(String ticker, String nome) {
        if (ticker == null || ticker.isBlank()) {
            throw new IllegalArgumentException("ticker do ativo não pode ser vazio");
        }
        this.ticker = ticker;
        this.nome = nome;
        Objects.requireNonNull(nome, "nome do ativo não pode ser nulo");
    }

    public String getTicker() {
        return ticker;
    }

    public String getNome() {
        return nome;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Ativo ativo)) return false;
        return Objects.equals(ticker, ativo.ticker);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(ticker);
    }

    @Override
    public String toString() {
        return ticker;
    }
}
