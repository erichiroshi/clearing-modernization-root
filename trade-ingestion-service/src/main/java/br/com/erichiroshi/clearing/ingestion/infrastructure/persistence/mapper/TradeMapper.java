package br.com.erichiroshi.clearing.ingestion.infrastructure.persistence.mapper;

import br.com.erichiroshi.clearing.domain.model.Ativo;
import br.com.erichiroshi.clearing.domain.model.Trade;
import br.com.erichiroshi.clearing.ingestion.infrastructure.persistence.entity.CompradorEntity;
import br.com.erichiroshi.clearing.ingestion.infrastructure.persistence.entity.TradeEntity;
import br.com.erichiroshi.clearing.ingestion.infrastructure.persistence.entity.VendedorEntity;

public final class TradeMapper {

    private TradeMapper() {
    }

    public static Trade paraDominio(TradeEntity entity) {
        return Trade.reconstituir(
                entity.getId(),
                CompradorMapper.paraDominio(entity.getComprador()),
                VendedorMapper.paraDominio(entity.getVendedor()),
                new Ativo(entity.getAtivoTicker(), entity.getAtivoNome()),
                entity.getQuantidade(),
                entity.getPreco(),
                entity.getValorTotal(),
                entity.getStatus(),
                entity.getRegistradoEm(),
                entity.getLiquidadoEm());
    }

    public static TradeEntity paraEntidade(Trade dominio, CompradorEntity compradorRef, VendedorEntity vendedorRef) {
        Ativo ativo = dominio.getAtivo();
        return new TradeEntity(
                dominio.getId(),
                compradorRef,
                vendedorRef,
                ativo.getTicker(),
                ativo.getNome(),
                dominio.getQuantidade(),
                dominio.getPreco(),
                dominio.getValorTotal(),
                dominio.getStatus(),
                dominio.getRegistradoEm(),
                dominio.getLiquidadoEm());
    }

    /**
     * Sincroniza uma entidade JPA já gerenciada com o estado atual do
     * trade de domínio (status e liquidadoEm são os únicos campos mutáveis
     * depois do registro).
     */
    public static void atualizarEntidade(TradeEntity entity, Trade dominio) {
        entity.setStatus(dominio.getStatus());
        entity.setLiquidadoEm(dominio.getLiquidadoEm());
    }
}
