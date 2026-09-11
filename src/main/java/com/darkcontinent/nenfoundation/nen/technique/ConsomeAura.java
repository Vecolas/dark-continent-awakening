package com.darkcontinent.nenfoundation.nen.technique;

/**
 * Uma tecnica que cobra Aura enquanto estiver ativa.
 *
 * <p>DECISOES QUE ESTE ARQUIVO CARREGA:
 *
 * <p>1. QUEM COBRA E O SERVICO, e nao a tecnica. A tecnica declara o preco; o
 * ciclo de vida debita antes de tickar, e desliga com
 * {@link StopReason#OUT_OF_AURA} quando nao da. Se cada tecnica cobrasse a
 * propria manutencao, "aura zero encerra a tecnica" viraria uma promessa
 * repetida em N lugares -- e a enesima esqueceria, com a tecnica seguindo de
 * graca.
 *
 * <p>2. E UMA INTERFACE A PARTE, pelo mesmo motivo de
 * {@link ModificaRegeneracao}: {@code NenTechnique} e contrato congelado
 * (ADR-004), e nem toda tecnica cobra manutencao. Implementar e uma afirmacao;
 * herdar um padrao nao e.
 *
 * <p>3. O DEBITO E TUDO OU NADA. Meio tick pago nao existe: ou a tecnica paga o
 * preco inteiro, ou ela cai. Debito parcial e a origem classica de aura
 * consumida sem efeito nenhum.
 */
public interface ConsomeAura {

    /**
     * Quanto esta tecnica cobra NESTE tick.
     *
     * <p>Perguntado a cada tick, e nunca guardado pelo chamador: congelar o
     * custo na ativacao faria a tecnica ignorar toda recarga de config
     * posterior, em silencio.
     *
     * <p>Zero ou negativo significa "nao cobra agora" e nao derruba nada.
     */
    double custoPorTick();
}
