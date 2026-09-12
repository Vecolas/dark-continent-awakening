package com.darkcontinent.nenfoundation.enemy.content;

import com.darkcontinent.nenfoundation.enemy.api.CanonLevel;
import com.darkcontinent.nenfoundation.enemy.api.EnemyFaction;
import com.darkcontinent.nenfoundation.enemy.api.EnemyMetadata;
import com.darkcontinent.nenfoundation.enemy.api.ThreatTier;
import com.darkcontinent.nenfoundation.enemy.ai.AmbushRules;
import com.darkcontinent.nenfoundation.enemy.ai.DisguiseRules;
import com.darkcontinent.nenfoundation.enemy.combat.AttackDefinition;
import com.darkcontinent.nenfoundation.enemy.combat.ChargeRules;
import com.darkcontinent.nenfoundation.enemy.combat.GrabRules;
import com.darkcontinent.nenfoundation.enemy.combat.WeakPoint;
import com.darkcontinent.nenfoundation.enemy.combat.WeakPointRegistry;
import com.darkcontinent.nenfoundation.enemy.combat.WeakPointResolver;
import com.darkcontinent.nenfoundation.enemy.data.EnemyAttributes;
import com.darkcontinent.nenfoundation.enemy.data.EnemyDefinition;
import com.darkcontinent.nenfoundation.enemy.spawn.SpawnRule;
import java.util.Map;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;

/** Perfis de balanceamento do primeiro vertical slice; nao registra EntityType. */
public final class HunterExamProfiles {
    private static final String MOD = "nenfoundation";
    private HunterExamProfiles() { }

    public static EnemyDefinition greatStamp() {
        // maxLight 15: great stamp e MobCategory.CREATURE e nasce em manada. Exigir
        // escuridao faria a manada simplesmente nunca nascer -- e isso nao da erro
        // nenhum, aparece como um bioma vazio que ninguem consegue explicar.
        return new EnemyDefinition(metadata("great_stamp", ThreatTier.HUNTER, true, true),
                new EnemyAttributes(70, 0.23F, 11, 7, 28, 0.55F),
                spawn("#nenfoundation:great_stamp_biomes", 0, 15, true, false, 4));
    }

    /**
     * Telegrafo de 18 ticks, corrida de 20, recuperacao de 24.
     *
     * <p>A JANELA ACTIVE NAO E LIVRE: ela e o que decide quantos blocos a carga
     * percorre (activeTicks x velocidade base x multiplicador). Com os 6 ticks
     * originais a corrida cobria ~3,2 blocos e a faixa de disparo comecava em 4 --
     * o great stamp investia e parava ANTES do alvo, sempre, sem erro nenhum no
     * log. Quem mede isso e {@code GreatStampPerfilTest}.</p>
     */
    public static AttackDefinition greatStampCharge() {
        return new AttackDefinition("charge", 18, 20, 24, 16, 1.8F, true, false, true);
    }

    /**
     * Faixa de disparo, espera entre cargas, velocidade e atordoamento da carga.
     *
     * <p>A distancia maxima esta amarrada a janela ACTIVE de
     * {@link #greatStampCharge()}: disparar de mais longe do que a corrida
     * alcanca produz uma investida que nunca chega.</p>
     */
    public static ChargeRules greatStampChargeRules() {
        return new ChargeRules(4.0D, 10.0D, 60, 2.35D, 40);
    }

    public static WeakPointRegistry greatStampWeakPoints() {
        return new WeakPointRegistry(Map.of("forehead", new WeakPoint("forehead", "head", 4.0F, true)));
    }

    /** Geometria que define a testa: acima de 62% da caixa e dentro do cone frontal. */
    public static WeakPointResolver greatStampWeakPoint() {
        return new WeakPointResolver("forehead", "body", 0.62D, 0.5D);
    }

