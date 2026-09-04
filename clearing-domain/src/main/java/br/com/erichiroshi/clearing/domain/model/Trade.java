package br.com.erichiroshi.clearing.domain.model;

import br.com.erichiroshi.clearing.domain.exception.PosicaoInsuficienteException;
import br.com.erichiroshi.clearing.domain.exception.SaldoInsuficienteException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Aggregate root que representa uma operação de compra/venda passando pelo
 * fluxo de uma câmara de compensação: registro da intenção, validação das
 * garantias e liquidação.
 */
public class Trade {

    private final String id;
    private final Comprador comprador;
    private final Vendedor vendedor;
    private final Ativo ativo;
    private final BigDecimal quantidade;
    private final BigDecimal preco;
    private final BigDecimal valorTotal;
    private final Instant registradoEm;
    private StatusTrade status;
    private Instant liquidadoEm;

    private Trade(String id, Comprador comprador, Vendedor vendedor, Ativo ativo,
                  BigDecimal quantidade, BigDecimal preco, BigDecimal valorTotal,
                  StatusTrade status, Instant registradoEm, Instant liquidadoEm) {
        this.id = id;
        this.comprador = comprador;
        this.vendedor = vendedor;
        this.ativo = ativo;
        this.quantidade = quantidade;
        this.preco = preco;
        this.valorTotal = valorTotal;
        this.status = status;
        this.registradoEm = registradoEm;
        this.liquidadoEm = liquidadoEm;
    }

    /**
     * Registra a intenção de um trade novo, sempre como {@link StatusTrade#PENDENTE}.
     * Não mexe em saldo/posição ainda — isso só acontece em {@link #validar()}.
     */
    public static Trade registrar(Comprador comprador, Vendedor vendedor, Ativo ativo,
                                  BigDecimal quantidade, BigDecimal preco) {
        Objects.requireNonNull(comprador, "comprador não pode ser nulo");
        Objects.requireNonNull(vendedor, "vendedor não pode ser nulo");
        Objects.requireNonNull(ativo, "ativo não pode ser nulo");
        if (quantidade == null || quantidade.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("quantidade deve ser maior que zero");
        }
        if (preco == null || preco.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("preço deve ser maior que zero");
        }
        BigDecimal valorTotal = quantidade.multiply(preco);
        return new Trade(UUID.randomUUID().toString(), comprador, vendedor, ativo,
                quantidade, preco, valorTotal, StatusTrade.PENDENTE, Instant.now(), null);
    }

    /**
     * Reconstitui um Trade já existente a partir dos dados persistidos —
     * usado pelos adaptadores de persistência (Task 2.2), nunca pela lógica
     * de negócio, que sempre parte de {@link #registrar}.
     */
    public static Trade reconstituir(String id, Comprador comprador, Vendedor vendedor, Ativo ativo,
                                     BigDecimal quantidade, BigDecimal preco, BigDecimal valorTotal,
                                     StatusTrade status, Instant registradoEm, Instant liquidadoEm) {
        Objects.requireNonNull(id, "id não pode ser nulo");
        Objects.requireNonNull(status, "status não pode ser nulo");
        Objects.requireNonNull(registradoEm, "registradoEm não pode ser nulo");
        return new Trade(id, comprador, vendedor, ativo, quantidade, preco, valorTotal,
                status, registradoEm, liquidadoEm);
    }

    /**
     * Valida as duas garantias da operação — saldo do comprador e posição do
     * vendedor — antes de aplicar qualquer mutação (Delivery versus Payment:
     * nenhuma ponta é executada a menos que as duas possam ser cumpridas).
     * Se ambas estiverem OK, debita o comprador e reduz a posição do
     * vendedor atomicamente e o trade avança para {@link StatusTrade#VALIDADO}.
     * Caso contrário, avança para {@link StatusTrade#REJEITADO} e lança a
     * exceção correspondente.
     */
    public void validar() {
        exigirStatus(StatusTrade.PENDENTE);

        if (!comprador.possuiSaldoSuficiente(valorTotal)) {
            this.status = StatusTrade.REJEITADO;
            throw new SaldoInsuficienteException(comprador.getId(), valorTotal, comprador.getSaldoDisponivel());
        }
        if (!vendedor.possuiPosicaoSuficiente(ativo.getTicker(), quantidade)) {
            this.status = StatusTrade.REJEITADO;
            throw new PosicaoInsuficienteException(
                    vendedor.getId(), ativo.getTicker(), quantidade, vendedor.posicaoDe(ativo.getTicker()));
        }

        comprador.debitar(valorTotal);
        vendedor.reduzirPosicao(ativo.getTicker(), quantidade);
        this.status = StatusTrade.VALIDADO;
    }

    /**
     * Confirma a liquidação do trade. Chamado depois que a persistência
     * transacional (Task 2.2) e a publicação do evento no Kafka (Task 2.3)
     * forem concluídas com sucesso.
     */
    public void liquidar() {
        exigirStatus(StatusTrade.VALIDADO);
        this.status = StatusTrade.LIQUIDADO;
        this.liquidadoEm = Instant.now();
    }

    private void exigirStatus(StatusTrade esperado) {
        if (this.status != esperado) {
            throw new IllegalStateException(
                    "Trade %s está em status %s; esperado %s".formatted(id, status, esperado));
        }
    }

    public String getId() {
        return id;
    }

    public Comprador getComprador() {
        return comprador;
    }

    public Vendedor getVendedor() {
        return vendedor;
    }

    public Ativo getAtivo() {
        return ativo;
    }

    public BigDecimal getQuantidade() {
        return quantidade;
    }

    public BigDecimal getPreco() {
        return preco;
    }

    public BigDecimal getValorTotal() {
        return valorTotal;
    }

    public StatusTrade getStatus() {
        return status;
    }

    public Instant getRegistradoEm() {
        return registradoEm;
    }

    public Instant getLiquidadoEm() {
        return liquidadoEm;
    }
}
