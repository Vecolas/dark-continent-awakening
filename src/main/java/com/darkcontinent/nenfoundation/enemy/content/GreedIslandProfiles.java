package com.darkcontinent.nenfoundation.enemy.content;

import com.darkcontinent.nenfoundation.enemy.api.CanonLevel;
import com.darkcontinent.nenfoundation.enemy.api.EnemyFaction;
import com.darkcontinent.nenfoundation.enemy.api.EnemyMetadata;
import com.darkcontinent.nenfoundation.enemy.api.ThreatTier;
import com.darkcontinent.nenfoundation.enemy.balance.StaggerPorPapel;
import com.darkcontinent.nenfoundation.enemy.combat.StaggerRules;
import com.darkcontinent.nenfoundation.enemy.data.EnemyAttributes;
import com.darkcontinent.nenfoundation.enemy.data.EnemyDefinition;
import com.darkcontinent.nenfoundation.enemy.greedisland.CaptureCondition;
import com.darkcontinent.nenfoundation.enemy.greedisland.CardSpec;
import com.darkcontinent.nenfoundation.enemy.spawn.SpawnCaps;
import com.darkcontinent.nenfoundation.enemy.spawn.SpawnProfile;
import com.darkcontinent.nenfoundation.enemy.spawn.SpawnRule;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;

/**
 * Perfis das sete criaturas de Greed Island (issue #119).
 *
 * <p><b>Por que um arquivo proprio, e nao mais metodos em HunterExamProfiles.</b>
 * As duas familias respondem a perguntas diferentes. As do exame nascem no mundo
 * e precisam de tag de bioma, faixa de luz e teto por chunk; as da ilha NAO
 * nascem sozinhas -- elas chegam por encontro, dentro de uma dimensao que a
 * {@code GreedIslandRegion} isola. Misturar as duas no mesmo arquivo faria o
 * proximo mob herdar por descuido o formato da familia errada, e o erro seria
 * silencioso nos dois sentidos: um bicho de GI vazando para o pool de bioma, ou
 * um bicho de exame que nunca nasce.</p>
 *
 * <p><b>TODAS as sete sao ENCOUNTER_ONLY.</b> Isso nao e provisorio: e o
 * isolamento da ilha escrito onde o registro le. Enquanto a dimensao de Greed
 * Island nao existir, um perfil natural apontando para ela precisaria de tag e
 * biome modifier para um lugar que nao ha -- alarme orfao. E depois que ela
 * existir, o spawn continua sendo do controlador de encontro, porque e ele que
 * garante que cada criatura apareca uma vez.</p>
 */
public final class GreedIslandProfiles {
    private static final String MOD = "nenfoundation";

    private GreedIslandProfiles() { }

    /**
     * HP 120, dano 14, velocidade 0.24, armadura 6.
     *
     * <p>Corpo de ELITE e visao de UM olho. O cone estreito nao e detalhe de lore: e a resposta que o encontro ensina -- circular funciona, correr de frente nao. Dar a ele visao normal apagaria o mob inteiro sem nada acusar.</p>
     */
    public static EnemyDefinition cyclops() {
        return new EnemyDefinition(
                new EnemyMetadata(ResourceLocation.fromNamespaceAndPath(MOD, "cyclops"),
                        CanonLevel.CANON_EXACT, EnemyFaction.GREED_ISLAND_MONSTER,
                        ThreatTier.ELITE, true, false, "cyclops"),
                new EnemyAttributes(120, 0.24F, 14, 6, 32, 0.6F),
                new SpawnRule(Set.of(), Set.of(GREED_ISLAND), 0, 15,
                        true, false, false, 1,
                        SpawnProfile.ENCOUNTER_ONLY, new SpawnCaps(1, 0, 0)));
    }

    /** Ticks de recarga entre golpes deste bicho. */
    public static int cyclopsRecarga() { return 50; }

