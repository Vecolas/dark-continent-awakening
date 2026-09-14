package com.darkcontinent.nenfoundation.enemy.content;

import com.darkcontinent.nenfoundation.enemy.api.CanonLevel;
import com.darkcontinent.nenfoundation.enemy.api.EnemyFaction;
import com.darkcontinent.nenfoundation.enemy.api.EnemyMetadata;
import com.darkcontinent.nenfoundation.enemy.api.ThreatTier;
import com.darkcontinent.nenfoundation.enemy.chimera.ChimeraDefinition;
import com.darkcontinent.nenfoundation.enemy.chimera.ChimeraOfficerDefinitions;
import com.darkcontinent.nenfoundation.enemy.chimera.ChimeraPeonDefinitions;
import com.darkcontinent.nenfoundation.enemy.combat.StaggerRules;
import com.darkcontinent.nenfoundation.enemy.data.EnemyAttributes;
import com.darkcontinent.nenfoundation.enemy.data.EnemyDefinition;
import com.darkcontinent.nenfoundation.enemy.spawn.SpawnCaps;
import com.darkcontinent.nenfoundation.enemy.spawn.SpawnProfile;
import com.darkcontinent.nenfoundation.enemy.spawn.SpawnRule;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;

/**
 * Perfis das NOVE formigas quimera -- tres peons (#121) e seis officers (#146).
 *
 * <p><b>Arquivo proprio, pelo mesmo motivo de GreedIslandProfiles.</b> As
 * familias respondem a perguntas diferentes, e misturadas o proximo mob herdaria
 * por descuido o formato da errada -- um bicho de colonia vazando para o pool de
 * bioma, ou um bicho de exame que nunca nasce.</p>
 *
 * <p><b>TODAS as nove sao ENCOUNTER_ONLY.</b> Formiga quimera nao brota do bioma:
 * ela nasce de uma COLONIA, e quem decide isso e o controlador de colonia com o
 * teto de rastreamento na mao. Deixa-las no pool de bioma faria o mundo produzir
 * formigas sem colonia nenhuma -- orfas por construcao, alimentando uma colonia
 * fantasma -- e cada uma seria uma entidade legitima, sem nada acusando.</p>
 *
 * <p>A faccao e {@code CHIMERA_ANT}, e e ela que faz civis virarem PRESA e
 * hunters virarem HOSTIL pelo {@code FactionRelations} padrao. Marcar WILDLIFE
 * por descuido tornaria a colonia inteira pacifica, sem erro nenhum.</p>
 */
public final class ChimeraProfiles {
    private static final String MOD = "nenfoundation";

    private ChimeraProfiles() { }

    /**
     * HP 60, dano 9, velocidade 0.21, armadura 8 -- rank PEON.
     *
     * <p>O que SEGURA a linha. Armadura 8 vale de FRENTE, e o encontro e sobre chegar por tras -- baixar a armadura transformaria o quebra-cabeca num saco de pancada. Ele AGARRA, e reusa o GrabController compartilhado.</p>
     */
    public static EnemyDefinition crabHeavy() {
        return new EnemyDefinition(
                new EnemyMetadata(ResourceLocation.fromNamespaceAndPath(MOD, "crab_heavy"),
                        CanonLevel.CANON_DERIVED, EnemyFaction.CHIMERA_ANT,
                        ThreatTier.DANGEROUS, false, true, "crab_heavy"),
                new EnemyAttributes(60, 0.21F, 9, 8, 22, 0.7F),
                new SpawnRule(Set.of(), Set.of("minecraft:overworld"), 0, 15,
                        true, false, false, 4,
                        SpawnProfile.ENCOUNTER_ONLY, new SpawnCaps(4, 0, 0)));
    }

    /** O molde genetico desta familia; ele decide traits e papel no bando. */
    public static ChimeraDefinition crabHeavyMolde() { return ChimeraPeonDefinitions.crabHeavy(); }

    /** Ticks de recarga entre golpes. */
    public static int crabHeavyRecarga() { return 50; }

    /** Limiar, resistencia, decaimento e janela de cambaleio. */
    public static StaggerRules crabHeavyStagger() {
        return new StaggerRules(28.0F, 6.0F, 0.5F, 40);
    }

