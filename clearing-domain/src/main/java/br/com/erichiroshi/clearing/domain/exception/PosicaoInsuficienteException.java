package br.com.erichiroshi.clearing.domain.exception;

import java.math.BigDecimal;

/**
 * Lançada quando um Vendedor não possui, em custódia, quantidade suficiente
 * do ativo negociado para cobrir um trade.
 */
public class PosicaoInsuficienteException extends RuntimeException {

    public PosicaoInsuficienteException(String vendedorId, String ticker,
                                         BigDecimal quantidadeNecessaria, BigDecimal quantidadeDisponivel) {
        super("Vendedor %s não possui posição suficiente em %s: necessário %s, disponível %s"
                .formatted(vendedorId, ticker, quantidadeNecessaria, quantidadeDisponivel));
    }
}