    /**
     * Cai em 2 acertos NO OLHO -- e em nenhum numero de acertos na perna.
     *
     * <p>A absorcao do golpe comum agora e declarada, e nao obtida de lado. O
     * javadoc antigo dizia que a perna "entra com 4 e precisaria de sete acertos";
     * a conta que ficou de fora era o decaimento, que comia 6 entre duas espadadas
     * -- a perna nunca chegaria a lugar nenhum e os sete acertos eram folclore. O
     * comportamento em jogo estava certo pelo motivo errado, e motivo errado nao
     * sobrevive ao proximo ajuste.</p>
     *
     * <p>O multiplicador vem de {@link CyclopsTuning#MULTIPLICADOR_DO_OLHO}, e nao
     * de uma copia: os dois girando juntos e o que impede a regua de medir um olho
     * que mudou de valor.</p>
     */
    public static StaggerRules cyclopsStagger() {
        return StaggerPorPapel.porPontoFraco(ThreatTier.ELITE,
                CyclopsTuning.MULTIPLICADOR_DO_OLHO, 2, 40);
    }

    /**
     * HP 18, dano 0, velocidade 0.0, armadura 0.
     *
     * <p>Velocidade ZERO, e e a ficha. Ele nao persegue: ele espera. Dar a ele qualquer velocidade transformaria um obstaculo posicional num perseguidor fraco, que e o tipo de mob que ninguem lembra.</p>
     */
    public static EnemyDefinition hyperPuffball() {
        return new EnemyDefinition(
                new EnemyMetadata(ResourceLocation.fromNamespaceAndPath(MOD, "hyper_puffball"),
                        CanonLevel.CANON_EXACT, EnemyFaction.GREED_ISLAND_MONSTER,
                        ThreatTier.LOW, true, false, "hyper_puffball"),
                new EnemyAttributes(18, 0.0F, 0, 0, 12, 1.0F),
                new SpawnRule(Set.of(), Set.of(GREED_ISLAND), 0, 15,
                        true, false, false, 1,
                        SpawnProfile.ENCOUNTER_ONLY, new SpawnCaps(1, 0, 0)));
    }

    /** Ticks de recarga entre golpes deste bicho. */
    public static int hyperPuffballRecarga() { return 60; }

    /**
     * Cai em 2 acertos seguidos: ele nao anda e nao persegue; a unica defesa dele e o esporo.
     *
     * <p>Os quatro numeros saem de {@link StaggerPorPapel}, e nao da mao de
     * ninguem. Escritos a mao eles passavam no construtor e a interrupcao nunca
     * acontecia em jogo -- o decaimento entre dois golpes comia mais do que um
     * golpe somava, e nada reprovava.</p>
     */
    public static StaggerRules hyperPuffballStagger() {
        return StaggerPorPapel.de(ThreatTier.LOW, 2, 20);
    }

    /**
     * HP 40, dano 7, velocidade 0.3, armadura 3.
     *
     * <p>Ele AGARRA, e reusa o GrabController compartilhado -- e a segunda prova de que o contrato de agarrao e um so. A captura e nao-letal porque um lagarto morto nao guarda a cor que o card representa.</p>
     */
    public static EnemyDefinition melaninLizard() {
        return new EnemyDefinition(
                new EnemyMetadata(ResourceLocation.fromNamespaceAndPath(MOD, "melanin_lizard"),
                        CanonLevel.CANON_EXACT, EnemyFaction.GREED_ISLAND_MONSTER,
                        ThreatTier.HUNTER, false, false, "melanin_lizard"),
                new EnemyAttributes(40, 0.3F, 7, 3, 24, 0.3F),
                new SpawnRule(Set.of(), Set.of(GREED_ISLAND), 0, 15,
                        true, false, false, 1,
                        SpawnProfile.ENCOUNTER_ONLY, new SpawnCaps(1, 0, 0)));
    }

    /** Ticks de recarga entre golpes deste bicho. */
    public static int melaninLizardRecarga() { return 45; }

