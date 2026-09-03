package br.com.erichiroshi.clearing.domain.model;

import br.com.erichiroshi.clearing.domain.exception.SaldoInsuficienteException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CompradorTest {

    @Test
    void deveDebitarQuandoSaldoSuficiente() {
        // Arrange
        Comprador comprador = new Comprador("comp-1", new BigDecimal("1000.00"));

        // Act
        comprador.debitar(new BigDecimal("400.00"));

        // Assert
        assertThat(comprador.getSaldoDisponivel()).isEqualByComparingTo("600.00");
    }

    @Test
    void deveLancarExcecaoAoDebitarSemSaldoSuficiente() {
        // Arrange
        Comprador comprador = new Comprador("comp-1", new BigDecimal("100.00"));

        // Act & Assert
        BigDecimal valor = new BigDecimal("400.00");
        assertThatThrownBy(() -> comprador.debitar(valor))
                .isInstanceOf(SaldoInsuficienteException.class)
                .hasMessageContaining("comp-1");

        // saldo não deve ter sido alterado numa tentativa que falhou
        assertThat(comprador.getSaldoDisponivel()).isEqualByComparingTo("100.00");
    }

    @Test
    void possuiSaldoSuficienteDeveConsiderarValorExato() {
        Comprador comprador = new Comprador("comp-1", new BigDecimal("500.00"));

        assertThat(comprador.possuiSaldoSuficiente(new BigDecimal("500.00"))).isTrue();
        assertThat(comprador.possuiSaldoSuficiente(new BigDecimal("500.01"))).isFalse();
    }
}
