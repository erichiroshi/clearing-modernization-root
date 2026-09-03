package br.com.erichiroshi.clearing.domain.model;

/**
 * Ciclo de vida de um {@link Trade}, refletindo as etapas de uma câmara de
 * compensação real:
 *
 * <ul>
 *   <li>{@link #PENDENTE} — intenção de trade registrada, ainda não validada.</li>
 *   <li>{@link #VALIDADO} — garantias conferidas e bloqueadas (saldo debitado
 *       do comprador, posição reduzida do vendedor). Equivale ao momento em
 *       que a clearing "trava" os dois lados da operação.</li>
 *   <li>{@link #LIQUIDADO} — liquidação confirmada (persistência transacional
 *       e publicação do evento concluídas com sucesso).</li>
 *   <li>{@link #REJEITADO} — validação falhou (saldo ou posição insuficiente);
 *       estado terminal, o trade não avança mais.</li>
 * </ul>
 */
public enum StatusTrade {
    PENDENTE,
    VALIDADO,
    LIQUIDADO,
    REJEITADO
}
