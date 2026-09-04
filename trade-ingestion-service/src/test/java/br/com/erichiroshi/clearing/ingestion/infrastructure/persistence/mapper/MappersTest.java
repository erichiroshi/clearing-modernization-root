package br.com.erichiroshi.clearing.ingestion.infrastructure.persistence.mapper;

import br.com.erichiroshi.clearing.domain.model.*;
import br.com.erichiroshi.clearing.ingestion.infrastructure.persistence.entity.CompradorEntity;
import br.com.erichiroshi.clearing.ingestion.infrastructure.persistence.entity.TradeEntity;
import br.com.erichiroshi.clearing.ingestion.infrastructure.persistence.entity.VendedorEntity;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class MappersTest {

    @Test
    void compradorRoundTripDevePreservarIdESaldo() {
        Comprador original = new Comprador("comp-1", new BigDecimal("500.00"));

        CompradorEntity entity = CompradorMapper.paraEntidade(original);
        Comprador reconstruido = CompradorMapper.paraDominio(entity);

        assertThat(reconstruido.getId()).isEqualTo(original.getId());
        assertThat(reconstruido.getSaldoDisponivel()).isEqualByComparingTo(original.getSaldoDisponivel());
    }

    @Test
    void compradorAtualizarEntidadeDeveRefletirSaldoMutado() {
        Comprador comprador = new Comprador("comp-1", new BigDecimal("500.00"));
        CompradorEntity entity = CompradorMapper.paraEntidade(comprador);

        comprador.debitar(new BigDecimal("200.00"));
        CompradorMapper.atualizarEntidade(entity, comprador);

        assertThat(entity.getSaldoDisponivel()).isEqualByComparingTo("300.00");
    }

    @Test
    void vendedorRoundTripDevePreservarPosicoes() {
        Vendedor original = new Vendedor("vend-1", Map.of("PETR4", new BigDecimal("100"), "VALE3", new BigDecimal("50")));

        VendedorEntity entity = VendedorMapper.paraEntidade(original);
        Vendedor reconstruido = VendedorMapper.paraDominio(entity);

        assertThat(reconstruido.posicaoDe("PETR4")).isEqualByComparingTo("100");
        assertThat(reconstruido.posicaoDe("VALE3")).isEqualByComparingTo("50");
    }

    @Test
    void vendedorAtualizarEntidadeDeveRefletirPosicaoReduzida() {
        Vendedor vendedor = new Vendedor("vend-1", Map.of("PETR4", new BigDecimal("100")));
        VendedorEntity entity = VendedorMapper.paraEntidade(vendedor);

        vendedor.reduzirPosicao("PETR4", new BigDecimal("40"));
        VendedorMapper.atualizarEntidade(entity, vendedor);

        assertThat(entity.getPosicoes().get("PETR4")).isEqualByComparingTo("60");
    }

    @Test
    void tradeRoundTripDevePreservarTodosOsCampos() {
        Trade original = Trade.reconstituir(
                "trade-1",
                new Comprador("comp-1", new BigDecimal("700.00")),
                new Vendedor("vend-1", Map.of("PETR4", new BigDecimal("90"))),
                new Ativo("PETR4", "Petrobras PN"),
                new BigDecimal("10"), new BigDecimal("30.00"), new BigDecimal("300.00"),
                StatusTrade.LIQUIDADO, Instant.parse("2026-01-01T10:00:00Z"), Instant.parse("2026-01-01T10:00:05Z"));

        CompradorEntity compradorEntity = CompradorMapper.paraEntidade(original.getComprador());
        VendedorEntity vendedorEntity = VendedorMapper.paraEntidade(original.getVendedor());
        TradeEntity tradeEntity = TradeMapper.paraEntidade(original, compradorEntity, vendedorEntity);
        Trade reconstruido = TradeMapper.paraDominio(tradeEntity);

        assertThat(reconstruido.getId()).isEqualTo(original.getId());
        assertThat(reconstruido.getAtivo()).isEqualTo(original.getAtivo());
        assertThat(reconstruido.getQuantidade()).isEqualByComparingTo(original.getQuantidade());
        assertThat(reconstruido.getPreco()).isEqualByComparingTo(original.getPreco());
        assertThat(reconstruido.getValorTotal()).isEqualByComparingTo(original.getValorTotal());
        assertThat(reconstruido.getStatus()).isEqualTo(original.getStatus());
        assertThat(reconstruido.getRegistradoEm()).isEqualTo(original.getRegistradoEm());
        assertThat(reconstruido.getLiquidadoEm()).isEqualTo(original.getLiquidadoEm());
    }
}