    /**
     * HP 50, dano de rajada 10, velocidade FORA DA TERRA 0.12 -- o sapo so anda
     * depois de desenterrar.
     *
     * <p>maxLight 15: o frog-in-waiting passa o tempo ENTERRADO. Exigir
     * escuridao para ele nascer faria a emboscada simplesmente nunca existir --
     * e isso nao da erro nenhum, aparece como um pantano vazio que ninguem
     * consegue explicar. A faixa antiga (0 a 7) foi escrita antes de existir o
     * estado enterrado, quando "emboscador" ainda queria dizer "noturno".</p>
     */
    public static EnemyDefinition frogInWaiting() {
        return new EnemyDefinition(metadata("frog_in_waiting", ThreatTier.DANGEROUS, true, false),
                new EnemyAttributes(50, 0.12F, 10, 3, 20, 0.35F),
                spawn("#nenfoundation:swamp_predator_biomes", 0, 15, false, true, 2));
    }

    /**
     * Emerge de 10 ticks, bocada de 4, digestao de 20.
     *
     * <p>O WINDUP E O AVISO. Meio segundo de solo se abrindo e o unico tempo
     * que o jogador tem antes de ser engolido; encurtar isto transforma o mob
     * em morte sem telegrafo, que e justamente o que o plano proibe. O WINDUP e
     * interrompivel, a bocada NAO: quem ja foi mordido nao perde a mordida por
     * um tapa dado no mesmo tick.</p>
     */
    public static AttackDefinition frogSwallow() {
        return new AttackDefinition("swallow", 10, 4, 20, 10, 0.4F, true, false, true);
    }

    /**
     * Cilindro de gatilho de 2.5 blocos por 2 de altura, 5 segundos de recarga
     * e 3 segundos sem alvo antes de se enterrar de novo.
     *
     * <p>O raio esta amarrado a caixa (1.4 x 1.0): gatilho maior do que o
     * alcance da bocada produz uma emboscada que emerge longe e nao pega
     * ninguem -- sem erro nenhum no log.</p>
     */
    public static AmbushRules frogAmbushRules() {
        return new AmbushRules(2.5D, 2.0D, 100, 60);
    }

    /**
     * 5 segundos preso, um pulso de 3 de dano por segundo, e 12 de dano NO SAPO
     * compram a soltura.
     *
     * <p>Os numeros se leem juntos: aguentar os 100 ticks calado custa 15 de
     * vida (5 pulsos), enquanto reagir custa acertar 12 num sapo de 50 -- bater
     * tem de ser MELHOR do que esperar, ou a janela de escape e decorativa.</p>
     */
    public static GrabRules frogGrabRules() {
        return new GrabRules(100, 20, 3.0F, 12.0F);
    }

    /**
     * HP 24, dano 4, velocidade 0.29, armadura 1 -- corpo FRAGIL de proposito.
     *
     * <p>A forca do man-faced ape e engano e numeros, nao couro. Dar a ele um
     * corpo que aguenta troca de golpes premiaria justamente o jogador que NAO
     * percebeu o disfarce, e a pista observavel viraria enfeite. Pelo mesmo
     * motivo ele nao tem ponto fraco: nao ha regiao a acertar num bicho que ja
     * morre rapido inteiro.</p>
     *
     * <p>territorial=false e social=true: ele nao defende lugar nenhum (nunca
     * avisa, so espreita), e nasce em BANDO -- o limite de 4 por grupo e o que
     * faz "revelar junto" ter com quem acontecer.</p>
     *
     * <p>maxLight 15: disfarcado de gente, ele precisa nascer de DIA, que e
     * quando alguem passa pela selva e pode ser enganado. Exigir escuridao faria
     * o mob nunca existir na pratica -- e isso nao da erro nenhum, aparece como
     * uma selva vazia que ninguem consegue explicar.</p>
     */
    public static EnemyDefinition manFacedApe() {
        return new EnemyDefinition(metadata("man_faced_ape", ThreatTier.DANGEROUS, false, true),
                new EnemyAttributes(24, 0.29F, 4, 1, 24, 0.1F),
                spawn("#nenfoundation:jungle_ambusher_biomes", 0, 15, true, false, 4));
    }

