package br.com.erichiroshi.clearing.domain.model;

import java.util.Objects;

/**
 * Value object representando o ativo negociado. Igualdade por ticker.
 */
public record Ativo(

        String ticker,
        String nome) {

    public Ativo(String ticker, String nome) {
        if (ticker == null || ticker.isBlank()) {
            throw new IllegalArgumentException("ticker do ativo não pode ser vazio");
        }
        this.ticker = ticker;
        this.nome = Objects.requireNonNull(nome, "nome do ativo não pode ser nulo");
    }
}
