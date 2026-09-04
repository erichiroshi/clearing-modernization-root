package br.com.erichiroshi.clearing.ingestion.infrastructure.persistence.repository;

import br.com.erichiroshi.clearing.ingestion.infrastructure.persistence.entity.VendedorEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface VendedorJpaRepository extends JpaRepository<VendedorEntity, String> {

    /**
     * SELECT ... FOR UPDATE — serializa trades concorrentes contra o mesmo
     * vendedor, evitando duas operações reduzirem a mesma posição em
     * custódia em paralelo.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select v from VendedorEntity v where v.id = :id")
    Optional<VendedorEntity> buscarComLockParaAtualizacao(@Param("id") String id);
}
