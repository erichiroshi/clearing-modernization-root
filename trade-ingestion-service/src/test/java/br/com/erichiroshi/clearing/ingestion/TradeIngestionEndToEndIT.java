package br.com.erichiroshi.clearing.ingestion;

import br.com.erichiroshi.clearing.contracts.event.TradeExecutedEvent;
import br.com.erichiroshi.clearing.ingestion.application.ExecutarTradeUseCase;
import br.com.erichiroshi.clearing.ingestion.infrastructure.persistence.entity.CompradorEntity;
import br.com.erichiroshi.clearing.ingestion.infrastructure.persistence.entity.TradeEntity;
import br.com.erichiroshi.clearing.ingestion.infrastructure.persistence.entity.VendedorEntity;
import br.com.erichiroshi.clearing.ingestion.infrastructure.persistence.repository.CompradorJpaRepository;
import br.com.erichiroshi.clearing.ingestion.infrastructure.persistence.repository.TradeJpaRepository;
import br.com.erichiroshi.clearing.ingestion.infrastructure.persistence.repository.VendedorJpaRepository;
import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.ConfluentKafkaContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Teste de ponta a ponta de verdade: sobe Postgres, um cluster Kafka de 1 nó
 * e o Schema Registry via Testcontainers, executa um trade através do
 * ExecutarTradeUseCase e confirma que:
 * <p>
 * 1. a mensagem chega no tópico {@code market.trades.v1}, serializada em
 *    Avro contra o Schema Registry real (não um mock do codec);
 * 2. o {@code OutboxPoller} (rodando de verdade, com scheduling ligado)
 *    processa o evento e o trade avança para {@code LIQUIDADO}.
 * <p>
 * ⚠️ Este é o teste de maior risco desta entrega — encadeia 3 containers,
 * rede Docker customizada e um consumidor Avro manual. Não foi possível
 * executá-lo neste ambiente (sem acesso ao Maven Central / Docker aqui).
 * Se falhar na primeira tentativa, os pontos mais prováveis de ajuste são:
 * (a) o alias de rede "kafka" não bater com o listener interno que o
 * KafkaContainer anuncia — nesse caso, confirme via
 * {@code kafka.getNetworkAliases()}; (b) o Schema Registry demorar mais
 * que o timeout padrão do Testcontainers para ficar pronto — adicione um
 * {@code waitingFor(Wait.forHttp("/subjects"))} explícito.
 */
@Testcontainers
@SpringBootTest(properties = {
        "clearing.outbox.poll-interval-ms=500",
        "clearing.outbox.poller.enabled=true"
})
class TradeIngestionEndToEndIT {

    private static final String TOPICO = "market.trades.v1";
    private static final Network REDE = Network.newNetwork();

    @Container
    static PostgreSQLContainer postgres = new PostgreSQLContainer(
            DockerImageName.parse("postgres:17-alpine")
    )
    .withDatabaseName("clearing")
    .withUsername("clearing")
    .withPassword("clearing")
    .withNetwork(REDE);

    @Container
    static ConfluentKafkaContainer kafka = new ConfluentKafkaContainer(
            DockerImageName.parse("confluentinc/cp-kafka:8.0.7")
    )
    .withNetwork(REDE)
    .withNetworkAliases("kafka");

    @Container
    static GenericContainer<?> schemaRegistry = new GenericContainer<>(
            DockerImageName.parse("confluentinc/cp-schema-registry:7.6.13")
    )
    .withNetwork(REDE)
    .withNetworkAliases("schema-registry")
    .withExposedPorts(8081)
    .withEnv("SCHEMA_REGISTRY_HOST_NAME", "schema-registry")
    .withEnv("SCHEMA_REGISTRY_KAFKASTORE_BOOTSTRAP_SERVERS", "PLAINTEXT://kafka:9093")
    .withEnv("SCHEMA_REGISTRY_LISTENERS", "http://0.0.0.0:8081")
    .dependsOn(kafka)
    .waitingFor(Wait.forHttp("/subjects").forStatusCode(200));

    @DynamicPropertySource
    static void propriedades(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("spring.kafka.producer.properties.schema.registry.url",
                TradeIngestionEndToEndIT::urlSchemaRegistry);
    }

    private static String urlSchemaRegistry() {
        return "http://" + schemaRegistry.getHost() + ":" + schemaRegistry.getMappedPort(8081);
    }

    @Autowired
    private ExecutarTradeUseCase executarTradeUseCase;
    @Autowired
    private CompradorJpaRepository compradorJpaRepository;
    @Autowired
    private VendedorJpaRepository vendedorJpaRepository;
    @Autowired
    private TradeJpaRepository tradeJpaRepository;

    private KafkaConsumer<String, TradeExecutedEvent> consumidorDeTeste;

    @BeforeEach
    void setUp() {
        tradeJpaRepository.deleteAll();
        compradorJpaRepository.deleteAll();
        vendedorJpaRepository.deleteAll();
        compradorJpaRepository.save(new CompradorEntity("comp-1", new BigDecimal("1000.00")));
        vendedorJpaRepository.save(new VendedorEntity("vend-1", Map.of("PETR4", new BigDecimal("100"))));

        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "teste-e2e-" + UUID.randomUUID());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, KafkaAvroDeserializer.class);
        props.put("schema.registry.url", urlSchemaRegistry());
        props.put("specific.avro.reader", true);

        consumidorDeTeste = new KafkaConsumer<>(props);
        consumidorDeTeste.subscribe(List.of(TOPICO));
    }

    @AfterEach
    void tearDown() {
        if (consumidorDeTeste != null) {
            consumidorDeTeste.close();
        }
    }

    @Test
    void deveExecutarTradePublicarNoKafkaEmAvroELiquidarAposConfirmacao() {
        // Act
        String tradeId = executarTradeUseCase.executar(
                "comp-1", "vend-1", "PETR4", "Petrobras PN", new BigDecimal("10"), new BigDecimal("30.00")
        ).getId();

        // Assert — a mensagem chega no tópico, serializada em Avro de verdade
        await().atMost(Duration.ofSeconds(20)).untilAsserted(() -> {
            ConsumerRecords<String, TradeExecutedEvent> registros = consumidorDeTeste.poll(Duration.ofMillis(500));
            List<ConsumerRecord<String, TradeExecutedEvent>> encontrados = new java.util.ArrayList<>();
            registros.forEach(encontrados::add);

            assertThat(encontrados)
                    .anySatisfy(registro -> {
                        assertThat(registro.key()).isEqualTo(tradeId);
                        assertThat(registro.value().getTradeId()).isEqualTo(tradeId);
                        assertThat(registro.value().getAssetSymbol()).isEqualTo("PETR4");
                    });
        });

        // Assert — o outbox poller confirmou a publicação e liquidou o trade
        await().atMost(Duration.ofSeconds(20)).untilAsserted(() -> {
            TradeEntity trade = tradeJpaRepository.findById(tradeId).orElseThrow();
            assertThat(trade.getStatus().name()).isEqualTo("LIQUIDADO");
        });
    }
}
