package com.darkcontinent.nenfoundation.client.vfx;

/**
 * Qualidade visual escolhida por ESTE cliente.
 *
 * <p>NAO MUDA CUSTO, OUTPUT NEM VISIBILIDADE AUTORITATIVA. Dois jogadores no
 * mesmo servidor podem escolher valores diferentes sem que o jogo fique
 * diferente para eles -- e e por isso que isto e config de cliente, e nunca
 * entraria num spec COMMON.
 *
 * <p>ELA IMPOE UM TETO, e nao um deslocamento. Quem escolhe LOW quer menos
 * trabalho perto; longe ja estava barato, e rebaixar de novo deixaria a aura
 * invisivel a media distancia sem ganho nenhum.
 */
public enum AuraVisualQuality {

    /** Sem aura. O Nen continua valendo no servidor; o que some e o desenho. */
    OFF(AuraRenderLod.HIDDEN),

    /** Nunca passa da borda: a leitura sobrevive, o custo cai muito. */
    LOW(AuraRenderLod.FAR),

    /** Sem halo externo e com poucos filamentos. */
    MEDIUM(AuraRenderLod.MEDIUM),

    /** A shell inteira, com filamentos reduzidos perto. */
    HIGH(AuraRenderLod.NEAR),

    /** Tudo o que a distancia permitir. */
    ULTRA(AuraRenderLod.FULL);

    private final AuraRenderLod teto;

    AuraVisualQuality(AuraRenderLod teto) {
        this.teto = teto;
    }

    /**
     * O nivel efetivo: o mais POBRE entre o que a distancia pede e o que a
     * qualidade permite.
     *
     * <p>Como o enum esta ordenado do mais rico para o mais pobre, "o mais
     * pobre" e o de maior ordinal. Essa dependencia da ordem esta fixada por
     * teste, porque reordenar o enum quebraria isto em silencio.
     */
    public AuraRenderLod limitar(AuraRenderLod porDistancia) {
        if (porDistancia == null) {
            throw new NullPointerException("nivel por distancia obrigatorio");
        }
        return porDistancia.ordinal() >= this.teto.ordinal() ? porDistancia : this.teto;
    }

    /** O melhor nivel que esta qualidade permite, independente da distancia. */
    public AuraRenderLod teto() {
        return this.teto;
    }
}