    /**
     * Cai em 3 acertos seguidos: ele vale vivo, entao a interrupcao e a ferramenta de captura.
     *
     * <p>Os quatro numeros saem de {@link StaggerPorPapel}, e nao da mao de
     * ninguem. Escritos a mao eles passavam no construtor e a interrupcao nunca
     * acontecia em jogo -- o decaimento entre dois golpes comia mais do que um
     * golpe somava, e nada reprovava.</p>
     */
    public static StaggerRules melaninLizardStagger() {
        return StaggerPorPapel.de(ThreatTier.HUNTER, 3, 30);
    }

    /**
     * HP 10, dano 2, velocidade 0.4, armadura 0.
     *
     * <p>Dano 2 de proposito: ele nao e uma ameaca, e um ALARME. O perigo dele e o que ele chama. Subir o dano faria o jogador mata-lo por reflexo antes de entender o que ele faz, e a licao se perderia.</p>
     */
    public static EnemyDefinition radioRat() {
        return new EnemyDefinition(
                new EnemyMetadata(ResourceLocation.fromNamespaceAndPath(MOD, "radio_rat"),
                        CanonLevel.CANON_EXACT, EnemyFaction.GREED_ISLAND_MONSTER,
                        ThreatTier.LOW, false, true, "radio_rat"),
                new EnemyAttributes(10, 0.4F, 2, 0, 20, 0.0F),
                new SpawnRule(Set.of(), Set.of(GREED_ISLAND), 0, 15,
                        true, false, false, 4,
                        SpawnProfile.ENCOUNTER_ONLY, new SpawnCaps(4, 0, 0)));
    }

    /** Ticks de recarga entre golpes deste bicho. */
    public static int radioRatRecarga() { return 40; }

    /**
     * Cai em 2 acertos seguidos: calar o mensageiro depressa e a leitura certa do encontro.
     *
     * <p>Os quatro numeros saem de {@link StaggerPorPapel}, e nao da mao de
     * ninguem. Escritos a mao eles passavam no construtor e a interrupcao nunca
     * acontecia em jogo -- o decaimento entre dois golpes comia mais do que um
     * golpe somava, e nada reprovava.</p>
     */
    public static StaggerRules radioRatStagger() {
        return StaggerPorPapel.de(ThreatTier.LOW, 2, 20);
    }

    /**
     * HP 30, dano 3, velocidade 0.45, armadura 1.
     *
     * <p>SO VALE VIVO. A condicao nao-letal e o mob inteiro: matar cancela o card, e e isso que faz o jogador ter de parar de bater. Equilibrar isto para um combate agradavel apagaria a unica coisa que ele ensina.</p>
     */
    public static EnemyDefinition bubbleHorse() {
        return new EnemyDefinition(
                new EnemyMetadata(ResourceLocation.fromNamespaceAndPath(MOD, "bubble_horse"),
                        CanonLevel.CANON_EXACT, EnemyFaction.GREED_ISLAND_MONSTER,
                        ThreatTier.LOW, false, false, "bubble_horse"),
                new EnemyAttributes(30, 0.45F, 3, 1, 24, 0.2F),
                new SpawnRule(Set.of(), Set.of(GREED_ISLAND), 0, 15,
                        true, false, false, 1,
                        SpawnProfile.ENCOUNTER_ONLY, new SpawnCaps(1, 0, 0)));
    }

    /** Ticks de recarga entre golpes deste bicho. */
    public static int bubbleHorseRecarga() { return 70; }

    /**
     * Cai em 2 acertos seguidos: ele so vale vivo: interromper a fuga e o jogo inteiro.
     *
     * <p>Os quatro numeros saem de {@link StaggerPorPapel}, e nao da mao de
     * ninguem. Escritos a mao eles passavam no construtor e a interrupcao nunca
     * acontecia em jogo -- o decaimento entre dois golpes comia mais do que um
     * golpe somava, e nada reprovava.</p>
     */
    public static StaggerRules bubbleHorseStagger() {
        return StaggerPorPapel.de(ThreatTier.LOW, 2, 25);
    }

