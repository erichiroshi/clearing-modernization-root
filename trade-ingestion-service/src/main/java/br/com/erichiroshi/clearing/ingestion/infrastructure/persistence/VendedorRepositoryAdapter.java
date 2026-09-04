package br.com.erichiroshi.clearing.ingestion.infrastructure.persistence;

import br.com.erichiroshi.clearing.domain.model.Vendedor;
import br.com.erichiroshi.clearing.domain.repository.VendedorRepository;
import br.com.erichiroshi.clearing.ingestion.infrastructure.persistence.entity.VendedorEntity;
import br.com.erichiroshi.clearing.ingestion.infrastructure.persistence.mapper.VendedorMapper;
import br.com.erichiroshi.clearing.ingestion.infrastructure.persistence.repository.VendedorJpaRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class VendedorRepositoryAdapter implements VendedorRepository {

    private final VendedorJpaRepository jpaRepository;

    public VendedorRepositoryAdapter(VendedorJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Optional<Vendedor> buscarPorId(String id) {
        // lock pessimista: dentro da transação do use case, nenhum outro
        // trade consegue ler/alterar esse vendedor até o commit
        return jpaRepository.buscarComLockParaAtualizacao(id).map(VendedorMapper::paraDominio);
    }

    @Override
    public Vendedor salvar(Vendedor vendedor) {
        VendedorEntity entity = jpaRepository.findById(vendedor.getId())
                .map(existente -> {
                    VendedorMapper.atualizarEntidade(existente, vendedor);
                    return existente;
                })
                .orElseGet(() -> VendedorMapper.paraEntidade(vendedor));

        jpaRepository.save(entity);
        return vendedor;
    }
}
