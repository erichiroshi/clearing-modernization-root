package br.com.erichiroshi.clearing.ingestion.infrastructure.outbox;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OutboxEventJpaRepository extends JpaRepository<OutboxEventEntity, Long> {

    List<OutboxEventEntity> findTop50ByStatusOrderByCriadoEmAsc(StatusOutbox status);
}
