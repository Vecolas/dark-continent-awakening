package com.darkcontinent.nenfoundation.client.vfx;

import java.util.Optional;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;

/**
 * O adapter do JOGADOR LOCAL: do conjunto de tecnicas ligadas ao modo de shell.
 *
 * <p><b>ELE NAO DECIDE MAIS NADA.</b> Ate 2026-09-25 este arquivo carregava a
 * propria tabela -- {@code List.of(Zetsu, Ren, Ten)} -- com a regra "tecnica
 * desconhecida nao acende nada". A regra e correta para um datapack de terceiro
 * e foi um desastre para tecnica de primeira parte: ela engoliu Ken, Gyo, Shu e
 * Ko em silencio. Como Ken exclui Ten e Ren, ligar Ken apagava a aura do
 * jogador na tela dele enquanto todos em volta continuavam vendo.
 *
 * <p>A decisao mora em {@link ModoVisualCanonico}. O que sobrou aqui e a unica
 * coisa especifica deste observador: <b>o jogador local sabe o conjunto exato
 * de tecnicas que ligou</b>. O adapter de terceiros nao sabe, e e essa
 * diferenca de CONHECIMENTO que justifica dois adapters -- nunca uma diferenca
 * de decisao.
 */
public final class ModoVisualDeTecnica {

    private ModoVisualDeTecnica() {
    }

    /**
     * A tecnica que manda na tela, ou vazio quando nao ha nenhuma conhecida.
     *
     * <p>Usada para a COR. O modo vem de {@link #de}; separar os dois permite
     * que Ken, Ko e Ren compartilhem a shell de REN e mesmo assim se distingam.
     */
    public static Optional<ResourceLocation> dominante(Set<ResourceLocation> ativas) {
        return ModoVisualCanonico.dominante(ativas);
    }

    /** O modo visual correspondente ao conjunto de tecnicas ligadas. */
    public static AuraVisualMode de(Set<ResourceLocation> ativas) {
        return ModoVisualCanonico.deConjunto(ativas);
    }
}
