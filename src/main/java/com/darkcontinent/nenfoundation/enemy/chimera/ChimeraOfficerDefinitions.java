package com.darkcontinent.nenfoundation.enemy.chimera;

import com.darkcontinent.nenfoundation.NenFoundation;
import com.darkcontinent.nenfoundation.enemy.ai.squad.SquadRole;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Os SEIS officers e squadron leaders da issue #146.
 *
 * <p><b>Arquivo separado dos peons de proposito.</b> As duas familias respondem a
 * perguntas diferentes: peon e tropa e nasce em quantidade, oficial e excecao e
 * nasce contado. Misturados, o teto por rank de {@code ChimeraTrackingBudget}
 * perderia a leitura mais util que ele tem -- a de que trinta peoes sao um jogo e
 * trinta oficiais sao uma parede.</p>
 *
 * <p><b>Nenhum molde nasce LEADER.</b> Lideranca e PROMOCAO em {@code Squad}, e um
 * molde que ja nascesse lider daria dois lideres ao primeiro bando que alistasse
 * duas dessas formigas -- duas ordens para o mesmo bando, e a ultima vence.</p>
 */
public final class ChimeraOfficerDefinitions {

    private ChimeraOfficerDefinitions() { }

    /**
     * Spider Webber -- rank OFFICER.
     *
     * <p>WEB e garantido porque a teia E o mob: sem ela sobra um oficial fraco com alcance, e o esquadrao perde a peca que prende o jogador no lugar.</p>
     */
    public static ChimeraDefinition spiderWebber() {
        return new ChimeraDefinition(NenFoundation.id("spider_webber"), ChimeraRank.OFFICER,
                ChimeraMorphology.INSECTOID,
                Set.of(ChimeraTrait.WEB, ChimeraTrait.VENOM, ChimeraTrait.CLAWS, ChimeraTrait.EXTRA_ARMS),
                Set.of(ChimeraTrait.WEB),
                SquadRole.RANGED);
    }

    /**
     * Mosquito Officer -- rank OFFICER.
     *
     * <p>WINGS e VENOM garantidos: ela precisa CHEGAR e precisa DRENAR. Um mosquito sem asas vira um oficial terrestre fraco, e sem veneno vira um que so incomoda.</p>
     */
    public static ChimeraDefinition mosquitoOfficer() {
        return new ChimeraDefinition(NenFoundation.id("mosquito_officer"), ChimeraRank.OFFICER,
                ChimeraMorphology.WINGED,
                Set.of(ChimeraTrait.WINGS, ChimeraTrait.VENOM, ChimeraTrait.SPEED, ChimeraTrait.NIGHT_VISION),
                Set.of(ChimeraTrait.WINGS, ChimeraTrait.VENOM),
                SquadRole.FLANKER);
    }

    /**
     * Multiarm Centipede -- rank OFFICER.
     *
     * <p>EXTRA_ARMS e o mob inteiro: varios bracos sao varias janelas seguidas, e o que o jogador aprende e esperar a ULTIMA.</p>
     */
    public static ChimeraDefinition multiarmCentipede() {
        return new ChimeraDefinition(NenFoundation.id("multiarm_centipede"), ChimeraRank.OFFICER,
                ChimeraMorphology.MULTIARM,
                Set.of(ChimeraTrait.EXTRA_ARMS, ChimeraTrait.ARMOR_PLATE, ChimeraTrait.CLAWS, ChimeraTrait.TAIL),
                Set.of(ChimeraTrait.EXTRA_ARMS),
                SquadRole.FRONTLINER);
    }

    /**
     * Cheetah Leader -- rank SQUADRON_LEADER.
     *
     * <p>SPEED e LEAP garantidos: o poder dela e chegar primeiro. Um lider guepardo lento nao e um lider ruim -- e outro bicho.</p>
     */
    public static ChimeraDefinition cheetahLeader() {
        return new ChimeraDefinition(NenFoundation.id("cheetah_leader"), ChimeraRank.SQUADRON_LEADER,
                ChimeraMorphology.QUADRUPED,
                Set.of(ChimeraTrait.SPEED, ChimeraTrait.LEAP, ChimeraTrait.CLAWS, ChimeraTrait.NIGHT_VISION),
                Set.of(ChimeraTrait.SPEED, ChimeraTrait.LEAP),
                SquadRole.FRONTLINER);
    }

    /**
     * Scorpion Leader -- rank SQUADRON_LEADER.
     *
     * <p>VENOM e TAIL garantidos: o preco real do golpe dela vem DEPOIS do golpe, e um veneno que some rapido nao e veneno -- e um segundo tipo de dano.</p>
     */
    public static ChimeraDefinition scorpionLeader() {
        return new ChimeraDefinition(NenFoundation.id("scorpion_leader"), ChimeraRank.SQUADRON_LEADER,
                ChimeraMorphology.INSECTOID,
                Set.of(ChimeraTrait.VENOM, ChimeraTrait.TAIL, ChimeraTrait.ARMOR_PLATE, ChimeraTrait.CLAWS),
                Set.of(ChimeraTrait.VENOM, ChimeraTrait.TAIL),
                SquadRole.FRONTLINER);
    }

    /**
     * Avian Commander -- rank SQUADRON_LEADER.
     *
     * <p>WINGS garantido: ela comanda DE CIMA, e e dai que ve o campo inteiro. Traze-la para o chao apaga a unica coisa que a distingue de um oficial forte.</p>
     */
    public static ChimeraDefinition avianCommander() {
        return new ChimeraDefinition(NenFoundation.id("avian_commander"), ChimeraRank.SQUADRON_LEADER,
                ChimeraMorphology.WINGED,
                Set.of(ChimeraTrait.WINGS, ChimeraTrait.CLAWS, ChimeraTrait.NIGHT_VISION, ChimeraTrait.STRENGTH),
                Set.of(ChimeraTrait.WINGS),
                SquadRole.RANGED);
    }

    /**
     * Os seis, por id.
     *
     * <p>ACRESCENTE O MOLDE AQUI NO MESMO PR QUE O REGISTRA. Fora desta lista ele
     * fica sem portao, e nada acusa.</p>
     */
    public static Map<String, ChimeraDefinition> todos() {
        Map<String, ChimeraDefinition> mapa = new LinkedHashMap<>();
        mapa.put("spider_webber", spiderWebber());
        mapa.put("mosquito_officer", mosquitoOfficer());
        mapa.put("multiarm_centipede", multiarmCentipede());
        mapa.put("cheetah_leader", cheetahLeader());
        mapa.put("scorpion_leader", scorpionLeader());
        mapa.put("avian_commander", avianCommander());
        return Map.copyOf(mapa);
    }
}
