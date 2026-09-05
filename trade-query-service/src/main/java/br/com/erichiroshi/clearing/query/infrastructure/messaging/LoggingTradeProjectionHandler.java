package br.com.erichiroshi.clearing.query.infrastructure.messaging;

import br.com.erichiroshi.clearing.query.application.TradeProjecaoComando;
import br.com.erichiroshi.clearing.query.application.TradeProjectionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Implementação temporária do {@link TradeProjectionHandler} — só loga o
 * evento recebido. Existe pra o contexto Spring subir com um bean real
 * enquanto a Task 3.2 (persistência MongoDB) não chega. Trocar/remover esta
 * classe quando a implementação de verdade existir.
 */
@Component
public class LoggingTradeProjectionHandler implements TradeProjectionHandler {

    private static final Logger log = LoggerFactory.getLogger(LoggingTradeProjectionHandler.class);

    @Override
    public void projetar(TradeProjecaoComando comando) {
        log.info("[stub Task 3.1] Trade recebido para projeção (MongoDB ainda não implementado — Task 3.2): {}",
                comando);
    }
}
