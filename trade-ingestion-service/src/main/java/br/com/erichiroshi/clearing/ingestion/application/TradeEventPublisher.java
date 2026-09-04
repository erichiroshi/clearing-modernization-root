package br.com.erichiroshi.clearing.ingestion.application;

import br.com.erichiroshi.clearing.domain.model.Trade;

/**
 * Port de aplicação (não é um port do domínio — publicar evento de
 * integração é uma decisão de como ESTE microsserviço se comunica, não uma
 * regra de negócio do clearing-domain). Implementado via Transactional
 * Outbox em {@code infrastructure.outbox.OutboxTradeEventPublisher}.
 */
public interface TradeEventPublisher {

    void registrarEventoPendente(Trade trade);
}
