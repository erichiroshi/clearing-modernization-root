package br.com.erichiroshi.clearing.query.infrastructure.messaging;

/**
 * Envolve uma falha do {@code TradeProjectionHandler} com o tradeId como
 * contexto. Existe para satisfazer a regra do Sonar de não logar e relançar
 * a mesma exceção sem alterações — o container do Spring Kafka já loga a
 * exceção não tratada de um listener, então aqui só agregamos contexto em
 * vez de logar de novo.
 */
public class TradeProjectionException extends RuntimeException {

    public TradeProjectionException(String tradeId, Throwable cause) {
        super("Falha ao projetar trade " + tradeId, cause);
    }
}
