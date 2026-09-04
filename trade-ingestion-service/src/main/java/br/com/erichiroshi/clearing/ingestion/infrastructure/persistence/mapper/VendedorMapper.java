package br.com.erichiroshi.clearing.ingestion.infrastructure.persistence.mapper;

import br.com.erichiroshi.clearing.domain.model.Vendedor;
import br.com.erichiroshi.clearing.ingestion.infrastructure.persistence.entity.VendedorEntity;

public final class VendedorMapper {

    private VendedorMapper() {
    }

    public static Vendedor paraDominio(VendedorEntity entity) {
        return new Vendedor(entity.getId(), entity.getPosicoes());
    }

    public static VendedorEntity paraEntidade(Vendedor dominio) {
        return new VendedorEntity(dominio.getId(), dominio.getPosicoes());
    }

    /**
     * Sincroniza uma entidade JPA já gerenciada com o estado atual do
     * agregado de domínio (depois de {@code Trade.validar()} ter reduzido
     * a posição, por exemplo).
     */
    public static void atualizarEntidade(VendedorEntity entity, Vendedor dominio) {
        entity.setPosicoes(dominio.getPosicoes());
    }
}