    /**
     * Golpe de 8 ticks de aviso, 4 de janela e 12 de recuperacao.
     *
     * <p>O DANO NAO E UM NUMERO PROPRIO: ele e lido de {@link #manFacedApe()},
     * porque o corpo a corpo do macaco e o ataque comum dele -- o mesmo
     * ATTACK_DAMAGE que o atributo publica. Repetir o 4 aqui criaria duas fontes
     * para a mesma verdade: girar o atributo numa sessao de balanceamento
     * mudaria o golpe em jogo e nao mudaria este numero, e a divergencia so
     * apareceria como uma representacao que promete um dano diferente do que o
     * jogador leva.</p>
     */
    public static AttackDefinition manFacedApeStrike() {
        return new AttackDefinition("strike", 8, 4, 12,
                manFacedApe().attributes().attackDamage(), 0.5F, true, false, true);
    }

    /**
     * Revela a 3.5 blocos, encara a partir de cosseno 0.6, chama o bando num
     * raio de 12 e telegrafa por 10 ticks.
     *
     * <p>Os numeros se leem juntos: 0.6 e cerca de 53 graus para cada lado do
     * olhar -- o jogador nao precisa mirar em cheio, basta manter o macaco no
     * campo de visao, que e a contrapartida ensinavel. O raio do bando e maior
     * que a distancia de revelacao DE PROPOSITO: quem revela precisa alcancar
     * companheiros que ainda estao longe do jogador, ou "pack ambush" viraria
     * "um macaco por vez".</p>
     */
    public static DisguiseRules manFacedApeDisguise() {
        return new DisguiseRules(3.5D, 0.6D, 12.0D, 10);
    }

    /**
     * Os ids que ESTE repositorio ja publica como entidade registrada.
     *
     * <p>ACRESCENTE O MOB AQUI NO MESMO PR QUE REGISTRA O ENTITYTYPE DELE. O
     * portao de spawn varre esta lista para exigir que toda tag de bioma
     * declarada exista como arquivo E seja referenciada por um biome modifier;
     * um mob de FORA desta lista fica SEM PORTAO, e nada acusa -- ele apenas
     * nunca aparece no mundo, e o relato de bug vira "o bioma esta vazio".</p>
     *
     * <p>"foxbear" fica de fora DE PROPOSITO: ele pertence a outra frente, hoje
     * nao tem arquivo de tag nem biome modifier, e essa divida e dela. Inclui-lo
     * aqui transformaria o portao num alarme que esta lane nao pode desligar --
     * e portao que ninguem consegue apagar acaba ignorado.</p>
     */
    public static Map<String, EnemyDefinition> publicados() {
        return Map.of(
                "great_stamp", greatStamp(),
                "frog_in_waiting", frogInWaiting(),
                "man_faced_ape", manFacedApe());
    }

    public static EnemyDefinition foxbear() {
        return new EnemyDefinition(metadata("foxbear", ThreatTier.HUNTER, true, false),
                new EnemyAttributes(44, 0.25F, 6, 2, 20, 0.25F),
                spawn("#nenfoundation:foxbear_biomes", 0, 12, true, false, 3));
    }

    private static EnemyMetadata metadata(String id, ThreatTier tier, boolean territorial, boolean social) {
        return new EnemyMetadata(ResourceLocation.fromNamespaceAndPath(MOD, id), CanonLevel.CANON_EXACT,
                EnemyFaction.WILDLIFE, tier, territorial, social, id);
    }

    private static SpawnRule spawn(String biome, int minLight, int maxLight, boolean ground,
            boolean water, int groupLimit) {
        return new SpawnRule(Set.of(biome), Set.of("minecraft:overworld"), minLight, maxLight,
                ground, water, false, groupLimit);
    }
}
