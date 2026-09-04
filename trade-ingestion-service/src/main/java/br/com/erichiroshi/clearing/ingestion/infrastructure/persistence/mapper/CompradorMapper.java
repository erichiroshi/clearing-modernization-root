package br.com.erichiroshi.clearing.ingestion.infrastructure.persistence.mapper;

import br.com.erichiroshi.clearing.domain.model.Comprador;
import br.com.erichiroshi.clearing.ingestion.infrastructure.persistence.entity.CompradorEntity;

public final class CompradorMapper {

    private CompradorMapper() {
    }

    public static Comprador paraDominio(CompradorEntity entity) {
        return new Comprador(entity.getId(), entity.getSaldoDisponivel());
    }

    public static CompradorEntity paraEntidade(Comprador dominio) {
        return new CompradorEntity(dominio.getId(), dominio.getSaldoDisponivel());
    }

    /**
     * Sincroniza uma entidade JPA já gerenciada com o estado atual do
     * agregado de domínio (depois de {@code Trade.validar()} ter debitado
     * o saldo, por exemplo).
     */
    public static void atualizarEntidade(CompradorEntity entity, Comprador dominio) {
        entity.setSaldoDisponivel(dominio.getSaldoDisponivel());
    }
}
