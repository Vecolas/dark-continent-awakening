package com.darkcontinent.nenfoundation.nen.technique;

/**
 * Uma tecnica que muda a velocidade com que a Aura se recupera (ADR-010).
 *
 * <p>POR QUE E UMA INTERFACE A PARTE, e nao um metodo em {@link NenTechnique}:
 *
 * <p>1. {@code NenTechnique} e CONTRATO CONGELADO (ADR-004). Acrescentar um
 * metodo com valor padrao seria compativel e ainda assim uma mudanca no
 * contrato -- e a regra do projeto e que descongelar exige ADR proprio. Aqui
 * nada congelado e tocado.
 *
 * <p>2. A maioria das tecnicas NAO mexe em regeneracao. Um metodo na base faria
 * toda tecnica futura carregar um valor que ela ignora, e "devolve 1.0 porque
 * nao me importo" e indistinguivel de "devolve 1.0 porque alguem esqueceu".
 * Implementar a interface e uma afirmacao; herdar um padrao nao e.
 *
 * <p>O valor NAO e aplicado direto: o motor limita pelo teto de config antes de
 * usar. Ver {@code AuraFormulas.limitarMultiplicador} e o item 5 do ADR-010.
 */
public interface ModificaRegeneracao {

    /**
     * Quanto esta tecnica multiplica a regeneracao enquanto estiver ativa.
     *
     * <p>{@code 1.0} e neutro. Acima acelera, abaixo desacelera, e zero
     * interrompe. O produto entre as tecnicas ativas e o que chega ao motor,
     * limitado pelo teto.
     *
     * <p>ISTO NAO PAGA A MANUTENCAO. Pelo item 6 do ADR-010, o saldo de um
     * estado sustentado e negativo: a regeneracao melhorada REDUZ o custo de
     * manter a tecnica, e nao o anula. Uma tecnica que rendesse mais do que
     * custa viraria o estado obviamente sempre-ligado.
     */
    double multiplicadorDeRegeneracao();
}