    /**
     * HP 16, dano 3, velocidade 0.42, armadura 0 -- rank PEON.
     *
     * <p>O que AVISA. Dano 3 de proposito: ele nao e ameaca, e o olho da colonia. O alcance 32 e maior que o de qualquer peon porque ver primeiro E a funcao; matar o batedor antes do relatorio e a resposta que ele ensina.</p>
     */
    public static EnemyDefinition batScout() {
        return new EnemyDefinition(
                new EnemyMetadata(ResourceLocation.fromNamespaceAndPath(MOD, "bat_scout"),
                        CanonLevel.CANON_DERIVED, EnemyFaction.CHIMERA_ANT,
                        ThreatTier.LOW, false, true, "bat_scout"),
                new EnemyAttributes(16, 0.42F, 3, 0, 32, 0.0F),
                new SpawnRule(Set.of(), Set.of("minecraft:overworld"), 0, 15,
                        true, false, false, 4,
                        SpawnProfile.ENCOUNTER_ONLY, new SpawnCaps(4, 0, 0)));
    }

    /** O molde genetico desta familia; ele decide traits e papel no bando. */
    public static ChimeraDefinition batScoutMolde() { return ChimeraPeonDefinitions.batScout(); }

    /** Ticks de recarga entre golpes. */
    public static int batScoutRecarga() { return 30; }

    /** Limiar, resistencia, decaimento e janela de cambaleio. */
    public static StaggerRules batScoutStagger() {
        return new StaggerRules(8.0F, 0.5F, 0.3F, 20);
    }

    /**
     * HP 28, dano 7, velocidade 0.38, armadura 2 -- rank PEON.
     *
     * <p>O que FLANQUEIA. Ele nao vence pelo golpe, vence por CHEGAR -- e a memoria de rastro com prazo e o que o faz perseguir quem fugiu sem virar perseguicao eterna. O prazo separa tenso de injusto.</p>
     */
    public static EnemyDefinition wolfRunner() {
        return new EnemyDefinition(
                new EnemyMetadata(ResourceLocation.fromNamespaceAndPath(MOD, "wolf_runner"),
                        CanonLevel.CANON_DERIVED, EnemyFaction.CHIMERA_ANT,
                        ThreatTier.HUNTER, false, true, "wolf_runner"),
                new EnemyAttributes(28, 0.38F, 7, 2, 30, 0.1F),
                new SpawnRule(Set.of(), Set.of("minecraft:overworld"), 0, 15,
                        true, false, false, 4,
                        SpawnProfile.ENCOUNTER_ONLY, new SpawnCaps(4, 0, 0)));
    }

    /** O molde genetico desta familia; ele decide traits e papel no bando. */
    public static ChimeraDefinition wolfRunnerMolde() { return ChimeraPeonDefinitions.wolfRunner(); }

    /** Ticks de recarga entre golpes. */
    public static int wolfRunnerRecarga() { return 35; }

    /** Limiar, resistencia, decaimento e janela de cambaleio. */
    public static StaggerRules wolfRunnerStagger() {
        return new StaggerRules(14.0F, 1.5F, 0.4F, 25);
    }

    /**
     * HP 70, dano 10, velocidade 0.28, armadura 4 -- rank OFFICER.
     *
     * <p>A que PRENDE a distancia. O perigo dela nao e o dano: e o jogador imobilizado quando o resto do esquadrao chega. Dar a ela dano alto faria a teia virar enfeite, porque ninguem repararia no que a teia custou.</p>
     */
    public static EnemyDefinition spiderWebber() {
        return new EnemyDefinition(
                new EnemyMetadata(ResourceLocation.fromNamespaceAndPath(MOD, "spider_webber"),
                        CanonLevel.CANON_DERIVED, EnemyFaction.CHIMERA_ANT,
                        ThreatTier.ELITE, false, true, "spider_webber"),
                new EnemyAttributes(70, 0.28F, 10, 4, 28, 0.3F),
                new SpawnRule(Set.of(), Set.of("minecraft:overworld"), 0, 15,
                        true, false, false, 1,
                        SpawnProfile.ENCOUNTER_ONLY, new SpawnCaps(1, 0, 0)));
    }

    /** O molde genetico desta familia; ele decide traits e papel no bando. */
    public static ChimeraDefinition spiderWebberMolde() { return ChimeraOfficerDefinitions.spiderWebber(); }

    /** Ticks de recarga entre golpes. */
    public static int spiderWebberRecarga() { return 55; }

    /** Limiar, resistencia, decaimento e janela de cambaleio. */
    public static StaggerRules spiderWebberStagger() {
        return new StaggerRules(34.0F, 5.0F, 0.5F, 40);
    }

