package com.darkcontinent.nenfoundation.enemy.balance;

import com.darkcontinent.nenfoundation.enemy.api.ThreatTier;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/**
 * Quanto tempo um bicho de cada papel DEVE durar -- e a faixa e o contrato.
 *
 * <p><b>O erro que isto pega nao da erro.</b> Um ELITE que morre em dois segundos
 * e um LOW que leva um minuto passam por todos os portoes: os numeros sao
 * validos, os atributos sao finitos, o mob nasce e morre. O que eles quebram e o
 * ROTULO -- o jogador aprende que ThreatTier nao quer dizer nada, e um rotulo em
 * que ninguem confia e pior do que nenhum rotulo.</p>
 *
 * <p><b>Faixa, e nao valor.</b> Um alvo unico por papel faria toda criatura do
 * mesmo tier ter a mesma vida efetiva, e o bestiario ficaria plano. A faixa da
 * espaco para um LOW duro e um LOW fragil serem os dois LOW.</p>
 */
public record FaixaDeTempo(double minimoEmSegundos, double maximoEmSegundos) {

    public FaixaDeTempo {
        if (!Double.isFinite(minimoEmSegundos) || !Double.isFinite(maximoEmSegundos)
                || minimoEmSegundos <= 0 || maximoEmSegundos <= minimoEmSegundos) {
            throw new IllegalArgumentException("faixa de tempo invalida: " + minimoEmSegundos
                    + ".." + maximoEmSegundos);
        }
    }

    public boolean contem(double segundos) {
        return segundos >= minimoEmSegundos && segundos <= maximoEmSegundos;
    }

    /**
     * As faixas por papel, contra o loadout que ENCONTRA aquele papel.
     *
     * <p>Elas sobem com o tier e nao dobram a cada degrau de proposito: um
     * combate que passa de meio minuto deixa de ser tenso e vira desgaste, e o
     * limite de cima existe para impedir que "dificil" seja resolvido enchendo a
     * barra de vida -- que e exatamente o que a issue #150 proibe.</p>
     */
    public static Map<ThreatTier, FaixaDeTempo> porPapel() {
        EnumMap<ThreatTier, FaixaDeTempo> faixas = new EnumMap<>(ThreatTier.class);
        faixas.put(ThreatTier.PASSIVE, new FaixaDeTempo(0.4D, 6.0D));
        faixas.put(ThreatTier.LOW, new FaixaDeTempo(0.4D, 8.0D));
        faixas.put(ThreatTier.HUNTER, new FaixaDeTempo(1.5D, 16.0D));
        faixas.put(ThreatTier.DANGEROUS, new FaixaDeTempo(2.5D, 26.0D));
        faixas.put(ThreatTier.ELITE, new FaixaDeTempo(3.0D, 36.0D));
        faixas.put(ThreatTier.SQUADRON, new FaixaDeTempo(5.0D, 50.0D));
        faixas.put(ThreatTier.BOSS, new FaixaDeTempo(15.0D, 90.0D));
        faixas.put(ThreatTier.APEX, new FaixaDeTempo(20.0D, 180.0D));
        return Map.copyOf(faixas);
    }

    /**
     * O loadout que encontra cada papel.
     *
     * <p>Medir um oficial de Chimera contra a espada de pedra diria que ele e
     * impossivel, e medir um bicho do exame contra diamante diria que ele e
     * trivial. Os dois estariam certos e nenhum seria util: a faixa so significa
     * alguma coisa contra o jogador que de fato chega naquele conteudo.</p>
     */
    public static LoadoutDeReferencia loadoutDe(ThreatTier papel) {
        return switch (Objects.requireNonNull(papel, "papel ausente")) {
            case PASSIVE, LOW, HUNTER -> LoadoutDeReferencia.inicial();
            case DANGEROUS, ELITE -> LoadoutDeReferencia.preparado();
            case SQUADRON, BOSS, APEX -> LoadoutDeReferencia.veterano();
        };
    }
}
