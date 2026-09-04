package br.com.erichiroshi.clearing.ingestion.application;


import br.com.erichiroshi.clearing.domain.exception.SaldoInsuficienteException;
import br.com.erichiroshi.clearing.domain.model.StatusTrade;
import br.com.erichiroshi.clearing.domain.model.Trade;
import br.com.erichiroshi.clearing.ingestion.infrastructure.persistence.entity.CompradorEntity;
import br.com.erichiroshi.clearing.ingestion.infrastructure.persistence.entity.VendedorEntity;
import br.com.erichiroshi.clearing.ingestion.infrastructure.persistence.repository.CompradorJpaRepository;
import br.com.erichiroshi.clearing.ingestion.infrastructure.persistence.repository.VendedorJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Sobe um PostgreSQL real (Testcontainers) e roda as migrations do Flyway de
 * verdade — valida que o schema, o mapeamento JPA e o rollback transacional
 * funcionam juntos, não só que o código compila.
 */
@SpringBootTest
@Testcontainers
class ExecutarTradeUseCaseIT {

    @Container
    static PostgreSQLContainer postgres = new PostgreSQLContainer(DockerImageName.parse("postgres:17-alpine"))
            .withDatabaseName("clearing")
            .withUsername("clearing")
            .withPassword("clearing");

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private ExecutarTradeUseCase executarTradeUseCase;

    @Autowired
    private CompradorJpaRepository compradorJpaRepository;

    @Autowired
    private VendedorJpaRepository vendedorJpaRepository;

    @BeforeEach
    void setUp() {
        compradorJpaRepository.deleteAll();
        vendedorJpaRepository.deleteAll();
        compradorJpaRepository.save(new CompradorEntity("comp-1", new BigDecimal("1000.00")));
        vendedorJpaRepository.save(new VendedorEntity("vend-1", Map.of("PETR4", new BigDecimal("100"))));
    }

    @Test
    void deveExecutarTradeEPersistirDebitoEReducaoDePosicao() {
        // Act
        Trade trade = executarTradeUseCase.executar(
                "comp-1", "vend-1", "PETR4", "Petrobras PN", new BigDecimal("10"), new BigDecimal("30.00"));

        // Assert
        assertThat(trade.getStatus()).isEqualTo(StatusTrade.VALIDADO);

        CompradorEntity comprador = compradorJpaRepository.findById("comp-1").orElseThrow();
        assertThat(comprador.getSaldoDisponivel()).isEqualByComparingTo("700.00");

        VendedorEntity vendedor = vendedorJpaRepository.findById("vend-1").orElseThrow();
        assertThat(vendedor.getPosicoes().get("PETR4")).isEqualByComparingTo("90");
    }

    @Test
    void deveFazerRollbackCompletoQuandoCompradorNaoTemSaldo() {
        // Act & Assert
        BigDecimal quantidade = new BigDecimal("1000");
        BigDecimal preco = new BigDecimal("30.00");
        assertThatThrownBy(() -> executarTradeUseCase.executar(
                "comp-1", "vend-1", "PETR4", "Petrobras PN", quantidade, preco))
                .isInstanceOf(SaldoInsuficienteException.class);

        // nada deve ter sido persistido — nem o débito parcial do comprador,
        // nem a redução da posição do vendedor
        CompradorEntity comprador = compradorJpaRepository.findById("comp-1").orElseThrow();
        assertThat(comprador.getSaldoDisponivel()).isEqualByComparingTo("1000.00");

        VendedorEntity vendedor = vendedorJpaRepository.findById("vend-1").orElseThrow();
        assertThat(vendedor.getPosicoes().get("PETR4")).isEqualByComparingTo("100");
    }
}
