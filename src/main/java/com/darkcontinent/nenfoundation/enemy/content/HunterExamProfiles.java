package com.darkcontinent.nenfoundation.enemy.content;

import com.darkcontinent.nenfoundation.enemy.api.CanonLevel;
import com.darkcontinent.nenfoundation.enemy.api.EnemyFaction;
import com.darkcontinent.nenfoundation.enemy.api.EnemyMetadata;
import com.darkcontinent.nenfoundation.enemy.api.ThreatTier;
import com.darkcontinent.nenfoundation.enemy.ai.AmbushRules;
import com.darkcontinent.nenfoundation.enemy.ai.DisguiseRules;
import com.darkcontinent.nenfoundation.enemy.ai.NestGuardRules;
import com.darkcontinent.nenfoundation.enemy.combat.AttackDefinition;
import com.darkcontinent.nenfoundation.enemy.combat.ChargeRules;
import com.darkcontinent.nenfoundation.enemy.combat.GrabRules;
import com.darkcontinent.nenfoundation.enemy.combat.WeakPoint;
import com.darkcontinent.nenfoundation.enemy.combat.WeakPointRegistry;
import com.darkcontinent.nenfoundation.enemy.combat.WeakPointResolver;
import com.darkcontinent.nenfoundation.enemy.data.EnemyAttributes;
import com.darkcontinent.nenfoundation.enemy.data.EnemyDefinition;
import com.darkcontinent.nenfoundation.enemy.encounter.RegrasDeFisgada;
import com.darkcontinent.nenfoundation.enemy.encounter.RegrasDeJulgamento;
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
     * HP 28, dano 6, velocidade 0.35, armadura 2 -- corpo LEVE e rapido.
     *
     * <p>territorial=true e social=false: a spider eagle e o primeiro mob do
     * repositorio em que "territorio" e literal -- ela defende um LUGAR (o
     * ninho), nao um raio ao redor do proprio corpo. E ela nasce sozinha: um
     * ninho e de um casal, nao de um bando, e o limite de 2 por grupo existe
     * para o canyon nao virar revoada.</p>
     *
     * <p>maxLight 15: ela nasce POUSADA no alto, de dia, que e quando alguem
     * escala o canyon e chega perto do ninho. Exigir escuridao faria o mob
     * nunca existir na pratica -- e isso nao da erro nenhum, aparece como um
     * canyon vazio que ninguem consegue explicar.</p>
     *
     * <p>followRange 32 e maior que o raio de aviso DE PROPOSITO: a ave precisa
     * enxergar o intruso antes de ele entrar na zona avisada, ou o aviso
     * comecaria tarde demais para servir de aviso.</p>
     */
    public static EnemyDefinition spiderEagle() {
        return new EnemyDefinition(metadata("spider_eagle", ThreatTier.HUNTER, true, false),
                new EnemyAttributes(28, 0.35F, 6, 2, 32, 0.0F),
                spawn("#nenfoundation:canyon_nest_biomes", 0, 15, true, false, 2));
    }

    /**
     * Mergulho de 14 ticks de subida, 6 de descida e 18 de volta ao alto.
     *
     * <p>O DANO NAO E UM NUMERO PROPRIO: ele e lido de {@link #spiderEagle()},
     * porque o mergulho e o ataque da ave -- o mesmo ATTACK_DAMAGE que o
     * atributo publica. Repetir o 6 aqui criaria duas fontes para a mesma
     * verdade: girar o atributo numa sessao de balanceamento mudaria o dano em
     * jogo e nao mudaria este numero, e a divergencia so apareceria como uma
     * representacao que promete um dano diferente do que o jogador leva.</p>
     *
     * <p>O WINDUP E O AVISO VISUAL: 14 ticks de subida sao o que o jogador ve
     * antes da descida. A janela ACTIVE e curta porque ela e a queda em si -- e
     * ela decide quantos blocos o mergulho percorre, entao encurtar aqui sem
     * olhar o raio de bote produz uma ave que mergulha e para no ar.</p>
     */
    public static AttackDefinition spiderEagleDive() {
        return new AttackDefinition("dive", 14, 6, 18,
                spiderEagle().attributes().attackDamage(), 0.9F, true, false, true);
    }

    /**
     * Avisa a 16 blocos do ninho, bota a 6, larga a perseguicao a 28 e exige
     * 30 ticks de aviso antes do bote.
     *
     * <p>Os numeros se leem juntos: o intruso ve a ave subir e encarar a 16
     * blocos e tem um segundo e meio DENTRO do raio de bote antes do primeiro
     * mergulho -- tempo de sobra para recuar, que e a resposta que o mob
     * ensina. A coleira de 28 e quase o dobro do aviso DE PROPOSITO: ela existe
     * para parar a ave que se empolgou atras de quem ja estava recuando, nao
     * para encurtar o alcance dela dentro da propria zona.</p>
     */
    public static NestGuardRules spiderEagleNest() {
        return new NestGuardRules(16.0D, 6.0D, 28.0D, 30);
    }

    /**
     * HP 60, dano 6, velocidade 0.6, armadura 4 -- corpo GRANDE e teimoso.
     *
     * <p>Os numeros nao existem para tornar o mob um bom combate; existem para
     * tornar o combate um MAU caminho. Sessenta de vida com armadura 4 num
     * bicho que foge ao primeiro golpe da uma briga longa, chata e sem premio,
     * e esse e o ponto: a recompensa esta na CAPTURA. Se alguem "corrigir"
     * estes numeros para um combate agradavel, o mob inteiro deixa de ensinar
     * o que veio ensinar, e nada acusa -- o build segue verde.</p>
     *
     * <p>Velocidade 0.6 e alta de proposito: e ela que faz o cabo de guerra
     * existir. Um peixe lento seria acompanhado sem esforco, a tensao nunca
     * subiria e a linha nunca arrebentaria.</p>
     *
     * <p>territorial=false e social=false: ele nao defende lugar nenhum e nao
     * anda em cardume. E UM por regiao -- o limite de 1 por grupo e o que faz
     * "encontro raro" querer dizer alguma coisa.</p>
     *
     * <p>allowWater=true e requireGround=false: e o primeiro perfil AQUATICO do
     * repositorio. Marcado como os outros, ele nunca nasceria.</p>
     *
     * <p>maxLight 15: ele vive na agua, e agua de pantano pega sol. Exigir
     * escuridao faria o encontro nunca acontecer -- e isso nao da erro nenhum,
     * aparece como um pantano vazio que ninguem consegue explicar.</p>
     */
    public static EnemyDefinition masterOfTheSwamp() {
        return new EnemyDefinition(metadata("master_of_the_swamp", ThreatTier.DANGEROUS, false, false),
                new EnemyAttributes(60, 0.6F, 6, 4, 24, 0.6F),
                spawn("#nenfoundation:swamp_water_biomes", 0, 15, false, true, 1));
    }

    /**
     * Bocada de 12 ticks de aviso, 4 de janela e 16 de recuperacao.
     *
     * <p>O DANO NAO E UM NUMERO PROPRIO: ele e lido de
     * {@link #masterOfTheSwamp()}, porque a bocada e o unico ataque do peixe --
     * o mesmo ATTACK_DAMAGE que o atributo publica. Repetir o 6 aqui criaria
     * duas fontes para a mesma verdade: girar o atributo numa sessao de
     * balanceamento mudaria o dano em jogo e nao mudaria este numero, e a
     * divergencia so apareceria como uma representacao que promete um dano
     * diferente do que o jogador leva.</p>
     *
     * <p>O WINDUP E O AVISO, e aqui ele avisa uma coisa incomum: nao "voce vai
     * apanhar", e sim "sua isca vai sumir". Doze ticks sao o tempo de ver a
     * boca abrir e recolher a linha antes da mordida -- quem recolhe a tempo
     * escapa da fisgada inteira. Encurtar isto transforma o encontro numa
     * fisgada sem telegrafo.</p>
     *
     * <p>O empurrao de 0.3 e pequeno DE PROPOSITO: quem estiver nadando ao lado
     * da propria boia leva a bocada, mas nao e arremessado para longe -- o
     * perigo deste mob e perder a linha, nao morrer.</p>
     */
    public static AttackDefinition masterOfTheSwampBite() {
        return new AttackDefinition("bite", 12, 4, 16,
                masterOfTheSwamp().attributes().attackDamage(), 0.3F, true, false, true);
    }

    /**
     * Isca a 8 blocos, 200 ticks de linha presa para cansar, 100 de tensao
     * maxima, 6 de tensao por bloco afastado e 4 de alivio por bloco
     * aproximado.
     *
     * <p>Os cinco numeros se leem JUNTOS, e a conta e o mob inteiro: correndo
     * para longe a 0.28 bloco por tick, o jogador poe 1.68 de tensao por tick e
     * arrebenta a linha em cerca de 60 ticks -- tres segundos de puxao errado.
     * Os dez segundos que cansam o peixe so cabem nesse orcamento para quem
     * ACOMPANHA, porque o alivio (4) e menor que a tensao (6): seguir o peixe
     * paga o puxao dele, mas nao gera credito. Inverter essa relacao faria a
     * captura virar um passeio, e igualar as duas faria o cabo de guerra virar
     * uma soma que da sempre zero -- nenhum dos dois daria erro.</p>
     */
    public static RegrasDeFisgada masterOfTheSwampFishing() {
        return new RegrasDeFisgada(8.0D, 200, 100.0D, 6.0D, 4.0D);
    }

    /**
     * HP 40, dano 7, velocidade 0.32, armadura 3 -- corpo de ELITE que quase
     * nunca e usado.
     *
     * <p>OS NUMEROS SAO O AVISO, E NAO O ENCONTRO. Este e o primeiro mob do
     * repositorio cujo caminho bom nao passa por combate nenhum: o jogador vence
     * NAO lutando. O corpo e forte -- e o ThreatTier e ELITE -- justamente para
     * que a alternativa seja ruim: quem reprova no exame enfrenta um bicho que
     * bate a 7 com armadura 3, e e isso que transforma "eu podia ter ficado
     * parado" em arrependimento. Se alguem "equilibrar" estes numeros para uma
     * briga agradavel, o mob deixa de ensinar o que veio ensinar e nada acusa --
     * o build segue verde.</p>
     *
     * <p>territorial=false e social=false: ele nao defende lugar nenhum e nao
     * anda em bando. E UM, e o limite de 1 por grupo e o que faz o encontro ser
     * um encontro -- dois kirikos avaliando o mesmo jogador dariam dois
     * vereditos sobre a mesma pessoa, e a cena perderia o sentido.</p>
     *
     * <p>maxLight 15: ele se disfarca de GENTE e precisa ser visto para ser
     * avaliado. Exigir escuridao faria o encontro nunca acontecer -- e isso nao
     * da erro nenhum, aparece como um bioma vazio que ninguem consegue
     * explicar.</p>
     *
     * <p>PONTO CEGO DECLARADO: a faccao sai como WILDLIFE, e nao MAGICAL_BEAST,
     * porque ela vem do helper {@link #metadata} que todos os perfis usam. A
     * ficha do kiriko diz "Magical Beast != monster", e o comportamento cumpre
     * isso; o rotulo de faccao ainda nao. Trocar so para ele exigiria um segundo
     * helper, e a divergencia entre os dois seria pior do que o rotulo errado --
     * a mudanca certa e dar faccao a TODOS os perfis de uma vez, e isso e outra
     * entrega.</p>
     */
    public static EnemyDefinition kiriko() {
        return new EnemyDefinition(metadata("kiriko", ThreatTier.ELITE, false, false),
                new EnemyAttributes(40, 0.32F, 7, 3, 24, 0.2F),
                spawn("#nenfoundation:magical_beast_biomes", 0, 15, true, false, 1));
    }

    /**
     * Golpe de 10 ticks de aviso, 5 de janela e 14 de recuperacao.
     *
     * <p>O DANO NAO E UM NUMERO PROPRIO: ele e lido de {@link #kiriko()}, porque
     * a garra e o unico ataque dele -- o mesmo ATTACK_DAMAGE que o atributo
     * publica. Repetir o 7 aqui criaria duas fontes para a mesma verdade: girar o
     * atributo numa sessao de balanceamento mudaria o golpe em jogo e nao mudaria
     * este numero, e a divergencia so apareceria como uma representacao que
     * promete um dano diferente do que o jogador leva.</p>
     *
     * <p>ESTE ATAQUE SO EXISTE DEPOIS DE UMA REPROVACAO. Ele nao e o mob: e a
     * consequencia de ter errado o mob. Por isso o windup e curto se comparado ao
     * dos outros -- o aviso de verdade ja foi dado, e durou o clipe inteiro da
     * transformacao.</p>
     */
    public static AttackDefinition kirikoStrike() {
        return new AttackDefinition("strike", 10, 5, 14,
                kiriko().attributes().attackDamage(), 0.6F, true, false, true);
    }

    /**
     * 200 ticks de observacao, 40 por agressao, 25 por crueldade, 1 por tick de
     * paciencia e 60 para aprovar.
     *
     * <p>Os cinco numeros se leem JUNTOS, e a conta e o encontro inteiro: a
     * paciencia enche o teto de 60 em 60 ticks e a aprovacao so acontece aos 200,
     * entao quem se comporta passa sete segundos a mais de bom comportamento do
     * que precisaria -- e esse excedente e o que da ao jogador tempo de PERCEBER
     * que esta sendo observado. Uma pancada custa 40: de teto cheio sobra 20, e
     * sao precisos mais 40 ticks de paz para voltar. A SEGUNDA pancada em menos
     * de 40 ticks leva a pontuacao a negativa e reprova -- e como a recarga de
     * uma espada e menor do que isso, atacar reprova na pratica, que e o que a
     * ficha manda. O que a janela compra e o caso honesto: quem acertou sem
     * querer e parou na hora.</p>
     *
     * <p>CRUELDADE CUSTA MENOS QUE AGRESSAO (25 contra 40) de proposito: bater no
     * proprio kiriko e uma escolha sobre ELE, e ferir um bicho por perto e uma
     * escolha sobre o mundo. As duas reprovam, a primeira mais depressa.</p>
     */
    /**
     * O custo da agressao e MAIOR que o limite de aprovacao, e isso nao e folga de
     * balanceamento: e o que faz um golpe reprovar DE QUALQUER nota alcancavel.
     *
     * <p>A pontuacao tem teto no proprio limite de aprovacao -- sem teto, esperar
     * bastante viraria credito, e o jogador paciente compraria o direito de bater. Com
     * teto e com custo 80 contra limite 60, o melhor aluno possivel ainda afunda para
     * -20 no primeiro golpe. A ficha diz que quem ataca e reprovado, sem excecao, e e
     * a RELACAO entre estes dois numeros que sustenta isso -- nao a regra, que so
     * soma e subtrai.</p>
     */
    public static RegrasDeJulgamento kirikoJulgamento() {
        return new RegrasDeJulgamento(200, 80, 25, 1, 60);
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
     * <p>O "foxbear" ficou de fora daqui ate a issue #266, com a divida atribuida
     * a outra frente. O preco disso foi exatamente o que a linha acima descreve:
     * sem tag e sem biome modifier ele NUNCA nasceu no mundo, e nenhum portao
     * podia dizer isso porque ele nao estava nesta lista. Agora esta, com a tag e
     * o modifier no mesmo PR -- que e a unica ordem que nao produz alarme orfao.</p>
     */
    public static Map<String, EnemyDefinition> publicados() {
        return Map.of(
                "great_stamp", greatStamp(),
                "frog_in_waiting", frogInWaiting(),
                "man_faced_ape", manFacedApe(),
                "spider_eagle", spiderEagle(),
                "master_of_the_swamp", masterOfTheSwamp(),
                "kiriko", kiriko(),
                "foxbear", foxbear());
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
