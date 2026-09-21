package com.darkcontinent.nenfoundation.client.vfx;

import com.darkcontinent.nenfoundation.api.SinalDeAura;

/**
 * Quanto da aura do alvo ESTE observador enxerga, de 0 a 1.
 *
 * <p><b>POR QUE ELE EXISTE, JA QUE HOJE A RESPOSTA E QUASE SEMPRE TRIVIAL.</b>
 * Para que Gyo, In, Ken e En sejam MUDANCA DE NUMERO, e nao um {@code if} novo
 * espalhado por cada renderer. Quando a primeira tecnica de percepcao chegar,
 * ela muda uma tabela; sem este lugar, ela mudaria a layer de jogador, a de mob,
 * a primeira pessoa, a particula e o passe de brilho -- e um deles ficaria para
 * tras, mostrando a aura de quem deveria estar escondido.
 *
 * <p><b>A REGRA QUE MANDA EM TUDO (ADR-001): o cliente nao recebe o que nao deve
 * enxergar.</b> Este resolvedor decide o que desenhar com o que JA CHEGOU; ele
 * nunca e a desculpa para mandar mais. Quem esta em Zetsu chega como
 * {@link SinalDeAura#NENHUM} -- o mesmo byte de quem nunca despertou --, e nao
 * ha o que resolver: nao chegou nada.
 *
 * <p><b>PURA, E SEM NENHUM TIPO DE MINECRAFT.</b> Pelo mesmo motivo de
 * {@code SessaoDeVfxDeAura}: a tabela inteira se prova sem subir o jogo, e e
 * ela que erra. Nao chama {@code Minecraft.getInstance()}, nao guarda estado e
 * nao decide nada de gameplay.
 *
 * <p>OS DOIS RECORDS CARREGAM IDENTIDADE, e so. Nao e pobreza: e o que ha para
 * ler hoje, e um campo sem leitor seria o erro numero 7 do {@code CLAUDE.md}
 * numa variante nova. Eles existem como a COSTURA -- En precisa do PAR
 * (observador, alvo) para responder, e a assinatura ja o tem.
 */
public final class AuraVisibilityResolver {

    /** Quem esta olhando. */
    public record ObservadorDeAura(int idDaEntidade) { }

    /** Quem esta sendo olhado. */
    public record AlvoDeAura(int idDaEntidade) { }

    /**
     * O modo permissivo, SO EM DESENVOLVIMENTO.
     *
     * <p>ELE NAO MUDA O QUE O SERVIDOR ENVIA, e nao pode mudar: ele e uma
     * escolha de DESENHO sobre o que ja chegou. O que ele faz e ignorar a
     * supressao por In -- util para conferir num mundo de teste que a geometria
     * continua certa quando ela volta.
     *
     * <p>NAO E CONFIG DE CLIENTE, e isso e deliberado. Uma chave em
     * {@code NenClientConfig} seria um botao permanente na maquina de qualquer
     * pessoa; aqui e um estado de sessao, ligado por comando e apagado no
     * logout, que ACENDE o aviso de sobreposicao no overlay -- porque uma
     * captura tirada com ele ligado nao vale como aprovacao.
     */
    private static volatile boolean permissivo;

    private AuraVisibilityResolver() {
    }

    /**
     * Quanto desta aura aparece para este observador.
     *
     * @param tecnicaDoAlvo    o sinal JA FILTRADO que o servidor mandou
     * @param gyoDoObservador  se o observador esta concentrando percepcao (F1)
     * @param inDoAlvo         se o alvo esta escondendo a propria aura (F2)
     * @return de 0 a 1
     */
    public static float visibilidade(ObservadorDeAura observador, AlvoDeAura alvo,
            SinalDeAura tecnicaDoAlvo, boolean gyoDoObservador, boolean inDoAlvo) {
        if (observador == null || alvo == null) {
            // SEM O PAR, NAO HA PERGUNTA. Devolver 1 aqui seria desenhar uma
            // aura que ninguem pediu; devolver 0 e a falha na direcao certa --
            // ver de menos, e nunca ver o que deveria estar escondido.
            return 0.0F;
        }
        if (observador.idDaEntidade() == alvo.idDaEntidade()) {
            // VOCE SEMPRE VE A SUA PROPRIA AURA, inclusive escondida dos outros.
            // In esconde de QUEM OLHA, e nao de quem usa -- e sem esta linha o
            // jogador perderia de vista a propria tecnica no instante em que ela
            // funcionasse.
            return 1.0F;
        }
        if (tecnicaDoAlvo == null || tecnicaDoAlvo == SinalDeAura.NENHUM) {
            // QUEM ESTA EM ZETSU E QUEM NUNCA DESPERTOU CHEGAM IGUAIS, e e
            // assim que tem de ser: nao ha bandeira de "escondido" para um
            // cliente modificado ler, porque os dois casos sao o MESMO byte.
            return 0.0F;
        }
        if (inDoAlvo && !gyoDoObservador && !permissivo) {
            // IN ESCONDE A AURA; GYO A ENCONTRA. A tabela e esta, e ela e o
            // motivo de este arquivo existir: quando In chegar de verdade, o
            // que muda e esta linha, e nao cinco renderers.
            return 0.0F;
        }
        return 1.0F;
    }

    /**
     * Liga ou desliga o modo permissivo.
     *
     * <p>Quem chama e o comando de dev, e ele so o aceita com o modo de
     * desenvolvimento ativo. O par -- {@link #limpar()} -- mora no mesmo ponto
     * de saida que ja limpa o cache, a sessao e a sobreposicao de arte.
     */
    public static void permissivo(boolean valor) {
        permissivo = valor;
    }

    /** Se o modo permissivo esta ligado. E o que acende o aviso no overlay. */
    public static boolean permissivo() {
        return permissivo;
    }

    /** Quem liga, desliga: o modo permissivo nao sobrevive ao logout. */
    public static void limpar() {
        permissivo = false;
    }
}
