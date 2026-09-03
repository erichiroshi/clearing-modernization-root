package br.com.erichiroshi.clearing.domain.model;

import br.com.erichiroshi.clearing.domain.exception.PosicaoInsuficienteException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class VendedorTest {

    @Test
    void deveReduzirPosicaoQuandoCustodiaSuficiente() {
        // Arrange
        Vendedor vendedor = new Vendedor("vend-1", Map.of("PETR4", new BigDecimal("100")));

        // Act
        vendedor.reduzirPosicao("PETR4", new BigDecimal("40"));

        // Assert
        assertThat(vendedor.posicaoDe("PETR4")).isEqualByComparingTo("60");
    }

    @Test
    void deveLancarExcecaoAoReduzirSemPosicaoSuficiente() {
        // Arrange
        Vendedor vendedor = new Vendedor("vend-1", Map.of("PETR4", new BigDecimal("10")));

        // Act & Assert
        BigDecimal quantidade = new BigDecimal("40");
        assertThatThrownBy(() -> vendedor.reduzirPosicao("PETR4", quantidade))
                .isInstanceOf(PosicaoInsuficienteException.class)
                .hasMessageContaining("vend-1")
                .hasMessageContaining("PETR4");

        assertThat(vendedor.posicaoDe("PETR4")).isEqualByComparingTo("10");
    }

    @Test
    void deveLancarExcecaoAoNegociarAtivoQueNaoPossui() {
        Vendedor vendedor = new Vendedor("vend-1", Map.of("PETR4", new BigDecimal("10")));

        BigDecimal quantidade = new BigDecimal("1");
        assertThatThrownBy(() -> vendedor.reduzirPosicao("VALE3", quantidade))
                .isInstanceOf(PosicaoInsuficienteException.class);
    }
}
