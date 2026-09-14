package com.darkcontinent.nenfoundation.enemy.entity;

import com.darkcontinent.nenfoundation.enemy.ai.AwarenessTuning;
import com.darkcontinent.nenfoundation.enemy.ai.DecisaoDeArranque;
import com.darkcontinent.nenfoundation.enemy.ai.EnemyBrain;
import com.darkcontinent.nenfoundation.enemy.ai.EstadoDeArranque;
import com.darkcontinent.nenfoundation.enemy.ai.FaseDoArranque;
import com.darkcontinent.nenfoundation.enemy.ai.PosturaDeCaca;
import com.darkcontinent.nenfoundation.enemy.ai.RegistroDeMatilhas;
import com.darkcontinent.nenfoundation.enemy.ai.RegrasDeArranque;
import com.darkcontinent.nenfoundation.enemy.ai.squad.Squad;
import com.darkcontinent.nenfoundation.enemy.ai.squad.SquadRegistry;
import com.darkcontinent.nenfoundation.enemy.ai.squad.SquadRole;
import com.darkcontinent.nenfoundation.enemy.ai.squad.SquadRules;
import com.darkcontinent.nenfoundation.enemy.api.EnemyCombatState;
import com.darkcontinent.nenfoundation.enemy.base.BaseChimeraAnt;
import com.darkcontinent.nenfoundation.enemy.base.EnemyRuntime;
import com.darkcontinent.nenfoundation.enemy.chimera.ChimeraRank;
import com.darkcontinent.nenfoundation.enemy.chimera.nen.TacticalNenController;
import com.darkcontinent.nenfoundation.enemy.chimera.nen.TacticalNenSituation;
import com.darkcontinent.nenfoundation.enemy.combat.AttackController;
import com.darkcontinent.nenfoundation.enemy.combat.AttackHitbox;
import com.darkcontinent.nenfoundation.enemy.combat.AttackPhase;
import com.darkcontinent.nenfoundation.enemy.combat.StaggerResult;
import com.darkcontinent.nenfoundation.enemy.combat.StaggerState;
import com.darkcontinent.nenfoundation.enemy.content.CheetahLeaderTuning;
import com.darkcontinent.nenfoundation.enemy.content.ChimeraProfiles;
import com.darkcontinent.nenfoundation.enemy.faction.FactionRelations;
import com.darkcontinent.nenfoundation.enemy.perception.PercepcaoDeAura;
import com.darkcontinent.nenfoundation.enemy.perception.PerceptionBudget;
import com.darkcontinent.nenfoundation.enemy.perception.PerceptionController;
import com.darkcontinent.nenfoundation.enemy.perception.PerceptionSnapshot;
import com.darkcontinent.nenfoundation.enemy.perception.TargetCandidate;
import com.darkcontinent.nenfoundation.enemy.perception.TargetEvaluator;
import com.darkcontinent.nenfoundation.enemy.perception.ThreatMemory;
import com.darkcontinent.nenfoundation.enemy.perception.VisionCone;
import com.darkcontinent.nenfoundation.enemy.registry.EnemyEntityTypes;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Cheetah Leader -- formiga quimera, rank SQUADRON_LEADER. A que CHEGA primeiro.
 *
 * <p><b>A ficha inteira dela e uma troca: velocidade 0.46 contra armadura 3.</b>
 * Ela alcanca qualquer um e apanha de qualquer um. Somar couro a velocidade daria
 * um lider que ninguem alcanca E ninguem fere -- e um bicho assim nao e dificil,
 * e so interminavel. Tres comportamentos escrevem essa troca, e cada um existe
 * contra uma leitura errada do encontro.</p>
 *
 * <p><b>1. O ARRANQUE TEM FADIGA, e a fadiga e a resposta do jogador.</b> Ela nao
 * corre rapido o tempo todo: sao 30 ticks acima da propria velocidade seguidos de
 * 45 abaixo dela, e depois 60 em que o arranque segue negado. A conta e cobrada
 * no construtor de {@link RegrasDeArranque}: ao longo do ciclo ela nao pode andar
 * mais do que andaria sem arranque nenhum. Sem essa cobranca, o arranque deixa de
 * ser redistribuicao e vira um bonus permanente com uma animacao de cansaco em
 * cima -- e o encontro vira uma perseguicao que so acaba quando alguem morre, sem
 * que o jogador consiga nomear o que esta errado. A regra mora FORA desta classe,
 * em {@link EstadoDeArranque} e {@link RegrasDeArranque}, porque o ciclo precisa
 * ser provavel sem abrir um jogo.</p>
 *
 * <p><b>2. Ela ABRE o combate -- e so o combate que ainda nao abriu.</b> Quando
 * lidera um esquadrao ela chega primeiro e os outros alcancam depois: e a
 * diferenca de velocidade que faz isso, nao um privilegio escrito. O que ESTA
 * escrito e a recusa: com um companheiro ja a {@code DISTANCIA_DE_ABERTURA} do
 * alvo, o arranque nao sai ({@link DecisaoDeArranque#ALIADO_JA_ABRIU}). Gasto
 * para entrar numa briga que ja comecou, ele nao abre nada e ainda entrega a
 * lider FATIGADA no meio dela.</p>
 *
 * <p><b>3. O bote e curto, e o preco dele e a recuperacao.</b> Doze ticks de
 * aviso contra os trinta do ciclope: a ameaca deste bicho nunca foi o golpe, foi
 * ter CHEGADO. A recuperacao de 14 ticks e o que o jogador compra ao sobreviver
 * ao bote.</p>
 *
 * <p><b>O que ela NAO faz: ativar tecnica de Nen.</b> A intencao tatica sai do
 * {@link TacticalNenController} herdado de {@link BaseChimeraAnt}, e aqui ela e
 * traduzida por {@link PosturaDeCaca} em POSTURA -- cacar, guardar ou romper
 * contato. Postura e movimento, e movimento e propriedade do inimigo. Nenhuma
 * linha desta classe le, escreve ou calcula aura: o Nen Foundation e a unica
 * autoridade sobre Nen (CLAUDE.md), e inventar aqui a segunda seria o
 * desbalanceamento que ninguem consegue explicar.</p>
 *
 * <p>Ela usa {@link EnemyRuntime} por inteiro -- percepcao, cerebro, ataque e
 * stagger -- e o arranque e o esquadrao entram SOBRE a fundacao, nunca ao lado
 * dela.</p>
 */
public final class CheetahLeaderEntity extends BaseChimeraAnt
        implements software.bernie.geckolib.animatable.GeoEntity {

    /** Fase do ataque: o unico estado de combate que o cliente precisa conhecer. */
    private static final EntityDataAccessor<Integer> FASE_DE_ATAQUE =
            SynchedEntityData.defineId(CheetahLeaderEntity.class, EntityDataSerializers.INT);
    /** Cambaleando: sincronizado porque interrupcao invisivel e stagger que so o servidor conhece. */
    private static final EntityDataAccessor<Boolean> CAMBALEANDO =
            SynchedEntityData.defineId(CheetahLeaderEntity.class, EntityDataSerializers.BOOLEAN);
    /**
     * Fase do arranque, sincronizada.
     *
     * <p>Ela viaja porque o CLIENTE precisa dela: o ritmo da passada sai do
     * multiplicador da fase, e o cliente nao tem {@link EstadoDeArranque}. Sem
     * este campo, o jeito de fazer a fadiga aparecer seria recalcula-la no
     * cliente -- e ai haveria duas contas para a mesma verdade, que divergiriam no
     * primeiro tick perdido de rede. O sintoma seria um guepardo patinando: o
     * corpo lento e as patas rapidas, sem nada no log.</p>
     */
    private static final EntityDataAccessor<Integer> FASE_DO_ARRANQUE =
            SynchedEntityData.defineId(CheetahLeaderEntity.class, EntityDataSerializers.INT);

    /**
     * As pecas imutaveis da ficha, resolvidas uma vez.
     *
     * <p>Estaticas porque NENHUMA delas guarda estado de jogador -- sao records
     * imutaveis. Um campo estatico com estado seria o erro numero dois da lista
     * do CLAUDE.md: dois guepardos decidindo com a mesma memoria, e matar um
     * deixando o outro cansado.</p>
     */
    private static final RegrasDeArranque REGRAS_DO_ARRANQUE = CheetahLeaderTuning.arranque();
    private static final SquadRules REGRAS_DO_ESQUADRAO = SquadRules.esquadrao();
    private static final AttackHitbox CAIXA_DO_BOTE = CheetahLeaderTuning.caixaDoBote();

    /**
     * A fracao de aura que o nucleo AINDA nao publica.
     *
     * <p>Zero, e nao um valor inventado. A porta de percepcao de aura desta
     * formiga segue {@code PercepcaoDeAura.NENHUMA} enquanto a #126 (Gyo) estiver
     * aberta no nucleo, e {@link TacticalNenSituation} pede uma fracao JA MEDIDA.
     * Passar 1.0 aqui seria inventar aura -- a segunda autoridade sobre Nen que o
     * CLAUDE.md proibe -- e o efeito seria uma formiga que se acha cheia de aura
     * para sempre. Com zero, o controlador recusa tudo e devolve {@code NENHUMA},
     * que e a resposta honesta de quem nao mediu nada.</p>
     */
    private static final double FRACAO_DE_AURA_NAO_MEDIDA = 0.0D;

    /**
     * Quanto um golpe NELA tira da moral do esquadrao.
     *
     * <p>Mais do que tiraria um golpe num membro comum, porque ela e o comando: o
     * esquadrao inteiro esta atras dela justamente porque ela chegou primeiro. E
     * numero do BICHO, nao do framework, e por isso mora aqui e nao em
     * {@link SquadRules}.</p>
     */
    private static final int ABALO_POR_GOLPE_NA_LIDER = 9;

    /** Tolerancia para nao refazer o caminho quando o destino mal se moveu, em blocos. */
    private static final double TOLERANCIA_DO_DESTINO = 0.75D;

    /** Multiplicador de navegacao quando nem arranque nem fadiga estao em curso. */
    private static final double VELOCIDADE_NORMAL = 1.0D;

    /** Deslocamento por tick acima do qual o clipe passa de ocio para caminhada. */
    private static final double LIMIAR_DE_CAMINHADA = 0.02D;

    private final software.bernie.geckolib.animatable.instance.AnimatableInstanceCache cacheDeAnimacao =
            software.bernie.geckolib.util.GeckoLibUtil.createInstanceCache(this);

    // ------------------------------------------------------- estado de servidor
    //
    // Tudo abaixo e POR INSTANCIA e so existe no servidor.

    /** O relogio do arranque desta guepardo. Um por bicho, sempre. */
    private final EstadoDeArranque arranque = new EstadoDeArranque(REGRAS_DO_ARRANQUE);
    /** A postura deste tick, traduzida da intencao tatica de Nen. */
    private PosturaDeCaca postura = PosturaDeCaca.CACAR;
    /** Por que o arranque nao saiu da ultima vez que foi PEDIDO. Leitura de diagnostico. */
    private DecisaoDeArranque ultimaDecisaoDoArranque = DecisaoDeArranque.SEM_ALVO;
    /** Distancia do companheiro mais proximo ao alvo, medida no orcamento de coordenacao. */
    private double distanciaDoAliadoAoAlvo = Double.POSITIVE_INFINITY;
    /** O esquadrao quebrou pela moral? Lido no orcamento, consumido todo tick. */
    private boolean esquadraoQuebrado;
    /** Para onde fugir, quando a postura e romper contato. Recalculado no orcamento. */
    private Vec3 destinoDeFuga;
    /** A instancia de ataque que ja recebeu o impulso -- um por bote, nunca cinco. */
    private long instanciaComImpulso;

    public CheetahLeaderEntity(EntityType<? extends CheetahLeaderEntity> type, Level level) {
        super(type, level, ChimeraProfiles.cheetahLeader().metadata(),
                new AwarenessTuning(CheetahLeaderTuning.TICKS_DE_AVISO,
                        CheetahLeaderTuning.MEMORIA_DE_ALVO_TICKS),
                ChimeraProfiles.cheetahLeaderMolde());
        instalarRuntime(montarRuntime());
    }

    private EnemyRuntime montarRuntime() {
        var perfil = ChimeraProfiles.cheetahLeader();
        PerceptionController percepcao = new PerceptionController(
                PerceptionBudget.padrao(),
                VisionCone.deGraus(perfil.attributes().followRange(),
                        CheetahLeaderTuning.MEIA_ABERTURA_DA_VISAO_EM_GRAUS),
                new ThreatMemory(CheetahLeaderTuning.MEMORIA_DE_ALVO_TICKS),
                new TargetEvaluator(FactionRelations.padrao(), perfil.metadata().faction(),
                        perfil.attributes().followRange(), perfil.metadata().territorial()),
                // A porta de aura da formiga continua INERTE enquanto a #126 (Gyo)
                // estiver aberta no nucleo: inventar aqui a regra de quem-ve-o-que
                // seria a segunda autoridade sobre Nen que o CLAUDE.md proibe.
                PercepcaoDeAura.NENHUMA, CheetahLeaderTuning.ALCANCE_DE_AUDICAO,
                getUUID().hashCode());
        return new EnemyRuntime(percepcao,
                new EnemyBrain(new AwarenessTuning(CheetahLeaderTuning.TICKS_DE_AVISO,
                        CheetahLeaderTuning.MEMORIA_DE_ALVO_TICKS)),
                new AttackController(ChimeraProfiles.cheetahLeaderRecarga()),
                new StaggerState(ChimeraProfiles.cheetahLeaderStagger()));
    }

    public static AttributeSupplier.Builder createAttributes() {
        var attrs = ChimeraProfiles.cheetahLeader().attributes();
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, attrs.maxHealth())
                .add(Attributes.MOVEMENT_SPEED, attrs.movementSpeed())
                .add(Attributes.ATTACK_DAMAGE, attrs.attackDamage())
                .add(Attributes.ARMOR, attrs.armor())
                .add(Attributes.FOLLOW_RANGE, attrs.followRange())
                .add(Attributes.KNOCKBACK_RESISTANCE, attrs.knockbackResistance());
    }

    public static EntityType<CheetahLeaderEntity> registeredType() { return EnemyEntityTypes.CHEETAH_LEADER.get(); }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(FASE_DE_ATAQUE, AttackPhase.IDLE.ordinal());
        builder.define(CAMBALEANDO, false);
        builder.define(FASE_DO_ARRANQUE, FaseDoArranque.PRONTA.ordinal());
    }

    public AttackPhase faseDeAtaque() {
        int ordinal = this.entityData.get(FASE_DE_ATAQUE);
        AttackPhase[] fases = AttackPhase.values();
        return ordinal >= 0 && ordinal < fases.length ? fases[ordinal] : AttackPhase.IDLE;
    }

    public boolean cambaleando() { return this.entityData.get(CAMBALEANDO); }

    /** A fase do arranque como o CLIENTE a conhece. Servidor e cliente leem daqui. */
    public FaseDoArranque faseDoArranque() {
        int ordinal = this.entityData.get(FASE_DO_ARRANQUE);
        FaseDoArranque[] fases = FaseDoArranque.values();
        return ordinal >= 0 && ordinal < fases.length ? fases[ordinal] : FaseDoArranque.PRONTA;
    }

    /** A postura deste tick -- leitura de SERVIDOR, para gametest e diagnostico. */
    public PosturaDeCaca postura() { return postura; }

    /**
     * Por que o arranque nao saiu -- leitura de SERVIDOR.
     *
     * <p>Ela so muda quando o arranque foi PEDIDO. Com a postura em
     * {@link PosturaDeCaca#GUARDAR} ou {@link PosturaDeCaca#ROMPER} ele nem chega
     * a ser pedido, e o motivo mora em {@link #postura()}. As duas leituras juntas
     * respondem "por que ela nao correu" em todo caso -- que e a exigencia do
     * CLAUDE.md: recusa sempre tem motivo.</p>
     */
    public DecisaoDeArranque decisaoDoArranque() { return ultimaDecisaoDoArranque; }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new BoteGoal(this));
        goalSelector.addGoal(3, new CacaGoal(this));
        goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.8D));
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 12.0F));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        // Sem target goal vanilla: quem escolhe alvo e o TargetEvaluator, por
        // faccao. Um NearestAttackableTargetGoal ao lado seria uma segunda
        // autoridade sobre a mesma decisao, e as duas discordariam em silencio --
        // aqui com um agravante, porque ela PUBLICA o alvo para o esquadrao
        // inteiro: o grupo passaria a convergir para quem a Goal vanilla escolheu.
    }

    // ------------------------------------------------------------------ tick

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (level().isClientSide) return;
        EnemyRuntime runtime = runtimeExigido();
        PerceptionSnapshot snapshot = runtime.tick(tickCount, this::varrerCandidatos,
                vidaCritica(), false);
        aplicarAlvo(snapshot);

        // O relogio do arranque avanca TODO tick, inclusive sem alvo. Preso ao
        // orcamento de coordenacao ele andaria de dez em dez, e um ciclo de 135
        // ticks passaria a durar 1350 -- sem erro nenhum, so com um guepardo
        // permanentemente cansado. Preso a uma Goal, ele congelaria no instante em
        // que a Goal parasse, e a fadiga nunca acabaria.
        arranque.tick();
        postura = decidirPostura();

        SquadRegistry registro = RegistroDeMatilhas.doNivel((ServerLevel) level());
        Squad esquadrao = registro.bandoDe(getUUID()).orElse(null);
        // A desfasagem sai do id do ESQUADRAO, e nao do proprio: membros do mesmo
        // grupo tem de decidir no MESMO tick, senao metade converge para a posicao
        // velha do alvo. Sem esquadrao ainda, a do proprio serve -- ela so espalha
        // no tempo as tentativas de fundar um.
        int desfasagem = (esquadrao != null ? esquadrao.id() : getUUID()).hashCode();
        if (REGRAS_DO_ESQUADRAO.atualizaNesteTick(tickCount, desfasagem)) {
            coordenar(registro, esquadrao);
        }

        tickDoGolpe(runtime);
        publicarEstado(runtime);
    }

    private boolean vidaCritica() {
        return getHealth() <= getMaxHealth() * CheetahLeaderTuning.FRACAO_DE_VIDA_CRITICA;
    }

    // -------------------------------------------------------------- postura

    /**
     * A intencao tatica de Nen vira POSTURA -- e nada alem disso.
     *
     * <p>O controlador recebe fatos ja medidos e devolve uma intencao; esta classe
     * traduz a intencao em para-onde-andar. Nenhuma aura e lida, gasta ou
     * inventada: o valor de aura que entra e {@link #FRACAO_DE_AURA_NAO_MEDIDA},
     * declarado, e enquanto ele for zero o controlador devolve {@code NENHUMA} e a
     * postura e sempre {@link PosturaDeCaca#CACAR}.</p>
     *
     * <p><b>A moral do esquadrao vence a intencao individual.</b> Quando o grupo
     * quebra, a lider sai com ele. Um lider que continua avancando sozinho depois
     * de o proprio esquadrao ter recuado nao le como bravura -- le como um bicho
     * que ignora a regra de moral que o resto do grupo acabou de obedecer, e nada
     * no log explicaria a diferenca.</p>
     */
    private PosturaDeCaca decidirPostura() {
        if (esquadraoQuebrado) return PosturaDeCaca.ROMPER;
        TacticalNenController controlador = nenTatico().orElse(null);
        // Sem identidade ainda (primeiro tick de servidor) nao ha controlador.
        // Cacar e o padrao, e nao um disfarce: e exatamente o que o controlador
        // devolveria para uma formiga dormente, que e o que quase toda formiga e.
        if (controlador == null) return PosturaDeCaca.CACAR;

        LivingEntity alvo = getTarget();
        boolean emCombate = alvo != null && alvo.isAlive();
        // aoAlcance so pode ser verdadeiro COM combate: TacticalNenSituation recusa
        // o par contraditorio no construtor, e a recusa viria como excecao no meio
        // do tick do servidor.
        boolean aoAlcance = emCombate && distanceTo(alvo) <= CheetahLeaderTuning.ALCANCE_DO_BOTE;
        TacticalNenSituation situacao = new TacticalNenSituation(
                FRACAO_DE_AURA_NAO_MEDIDA, fracaoDeVida(), emCombate, aoAlcance,
                false, vidaCritica(), true);
        return PosturaDeCaca.de(controlador.decidir(situacao));
    }

    /** Vida atual sobre a maxima, presa em 0..1. */
    private double fracaoDeVida() {
        float maxima = getMaxHealth();
        // Divisao por zero aqui daria NaN, e NaN em TacticalNenSituation levanta
        // excecao no meio do tick. Maxima zerada nao deveria acontecer -- mas um
        // modificador de atributo de outro mod pode produzi-la, e o preco de estar
        // errado sobre isso e o servidor caindo, nao um bicho estranho.
        if (!(maxima > 0.0F)) return 0.0D;
        return Mth.clamp(getHealth() / (double) maxima, 0.0D, 1.0D);
    }

    // ---------------------------------------------------------- coordenacao

    /**
     * Um passo de coordenacao -- e o UNICO lugar que mexe no esquadrao.
     *
     * <p>Concentrado aqui de proposito. Espalhado pelas Goals, cada uma leria e
     * escreveria o grupo no proprio ritmo, e duas delas trocariam o alvo no mesmo
     * tick; a ultima venceria, e o sintoma e um bicho indeciso sem causa
     * visivel.</p>
     *
     * <p><b>O id do esquadrao NAO vai para a identidade persistida.</b>
     * {@code BaseChimeraAnt.alistarNoEsquadrao} existe e nao e chamado aqui de
     * proposito: o {@link SquadRegistry} e RUNTIME e morre com o processo, e a
     * identidade e PERSISTENTE (ADR-002). Gravar um id de runtime no save faria a
     * formiga voltar do restart dizendo pertencer a um esquadrao que nao existe
     * mais -- e nada acusaria isso, porque um uuid orfao e indistinguivel de um
     * uuid valido.</p>
     */
    private void coordenar(SquadRegistry registro, Squad atual) {
        Squad esquadrao = atual;
        if (esquadrao == null) {
            // Ela FUNDA e ela lidera. O molde dela nao nasce LEADER -- lideranca e
            // promocao, e ChimeraDefinition reprova um molde que nasca lider --,
            // entao o unico caminho legitimo para ela comandar e criar o grupo:
            // Squad poe quem cria no comando. Entrar num grupo existente como
            // LEADER e proibido por Squad.entrar, e com razao: dois lideres dao
            // duas ordens ao mesmo bando.
            esquadrao = registro.criar(UUID.randomUUID(), REGRAS_DO_ESQUADRAO, getUUID());
        }
        SquadRole papel = esquadrao.papelDe(getUUID());
        if (papel == null) {
            // O indice e o grupo discordaram. Sair e a saida segura: seguir com um
            // papel nulo faria toda decisao adiante estourar um NullPointerException
            // no meio do tick do servidor, com pilha que nao diz qual formiga era.
            registro.sair(getUUID());
            esquadraoQuebrado = false;
            distanciaDoAliadoAoAlvo = Double.POSITIVE_INFINITY;
            return;
        }

        dispensarAusentes(registro, esquadrao);
        recrutarVizinhos(registro, esquadrao);
        // A faxina roda no MESMO ritmo da coordenacao, e nao "quando der". Grupo
        // sem ninguem nao consome tick e nao aparece em lugar nenhum; ele so ocupa
        // memoria e pode ressuscitar mais tarde com o alvo de uma hora atras.
        registro.removerDissolvidos();

        publicarAlvoDoEsquadrao(esquadrao);
        esquadraoQuebrado = esquadrao.desmoralizado();

        LivingEntity alvo = getTarget();
        distanciaDoAliadoAoAlvo = medirAliadoMaisProximo(esquadrao, alvo);
        destinoDeFuga = postura.rompeContato() ? pontoDeRecuo(alvo) : null;
        pedirArranque(alvo);
    }

    /**
     * A faxina que a LIDER faz, porque foi ela quem alistou.
     *
     * <p>Quem liga, desliga. As formigas que ela recruta nao sabem que estao num
     * esquadrao: a maioria delas nao tem uma linha de codigo de squad, e por isso
     * nenhuma delas chama {@code registro.sair} quando morre ou quando o chunk
     * descarrega. Sem esta varredura, o grupo continuaria contando formigas que
     * nao existem mais -- ocupando o teto de oito para sempre, e recusando o
     * reforco que deveria chegar com o grupo praticamente vazio. Nada disso da
     * erro: da um esquadrao de um bicho so que se acha cheio.</p>
     */
    private void dispensarAusentes(SquadRegistry registro, Squad esquadrao) {
        ServerLevel nivel = (ServerLevel) level();
        for (UUID membro : esquadrao.ids()) {
            if (membro.equals(getUUID())) continue;
            Entity achado = nivel.getEntity(membro);
            if (achado == null || !achado.isAlive()) registro.sair(membro);
        }
    }

    /**
     * Alista as formigas vizinhas que ainda nao tem grupo.
     *
     * <p>A varredura de mundo acontece SO aqui e SO no orcamento de coordenacao.
     * Ela para assim que o teto de {@link SquadRules#esquadrao()} e atingido -- e
     * e por isso que o teto tambem e um limite de CUSTO, e nao so de tamanho.</p>
     *
     * <p><b>Outro SQUADRON_LEADER nao entra.</b> Nao por hierarquia, por mecanica:
     * {@link Squad} tem um lider so, entao o segundo lider viraria membro comum e
     * passaria o encontro inteiro obedecendo em vez de fazer a unica coisa para a
     * qual ele foi desenhado. Isso nao da erro -- da um bicho caro que nunca
     * mostra a ficha.</p>
     *
     * <p>Quem entra, entra com o PAPEL DO PROPRIO MOLDE. Forcar um papel daqui
     * poria o flanqueador segurando a frente, e o molde dele -- que e onde a
     * decisao de papel esta registrada -- viraria letra morta.</p>
     */
    private void recrutarVizinhos(SquadRegistry registro, Squad esquadrao) {
        if (esquadrao.tamanho() >= REGRAS_DO_ESQUADRAO.maximoDeMembros()) return;
        AABB area = getBoundingBox().inflate(REGRAS_DO_ESQUADRAO.raioDeReforco());
        for (BaseChimeraAnt formiga : level().getEntitiesOfClass(BaseChimeraAnt.class, area,
                outra -> outra != this && outra.isAlive())) {
            if (esquadrao.tamanho() >= REGRAS_DO_ESQUADRAO.maximoDeMembros()) return;
            if (formiga.molde().rank().ordinal() >= ChimeraRank.SQUADRON_LEADER.ordinal()) continue;
            // Conferir ANTES de chamar entrar: o registro recusa quem ja consta em
            // outro grupo, e a recusa vem como IllegalStateException no meio do
            // tick do servidor -- derrubando o tick inteiro por causa de uma
            // formiga que so estava ocupada.
            if (registro.bandoDe(formiga.getUUID()).isPresent()) continue;
            registro.entrar(esquadrao.id(), formiga.getUUID(), formiga.molde().papelNoSquad());
        }
    }

    /**
     * O alvo do esquadrao e o DELA, e so ela escreve.
     *
     * <p>Ela e a unica que publica porque e a unica que chega primeiro: o alvo do
     * grupo tem de ser quem ela abriu. Com qualquer membro podendo escrever, um
     * que enxergasse um segundo jogador trocaria o alvo no meio da convergencia, e
     * o grupo oscilaria entre dois alvos sem nada acusar.</p>
     */
    private void publicarAlvoDoEsquadrao(Squad esquadrao) {
        LivingEntity meu = getTarget();
        if (meu != null && meu.isAlive()) {
            esquadrao.alvo(meu.getUUID());
            return;
        }
        if (resolver(esquadrao.alvo().orElse(null)) == null) esquadrao.alvo(null);
    }

    /**
     * Distancia do companheiro mais proximo ate o alvo, em blocos.
     *
     * <p>{@link Double#POSITIVE_INFINITY} quando nao ha companheiro vivo ou nao ha
     * alvo -- e o valor certo, porque "ninguem chegou" tem de passar por toda
     * comparacao de proximidade. Devolver zero faria o oposto: o arranque seria
     * recusado para sempre com {@code ALIADO_JA_ABRIU} num esquadrao de uma
     * formiga so.</p>
     */
    private double medirAliadoMaisProximo(Squad esquadrao, LivingEntity alvo) {
        if (alvo == null) return Double.POSITIVE_INFINITY;
        ServerLevel nivel = (ServerLevel) level();
        double menor = Double.POSITIVE_INFINITY;
        for (UUID membro : esquadrao.ids()) {
            if (membro.equals(getUUID())) continue;
            Entity achado = nivel.getEntity(membro);
            if (achado == null || !achado.isAlive()) continue;
            menor = Math.min(menor, achado.distanceTo(alvo));
        }
        return menor;
    }

    /**
     * Pede o arranque -- e so quando a postura autoriza fechar distancia.
     *
     * <p>Ele e pedido no orcamento de coordenacao, e nao todo tick, e isso e
     * deliberado: a decisao e sobre uma corrida de trinta ticks, e medir a
     * distancia de todos os companheiros vinte vezes por segundo custaria uma
     * varredura por tick para responder uma pergunta que nao muda em meio segundo.
     * O relogio, esse sim, corre todo tick.</p>
     */
    private void pedirArranque(LivingEntity alvo) {
        if (!postura.fechaDistancia()) return;
        boolean temAlvo = alvo != null && alvo.isAlive();
        double distancia = temAlvo ? distanceTo(alvo) : 0.0D;
        ultimaDecisaoDoArranque = arranque.tentar(distancia, distanciaDoAliadoAoAlvo, temAlvo,
                temAlvo && hasLineOfSight(alvo));
    }

    /**
     * Para onde recuar quando a postura e romper contato.
     *
     * <p>Sem alvo, recuar tem de continuar significando alguma coisa: uma lider
     * cujo esquadrao quebrou e que nao ve ninguem ainda precisa sair de onde esta,
     * senao "romper contato" vira "ficar parada" e o jogador nunca ve o grupo
     * quebrar.</p>
     */
    private Vec3 pontoDeRecuo(LivingEntity alvo) {
        Vec3 fuga = alvo == null ? getLookAngle().reverse() : position().subtract(alvo.position());
        Vec3 horizontal = new Vec3(fuga.x, 0.0D, fuga.z);
        if (horizontal.lengthSqr() < 1.0E-6D) return null;
        return position().add(horizontal.normalize()
                .scale(CheetahLeaderTuning.DISTANCIA_MINIMA_DO_ARRANQUE));
    }

    /** UUID -> entidade viva DESTE nivel, ou nulo. A referencia nunca e guardada. */
    private LivingEntity resolver(UUID id) {
        if (id == null || level().isClientSide) return null;
        Entity achado = ((ServerLevel) level()).getEntity(id);
        return achado instanceof LivingEntity vivo && vivo.isAlive() ? vivo : null;
    }

    private void aplicarAlvo(PerceptionSnapshot snapshot) {
        Optional<UUID> lembrado = snapshot.alvoOpcional();
        if (lembrado.isEmpty()) {
            if (getTarget() != null) setTarget(null);
            return;
        }
        LivingEntity vivo = resolver(lembrado.get());
        if (vivo != null) {
            if (getTarget() != vivo) setTarget(vivo);
        } else if (getTarget() != null) {
            setTarget(null);
        }
    }

    /** Varredura de mundo da percepcao -- a UNICA, e so quando o orcamento autoriza. */
    private List<TargetCandidate> varrerCandidatos() {
        double alcance = getAttributeValue(Attributes.FOLLOW_RANGE);
        AABB area = getBoundingBox().inflate(alcance);
        List<TargetCandidate> candidatos = new ArrayList<>();
        for (Player jogador : level().getEntitiesOfClass(Player.class, area,
                p -> p.isAlive() && !p.isSpectator() && !p.isCreative())) {
            candidatos.add(new TargetCandidate(jogador.getUUID(),
                    com.darkcontinent.nenfoundation.enemy.api.EnemyFaction.HUNTER_ASSOCIATION,
                    distanceTo(jogador), cossenoDoOlharAte(jogador), hasLineOfSight(jogador),
                    true, true, jogador == getLastHurtByMob()));
        }
        return candidatos;
    }

    private double cossenoDoOlharAte(Entity alvo) {
        Vec3 olhar = getLookAngle();
        Vec3 horizontal = new Vec3(olhar.x, 0.0D, olhar.z);
        Vec3 ateOAlvo = new Vec3(alvo.getX() - getX(), 0.0D, alvo.getZ() - getZ());
        if (horizontal.lengthSqr() < 1.0E-6D || ateOAlvo.lengthSqr() < 1.0E-6D) return 0.0D;
        return Mth.clamp(horizontal.normalize().dot(ateOAlvo.normalize()), -1.0D, 1.0D);
    }

    // ---------------------------------------------------------------- golpe

    /**
     * A janela ACTIVE consulta a caixa do bote; fora dela o golpe nao existe.
     *
     * <p>Roda TODO tick, e nao no orcamento de coordenacao: cinco ticks de janela
     * medidos de dez em dez ticks acertariam ou errariam por sorte.</p>
     */
    private void tickDoGolpe(EnemyRuntime runtime) {
        AttackController ataques = runtime.ataques();
        if (ataques.phase() != AttackPhase.ACTIVE) return;
        aplicarImpulso(ataques.attackInstanceId());

        LivingEntity alvo = getTarget();
        if (alvo == null || !alvo.isAlive()) return;
        ataques.tryHit(alvo.getId(), alvo.getBoundingBox(), CAIXA_DO_BOTE,
                        position(), getYRot(), "body")
                .ifPresent(hit -> {
                    alvo.hurt(damageSources().mobAttack(this), hit.damage());
                    alvo.knockback(CheetahLeaderTuning.EMPURRAO_DO_BOTE,
                            getX() - alvo.getX(), getZ() - alvo.getZ());
                });
    }

    /**
     * O avanco que paga a diferenca entre o focinho desenhado e a caixa do bote.
     *
     * <p>UMA vez por instancia de ataque. Aplicado nos cinco ticks da janela, ela
     * atravessaria o alvo e sairia do outro lado -- e {@code tryHit}, que so
     * acerta cada alvo uma vez por instancia, esconderia o defeito atras de um
     * dano perfeitamente correto.</p>
     */
    private void aplicarImpulso(long instancia) {
        if (instancia == instanciaComImpulso) return;
        instanciaComImpulso = instancia;
        Vec3 olhar = getLookAngle();
        Vec3 frente = new Vec3(olhar.x, 0.0D, olhar.z);
        if (frente.lengthSqr() < 1.0E-6D) return;
        frente = frente.normalize().scale(CheetahLeaderTuning.IMPULSO_DO_BOTE);
        setDeltaMovement(getDeltaMovement().add(frente.x, 0.0D, frente.z));
        this.hasImpulse = true;
    }

    /**
     * Apanhar interrompe o bote E abala o esquadrao.
     *
     * <p>Nao ha ponto fraco neste bicho, e a ausencia e deliberada: a licao dele e
     * esperar a fadiga, nao mirar um lugar. Um multiplicador por regiao daria ao
     * jogador uma resposta de precisao para um problema de tempo.</p>
     *
     * <p>Dano sem atacante -- fogo, queda -- nao cambaleia e nao abala: cambalear
     * por queimadura transformaria fogo numa interrupcao permanente, e um
     * esquadrao quebrado por lava nao ensina nada sobre esquadrao.</p>
     */
    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean levou = super.hurt(source, amount);
        if (!levou || level().isClientSide) return levou;
        if (!(source.getEntity() instanceof LivingEntity)) return true;

        EnemyRuntime runtime = runtimeExigido();
        StaggerResult resultado = runtime.sofrerStagger(runtime.ataques().attackInstanceId(),
                amount, CheetahLeaderTuning.RECARGA_APOS_INTERRUPCAO);
        if (resultado == StaggerResult.DISPAROU) {
            getNavigation().stop();
            this.entityData.set(CAMBALEANDO, true);
            this.entityData.set(FASE_DE_ATAQUE, AttackPhase.IDLE.ordinal());
        }
        RegistroDeMatilhas.doNivel((ServerLevel) level()).bandoDe(getUUID())
                .ifPresent(esquadrao -> esquadrao.abalar(ABALO_POR_GOLPE_NA_LIDER));
        return true;
    }

    private void publicarEstado(EnemyRuntime runtime) {
        int fase = runtime.ataques().phase().ordinal();
        if (this.entityData.get(FASE_DE_ATAQUE) != fase) this.entityData.set(FASE_DE_ATAQUE, fase);
        boolean cambaleando = runtime.stagger().cambaleando();
        if (this.entityData.get(CAMBALEANDO) != cambaleando) {
            this.entityData.set(CAMBALEANDO, cambaleando);
        }
        int arrancando = arranque.fase().ordinal();
        if (this.entityData.get(FASE_DO_ARRANQUE) != arrancando) {
            this.entityData.set(FASE_DO_ARRANQUE, arrancando);
        }
        if (combatState() != EnemyCombatState.DYING) {
            combatState(cambaleando ? EnemyCombatState.STAGGERED
                    : postura.rompeContato() ? EnemyCombatState.RETREAT
                    : getTarget() != null ? EnemyCombatState.AGGRO : EnemyCombatState.IDLE);
        }
    }

    // ---------------------------------------------------------- ciclo de vida

    /**
     * Morrer PUBLICA o repouso.
     *
     * <p>Depois de morta o passo de IA nao roda mais, e os campos sincronizados
     * ficam onde estavam: o bicho morreria com a pose do bote travada no cliente e
     * com as patas tocando no ritmo do arranque. Nao ha erro nisso -- so um
     * cadaver correndo parado.</p>
     */
    @Override
    public void die(DamageSource source) {
        if (!level().isClientSide) publicarRepouso();
        super.die(source);
    }

    /**
     * O UNICO ponto de saida do esquadrao e do arranque.
     *
     * <p>Morte, unload de chunk, troca de dimensao e comando administrativo passam
     * todos por {@code remove}. Espalhar a saida por {@code die} mais um evento de
     * unload mais outro de dimensao e como um deles fica esquecido -- e o
     * esquecido nao da erro: deixa o esquadrao SEM LIDER e contando uma formiga
     * que nao existe mais. {@code Squad.sair} promove a proxima no mesmo tick, que
     * e exatamente o que o jogador precisa ver acontecer depois de derrubar o
     * comando.</p>
     */
    @Override
    public void remove(Entity.RemovalReason reason) {
        if (!level().isClientSide && level() instanceof ServerLevel nivel) {
            publicarRepouso();
            arranque.limpar();
            SquadRegistry registro = RegistroDeMatilhas.doNivel(nivel);
            registro.sair(getUUID());
            registro.removerDissolvidos();
        }
        super.remove(reason);
    }

    private void publicarRepouso() {
        this.entityData.set(FASE_DE_ATAQUE, AttackPhase.IDLE.ordinal());
        this.entityData.set(CAMBALEANDO, false);
        this.entityData.set(FASE_DO_ARRANQUE, FaseDoArranque.PRONTA.ordinal());
    }

    // ------------------------------------------------------------- animacao

    @Override
    public void registerControllers(
            software.bernie.geckolib.animation.AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new software.bernie.geckolib.animation.AnimationController<CheetahLeaderEntity>(
                this, "corpo", 4, estado -> {
                    if (this.deathTime > 0) return estado.setAndContinue(DEATH);
                    if (cambaleando()) return estado.setAndContinue(STAGGER);
                    return switch (faseDeAtaque()) {
                        case WINDUP -> estado.setAndContinue(WINDUP);
                        case ACTIVE -> estado.setAndContinue(STRIKE);
                        case RECOVERY -> estado.setAndContinue(RECOVERY);
                        default -> estado.setAndContinue(
                                velocidadeHorizontal() >= LIMIAR_DE_CAMINHADA ? WALK : IDLE);
                    };
                }).setAnimationSpeedHandler(CheetahLeaderEntity::ritmoDaPassada));
    }

    /**
     * O ritmo em que o clipe toca -- UM clipe de caminhada, tres velocidades.
     *
     * <p>Tres clipes de passada seriam tres verdades sobre o mesmo movimento:
     * girar o multiplicador numa sessao de balanceamento mudaria a velocidade do
     * corpo e nao a das patas, e o bicho passaria a PATINAR -- deslizando no
     * arranque e marchando no lugar na fadiga. Com um clipe e um multiplicador, a
     * mesma fonte manda nos dois.</p>
     *
     * <p><b>O ataque, o cambaleio e a morte tocam sempre em ritmo 1.0, e essa
     * excecao e a parte que nao pode faltar.</b> O manipulador de velocidade vale
     * para o controlador INTEIRO, entao sem esta guarda o clipe de bote tocaria
     * 1.55x mais rapido durante um arranque -- acabaria antes da janela que o
     * servidor cobra, e a guepardo relaxaria no meio do golpe que ainda vai
     * acertar. E exatamente o defeito que {@code valida_duracao_de_ataque} existe
     * para impedir, reintroduzido em runtime, onde nenhum portao o ve.</p>
     *
     * <p>As condicoes aqui espelham, na mesma ordem, os ramos do predicado do
     * controlador. Elas TEM de espelhar: um ramo a mais la sem o par aqui volta a
     * acelerar um clipe que nao pode acelerar.</p>
     */
    private double ritmoDaPassada() {
        if (this.deathTime > 0 || cambaleando()) return VELOCIDADE_NORMAL;
        AttackPhase fase = faseDeAtaque();
        if (fase != AttackPhase.IDLE && fase != AttackPhase.COMPLETE) return VELOCIDADE_NORMAL;
        return REGRAS_DO_ARRANQUE.multiplicadorDe(faseDoArranque());
    }

    // Contrato com cheetah_leader.animation.json. LoopType.DEFAULT em todos: o tipo de
    // repeticao mora no arquivo, e cravar em Java o faria vencer o JSON.
    private static final software.bernie.geckolib.animation.RawAnimation IDLE = clipe("idle");
    private static final software.bernie.geckolib.animation.RawAnimation WALK = clipe("walk");
    private static final software.bernie.geckolib.animation.RawAnimation WINDUP = clipe("windup");
    private static final software.bernie.geckolib.animation.RawAnimation STRIKE = clipe("strike");
    private static final software.bernie.geckolib.animation.RawAnimation RECOVERY = clipe("recovery");
    private static final software.bernie.geckolib.animation.RawAnimation STAGGER = clipe("stagger");
    private static final software.bernie.geckolib.animation.RawAnimation DEATH = clipe("death");

    private static software.bernie.geckolib.animation.RawAnimation clipe(String nome) {
        return software.bernie.geckolib.animation.RawAnimation.begin()
                .then("animation.cheetah_leader." + nome,
                        software.bernie.geckolib.animation.Animation.LoopType.DEFAULT);
    }

    /** Blocos andados no ultimo tick, medidos por POSICAO -- o delta remoto fica zerado. */
    private double velocidadeHorizontal() {
        double dx = getX() - this.xo;
        double dz = getZ() - this.zo;
        return Math.sqrt(dx * dx + dz * dz);
    }

    @Override
    public software.bernie.geckolib.animatable.instance.AnimatableInstanceCache
            getAnimatableInstanceCache() {
        return cacheDeAnimacao;
    }

    @Override
    public double getTick(Object entidade) { return this.tickCount; }

    // ------------------------------------------------------------------ goals

    /**
     * Leva a guepardo para onde a postura mandou -- em cima do alvo ou para longe.
     *
     * <p>Ela nao DECIDE nada: le {@code postura} e {@code destinoDeFuga}, que a
     * coordenacao escreveu, e o multiplicador, que o relogio do arranque publica.
     * Uma Goal que decidisse a postura por conta propria seria a segunda
     * autoridade sobre a mesma pergunta, e as duas divergiriam no tick seguinte a
     * qualquer mudanca de intencao.</p>
     */
    private static final class CacaGoal extends Goal {
        private final CheetahLeaderEntity guepardo;
        /** O ultimo ponto para o qual um caminho foi TRACADO. Nao e um destino: e um registro. */
        private Vec3 tracado;
        /** O ultimo multiplicador entregue a navegacao, para so reescreve-lo quando mudar. */
        private double velocidadeAplicada = VELOCIDADE_NORMAL;

        private CacaGoal(CheetahLeaderEntity guepardo) {
            this.guepardo = guepardo;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override public boolean canUse() {
            if (guepardo.level().isClientSide) return false;
            if (guepardo.runtimeExigido().stagger().cambaleando()) return false;
            if (guepardo.postura.rompeContato()) return guepardo.destinoDeFuga != null;
            LivingEntity alvo = guepardo.getTarget();
            return alvo != null && alvo.isAlive();
        }

        @Override public boolean canContinueToUse() { return canUse(); }

        @Override public boolean requiresUpdateEveryTick() { return true; }

        @Override public void start() { tracado = null; }

        @Override public void tick() {
            // O multiplicador e lido AGORA, e nunca guardado por mais de um tick.
            // Congelado no start(), ele manteria a velocidade do arranque durante a
            // fadiga inteira -- com a fase certa no debug, o clipe certo na tela e
            // o corpo andando errado.
            double velocidade = guepardo.arranque.multiplicadorDeVelocidade();
            if (velocidade != velocidadeAplicada) {
                guepardo.getNavigation().setSpeedModifier(velocidade);
                velocidadeAplicada = velocidade;
            }

            LivingEntity alvo = guepardo.getTarget();
            Vec3 destino = guepardo.postura.rompeContato()
                    ? guepardo.destinoDeFuga
                    : alvo == null ? null : alvo.position();
            if (destino == null) return;

            // O caminho so e refeito quando o destino MUDOU de verdade. Chamar
            // moveTo todo tick manda o pathfinder recalcular vinte vezes por
            // segundo por bicho: numa colonia isso e custo puro, e o sintoma nao e
            // erro -- e TPS caindo enquanto o grupo esta em tela.
            double tolerancia = TOLERANCIA_DO_DESTINO * TOLERANCIA_DO_DESTINO;
            if (tracado == null || tracado.distanceToSqr(destino) > tolerancia) {
                guepardo.getNavigation().moveTo(destino.x, destino.y, destino.z, velocidade);
                tracado = destino;
            }
            // Encarar o alvo enquanto fecha distancia e o que diz ao jogador que
            // ela vem atras dele, e nao passando por perto. Rompendo contato o
            // olhar fica solto -- quem foge nao encara.
            if (alvo != null && guepardo.postura.fechaDistancia()) {
                guepardo.getLookControl().setLookAt(alvo, 30.0F, 30.0F);
            }
        }

        @Override public void stop() {
            guepardo.getNavigation().stop();
            // Quem ligou, desliga. O modificador de velocidade e da NAVEGACAO e nao
            // da Goal: deixado em 1.55 ao sair, ele valeria para o passeio ocioso e
            // para toda Goal seguinte -- um guepardo em arranque permanente sem
            // nunca ter arrancado, e sem nada no log.
            guepardo.getNavigation().setSpeedModifier(VELOCIDADE_NORMAL);
            velocidadeAplicada = VELOCIDADE_NORMAL;
            tracado = null;
        }
    }

    /** Dispara e conduz o bote; quem mede a janela e o {@link AttackController}. */
    private static final class BoteGoal extends Goal {
        private final CheetahLeaderEntity guepardo;

        private BoteGoal(CheetahLeaderEntity guepardo) {
            this.guepardo = guepardo;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override public boolean canUse() {
            if (guepardo.level().isClientSide) return false;
            // Rompendo contato ela nao ataca: um bicho que decidiu sumir e da o
            // bote no caminho contradiz a propria decisao na tela.
            if (guepardo.postura.rompeContato()) return false;
            EnemyRuntime runtime = guepardo.runtimeExigido();
            if (runtime.stagger().cambaleando()) return false;
            LivingEntity alvo = guepardo.getTarget();
            if (alvo == null || !alvo.isAlive()) return false;
            return guepardo.distanceTo(alvo) <= CheetahLeaderTuning.ALCANCE_DO_BOTE
                    && guepardo.hasLineOfSight(alvo)
                    && runtime.ataques().canStart();
        }

        @Override public boolean canContinueToUse() {
            // O bote continua ate a fase acabar, MESMO que a postura ja tenha
            // mudado. Cortar o golpe no meio porque a moral caiu deixaria o jogador
            // vendo um ataque que some -- e o servidor ja cobrou o windup inteiro
            // por ele.
            AttackPhase fase = guepardo.runtimeExigido().ataques().phase();
            return fase != AttackPhase.IDLE && fase != AttackPhase.COMPLETE
                    && !guepardo.runtimeExigido().stagger().cambaleando();
        }

        @Override public boolean requiresUpdateEveryTick() { return true; }

        @Override public void start() {
            guepardo.getNavigation().stop();
            guepardo.runtimeExigido().ataques().start(CheetahLeaderTuning.bote());
        }

        @Override public void tick() {
            LivingEntity alvo = guepardo.getTarget();
            // Encarar o alvo so ate o fim do WINDUP: a partir da janela ACTIVE a
            // direcao esta TRAVADA. Sem travar, o bote viraria mira-laser -- ela
            // giraria junto com quem desvia, e sair do caminho deixaria de ser a
            // resposta ao unico golpe que ela tem.
            AttackPhase fase = guepardo.runtimeExigido().ataques().phase();
            if (alvo != null && fase == AttackPhase.WINDUP) {
                guepardo.getLookControl().setLookAt(alvo, 30.0F, 30.0F);
            }
            // Freia no chao fora da janela ativa. O impulso do bote e aplicado uma
            // unica vez, em aplicarImpulso; sem o freio, ele se somaria a inercia
            // do arranque -- que e a maior da colonia -- e ela passaria direto pelo
            // alvo com a caixa junto.
            if (fase != AttackPhase.ACTIVE) {
                guepardo.setDeltaMovement(guepardo.getDeltaMovement().multiply(0.2D, 1.0D, 0.2D));
            }
        }

        @Override public void stop() { guepardo.getNavigation().stop(); }
    }
}
