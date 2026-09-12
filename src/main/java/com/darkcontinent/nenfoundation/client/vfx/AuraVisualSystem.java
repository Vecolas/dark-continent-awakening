package com.darkcontinent.nenfoundation.client.vfx;

import net.minecraft.world.entity.player.Player;

/**
 * O unico lugar onde o renderer pergunta "qual e a aura desta pessoa?".
 *
 * <p>ELE EXISTE PARA QUE A LAYER NAO CONHECA REDE NEM CACHE. Uma
 * {@code RenderLayer} que buscasse o estado sozinha precisaria saber que o
 * jogador local usa {@link SessaoDeVfxDeAura} e que os outros vem de
 * {@link EstadoVisualDeTerceiro} -- e essa regra passaria a existir em cada
 * renderer novo, divergindo em cada um.
 *
 * <p>A FONTE E INJETADA, e nao construida aqui. Quem sabe montar o estado e o
 * ponto de entrada do cliente, que tem o cache e a sessao; este arquivo so a
 * guarda. Isso mantem {@code client.vfx} testavel sem subir o jogo.
 *
 * <p>QUEM LIGA, DESLIGA. {@link #desligar()} mora no MESMO ponto de saida que
 * ja limpa o cache e a sessao -- o logout. Sem isso, a fonte do mundo anterior
 * continuaria respondendo no intervalo entre sair de um servidor e entrar em
 * outro.
 *
 * <p>ESTADO ESTATICO, E DE PROPOSITO. O Minecraft carrega um mod de cliente por
 * JVM, e a alternativa seria enfiar uma referencia em cada construtor de layer.
 * Isto NAO e o erro numero 2 do CLAUDE.md -- estado de jogador num campo de
 * singleton: aqui nao ha estado de jogador nenhum, so a funcao que responde
 * <b>por jogador</b>.
 */
public final class AuraVisualSystem {

    /** De um jogador para o que desenhar nele. */
    @FunctionalInterface
    public interface FonteDeEstado {
        /** Nunca nulo: quem nao tem aura devolve {@link AuraVisualState#desligado()}. */
        AuraVisualState para(Player jogador);
    }

    private static volatile FonteDeEstado fonte;

    private AuraVisualSystem() {
    }

    /** Liga a fonte. Chamado uma vez, pelo ponto de entrada do cliente. */
    public static void ligar(FonteDeEstado novaFonte) {
        if (novaFonte == null) {
            throw new NullPointerException("fonte de estado visual obrigatoria");
        }
        fonte = novaFonte;
    }

    /** Desliga. O par de {@link #ligar}, e mora no ciclo de vida de quem ligou. */
    public static void desligar() {
        fonte = null;
    }

    /**
     * O estado visual deste jogador. Nunca nulo.
     *
     * <p>SEM FONTE, DEVOLVE DESLIGADO em vez de lancar. Uma excecao no tick de
     * render derruba o desenho do mundo inteiro, e um efeito visual jamais pode
     * fazer isso -- a mesma regra que manda o bloom cair para o modo simples em
     * vez de crashar (ADR-016).
     */
    public static AuraVisualState estadoDe(Player jogador) {
        FonteDeEstado atual = fonte;
        if (atual == null || jogador == null) {
            return AuraVisualState.desligado();
        }
        AuraVisualState estado = atual.para(jogador);
        return estado == null ? AuraVisualState.desligado() : estado;
    }
}
