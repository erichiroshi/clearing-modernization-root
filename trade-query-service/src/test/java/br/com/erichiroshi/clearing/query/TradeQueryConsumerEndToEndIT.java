package br.com.erichiroshi.clearing.query;

import br.com.erichiroshi.clearing.contracts.event.TradeExecutedEvent;
import br.com.erichiroshi.clearing.query.application.TradeProjecaoComando;
import br.com.erichiroshi.clearing.query.application.TradeProjectionHandler;
import io.confluent.kafka.serializers.KafkaAvroSerializer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.ConfluentKafkaContainer;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Sobe Kafka + Schema Registry reais via Testcontainers, publica um
 * TradeExecutedEvent de verdade no tópico (como o trade-ingestion-service
 * faria) e confirma que {@code TradeExecutedEventListener} consome,
 * desserializa e chama o {@link TradeProjectionHandler} — sem depender do
 * MongoDB, que só existe na Task 3.2 (o handler é mockado aqui de propósito,
 * já que testar a persistência real não é escopo desta task).
 */
@Testcontainers
@SpringBootTest
class TradeQueryConsumerEndToEndIT {

    private static final String TOPICO = "market.trades.v1";
    private static final Network REDE = Network.newNetwork();

    @Container
    static ConfluentKafkaContainer kafka = new ConfluentKafkaContainer(
            DockerImageName.parse("confluentinc/cp-kafka:8.0.7")
    )
            .withNetwork(REDE)
            .withNetworkAliases("kafka");

    @Container
    static GenericContainer<?> schemaRegistry = new GenericContainer<>(
            DockerImageName.parse("confluentinc/cp-schema-registry:7.6.13"))
            .withNetwork(REDE)
            .withNetworkAliases("schema-registry")
            .withExposedPorts(8081)
            .withEnv("SCHEMA_REGISTRY_HOST_NAME", "schema-registry")
            .withEnv("SCHEMA_REGISTRY_KAFKASTORE_BOOTSTRAP_SERVERS", "PLAINTEXT://kafka:9093")
            .withEnv("SCHEMA_REGISTRY_LISTENERS", "http://0.0.0.0:8081")
            .dependsOn(kafka);

    @DynamicPropertySource
    static void propriedades(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("spring.kafka.consumer.properties.schema.registry.url",
                TradeQueryConsumerEndToEndIT::urlSchemaRegistry);
    }

    private static String urlSchemaRegistry() {
        return "http://" + schemaRegistry.getHost() + ":" + schemaRegistry.getMappedPort(8081);
    }

    @TestConfiguration
    static class MockHandlerConfig {
        @Bean
        @Primary
        TradeProjectionHandler tradeProjectionHandler() {
            return mock(TradeProjectionHandler.class);
        }
    }

    @org.springframework.beans.factory.annotation.Autowired
    private TradeProjectionHandler handler;

    private KafkaProducer<String, Object> produtorDeTeste;

    @AfterEach
    void tearDown() {
        if (produtorDeTeste != null) {
            produtorDeTeste.close();
        }
    }

    @Test
    void deveConsumirEventoPublicadoNoKafkaEChamarOHandler() {
        // Arrange
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaAvroSerializer.class);
        props.put("schema.registry.url", urlSchemaRegistry());
        produtorDeTeste = new KafkaProducer<>(props);

        TradeExecutedEvent evento = TradeExecutedEvent.newBuilder()
                .setTradeId("trade-e2e-1")
                .setBuyerId("comp-1")
                .setSellerId("vend-1")
                .setAssetSymbol("PETR4")
                .setQuantity(new BigDecimal("10"))
                .setPrice(new BigDecimal("30.00"))
                .setTotalAmount(new BigDecimal("300.00"))
                .setExecutedAt(Instant.now())
                .setTraceId(null)
                .build();

        // Act
        produtorDeTeste.send(new ProducerRecord<>(TOPICO, "trade-e2e-1", evento));
        produtorDeTeste.flush();

        // Assert
        await().atMost(Duration.ofSeconds(20)).untilAsserted(() ->
                verify(handler).projetar(org.mockito.ArgumentMatchers.argThat(
                        (TradeProjecaoComando comando) -> comando.tradeId().equals("trade-e2e-1")
                                && comando.assetSymbol().equals("PETR4"))));
    }
}