    /**
     * HP 55, dano 8, velocidade 0.4, armadura 2 -- rank OFFICER.
     *
     * <p>A que DRENA. Ela fica mais forte com o que tira, e por isso o corpo dela e fraco: um oficial que ja nasce forte nao precisa drenar, e a mecanica inteira vira decoracao.</p>
     */
    public static EnemyDefinition mosquitoOfficer() {
        return new EnemyDefinition(
                new EnemyMetadata(ResourceLocation.fromNamespaceAndPath(MOD, "mosquito_officer"),
                        CanonLevel.CANON_DERIVED, EnemyFaction.CHIMERA_ANT,
                        ThreatTier.ELITE, false, true, "mosquito_officer"),
                new EnemyAttributes(55, 0.4F, 8, 2, 30, 0.1F),
                new SpawnRule(Set.of(), Set.of("minecraft:overworld"), 0, 15,
                        true, false, false, 1,
                        SpawnProfile.ENCOUNTER_ONLY, new SpawnCaps(1, 0, 0)));
    }

    /** O molde genetico desta familia; ele decide traits e papel no bando. */
    public static ChimeraDefinition mosquitoOfficerMolde() { return ChimeraOfficerDefinitions.mosquitoOfficer(); }

    /** Ticks de recarga entre golpes. */
    public static int mosquitoOfficerRecarga() { return 40; }

    /** Limiar, resistencia, decaimento e janela de cambaleio. */
    public static StaggerRules mosquitoOfficerStagger() {
        return new StaggerRules(26.0F, 3.0F, 0.5F, 30);
    }

    /**
     * HP 110, dano 13, velocidade 0.25, armadura 7 -- rank OFFICER.
     *
     * <p>A que ataca em SEQUENCIA. Varios bracos querem dizer varias janelas seguidas, e o que o jogador aprende e esperar a ULTIMA. Telegrafo curto aqui tornaria a sequencia ilegivel, e a resposta viraria sorte.</p>
     */
    public static EnemyDefinition multiarmCentipede() {
        return new EnemyDefinition(
                new EnemyMetadata(ResourceLocation.fromNamespaceAndPath(MOD, "multiarm_centipede"),
                        CanonLevel.CANON_DERIVED, EnemyFaction.CHIMERA_ANT,
                        ThreatTier.ELITE, false, true, "multiarm_centipede"),
                new EnemyAttributes(110, 0.25F, 13, 7, 26, 0.7F),
                new SpawnRule(Set.of(), Set.of("minecraft:overworld"), 0, 15,
                        true, false, false, 1,
                        SpawnProfile.ENCOUNTER_ONLY, new SpawnCaps(1, 0, 0)));
    }

    /** O molde genetico desta familia; ele decide traits e papel no bando. */
    public static ChimeraDefinition multiarmCentipedeMolde() { return ChimeraOfficerDefinitions.multiarmCentipede(); }

    /** Ticks de recarga entre golpes. */
    public static int multiarmCentipedeRecarga() { return 65; }

    /** Limiar, resistencia, decaimento e janela de cambaleio. */
    public static StaggerRules multiarmCentipedeStagger() {
        return new StaggerRules(40.0F, 8.0F, 0.6F, 50);
    }

    /**
     * HP 95, dano 14, velocidade 0.46, armadura 3 -- rank SQUADRON_LEADER.
     *
     * <p>A que CHEGA primeiro. Armadura 3 e baixa de proposito: velocidade 0.46 ja e o poder dela, e somar couro faria um lider que ninguem alcanca e ninguem fere.</p>
     */
    public static EnemyDefinition cheetahLeader() {
        return new EnemyDefinition(
                new EnemyMetadata(ResourceLocation.fromNamespaceAndPath(MOD, "cheetah_leader"),
                        CanonLevel.CANON_DERIVED, EnemyFaction.CHIMERA_ANT,
                        ThreatTier.SQUADRON, false, true, "cheetah_leader"),
                new EnemyAttributes(95, 0.46F, 14, 3, 34, 0.2F),
                new SpawnRule(Set.of(), Set.of("minecraft:overworld"), 0, 15,
                        true, false, false, 1,
                        SpawnProfile.ENCOUNTER_ONLY, new SpawnCaps(1, 0, 0)));
    }

    /** O molde genetico desta familia; ele decide traits e papel no bando. */
    public static ChimeraDefinition cheetahLeaderMolde() { return ChimeraOfficerDefinitions.cheetahLeader(); }

