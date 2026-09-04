package br.com.erichiroshi.clearing.ingestion.infrastructure.persistence;

import br.com.erichiroshi.clearing.domain.model.Comprador;
import br.com.erichiroshi.clearing.domain.repository.CompradorRepository;
import br.com.erichiroshi.clearing.ingestion.infrastructure.persistence.entity.CompradorEntity;
import br.com.erichiroshi.clearing.ingestion.infrastructure.persistence.mapper.CompradorMapper;
import br.com.erichiroshi.clearing.ingestion.infrastructure.persistence.repository.CompradorJpaRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class CompradorRepositoryAdapter implements CompradorRepository {

    private final CompradorJpaRepository jpaRepository;

    public CompradorRepositoryAdapter(CompradorJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Optional<Comprador> buscarPorId(String id) {
        // lock pessimista: dentro da transação do use case, nenhum outro
        // trade consegue ler/alterar esse comprador até o commit
        return jpaRepository.buscarComLockParaAtualizacao(id).map(CompradorMapper::paraDominio);
    }

    @Override
    public Comprador salvar(Comprador comprador) {
        CompradorEntity entity = jpaRepository.findById(comprador.getId())
                .map(existente -> {
                    CompradorMapper.atualizarEntidade(existente, comprador);
                    return existente;
                })
                .orElseGet(() -> CompradorMapper.paraEntidade(comprador));

        jpaRepository.save(entity);
        return comprador;
    }
}
