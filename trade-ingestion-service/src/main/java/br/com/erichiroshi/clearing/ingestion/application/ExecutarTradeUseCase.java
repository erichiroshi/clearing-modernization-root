package br.com.erichiroshi.clearing.ingestion.application;

import br.com.erichiroshi.clearing.domain.model.Ativo;
import br.com.erichiroshi.clearing.domain.model.Comprador;
import br.com.erichiroshi.clearing.domain.model.Trade;
import br.com.erichiroshi.clearing.domain.model.Vendedor;
import br.com.erichiroshi.clearing.domain.repository.CompradorRepository;
import br.com.erichiroshi.clearing.domain.repository.TradeRepository;
import br.com.erichiroshi.clearing.domain.repository.VendedorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Orquestra o fluxo de ingestão de um trade: carrega comprador e vendedor
 * (com lock pessimista, via os adaptadores), delega a validação das
 * garantias ao domínio e persiste o resultado — tudo numa única transação.
 * <p>
 * Se {@link Trade#validar()} lançar {@code SaldoInsuficienteException} ou
 * {@code PosicaoInsuficienteException} (RuntimeException), o Spring faz
 * rollback automático: nada fica gravado.
 * <p>
 * A liquidação ({@link Trade#liquidar()}) não acontece aqui — só depois que
 * a publicação do evento no Kafka (Task 2.3) confirmar sucesso.
 */
@Service
public class ExecutarTradeUseCase {

    private final CompradorRepository compradorRepository;
    private final VendedorRepository vendedorRepository;
    private final TradeRepository tradeRepository;

    public ExecutarTradeUseCase(CompradorRepository compradorRepository,
                                 VendedorRepository vendedorRepository,
                                 TradeRepository tradeRepository) {
        this.compradorRepository = compradorRepository;
        this.vendedorRepository = vendedorRepository;
        this.tradeRepository = tradeRepository;
    }

    @Transactional
    public Trade executar(String compradorId, String vendedorId, String ativoTicker, String ativoNome,
                           BigDecimal quantidade, BigDecimal preco) {
        Comprador comprador = compradorRepository.buscarPorId(compradorId)
                .orElseThrow(() -> new IllegalArgumentException("Comprador não encontrado: " + compradorId));
        Vendedor vendedor = vendedorRepository.buscarPorId(vendedorId)
                .orElseThrow(() -> new IllegalArgumentException("Vendedor não encontrado: " + vendedorId));
        Ativo ativo = new Ativo(ativoTicker, ativoNome);

        Trade trade = Trade.registrar(comprador, vendedor, ativo, quantidade, preco);
        trade.validar();

        compradorRepository.salvar(comprador);
        vendedorRepository.salvar(vendedor);
        return tradeRepository.salvar(trade);
    }
}
