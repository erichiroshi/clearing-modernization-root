package br.com.erichiroshi.clearing.ingestion.infrastructure.persistence;

import br.com.erichiroshi.clearing.domain.model.Trade;
import br.com.erichiroshi.clearing.domain.repository.TradeRepository;
import br.com.erichiroshi.clearing.ingestion.infrastructure.persistence.entity.CompradorEntity;
import br.com.erichiroshi.clearing.ingestion.infrastructure.persistence.entity.TradeEntity;
import br.com.erichiroshi.clearing.ingestion.infrastructure.persistence.entity.VendedorEntity;
import br.com.erichiroshi.clearing.ingestion.infrastructure.persistence.mapper.TradeMapper;
import br.com.erichiroshi.clearing.ingestion.infrastructure.persistence.repository.CompradorJpaRepository;
import br.com.erichiroshi.clearing.ingestion.infrastructure.persistence.repository.TradeJpaRepository;
import br.com.erichiroshi.clearing.ingestion.infrastructure.persistence.repository.VendedorJpaRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class TradeRepositoryAdapter implements TradeRepository {

    private final TradeJpaRepository tradeJpaRepository;
    private final CompradorJpaRepository compradorJpaRepository;
    private final VendedorJpaRepository vendedorJpaRepository;

    public TradeRepositoryAdapter(TradeJpaRepository tradeJpaRepository,
                                   CompradorJpaRepository compradorJpaRepository,
                                   VendedorJpaRepository vendedorJpaRepository) {
        this.tradeJpaRepository = tradeJpaRepository;
        this.compradorJpaRepository = compradorJpaRepository;
        this.vendedorJpaRepository = vendedorJpaRepository;
    }

    @Override
    public Optional<Trade> buscarPorId(String id) {
        return tradeJpaRepository.findById(id).map(TradeMapper::paraDominio);
    }

    @Override
    public Trade salvar(Trade trade) {
        TradeEntity entity = tradeJpaRepository.findById(trade.getId())
                .map(existente -> {
                    TradeMapper.atualizarEntidade(existente, trade);
                    return existente;
                })
                .orElseGet(() -> {
                    // referência gerenciada, sem precisar de outro SELECT —
                    // comprador/vendedor já foram carregados (e travados) pelo
                    // use case antes do trade ser criado
                    CompradorEntity compradorRef = compradorJpaRepository.getReferenceById(trade.getComprador().getId());
                    VendedorEntity vendedorRef = vendedorJpaRepository.getReferenceById(trade.getVendedor().getId());
                    return TradeMapper.paraEntidade(trade, compradorRef, vendedorRef);
                });

        tradeJpaRepository.save(entity);
        return trade;
    }
}
