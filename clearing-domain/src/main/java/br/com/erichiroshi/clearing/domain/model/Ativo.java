package br.com.erichiroshi.clearing.domain.model;

import java.util.Objects;

/**
 * Value object representando o ativo negociado. Igualdade por ticker.
 */
public record Ativo(

        String ticker,
        String nome)

{
    public Ativo {
        if (ticker == null || ticker.isBlank()) {
            throw new IllegalArgumentException("ticker do ativo não pode ser vazio");
        }
        Objects.requireNonNull(nome, "nome do ativo não pode ser nulo");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Ativo ativo)) return false;
        return ticker.equals(ativo.ticker);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ticker);
    }

    @Override
    public String toString() {
        return ticker;
    }
}