    /**
     * HP 90, dano 12, velocidade 0.26, armadura 9.
     *
     * <p>Armadura 9 e a ficha: bater na carapaca nao paga. O ponto fraco fica EMBAIXO, e o encontro e sobre virar o bicho. Baixar a armadura transformaria o quebra-cabeca num saco de pancada.</p>
     */
    public static EnemyDefinition kingWhiteStagBeetle() {
        return new EnemyDefinition(
                new EnemyMetadata(ResourceLocation.fromNamespaceAndPath(MOD, "king_white_stag_beetle"),
                        CanonLevel.CANON_EXACT, EnemyFaction.GREED_ISLAND_MONSTER,
                        ThreatTier.DANGEROUS, true, false, "king_white_stag_beetle"),
                new EnemyAttributes(90, 0.26F, 12, 9, 28, 0.8F),
                new SpawnRule(Set.of(), Set.of(GREED_ISLAND), 0, 15,
                        true, false, false, 1,
                        SpawnProfile.ENCOUNTER_ONLY, new SpawnCaps(1, 0, 0)));
    }

    /** Ticks de recarga entre golpes deste bicho. */
    public static int kingWhiteStagBeetleRecarga() { return 55; }

    /**
     * Cai em 2 acertos NO VENTRE; a carapaca nao acumula nada, nunca.
     *
     * <p>"Vire-o. O ventre e a unica coisa que vale acertar" so e verdade se bater
     * na casca somar ZERO -- e agora soma zero por construcao, em vez de somar
     * quase nada e ser apagado pelo decaimento. A diferenca nao aparece em jogo
     * hoje; ela aparece no dia em que alguem girar o decaimento e a casca voltar a
     * contar sem que nada reprove.</p>
     */
    public static StaggerRules kingWhiteStagBeetleStagger() {
        return StaggerPorPapel.porPontoFraco(ThreatTier.DANGEROUS,
                KingWhiteStagBeetleTuning.MULTIPLICADOR_DO_VENTRE, 2, 45);
    }

    /**
     * HP 26, dano 6, velocidade 0.34, armadura 2.
     *
     * <p>Corpo FRACO de proposito: um lobo sozinho perde. O que vence e o bando, e por isso ele e o primeiro consumidor de Squad. Dar a ele corpo forte faria a matilha ser desnecessaria.</p>
     */
    public static EnemyDefinition wolfPackHunter() {
        return new EnemyDefinition(
                new EnemyMetadata(ResourceLocation.fromNamespaceAndPath(MOD, "wolf_pack_hunter"),
                        CanonLevel.CANON_EXACT, EnemyFaction.GREED_ISLAND_MONSTER,
                        ThreatTier.HUNTER, false, true, "wolf_pack_hunter"),
                new EnemyAttributes(26, 0.34F, 6, 2, 28, 0.1F),
                new SpawnRule(Set.of(), Set.of(GREED_ISLAND), 0, 15,
                        true, false, false, 4,
                        SpawnProfile.ENCOUNTER_ONLY, new SpawnCaps(4, 0, 0)));
    }

    /** Ticks de recarga entre golpes deste bicho. */
    public static int wolfPackHunterRecarga() { return 35; }

    /**
     * Cai em 3 acertos seguidos: sozinho ele recua; o que sustenta a matilha e o numero, nao o individuo.
     *
     * <p>Os quatro numeros saem de {@link StaggerPorPapel}, e nao da mao de
     * ninguem. Escritos a mao eles passavam no construtor e a interrupcao nunca
     * acontecia em jogo -- o decaimento entre dois golpes comia mais do que um
     * golpe somava, e nada reprovava.</p>
     */
    public static StaggerRules wolfPackHunterStagger() {
        return StaggerPorPapel.de(ThreatTier.HUNTER, 3, 25);
    }

    /**
     * A dimensao da ilha, escrita como texto porque SpawnRule fala em
     * strings de dimensao. O valor CASA com
     * GreedIslandRegion.DIMENSAO; duas grafias diferentes para a mesma
     * dimensao nao dariam erro -- deixariam a regra de spawn conferindo um lugar
     * e o isolamento conferindo outro.
     */
    private static final String GREED_ISLAND = "nenfoundation:greed_island";

