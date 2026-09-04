package br.com.erichiroshi.clearing.ingestion.infrastructure.persistence.repository;

import br.com.erichiroshi.clearing.ingestion.infrastructure.persistence.entity.TradeEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TradeJpaRepository extends JpaRepository<TradeEntity, String> {
}
