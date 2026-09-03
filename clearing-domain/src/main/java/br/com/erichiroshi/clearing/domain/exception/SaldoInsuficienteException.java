package br.com.erichiroshi.clearing.domain.exception;

import java.math.BigDecimal;

/**
 * Lançada quando um Comprador não possui saldo/garantia suficiente para
 * cobrir o valor total de um trade.
 */
public class SaldoInsuficienteException extends RuntimeException {

    public SaldoInsuficienteException(String compradorId, BigDecimal valorNecessario, BigDecimal saldoDisponivel) {
        super("Comprador %s não possui saldo suficiente: necessário %s, disponível %s"
                .formatted(compradorId, valorNecessario, saldoDisponivel));
    }
}