    /**
     * As sete, por id.
     *
     * <p>ACRESCENTE A CRIATURA AQUI NO MESMO PR QUE REGISTRA O ENTITYTYPE DELA.
     * Fora desta lista ela fica sem portao -- sem conferencia de atributos, de
     * loot nem de traducao -- e nada acusa.</p>
     */
    public static Map<String, EnemyDefinition> publicados() {
        Map<String, EnemyDefinition> mapa = new LinkedHashMap<>();
        mapa.put("cyclops", cyclops());
        mapa.put("hyper_puffball", hyperPuffball());
        mapa.put("melanin_lizard", melaninLizard());
        mapa.put("radio_rat", radioRat());
        mapa.put("bubble_horse", bubbleHorse());
        mapa.put("king_white_stag_beetle", kingWhiteStagBeetle());
        mapa.put("wolf_pack_hunter", wolfPackHunter());
        return Map.copyOf(mapa);
    }

    /**
     * O catalogo de cards, por criatura.
     *
     * <p>As copias no mundo sao a regra de Greed Island, e nao balanceamento: na
     * obra cada card tem um numero finito de copias, e e isso que transforma a
     * coleta em disputa. Zero significa ilimitado, e e assim que um card comum se
     * declara.</p>
     */
    public static Map<ResourceLocation, CardSpec> cards() {
        Map<ResourceLocation, CardSpec> mapa = new LinkedHashMap<>();
        mapa.put(ResourceLocation.fromNamespaceAndPath(MOD, "cyclops"),
                new CardSpec(ResourceLocation.fromNamespaceAndPath(MOD, "cyclops"), "A", 3));
        mapa.put(ResourceLocation.fromNamespaceAndPath(MOD, "hyper_puffball"),
                new CardSpec(ResourceLocation.fromNamespaceAndPath(MOD, "hyper_puffball"), "G", 0));
        mapa.put(ResourceLocation.fromNamespaceAndPath(MOD, "melanin_lizard"),
                new CardSpec(ResourceLocation.fromNamespaceAndPath(MOD, "melanin_lizard"), "E", 0));
        mapa.put(ResourceLocation.fromNamespaceAndPath(MOD, "radio_rat"),
                new CardSpec(ResourceLocation.fromNamespaceAndPath(MOD, "radio_rat"), "H", 0));
        mapa.put(ResourceLocation.fromNamespaceAndPath(MOD, "bubble_horse"),
                new CardSpec(ResourceLocation.fromNamespaceAndPath(MOD, "bubble_horse"), "F", 0));
        mapa.put(ResourceLocation.fromNamespaceAndPath(MOD, "king_white_stag_beetle"),
                new CardSpec(ResourceLocation.fromNamespaceAndPath(MOD, "king_white_stag_beetle"), "B", 1));
        mapa.put(ResourceLocation.fromNamespaceAndPath(MOD, "wolf_pack_hunter"),
                new CardSpec(ResourceLocation.fromNamespaceAndPath(MOD, "wolf_pack_hunter"), "D", 0));
        return Map.copyOf(mapa);
    }

    /**
     * A condicao de captura de cada criatura.
     *
     * <p>Ela e TABELA, e nao codigo por bicho: escrita como lambda em cada
     * entidade, a mesma pergunta sairia levemente diferente na decima, e a
     * divergencia so apareceria como um bicho que nunca vira card.</p>
     */
    public static Map<String, CaptureCondition> capturas() {
        Map<String, CaptureCondition> mapa = new LinkedHashMap<>();
        mapa.put("cyclops", CaptureCondition.porAbate());
        mapa.put("hyper_puffball", CaptureCondition.porAbate());
        mapa.put("melanin_lizard", CaptureCondition.porEnfraquecimento());
        mapa.put("radio_rat", CaptureCondition.porAbate());
        mapa.put("bubble_horse", CaptureCondition.porEnfraquecimento());
        mapa.put("king_white_stag_beetle", CaptureCondition.porAbate());
        mapa.put("wolf_pack_hunter", CaptureCondition.porAbate());
        return Map.copyOf(mapa);
    }
}
