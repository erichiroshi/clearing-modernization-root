package br.com.erichiroshi.clearing.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AtivoTest {

    @Nested
    @DisplayName("Testes de Construção e Métodos de Acesso")
    class Construcao {

        @Test
        @DisplayName("Deve criar um ativo com sucesso e validar os métodos de acesso automáticos do record")
        void deveCriarAtivoComSucesso() {
            Ativo ativo = new Ativo("PETR4", "Petrobras PN");

            // IMPORTANTE para Cobertura: Invocar os métodos de leitura automáticos do record
            assertThat(ativo.ticker()).isEqualTo("PETR4");
            assertThat(ativo.nome()).isEqualTo("Petrobras PN");
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {" ", "   "})
        @DisplayName("Deve lançar exceção quando o ticker for nulo ou vazio")
        void deveLancarExcecaoQuandoTickerInvalido(String tickerInvalido) {
            assertThatThrownBy(() -> new Ativo(tickerInvalido, "Petrobras PN"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("ticker do ativo não pode ser vazio");
        }

        @Test
        @DisplayName("Deve lançar exceção quando o nome for nulo")
        void deveLancarExcecaoQuandoNomeForNulo() {
            assertThatThrownBy(() -> new Ativo("PETR4", null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessage("nome do ativo não pode ser nulo");
        }
    }

    @Nested
    @DisplayName("Testes de Igualdade Customizada (Apenas por Ticker)")
    class Igualdade {

        @Test
        @DisplayName("Deve considerar iguais se tiverem o mesmo ticker, mesmo com nomes diferentes")
        void deveSerIgualApenasPeloTicker() {
            Ativo ativo1 = new Ativo("VALE3", "Vale S.A.");
            Ativo ativo2 = new Ativo("VALE3", "Nome Diferente");

            // Testa o seu equals customizado
            assertThat(ativo1).isEqualTo(ativo2);
            // Testa o seu hashCode customizado
            assertThat(ativo1.hashCode()).hasSameHashCodeAs(ativo2.hashCode());
        }

        @Test
        @DisplayName("Deve considerar diferentes se tiverem tickers diferentes")
        void deveSerDiferenteComTickersDiferentes() {
            Ativo ativo1 = new Ativo("VALE3", "Vale S.A.");
            Ativo ativo2 = new Ativo("PETR4", "Vale S.A.");

            assertThat(ativo1).isNotEqualTo(ativo2);
            assertThat(ativo1.hashCode()).isNotEqualTo(ativo2.hashCode());
        }

        @Test
        @DisplayName("Deve validar as regras básicas do contrato do equals")
        void deveTestarContratosDoEquals() {
            Ativo ativo = new Ativo("VALE3", "Vale S.A.");

            assertThat(ativo)
                    .isEqualTo(ativo) // Mesmo objeto em memória
                    .isNotEqualTo(null) // Comparação com nulo
                    .isNotEqualTo("VALE3"); // Comparação com tipos diferentes
        }
    }

    @Nested
    @DisplayName("Teste do Método ToString Customizado")
    class RepresentacaoTexto {

        @Test
        @DisplayName("Deve retornar apenas o ticker no método toString")
        void deveRetornarTickerNoToString() {
            Ativo ativo = new Ativo("ITUB4", "Itaú Unibanco");

            // Testa o seu toString customizado
            assertThat(ativo.toString()).hasToString("ITUB4");
        }
    }
}
