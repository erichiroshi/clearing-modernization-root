package br.com.erichiroshi.clearing.query.infrastructure.messaging;

import org.springframework.boot.kafka.autoconfigure.ConcurrentKafkaListenerContainerFactoryConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;

/**
 * Customiza o container factory autoconfigurado pelo Spring Boot (a partir
 * de {@code spring.kafka.consumer.*} no application.yml) em dois pontos:
 * <p>
 * 1. Ack manual ({@code MANUAL_IMMEDIATE}) — o offset só avança depois que
 * {@link TradeExecutedEventListener} confirma que o handler processou a
 * mensagem com sucesso. Sem isso, uma falha no meio do processamento
 * perderia a mensagem silenciosamente (ack automático já teria avançado o
 * offset antes do processamento terminar).
 * <p>
 * 2. Virtual Threads (Java 25) no executor que roda os listeners — usa
 * {@link SimpleAsyncTaskExecutor#setVirtualThreads(boolean)}, a forma
 * suportada nativamente pelo Spring Framework para isso, em vez de
 * configuração manual de {@code ExecutorService}.
 * <p>
 * Usamos {@link ConcurrentKafkaListenerContainerFactoryConfigurer} (também
 * autoconfigurado pelo Boot) para aplicar toda a configuração padrão vinda
 * das properties antes de sobrepor esses dois pontos — assim não
 * duplicamos manualmente o que o Boot já monta a partir do application.yml.
 */
@Configuration
public class KafkaConsumerConfig {

    static {
        // Avro 1.12+ bloqueia por padrão a reflexão sobre classes específicas
        // fora de pacotes "confiáveis" (CVE-2024-47561). Sem isso, o
        // KafkaAvroDeserializer lança SecurityException ao tentar
        // desserializar TradeExecutedEvent (mesmo ajuste feito no
        // OutboxEventProcessor do trade-ingestion-service, do lado produtor).
        System.setProperty("org.apache.avro.SERIALIZABLE_PACKAGES",
                "br.com.erichiroshi.clearing.contracts.event");
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<Object, Object> virtualThreadsKafkaListenerContainerFactory(
            ConcurrentKafkaListenerContainerFactoryConfigurer configurer,
            ConsumerFactory<Object, Object> consumerFactory) {

        ConcurrentKafkaListenerContainerFactory<Object, Object> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        configurer.configure(factory, consumerFactory);

        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);

        SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor("kafka-consumer-vt-");
        executor.setVirtualThreads(true);
        factory.getContainerProperties().setListenerTaskExecutor(executor);

        return factory;
    }
}
