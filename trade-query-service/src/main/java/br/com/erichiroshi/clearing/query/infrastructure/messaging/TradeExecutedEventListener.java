package br.com.erichiroshi.clearing.query.infrastructure.messaging;

import br.com.erichiroshi.clearing.contracts.event.TradeExecutedEvent;
import br.com.erichiroshi.clearing.query.application.TradeProjecaoComando;
import br.com.erichiroshi.clearing.query.application.TradeProjectionHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
public class TradeExecutedEventListener {

    private final TradeProjectionHandler handler;

    public TradeExecutedEventListener(TradeProjectionHandler handler) {
        this.handler = handler;
    }

    @KafkaListener(
            topics = "${clearing.kafka.topic.trade-executed}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "virtualThreadsKafkaListenerContainerFactory")
    public void consumir(TradeExecutedEvent evento, Acknowledgment ack) {
        try {
            handler.projetar(paraComando(evento));
            ack.acknowledge();
        } catch (RuntimeException e) {
            // Sem ack: a mensagem será redisponibilizada e reprocessada
            // (a política de retry vem da configuração do container factory,
            // já herdada do Spring Boot via spring.kafka.consumer.*).
            throw new TradeProjectionException(evento.getTradeId(), e);
        }
    }

    private TradeProjecaoComando paraComando(TradeExecutedEvent evento) {
        return new TradeProjecaoComando(
                evento.getTradeId(),
                evento.getBuyerId(),
                evento.getSellerId(),
                evento.getAssetSymbol(),
                evento.getQuantity(),
                evento.getPrice(),
                evento.getTotalAmount(),
                evento.getExecutedAt(),
                evento.getTraceId());
    }
}
