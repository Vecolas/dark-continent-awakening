package com.darkcontinent.nenfoundation.client.vfx;

/**
 * O nivel de detalhe FINAL, com as tres coisas que o decidem num lugar so.
 *
 * <p><b>ELE EXISTE PORQUE A CORRENTE ESTAVA COPIADA EM QUATRO LUGARES.</b> Ate
 * aqui, cada caminho de desenho escrevia a mesma sequencia a mao -- distancia,
 * sobreposicao do dev, teto de qualidade -- e tres deles ainda carregavam um
 * corte proprio em numero cru: 24 blocos no anel de pressao, 96 no passe de
 * brilho, nenhum nos filamentos. Quatro copias da mesma decisao e a definicao de
 * duas fontes para a mesma verdade, e a divergencia nao apareceria como erro:
 * apareceria como um anel que some antes da aura, ou um halo que continua depois
 * dela.
 *
 * <p><b>E ELE FECHA UMA DIVIDA DECLARADA.</b> {@code vfx.distanciaMaxima} estava
 * na tabela de {@code perfis-visuais.md} secao 9 como entrega do AV3 e nunca
 * ganhou consumidor -- uma chave prometida que ninguem implementou e o mesmo
 * defeito do numero orfao, virado do avesso. Aqui ela ganha o unico consumidor
 * que faz sentido: o corte de distancia.
 *
 * <p>A FUNCAO PURA E A PONTA MOLHADA SAO SEPARADAS de proposito, como em
 * {@code SessaoDeVfxDeAura}: {@link #de} nao le config nem estado global, entao a
 * tabela inteira se prova sem subir o jogo. {@link #doCliente} e a linha que
 * busca os tres valores vivos.
 */
public final class AuraLodEfetivo {

    private AuraLodEfetivo() {
    }

    /**
     * O nivel efetivo, dados os tres fatores.
     *
     * <p>A ORDEM IMPORTA, e ela e esta:
     *
     * <ol>
     *   <li><b>a distancia maxima primeiro</b> -- quem pediu para nao ver aura
     *       alem de trinta blocos nao quer ve-la por causa de um comando de dev;
     *   <li><b>a sobreposicao depois</b> -- dentro do alcance, o dev manda;
     *   <li><b>o teto de qualidade por ultimo</b> -- ele e um TETO, e teto se
     *       aplica ao resultado.
     * </ol>
     *
     * <p>Invertendo 1 e 2, {@code /nenvfx lod full} passaria a desenhar aura a
     * duzentos blocos para quem configurou trinta -- e "o meu limite parou de
     * valer" e um relato que ninguem liga a uma ordem de operacoes.
     *
     * @param distanciaMaxima em blocos; alem dela, nada e desenhado
     * @param forcado         o nivel que o comando de dev fixou, ou {@code null}
     */
    public static AuraRenderLod de(double distancia, double distanciaMaxima,
            AuraVisualQuality qualidade, AuraRenderLod forcado) {
        if (!Double.isFinite(distancia) || distancia < 0.0D) {
            // NaN PRECISA SER BARRADO DE PROPOSITO: toda comparacao com ele e
            // falsa, e ele atravessaria os cortes ate o fim. A falha vai na
            // direcao segura -- nao desenhar.
            return AuraRenderLod.HIDDEN;
        }
        if (distancia > distanciaMaxima) {
            return AuraRenderLod.HIDDEN;
        }
        AuraRenderLod porDistancia = forcado != null ? forcado
                : AuraRenderLod.porDistancia(distancia);
        return qualidade == null ? porDistancia : qualidade.limitar(porDistancia);
    }

    /**
     * O nivel efetivo para este cliente, agora.
     *
     * <p>UMA LINHA, E QUATRO CHAMADORES. Era esse o ponto: a layer, o anel, o
     * passe de brilho e a particula perguntam todos aqui, e no dia em que a
     * regra mudar ela muda uma vez.
     */
    public static AuraRenderLod doCliente(double distancia) {
        return de(distancia,
                com.darkcontinent.nenfoundation.config.NenClientConfig.distanciaMaxima(),
                com.darkcontinent.nenfoundation.config.NenClientConfig.qualidade(),
                SobreposicaoDeVfx.lodForcado());
    }

    /**
     * Ate onde vale PROCURAR aura, em blocos.
     *
     * <p>Ela existe para os lacos que varrem jogadores: perguntar o nivel de
     * cada um custa mais que descartar quem esta obviamente longe demais. E o
     * mesmo numero, lido do mesmo lugar -- e nao um corte proprio em numero
     * cru, que era o que havia antes.
     */
    public static double alcanceDeBusca() {
        return com.darkcontinent.nenfoundation.config.NenClientConfig.distanciaMaxima();
    }
}
