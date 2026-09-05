package br.com.erichiroshi.clearing.query.application;

/**
 * Port de aplicação para projetar um trade consumido do Kafka. Implementado
 * como stub (log) nesta task — a implementação real, gravando no MongoDB,
 * é a Task 3.2.
 */
public interface TradeProjectionHandler {

    void projetar(TradeProjecaoComando comando);
}
