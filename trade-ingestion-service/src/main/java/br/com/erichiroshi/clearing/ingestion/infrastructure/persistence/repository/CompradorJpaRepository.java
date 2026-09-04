package br.com.erichiroshi.clearing.ingestion.infrastructure.persistence.repository;

import br.com.erichiroshi.clearing.ingestion.infrastructure.persistence.entity.CompradorEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CompradorJpaRepository extends JpaRepository<CompradorEntity, String> {

    /**
     * SELECT ... FOR UPDATE — serializa trades concorrentes contra o mesmo
     * comprador dentro de uma transação, evitando dois trades debitarem o
     * mesmo saldo em paralelo (race condition clássica de clearing).
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from CompradorEntity c where c.id = :id")
    Optional<CompradorEntity> buscarComLockParaAtualizacao(@Param("id") String id);
}
