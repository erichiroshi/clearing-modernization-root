package br.com.erichiroshi.clearing.domain.model;

import br.com.erichiroshi.clearing.domain.exception.PosicaoInsuficienteException;
import br.com.erichiroshi.clearing.domain.exception.SaldoInsuficienteException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TradeTest {

    private static final Ativo PETR4 = new Ativo("PETR4", "Petrobras PN");

    @Test
    void registrarDeveIniciarComoPendenteSemMexerEmSaldoOuPosicao() {
        // Arrange
        Comprador comprador = new Comprador("comp-1", new BigDecimal("1000.00"));
        Vendedor vendedor = new Vendedor("vend-1", posicoes("PETR4", "100"));

        // Act
        Trade trade = Trade.registrar(comprador, vendedor, PETR4, new BigDecimal("10"), new BigDecimal("30.00"));

        // Assert
        assertThat(trade.getStatus()).isEqualTo(StatusTrade.PENDENTE);
        assertThat(trade.getValorTotal()).isEqualByComparingTo("300.00");
        assertThat(comprador.getSaldoDisponivel()).isEqualByComparingTo("1000.00");
        assertThat(vendedor.posicaoDe("PETR4")).isEqualByComparingTo("100");
    }

    @Test
    void validarComGarantiasOkDeveDebitarReduzirPosicaoEAvancarParaValidado() {
        // Arrange
        Comprador comprador = new Comprador("comp-1", new BigDecimal("1000.00"));
        Vendedor vendedor = new Vendedor("vend-1", posicoes("PETR4", "100"));
        Trade trade = Trade.registrar(comprador, vendedor, PETR4, new BigDecimal("10"), new BigDecimal("30.00"));

        // Act
        trade.validar();

        // Assert
        assertThat(trade.getStatus()).isEqualTo(StatusTrade.VALIDADO);
        assertThat(comprador.getSaldoDisponivel()).isEqualByComparingTo("700.00");
        assertThat(vendedor.posicaoDe("PETR4")).isEqualByComparingTo("90");
    }

    @Test
    void validarSemSaldoDoCompradorDeveRejeitarSemMexerNaPosicaoDoVendedor() {
        // Arrange — comprador não tem saldo, vendedor tem posição de sobra
        Comprador comprador = new Comprador("comp-1", new BigDecimal("50.00"));
        Vendedor vendedor = new Vendedor("vend-1", posicoes("PETR4", "100"));
        Trade trade = Trade.registrar(comprador, vendedor, PETR4, new BigDecimal("10"), new BigDecimal("30.00"));

        // Act & Assert
        assertThatThrownBy(trade::validar).isInstanceOf(SaldoInsuficienteException.class);

        assertThat(trade.getStatus()).isEqualTo(StatusTrade.REJEITADO);
        // Delivery versus Payment: nada é executado se uma ponta falha
        assertThat(comprador.getSaldoDisponivel()).isEqualByComparingTo("50.00");
        assertThat(vendedor.posicaoDe("PETR4")).isEqualByComparingTo("100");
    }

    @Test
    void validarSemPosicaoDoVendedorDeveRejeitarSemDebitarOComprador() {
        // Arrange — vendedor não tem o ativo em custódia suficiente, comprador tem saldo de sobra
        Comprador comprador = new Comprador("comp-1", new BigDecimal("1000.00"));
        Vendedor vendedor = new Vendedor("vend-1", posicoes("PETR4", "5"));
        Trade trade = Trade.registrar(comprador, vendedor, PETR4, new BigDecimal("10"), new BigDecimal("30.00"));

        // Act & Assert
        assertThatThrownBy(trade::validar).isInstanceOf(PosicaoInsuficienteException.class);

        assertThat(trade.getStatus()).isEqualTo(StatusTrade.REJEITADO);
        // Delivery versus Payment: nada é executado se uma ponta falha
        assertThat(comprador.getSaldoDisponivel()).isEqualByComparingTo("1000.00");
        assertThat(vendedor.posicaoDe("PETR4")).isEqualByComparingTo("5");
    }

    @Test
    void naoDevePermitirValidarUmTradeQueJaFoiValidado() {
        Comprador comprador = new Comprador("comp-1", new BigDecimal("1000.00"));
        Vendedor vendedor = new Vendedor("vend-1", posicoes("PETR4", "100"));
        Trade trade = Trade.registrar(comprador, vendedor, PETR4, new BigDecimal("10"), new BigDecimal("30.00"));
        trade.validar();

        assertThatThrownBy(trade::validar).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void liquidarDeveAvancarDeValidadoParaLiquidado() {
        Comprador comprador = new Comprador("comp-1", new BigDecimal("1000.00"));
        Vendedor vendedor = new Vendedor("vend-1", posicoes("PETR4", "100"));
        Trade trade = Trade.registrar(comprador, vendedor, PETR4, new BigDecimal("10"), new BigDecimal("30.00"));
        trade.validar();

        trade.liquidar();

        assertThat(trade.getStatus()).isEqualTo(StatusTrade.LIQUIDADO);
        assertThat(trade.getLiquidadoEm()).isNotNull();
    }

    @Test
    void naoDevePermitirLiquidarUmTradeAindaPendente() {
        Comprador comprador = new Comprador("comp-1", new BigDecimal("1000.00"));
        Vendedor vendedor = new Vendedor("vend-1", posicoes("PETR4", "100"));
        Trade trade = Trade.registrar(comprador, vendedor, PETR4, new BigDecimal("10"), new BigDecimal("30.00"));

        assertThatThrownBy(trade::liquidar).isInstanceOf(IllegalStateException.class);
    }

    private static Map<String, BigDecimal> posicoes(String ticker, String quantidade) {
        Map<String, BigDecimal> posicoes = new HashMap<>();
        posicoes.put(ticker, new BigDecimal(quantidade));
        return posicoes;
    }
}