    /** Ticks de recarga entre golpes. */
    public static int cheetahLeaderRecarga() { return 35; }

    /** Limiar, resistencia, decaimento e janela de cambaleio. */
    public static StaggerRules cheetahLeaderStagger() {
        return new StaggerRules(30.0F, 4.0F, 0.5F, 35);
    }

    /**
     * HP 120, dano 12, velocidade 0.27, armadura 8 -- rank SQUADRON_LEADER.
     *
     * <p>A que ENVENENA. O dano direto dela e menor que o do guepardo porque o preco real vem depois do golpe -- e um veneno que some rapido nao e veneno, e um segundo tipo de dano.</p>
     */
    public static EnemyDefinition scorpionLeader() {
        return new EnemyDefinition(
                new EnemyMetadata(ResourceLocation.fromNamespaceAndPath(MOD, "scorpion_leader"),
                        CanonLevel.CANON_DERIVED, EnemyFaction.CHIMERA_ANT,
                        ThreatTier.SQUADRON, false, true, "scorpion_leader"),
                new EnemyAttributes(120, 0.27F, 12, 8, 28, 0.6F),
                new SpawnRule(Set.of(), Set.of("minecraft:overworld"), 0, 15,
                        true, false, false, 1,
                        SpawnProfile.ENCOUNTER_ONLY, new SpawnCaps(1, 0, 0)));
    }

    /** O molde genetico desta familia; ele decide traits e papel no bando. */
    public static ChimeraDefinition scorpionLeaderMolde() { return ChimeraOfficerDefinitions.scorpionLeader(); }

    /** Ticks de recarga entre golpes. */
    public static int scorpionLeaderRecarga() { return 60; }

    /** Limiar, resistencia, decaimento e janela de cambaleio. */
    public static StaggerRules scorpionLeaderStagger() {
        return new StaggerRules(42.0F, 8.0F, 0.6F, 50);
    }

    /**
     * HP 130, dano 15, velocidade 0.32, armadura 6 -- rank SQUADRON_LEADER.
     *
     * <p>A que COMANDA de cima. Alcance 36 e o maior da familia: ela enxerga o campo inteiro e reposiciona o esquadrao. Traze-la para o chao apagaria a unica coisa que a distingue de um oficial forte.</p>
     */
    public static EnemyDefinition avianCommander() {
        return new EnemyDefinition(
                new EnemyMetadata(ResourceLocation.fromNamespaceAndPath(MOD, "avian_commander"),
                        CanonLevel.CANON_DERIVED, EnemyFaction.CHIMERA_ANT,
                        ThreatTier.SQUADRON, false, true, "avian_commander"),
                new EnemyAttributes(130, 0.32F, 15, 6, 36, 0.4F),
                new SpawnRule(Set.of(), Set.of("minecraft:overworld"), 0, 15,
                        true, false, false, 1,
                        SpawnProfile.ENCOUNTER_ONLY, new SpawnCaps(1, 0, 0)));
    }

    /** O molde genetico desta familia; ele decide traits e papel no bando. */
    public static ChimeraDefinition avianCommanderMolde() { return ChimeraOfficerDefinitions.avianCommander(); }

    /** Ticks de recarga entre golpes. */
    public static int avianCommanderRecarga() { return 50; }

    /** Limiar, resistencia, decaimento e janela de cambaleio. */
    public static StaggerRules avianCommanderStagger() {
        return new StaggerRules(44.0F, 9.0F, 0.6F, 45);
    }

    /**
     * As nove, por id.
     *
     * <p>ACRESCENTE A FORMIGA AQUI NO MESMO PR QUE REGISTRA O ENTITYTYPE DELA.
     * Fora desta lista ela sai do catalogo unico, e o portao segue verde varrendo
     * menos -- o falso verde mais barato que existe.</p>
     */
    public static Map<String, EnemyDefinition> publicados() {
        Map<String, EnemyDefinition> mapa = new LinkedHashMap<>();
        mapa.put("crab_heavy", crabHeavy());
        mapa.put("bat_scout", batScout());
        mapa.put("wolf_runner", wolfRunner());
        mapa.put("spider_webber", spiderWebber());
        mapa.put("mosquito_officer", mosquitoOfficer());
        mapa.put("multiarm_centipede", multiarmCentipede());
        mapa.put("cheetah_leader", cheetahLeader());
        mapa.put("scorpion_leader", scorpionLeader());
        mapa.put("avian_commander", avianCommander());
        return Map.copyOf(mapa);
    }
}
