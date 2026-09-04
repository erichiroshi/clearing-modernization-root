package br.com.erichiroshi.clearing.ingestion.application;

import br.com.erichiroshi.clearing.domain.exception.PosicaoInsuficienteException;
import br.com.erichiroshi.clearing.domain.exception.SaldoInsuficienteException;
import br.com.erichiroshi.clearing.domain.model.Comprador;
import br.com.erichiroshi.clearing.domain.model.StatusTrade;
import br.com.erichiroshi.clearing.domain.model.Trade;
import br.com.erichiroshi.clearing.domain.model.Vendedor;
import br.com.erichiroshi.clearing.domain.repository.CompradorRepository;
import br.com.erichiroshi.clearing.domain.repository.TradeRepository;
import br.com.erichiroshi.clearing.domain.repository.VendedorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExecutarTradeUseCaseTest {

    @Mock
    private CompradorRepository compradorRepository;
    @Mock
    private VendedorRepository vendedorRepository;
    @Mock
    private TradeRepository tradeRepository;
    @Mock
    private TradeEventPublisher tradeEventPublisher;

    private ExecutarTradeUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new ExecutarTradeUseCase(compradorRepository, vendedorRepository, tradeRepository, tradeEventPublisher);
    }

    @Test
    void deveValidarPersistirERegistrarEventoQuandoGarantiasEstaoOk() {
        // Arrange
        Comprador comprador = new Comprador("comp-1", new BigDecimal("1000.00"));
        Vendedor vendedor = new Vendedor("vend-1", Map.of("PETR4", new BigDecimal("100")));
        when(compradorRepository.buscarPorId("comp-1")).thenReturn(Optional.of(comprador));
        when(vendedorRepository.buscarPorId("vend-1")).thenReturn(Optional.of(vendedor));
        when(tradeRepository.salvar(any(Trade.class))).thenAnswer(chamada -> chamada.getArgument(0));

        // Act
        Trade trade = useCase.executar("comp-1", "vend-1", "PETR4", "Petrobras PN",
                new BigDecimal("10"), new BigDecimal("30.00"));

        // Assert
        assertThat(trade.getStatus()).isEqualTo(StatusTrade.VALIDADO);
        verify(compradorRepository).salvar(comprador);
        verify(vendedorRepository).salvar(vendedor);
        verify(tradeEventPublisher).registrarEventoPendente(trade);
    }

    @Test
    void deveLancarExcecaoQuandoCompradorNaoExiste() {
        // Arrange
        when(compradorRepository.buscarPorId("comp-x")).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> useCase.executar("comp-x", "vend-1", "PETR4", "Petrobras PN",
                BigDecimal.TEN, BigDecimal.TEN))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("comp-x");

        verifyNoInteractions(vendedorRepository, tradeRepository, tradeEventPublisher);
    }

    @Test
    void deveLancarExcecaoQuandoVendedorNaoExiste() {
        // Arrange
        when(compradorRepository.buscarPorId("comp-1"))
                .thenReturn(Optional.of(new Comprador("comp-1", BigDecimal.TEN)));
        when(vendedorRepository.buscarPorId("vend-x")).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> useCase.executar("comp-1", "vend-x", "PETR4", "Petrobras PN",
                BigDecimal.ONE, BigDecimal.ONE))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("vend-x");

        verifyNoInteractions(tradeRepository, tradeEventPublisher);
    }

    @Test
    void naoDevePersistirNemRegistrarEventoQuandoSaldoInsuficiente() {
        // Arrange
        Comprador comprador = new Comprador("comp-1", new BigDecimal("10.00"));
        Vendedor vendedor = new Vendedor("vend-1", Map.of("PETR4", new BigDecimal("100")));
        when(compradorRepository.buscarPorId("comp-1")).thenReturn(Optional.of(comprador));
        when(vendedorRepository.buscarPorId("vend-1")).thenReturn(Optional.of(vendedor));

        // Act & Assert
        BigDecimal quantidade = new BigDecimal("10");
        BigDecimal preco = new BigDecimal("30.00");
        assertThatThrownBy(() -> useCase.executar("comp-1", "vend-1", "PETR4", "Petrobras PN",
                quantidade, preco))
                .isInstanceOf(SaldoInsuficienteException.class);

        verify(tradeRepository, never()).salvar(any());
        verifyNoInteractions(tradeEventPublisher);
    }

    @Test
    void naoDevePersistirNemRegistrarEventoQuandoPosicaoInsuficiente() {
        // Arrange
        Comprador comprador = new Comprador("comp-1", new BigDecimal("1000.00"));
        Vendedor vendedor = new Vendedor("vend-1", Map.of("PETR4", new BigDecimal("5")));
        when(compradorRepository.buscarPorId("comp-1")).thenReturn(Optional.of(comprador));
        when(vendedorRepository.buscarPorId("vend-1")).thenReturn(Optional.of(vendedor));

        // Act & Assert
        BigDecimal quantidade = new BigDecimal("10");
        BigDecimal preco = new BigDecimal("30.00");
        assertThatThrownBy(() -> useCase.executar("comp-1", "vend-1", "PETR4", "Petrobras PN",
                quantidade, preco))
                .isInstanceOf(PosicaoInsuficienteException.class);

        verify(tradeRepository, never()).salvar(any());
        verifyNoInteractions(tradeEventPublisher);
    }

    @Test
    void ordemDeChamadasImportaEventoSoDepoisDoTradeSalvo() {
        // Arrange — garante que registrarEventoPendente() recebe o Trade JÁ retornado por
        // tradeRepository.salvar() (o "trade salvo"), não a instância anterior
        Comprador comprador = new Comprador("comp-1", new BigDecimal("1000.00"));
        Vendedor vendedor = new Vendedor("vend-1", Map.of("PETR4", new BigDecimal("100")));
        when(compradorRepository.buscarPorId("comp-1")).thenReturn(Optional.of(comprador));
        when(vendedorRepository.buscarPorId("vend-1")).thenReturn(Optional.of(vendedor));
        when(tradeRepository.salvar(any(Trade.class))).thenAnswer(chamada -> chamada.getArgument(0));

        // Act
        Trade tradeRetornado = useCase.executar("comp-1", "vend-1", "PETR4", "Petrobras PN",
                new BigDecimal("10"), new BigDecimal("30.00"));

        // Assert
        verify(tradeEventPublisher).registrarEventoPendente(tradeRetornado);
    }
}
