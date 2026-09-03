package br.com.erichiroshi.clearing.domain.repository;

import br.com.erichiroshi.clearing.domain.model.Trade;

import java.util.Optional;

public interface TradeRepository {

    Optional<Trade> buscarPorId(String id);

    Trade salvar(Trade trade);
}
