package com.darkcontinent.nenfoundation.gametest;

import com.darkcontinent.nenfoundation.NenFoundation;
import com.darkcontinent.nenfoundation.enemy.combat.AttackPhase;
import com.darkcontinent.nenfoundation.enemy.content.HunterExamProfiles;
import com.darkcontinent.nenfoundation.enemy.entity.DummyEnemyEntity;
import com.darkcontinent.nenfoundation.enemy.perception.HearingEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * O Boneco de Treino com o jogo de pe -- a framework inteira medida uma vez (#138).
 *
 * <p>O QUE O TESTE PURO JA COBRE: as tabelas. Quantas varreduras cabem em cem
 * ticks, que um ataque nao conta duas vezes na mesma instancia, que altura e
 * cosseno viram uma regiao. O que so existe aqui e a ENTIDADE medindo essa
 * geometria com a posicao que o servidor tem, os goals de verdade disputando o
 * mesmo mob, e os pontos de saida -- morte, interrupcao -- acontecendo em ordem
 * de servidor em vez de em ordem de teste.
 *
 * <p>A ARENA e {@code nenfoundation:arena}, e <b>o piso dela fica em y=1</b>:
 * {@code prepareTestStructure} cria o structure block um bloco ABAIXO do
 * conteudo, entao quem nasce em y=1 nasce dentro da pedra. O cabecalho do
 * {@code GreatStampGameTest} conta a investigacao que isso custou. Todo spawn
 * daqui usa y=2.
 *
 * <p><b>A VITIMA E UM GOLEM DE FERRO</b>, pelo mesmo motivo dos outros mobs:
 * dano de mob contra JOGADOR e reescalado pela dificuldade do servidor, e o
 * teste passaria a medir a sessao em vez do mob. O golem tem 100 de vida, nao
 * caca criatura pacifica e nao muda de dano com a dificuldade.
 *
 * <p><b>O BONECO SO PERSEGUE QUEM O FERIU, e isso e regra e nao acidente.</b> A
 * faccao dele e CUSTOM, a de um jogador e HUNTER_ASSOCIATION, e o par nao esta
 * declarado em {@code FactionRelations.padrao()} -- logo e NEUTRAL. Como ele
 * tambem nao e territorial, {@code TargetEvaluator} so o deixa mirar em quem ja
 * bateu nele. Por isso os cenarios de visao chamam {@code setLastHurtByMob}
 * antes de medir qualquer geometria: sem isso o boneco recusaria o alvo pela
 * faccao, o cone nunca seria consultado, e o teste passaria verde provando a
 * regra errada.
 *
 * <p><b>O JOGADOR DE MENTIRA AQUI E DE SOBREVIVENCIA</b>, e nao o de sempre.
 * {@code makeMockServerPlayerInLevel()} crava {@code isCreative()} em true, e a
 * varredura do boneco descarta jogador criativo de proposito -- com ele o mob
 * nao enxergaria ninguem e todo cenario de percepcao viraria carimbo. Por isso
 * os cenarios de visao usam {@code makeMockPlayer(GameType.SURVIVAL)} somado a
 * {@code addFreshEntity}: e a unica forma de ter, neste mundo de teste, um
 * jogador que a varredura aceita olhar. O criativo continua sendo o ATACANTE
 * nos cenarios de dano, onde ele nao precisa ser visto, so bater.
 *
 * <p><b>{@code spawnWithNoFreeWill} aqui NAO congela o mob.</b> Ele so remove os
 * goals ({@code removeFreeWill}), e {@code customServerAiStep} continua rodando
 * -- percepcao, cerebro, stagger e relogio de ataque seguem vivos. E o que os
 * cenarios de percepcao querem: um boneco que mede o mundo sem sair passeando
 * nem virar a cara para o alvo no meio da medida. Os cenarios de combate usam
 * {@code spawn} puro, porque quem dispara o golpe e o {@code GolpeGoal}.
 *
 * <h2>O QUE ESTE ARQUIVO NAO PROVA</h2>
 *
 * <ul>
 *   <li><b>A CONTAGEM de varreduras.</b> {@code PerceptionController} expoe
 *       {@code varredurasFeitas()}, mas a entidade nao expoe o runtime -- ele e
 *       protegido em {@code BaseHxHMob}. Daqui so da para medir a CONSEQUENCIA
 *       (bonecos com desfasagens diferentes adquirem o alvo em ticks
 *       diferentes), e e isso que {@link #aVarreduraNaoAconteceTodoTick} faz. O
 *       numero exato continua com o teste puro
 *       {@code PerceptionControllerTest}.</li>
 *   <li><b>Que o mesmo golpe nao atinge duas vezes.</b> O guarda por instancia
 *       existe e tem teste puro em {@code AttackControllerTest}, mas em mundo
 *       ele fica ESCONDIDO atras dos i-frames vanilla: um segundo acerto do
 *       mesmo tamanho dentro de {@code invulnerableTime} ja nao tira vida
 *       nenhuma. Medir vida aqui daria o mesmo resultado com e sem o guarda --
 *       e teste que da o mesmo resultado dos dois lados e carimbo.</li>
 *   <li><b>Que a audicao existe no jogo.</b> {@code DummyEnemyEntity.ouvir} nao
 *       tem NENHUM produtor no mundo: o unico chamador em todo o repositorio e
 *       este arquivo. Os cenarios de combate usam o evento a mao para entregar
 *       um alvo sem tambem entregar um soco, e isso prova a tubulacao, nao o
 *       gatilho. Enquanto ninguem tocar o sino, a audicao do boneco e uma porta
 *       sem batente.</li>
 *   <li><b>O lado CLIENTE.</b> {@code faseDeAtaque()} e {@code cambaleando()}
 *       sao lidos aqui do mesmo {@code SynchedEntityData} que o servidor
 *       escreveu; nenhum cliente esta conectado. Que o clipe certo toca no
 *       GeckoLib continua sem prova automatizada.</li>
 *   <li><b>O perfil de spawn.</b> ENCOUNTER_ONLY -- o boneco nao aparecer em
 *       bioma nenhum -- e responsabilidade dos portoes de spawn, nao daqui:
 *       todo boneco destes cenarios nasce por comando do teste.</li>
 * </ul>
 */
@GameTestHolder(NenFoundation.MOD_ID)
public final class DummyEnemyGameTest {

    private static final String ARENA = "arena";

    /**
     * Tamanho da amostra do cenario de orcamento.
     *
     * <p>Nao e botao de nada: e margem estatistica. A desfasagem de cada boneco
     * sai do hashCode do proprio uuid, entao existe a chance de dois cairem no
     * mesmo tick de varredura. Com oito bonecos, a chance de TODOS cairem no
     * mesmo tick -- unica forma de este cenario reprovar sem bug -- e de uma em
     * varios milhoes. Com dois, seria uma em seis, e o cenario viraria moeda.</p>
     */
    private static final int BONECOS_DA_AMOSTRA = 8;

    private DummyEnemyGameTest() { }

    // ------------------------------------------------------------ existencia

    /**
     * Ele registra, nasce e sobrevive aos proprios ticks de servidor.
     *
     * <p>O QUE O TESTE PURO NAO PEGA: o construtor instala o runtime, e
     * {@code customServerAiStep} chama {@code runtimeExigido()} todo tick. Um
     * boneco registrado sem essa instalacao compila, passa em todo teste puro e
     * so explode no primeiro tick de mundo -- e a pilha nao diz qual mob
     * esqueceu. Cinco ticks de vida provam a ligacao inteira: registro,
     * atributos do perfil, attachment de dados sincronizados e o runtime
     * tickando.</p>
     */
    @GameTest(template = ARENA, timeoutTicks = 60)
    @PrefixGameTestTemplate(false)
    public static void oBonecoNasceRegistradoEVivo(GameTestHelper helper) {
        DummyEnemyEntity boneco = helper.spawn(DummyEnemyEntity.registeredType(), new BlockPos(5, 2, 5));

        helper.startSequence()
                .thenExecuteAfter(5, () -> {
                    helper.assertTrue(boneco.isAlive(),
                            "o boneco nao sobreviveu a cinco ticks de servidor na propria arena.");
                    double vidaDoPerfil = HunterExamProfiles.dummyEnemy().attributes().maxHealth();
                    helper.assertTrue(Math.abs(boneco.getMaxHealth() - vidaDoPerfil) < 0.01D,
                            "a vida maxima em mundo e " + boneco.getMaxHealth() + " e o perfil diz "
                                    + vidaDoPerfil + ": createAttributes nao esta lendo o perfil, e"
                                    + " girar a ficha do boneco nao muda o boneco.");
                    helper.assertTrue(boneco.faseDeAtaque() == AttackPhase.IDLE,
                            "boneco recem-nascido ja publicava a fase " + boneco.faseDeAtaque()
                                    + ": o valor inicial do dado sincronizado nao e repouso.");
                    helper.assertFalse(boneco.cambaleando(),
                            "boneco recem-nascido ja nascia cambaleando.");
                    helper.assertTrue(boneco.getTarget() == null,
                            "o boneco escolheu alvo sem ninguem ter feito nada: ele nao e um mob"
                                    + " agressivo, e um boneco que caca sozinho vira conteudo que"
                                    + " ninguem projetou.");
                })
                .thenSucceed();
    }

    // ------------------------------------------------------------- percepcao

    /**
     * Passar na frente dele nao e motivo: o boneco so responde a quem bateu.
     *
     * <p>O QUE O TESTE PURO NAO PEGA: o {@code TargetEvaluator} responde "este
     * candidato e elegivel?", e nao "a entidade montou o candidato certo?". Quem
     * preenche faccao, territorio e "ja me feriu" e a varredura da ENTIDADE, e
     * um erro ali -- marcar todo jogador como hostil, por exemplo -- nao da erro
     * nenhum: da um boneco de treino que persegue quem passa perto, e a conta so
     * chega como um mob de ferramenta correndo atras de gente pelo mundo.</p>
     */
    @GameTest(template = ARENA, timeoutTicks = 100)
    @PrefixGameTestTemplate(false)
    public static void oBonecoNaoCacaQuemNuncaOFeriu(GameTestHelper helper) {
        DummyEnemyEntity boneco = helper.spawnWithNoFreeWill(
                DummyEnemyEntity.registeredType(), new BlockPos(5, 2, 3));
        encarar(boneco);
        Player passante = jogadorDeSobrevivencia(helper, new BlockPos(5, 2, 8));

        helper.startSequence()
                .thenExecuteFor(40, () -> helper.assertTrue(boneco.getTarget() == null,
                        "o boneco mirou em " + passante.getName().getString() + " sem ter apanhado"
                                + " dele. A relacao CUSTOM x HUNTER_ASSOCIATION e NEUTRAL e o boneco"
                                + " nao e territorial: quem nunca o feriu nao pode virar alvo."))
                .thenSucceed();
    }

    /**
     * De frente e com linha de visao livre, quem bateu nele vira alvo.
     *
     * <p>O QUE O TESTE PURO NAO PEGA: {@code VisionCone} recebe cosseno e
     * distancia ja prontos. Quem os calcula e a entidade, a partir de
     * {@code getLookAngle()} e {@code hasLineOfSight()} -- e um sinal trocado no
     * cosseno, ou o olhar lido do yHeadRot em vez do yRot, produziria um mob que
     * enxerga pelas costas sem nada acusar. Doze ticks de espera dao duas
     * janelas do orcamento de visao: se em duas ele nao viu, nao foi ritmo, foi
     * geometria.</p>
     */
    @GameTest(template = ARENA, timeoutTicks = 100)
    @PrefixGameTestTemplate(false)
    public static void oOlharAdquireQuemEstaNaFrente(GameTestHelper helper) {
        DummyEnemyEntity boneco = helper.spawnWithNoFreeWill(
                DummyEnemyEntity.registeredType(), new BlockPos(5, 2, 3));
        encarar(boneco);
        Player agressor = jogadorDeSobrevivencia(helper, new BlockPos(5, 2, 8));
        boneco.setLastHurtByMob(agressor);

        helper.startSequence()
                .thenExecuteAfter(12, () -> helper.assertTrue(boneco.getTarget() == agressor,
                        "doze ticks -- duas janelas do orcamento de visao -- e o boneco ainda mira"
                                + " em " + boneco.getTarget() + ". O agressor estava bem na frente,"
                                + " a cinco blocos, sem nada no caminho."))
                .thenSucceed();
    }

    /**
     * Parede no meio corta o alvo, mesmo com ele bem na frente.
     *
     * <p>O QUE O TESTE PURO NAO PEGA: linha de visao e a unica das tres
     * condicoes do cone que precisa do MUNDO. O teste puro recebe o booleano
     * pronto e nunca descobre se alguem esqueceu de consulta-lo. Um boneco que
     * enxerga atraves de pedra nao da erro: da um mob que sabe onde voce esta o
     * tempo todo, e o jogador sente isso como "esse bicho trapaceia".</p>
     */
    @GameTest(template = ARENA, timeoutTicks = 100)
    @PrefixGameTestTemplate(false)
    public static void aParedeCortaOAlvoMesmoDeFrente(GameTestHelper helper) {
        DummyEnemyEntity boneco = helper.spawnWithNoFreeWill(
                DummyEnemyEntity.registeredType(), new BlockPos(5, 2, 3));
        encarar(boneco);
        // A parede cobre a ALTURA DOS OLHOS dos dois: hasLineOfSight traca de olho
        // a olho, e uma parede de um bloco so passaria por baixo do traco.
        for (int x = 4; x <= 6; x++) {
            helper.setBlock(new BlockPos(x, 2, 5), Blocks.STONE);
            helper.setBlock(new BlockPos(x, 3, 5), Blocks.STONE);
        }
        Player agressor = jogadorDeSobrevivencia(helper, new BlockPos(5, 2, 8));
        boneco.setLastHurtByMob(agressor);

        helper.startSequence()
                .thenExecuteFor(40, () -> helper.assertTrue(boneco.getTarget() == null,
                        "o boneco mirou em quem estava atras de uma parede de pedra: a linha de"
                                + " visao nao esta entrando na decisao do cone."))
                .thenSucceed();
    }

    /**
     * Pelas costas ele nao ve, mesmo sem nada no caminho.
     *
     * <p>O QUE O TESTE PURO NAO PEGA: aqui o unico motivo possivel para a recusa
     * e o ANGULO -- mesmo agressor, mesma distancia e o mesmo ceu aberto do
     * cenario de frente. Se este passar e o de frente tambem, o cone existe; se
     * os dois passarem por acidente, o cenario de frente reprova junto e a dupla
     * nao mente. Um cone que aceita 360 graus nao aparece em log: aparece como
     * um mob que nunca da as costas para ninguem.</p>
     */
    @GameTest(template = ARENA, timeoutTicks = 100)
    @PrefixGameTestTemplate(false)
    public static void quemEstaForaDoConeNaoViraAlvo(GameTestHelper helper) {
        DummyEnemyEntity boneco = helper.spawnWithNoFreeWill(
                DummyEnemyEntity.registeredType(), new BlockPos(5, 2, 5));
        encarar(boneco);
        // Atras: o boneco olha para +Z, o agressor fica em -Z. O cosseno vale -1,
        // e a meia-abertura do perfil nem chega perto disso.
        Player agressor = jogadorDeSobrevivencia(helper, new BlockPos(5, 2, 1));
        boneco.setLastHurtByMob(agressor);

        helper.startSequence()
                .thenExecuteFor(40, () -> helper.assertTrue(boneco.getTarget() == null,
                        "o boneco mirou em quem estava exatamente atras dele. O cone de visao"
                                + " virou esfera, e o mob passou a enxergar em volta inteira."))
                .thenSucceed();
    }

    /**
     * A varredura nao acontece todo tick -- e da para ver isso sem contar nada.
     *
     * <p>O QUE O TESTE PURO NAO PEGA: o teste puro conta as chamadas ao sensor
     * porque ele mesmo e o sensor. Daqui, o runtime e protegido e o contador nao
     * se alcanca; o que se alcanca e a marca que o orcamento deixa no mundo.
     * Oito bonecos identicos, nascidos no mesmo tick, olhando o mesmo agressor:
     * se cada um consultasse o mundo todo tick, os oito mirariam no MESMO tick,
     * sempre. Como a visao so roda de tempos em tempos e a desfasagem sai do
     * uuid de cada um, eles miram em ticks diferentes.
     *
     * <p>Isso importa porque o custo de varrer nao aparece como erro: aparece
     * como TPS caindo devagar ao longo de uma sessao, com um relato de bug que
     * diz apenas "o servidor fica lento com o tempo".</p>
     */
    @GameTest(template = ARENA, timeoutTicks = 200)
    @PrefixGameTestTemplate(false)
    public static void aVarreduraNaoAconteceTodoTick(GameTestHelper helper) {
        Player agressor = jogadorDeSobrevivencia(helper, new BlockPos(5, 2, 9));
        DummyEnemyEntity[] bonecos = new DummyEnemyEntity[BONECOS_DA_AMOSTRA];
        for (int i = 0; i < BONECOS_DA_AMOSTRA; i++) {
            bonecos[i] = helper.spawnWithNoFreeWill(
                    DummyEnemyEntity.registeredType(), new BlockPos(1 + i, 2, 3));
            encarar(bonecos[i]);
            bonecos[i].setLastHurtByMob(agressor);
        }

        int[] quandoMirou = new int[BONECOS_DA_AMOSTRA];
        Arrays.fill(quandoMirou, -1);
        int[] relogio = { 0 };

        helper.startSequence()
                .thenExecuteFor(30, () -> {
                    int agora = relogio[0]++;
                    for (int i = 0; i < BONECOS_DA_AMOSTRA; i++) {
                        if (quandoMirou[i] < 0 && bonecos[i].getTarget() != null) quandoMirou[i] = agora;
                    }
                })
                .thenExecute(() -> {
                    for (int i = 0; i < BONECOS_DA_AMOSTRA; i++) {
                        helper.assertTrue(quandoMirou[i] >= 0,
                                "o boneco " + i + " nao mirou em ninguem em trinta ticks, com o"
                                        + " agressor na frente e sem obstaculo. O cenario nao chegou"
                                        + " a medir o orcamento: ele nem viu.");
                    }
                    boolean todosNoMesmoTick = true;
                    for (int i = 1; i < BONECOS_DA_AMOSTRA; i++) {
                        if (quandoMirou[i] != quandoMirou[0]) todosNoMesmoTick = false;
                    }
                    helper.assertFalse(todosNoMesmoTick,
                            "os oito bonecos miraram todos no tick " + quandoMirou[0] + ". Ou a"
                                    + " varredura cara esta rodando todo tick, ou a desfasagem por"
                                    + " uuid sumiu e a manada inteira varre junta -- e os dois viram"
                                    + " travada periodica, que se parece com problema de rede.");
                })
                .thenSucceed();
    }

    // ---------------------------------------------------------------- golpe

    /**
     * O golpe atravessa aviso, janela e recuperacao, sai sozinho e volta a sair.
     *
     * <p>O QUE O TESTE PURO NAO PEGA: a {@code AttackTimeline} avanca porque
     * alguem a ticka, e quem a ticka aqui e a entidade, com o {@code GolpeGoal}
     * podendo ser preemptado a qualquer momento pelo {@code goalSelector}. Uma
     * fase presa nao aparece como erro: aparece como um boneco que da um golpe e
     * nunca mais ataca. Por isso o cenario nao para no fim do primeiro golpe --
     * ele exige o SEGUNDO, que e a unica prova de que o ciclo fechou.
     *
     * <p>O repouso deste mob e IDLE ou COMPLETE, e nao so IDLE: {@code canStart}
     * aceita os dois. Exigir IDLE aqui seria o teste inventando uma regra que o
     * controlador nao tem.</p>
     */
    @GameTest(template = ARENA, timeoutTicks = 300)
    @PrefixGameTestTemplate(false)
    public static void oGolpePassaPelasTresJanelasESaiSozinho(GameTestHelper helper) {
        DummyEnemyEntity boneco = helper.spawn(DummyEnemyEntity.registeredType(), new BlockPos(5, 2, 5));
        IronGolem vitima = helper.spawnWithNoFreeWill(EntityType.IRON_GOLEM, new BlockPos(5, 2, 7));
        encarar(boneco);
        Vec3 posto = boneco.position();
        Vec3 postoDaVitima = vitima.position();

        List<AttackPhase> trilha = new ArrayList<>();
        helper.startSequence()
                .thenExecuteFor(120, () -> {
                    fixar(boneco, posto);
                    fixar(vitima, postoDaVitima);
                    entregarSom(boneco, vitima);
                    AttackPhase fase = boneco.faseDeAtaque();
                    if (trilha.isEmpty() || trilha.get(trilha.size() - 1) != fase) trilha.add(fase);
                })
                .thenExecute(() -> {
                    int aviso = indiceDe(trilha, AttackPhase.WINDUP, 0);
                    helper.assertTrue(aviso >= 0,
                            "o boneco nunca comecou um golpe com o alvo a dois blocos, dentro do"
                                    + " alcance do perfil. Trilha observada: " + trilha);
                    int janela = indiceDe(trilha, AttackPhase.ACTIVE, aviso);
                    helper.assertTrue(janela > aviso,
                            "o golpe nunca chegou a janela ativa depois do aviso. Trilha: " + trilha);
                    int recuperacao = indiceDe(trilha, AttackPhase.RECOVERY, janela);
                    helper.assertTrue(recuperacao > janela,
                            "o golpe nao teve recuperacao: ele saiu da janela ativa para outra coisa."
                                    + " Trilha: " + trilha);
                    int repouso = indiceDeRepouso(trilha, recuperacao);
                    helper.assertTrue(repouso > recuperacao,
                            "a fase nunca voltou ao repouso depois da recuperacao -- ela ficou presa."
                                    + " Trilha: " + trilha);
                    helper.assertTrue(indiceDe(trilha, AttackPhase.WINDUP, repouso) > repouso,
                            "o boneco deu um golpe e nunca deu o segundo, com o alvo parado a dois"
                                    + " blocos o tempo todo. A recarga nao zerou ou o ciclo nao"
                                    + " fechou. Trilha: " + trilha);
                })
                .thenSucceed();
    }

    /**
     * O golpe so tira vida dentro da janela ativa -- e tira ao menos uma vez.
     *
     * <p>O QUE O TESTE PURO NAO PEGA: a caixa do golpe e LOCAL, e quem a coloca
     * no mundo e a rotacao da entidade no instante do acerto. Um teste puro
     * confirma que a caixa intersecta o que o proprio teste posicionou; so aqui
     * o alvo esta onde o SERVIDOR o pos, com o boneco olhando para onde o
     * servidor diz.
     *
     * <p>As duas metades sao necessarias. "So machuca em ACTIVE" passa sozinha
     * quando o golpe nao machuca NUNCA -- que e a falha silenciosa mais barata
     * deste sistema: um mob que ataca, anima e nao encosta. Por isso o cenario
     * tambem cobra que a vida tenha caido.</p>
     */
    @GameTest(template = ARENA, timeoutTicks = 200)
    @PrefixGameTestTemplate(false)
    public static void oGolpeSoMachucaNaJanelaAtiva(GameTestHelper helper) {
        DummyEnemyEntity boneco = helper.spawn(DummyEnemyEntity.registeredType(), new BlockPos(5, 2, 5));
        IronGolem vitima = helper.spawnWithNoFreeWill(EntityType.IRON_GOLEM, new BlockPos(5, 2, 7));
        encarar(boneco);
        Vec3 posto = boneco.position();
        Vec3 postoDaVitima = vitima.position();

        float[] vidaAnterior = { vitima.getHealth() };
        int[] quedas = { 0 };
        // A janela inteira do golpe cabe em 45 ticks e a recarga do perfil segura o
        // proximo bem depois disso: a amostra mede UM golpe, e nao dois somados.
        helper.startSequence()
                .thenExecuteFor(45, () -> {
                    fixar(boneco, posto);
                    fixar(vitima, postoDaVitima);
                    entregarSom(boneco, vitima);
                    float agora = vitima.getHealth();
                    if (agora < vidaAnterior[0]) {
                        quedas[0]++;
                        AttackPhase fase = boneco.faseDeAtaque();
                        vidaAnterior[0] = agora;
                        helper.assertTrue(fase == AttackPhase.ACTIVE,
                                "a vitima perdeu vida com o golpe na fase " + fase + ". Fora da"
                                        + " janela ativa o golpe nao existe -- se ele machuca no"
                                        + " aviso, o telegrafo deixou de ser telegrafo.");
                    }
                })
                .thenExecute(() -> {
                    helper.assertTrue(quedas[0] > 0,
                            "o golpe inteiro passou e a vitima, parada a dois blocos BEM NA FRENTE"
                                    + " do boneco, nao perdeu um ponto de vida. A caixa do golpe nao"
                                    + " esta caindo onde o boneco olha: confira o sinal do eixo Z da"
                                    + " hitbox local contra AttackHitbox.noMundo, que assume yaw 0"
                                    + " apontando para +Z.");
                    helper.assertTrue(quedas[0] == 1,
                            "a vitima perdeu vida " + quedas[0] + " vezes num golpe so.");
                })
                .thenSucceed();
    }

    /**
     * O alvo pintado doi mais que as costas, com o MESMO dano cru.
     *
     * <p>O QUE O TESTE PURO NAO PEGA: quem decide a regiao e a geometria do
     * SERVIDOR -- traco do olho do atacante contra a caixa do boneco, altura
     * normalizada pela caixa de verdade, cosseno tirado do olhar de verdade. O
     * teste puro do resolvedor recebe os dois numeros prontos. Se a regiao fosse
     * resolvida errado, os dois bonecos perderiam a mesma vida e nada acusaria:
     * o ponto fraco existiria so no catalogo, e o boneco viraria esponja.</p>
     */
    @GameTest(template = ARENA, timeoutTicks = 100)
    @PrefixGameTestTemplate(false)
    public static void oAlvoPintadoDoiMaisQueAsCostas(GameTestHelper helper) {
        DummyEnemyEntity pelaFrente = helper.spawnWithNoFreeWill(
                DummyEnemyEntity.registeredType(), new BlockPos(3, 2, 5));
        DummyEnemyEntity pelasCostas = helper.spawnWithNoFreeWill(
                DummyEnemyEntity.registeredType(), new BlockPos(7, 2, 5));
        encarar(pelaFrente);
        encarar(pelasCostas);

        ServerPlayer atacante = helper.makeMockServerPlayerInLevel();
        DamageSource golpe = helper.getLevel().damageSources().playerAttack(atacante);

        // Pela FRENTE (o boneco olha para +Z, o atacante esta em +Z) e mirando um
        // pouco para baixo: o traco entra pela parte alta da caixa, onde o alvo
        // esta pintado.
        atacante.moveTo(pelaFrente.getX(), pelaFrente.getY(), pelaFrente.getZ() + 2.0D, 180.0F, 10.0F);
        float antesDoAlvo = pelaFrente.getHealth();
        pelaFrente.hurt(golpe, 4.0F);
        float perdaNoAlvo = antesDoAlvo - pelaFrente.getHealth();

        // PELAS COSTAS, na mesma altura: o cosseno reprova e a regiao vira corpo.
        atacante.moveTo(pelasCostas.getX(), pelasCostas.getY(), pelasCostas.getZ() - 2.0D, 0.0F, 10.0F);
        float antesDoCorpo = pelasCostas.getHealth();
        pelasCostas.hurt(golpe, 4.0F);
        float perdaNoCorpo = antesDoCorpo - pelasCostas.getHealth();

        helper.assertTrue(perdaNoCorpo > 0.0F,
                "o golpe pelas costas nao tirou vida nenhuma: o cenario nao esta medindo dano.");
        helper.assertTrue(perdaNoAlvo > perdaNoCorpo * 1.5F,
                "o alvo tirou " + perdaNoAlvo + " e as costas tiraram " + perdaNoCorpo
                        + ". O multiplicador do perfil e 2x; sem essa diferenca a textura promete"
                        + " um acerto que a regra nao paga.");
        helper.succeed();
    }

    // -------------------------------------------------------------- stagger

    /**
     * Dano suficiente corta o golpe em curso -- e o interrompido nao revida na hora.
     *
     * <p>O QUE O TESTE PURO NAO PEGA: {@code StaggerState} responde "acumulou o
     * bastante?" e {@code AttackController} responde "posso comecar?". Quem os
     * liga e o {@code EnemyRuntime} dentro do {@code hurt} da entidade, num tick
     * de servidor com o goal ainda por cima. O erro classico aqui e reset seco
     * em vez de recarga: a fase volta a repouso, {@code canStart} aprova no
     * mesmo tick, e o mob interrompido ataca MAIS DEPRESSA do que se ninguem
     * tivesse batido. Nao ha erro no log; ha um jogador aprendendo que
     * interromper e burrice.</p>
     */
    @GameTest(template = ARENA, timeoutTicks = 200)
    @PrefixGameTestTemplate(false)
    public static void oStaggerInterrompeOGolpeEmCurso(GameTestHelper helper) {
        DummyEnemyEntity boneco = helper.spawn(DummyEnemyEntity.registeredType(), new BlockPos(5, 2, 5));
        IronGolem vitima = helper.spawnWithNoFreeWill(EntityType.IRON_GOLEM, new BlockPos(5, 2, 7));
        encarar(boneco);
        Vec3 posto = boneco.position();
        Vec3 postoDaVitima = vitima.position();

        helper.startSequence()
                .thenWaitUntil(() -> {
                    fixar(boneco, posto);
                    fixar(vitima, postoDaVitima);
                    entregarSom(boneco, vitima);
                    helper.assertTrue(boneco.faseDeAtaque() == AttackPhase.WINDUP,
                            "o boneco nunca entrou no aviso do golpe; o cenario nao chegou a ter o"
                                    + " que interromper.");
                })
                .thenExecute(() -> {
                    ServerPlayer atacante = helper.makeMockServerPlayerInLevel();
                    // PELAS COSTAS de proposito: pelo alvo pintado o dano dobraria e
                    // mataria o boneco, e o cenario mediria morte em vez de interrupcao.
                    atacante.moveTo(boneco.getX(), boneco.getY(), boneco.getZ() - 2.0D, 0.0F, 0.0F);
                    boneco.hurt(helper.getLevel().damageSources().playerAttack(atacante), 14.0F);

                    helper.assertTrue(boneco.isAlive(),
                            "o boneco morreu com o golpe do cenario: a conta de vida do perfil mudou"
                                    + " e este cenario passou a medir outra coisa.");
                    helper.assertTrue(boneco.cambaleando(),
                            "catorze de dano (treze depois da resistencia, contra um limiar de doze)"
                                    + " nao interrompeu nada. O stagger nao esta sendo alimentado"
                                    + " pelo dano REAL, ou o limiar deixou de ser alcancavel.");
                    helper.assertTrue(boneco.faseDeAtaque() == AttackPhase.IDLE,
                            "o boneco cambaleou e a fase publicada continua " + boneco.faseDeAtaque()
                                    + ": a interrupcao nao cortou o golpe, so acendeu uma luz.");
                })
                // A recarga imposta a quem apanha e mais longa que o proprio cambaleio;
                // trinta ticks depois nenhum golpe novo pode ter comecado.
                .thenExecuteFor(30, () -> {
                    fixar(boneco, posto);
                    fixar(vitima, postoDaVitima);
                    entregarSom(boneco, vitima);
                    AttackPhase fase = boneco.faseDeAtaque();
                    helper.assertTrue(fase == AttackPhase.IDLE || fase == AttackPhase.COMPLETE,
                            "o boneco interrompido voltou a atacar (fase " + fase + ") antes de a"
                                    + " recarga de interrupcao acabar: interromper virou premio.");
                })
                .thenSucceed();
    }

    // -------------------------------------------------------------- limpeza

    /**
     * Morrer no meio do golpe nao deixa a fase presa numa janela de ataque.
     *
     * <p>O QUE O TESTE PURO NAO PEGA: a limpeza de {@code EnemyRuntime} e
     * testavel sozinha, mas quem a chama e {@code die()} da entidade, no meio de
     * um tick de servidor, com a fase JA publicada no dado sincronizado. Uma
     * fase congelada em ACTIVE num mob morto nao aparece em log: aparece como um
     * boneco que morre parado no golpe e, do lado de quem le a fase, como um
     * ataque que nunca termina.
     *
     * <p>A morte aqui e SEM ATACANTE (o mesmo caminho de fogo, queda ou
     * {@code /kill}) de proposito. Um golpe de jogador forte o bastante tambem
     * dispara o stagger, e o stagger publica repouso de carona -- o cenario
     * passaria por acidente e provaria o caminho errado. Ponto de saida
     * esquecido e sempre o que ninguem encena.</p>
     */
    @GameTest(template = ARENA, timeoutTicks = 200)
    @PrefixGameTestTemplate(false)
    public static void morrerNoMeioDoGolpeNaoDeixaAFasePresa(GameTestHelper helper) {
        DummyEnemyEntity boneco = helper.spawn(DummyEnemyEntity.registeredType(), new BlockPos(5, 2, 5));
        IronGolem vitima = helper.spawnWithNoFreeWill(EntityType.IRON_GOLEM, new BlockPos(5, 2, 7));
        encarar(boneco);
        Vec3 posto = boneco.position();
        Vec3 postoDaVitima = vitima.position();

        helper.startSequence()
                .thenWaitUntil(() -> {
                    fixar(boneco, posto);
                    fixar(vitima, postoDaVitima);
                    entregarSom(boneco, vitima);
                    helper.assertTrue(boneco.faseDeAtaque() == AttackPhase.WINDUP,
                            "o boneco nunca entrou no aviso do golpe; nao havia golpe para a morte"
                                    + " interromper.");
                })
                .thenExecute(() -> boneco.hurt(helper.getLevel().damageSources().genericKill(), 1000.0F))
                .thenExecuteAfter(3, () -> {
                    helper.assertFalse(boneco.isAlive(), "o boneco sobreviveu a mil de dano.");
                    AttackPhase fase = boneco.faseDeAtaque();
                    helper.assertTrue(fase != AttackPhase.WINDUP && fase != AttackPhase.ACTIVE
                                    && fase != AttackPhase.RECOVERY,
                            "o boneco morreu no meio do golpe e a fase publicada continua " + fase
                                    + ". O runtime foi limpo, mas o dado sincronizado ficou parado na"
                                    + " janela do golpe: quem le a fase ve um ataque que nunca"
                                    + " termina.");
                })
                .thenSucceed();
    }

    // --------------------------------------------------------------- cenario

    /**
     * Um jogador que a varredura do boneco aceita olhar.
     *
     * <p>{@code makeMockServerPlayerInLevel()} nao serve para isto: ele crava
     * {@code isCreative()} em true e a varredura descarta jogador criativo de
     * proposito. O jogador de sobrevivencia precisa ser posto no mundo a mao --
     * {@code makeMockPlayer} so o constroi -- porque a varredura procura
     * entidades do mundo, e nao a lista de jogadores conectados.</p>
     */
    private static Player jogadorDeSobrevivencia(GameTestHelper helper, BlockPos onde) {
        Player jogador = helper.makeMockPlayer(GameType.SURVIVAL);
        BlockPos absoluto = helper.absolutePos(onde);
        jogador.moveTo(absoluto.getX() + 0.5D, absoluto.getY(), absoluto.getZ() + 0.5D, 0.0F, 0.0F);
        jogador.setDeltaMovement(Vec3.ZERO);
        helper.getLevel().addFreshEntity(jogador);
        return jogador;
    }

    /**
     * Entrega um alvo ao boneco pelo unico caminho que nao envolve um soco.
     *
     * <p>Visao so aceita quem ja feriu o mob, e ferir o mob no comeco de um
     * cenario de GOLPE misturaria stagger com a medida. O evento de audicao
     * entra na memoria de ameaca direto -- e e por isso que ele e repetido a
     * cada tick nos cenarios longos: a memoria decai, e um alvo esquecido no
     * meio da amostra pareceria uma fase presa.</p>
     */
    private static void entregarSom(DummyEnemyEntity boneco, LivingEntity fonte) {
        if (!boneco.isAlive() || !fonte.isAlive()) return;
        boneco.ouvir(new HearingEvent(fonte.getUUID(), boneco.distanceTo(fonte), 1.0D));
    }

    /** Olhar para +Z, para a geometria do cenario nao depender de onde a entidade nasceu virada. */
    private static void encarar(Mob mob) {
        mob.setYRot(0.0F);
        mob.yBodyRot = 0.0F;
        mob.yHeadRot = 0.0F;
        mob.setXRot(0.0F);
    }

    /**
     * Devolve a entidade ao posto dela, todo tick.
     *
     * <p>Sem isto o cenario mede outra coisa: o boneco com goals passeia entre
     * dois golpes, o empurrao entre corpos afasta os dois da faixa do golpe, e o
     * knockback do proprio acerto tira a vitima de cena. Nenhuma dessas coisas e
     * bug do mob -- e todas fariam o cenario reprovar culpando ele.</p>
     */
    private static void fixar(Mob mob, Vec3 posto) {
        if (!mob.isAlive()) return;
        mob.setPos(posto.x, posto.y, posto.z);
        mob.setDeltaMovement(Vec3.ZERO);
        mob.getNavigation().stop();
        encarar(mob);
    }

    private static int indiceDe(List<AttackPhase> trilha, AttackPhase fase, int aPartirDe) {
        for (int i = Math.max(0, aPartirDe); i < trilha.size(); i++) {
            if (trilha.get(i) == fase) return i;
        }
        return -1;
    }

    /** Primeira fase de REPOUSO depois de um indice; IDLE e COMPLETE valem as duas. */
    private static int indiceDeRepouso(List<AttackPhase> trilha, int aPartirDe) {
        for (int i = Math.max(0, aPartirDe); i < trilha.size(); i++) {
            if (trilha.get(i) == AttackPhase.IDLE || trilha.get(i) == AttackPhase.COMPLETE) return i;
        }
        return -1;
    }
}
