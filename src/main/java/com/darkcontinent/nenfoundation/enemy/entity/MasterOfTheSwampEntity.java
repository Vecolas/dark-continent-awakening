package com.darkcontinent.nenfoundation.enemy.entity;

import com.darkcontinent.nenfoundation.enemy.ai.AwarenessInput;
import com.darkcontinent.nenfoundation.enemy.ai.AwarenessTuning;
import com.darkcontinent.nenfoundation.enemy.api.EnemyAwarenessState;
import com.darkcontinent.nenfoundation.enemy.api.EnemyCombatState;
import com.darkcontinent.nenfoundation.enemy.base.BaseHxHMob;
import com.darkcontinent.nenfoundation.enemy.combat.AttackDefinition;
import com.darkcontinent.nenfoundation.enemy.combat.AttackPhase;
import com.darkcontinent.nenfoundation.enemy.combat.AttackTimeline;
import com.darkcontinent.nenfoundation.enemy.content.HunterExamProfiles;
import com.darkcontinent.nenfoundation.enemy.encounter.RegrasDeFisgada;
import com.darkcontinent.nenfoundation.enemy.registry.EnemyEntityTypes;
import java.util.EnumSet;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.SmoothSwimmingLookControl;
import net.minecraft.world.entity.ai.control.SmoothSwimmingMoveControl;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomSwimmingGoal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.navigation.WaterBoundPathNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.Animation;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * Master of the swamp: o peixe enorme que nao se mata, se FISGA.
 *
 * <p>DECISAO CENTRAL QUE ESTE ARQUIVO CARREGA: a mecanica principal deste mob e
 * PESCA, e o combate e secundario por decisao. O jogador lanca a vara, o bicho
 * morde, e comeca um cabo de guerra: puxar demais arrebenta a linha e ele
 * escapa; acompanhar o peixe o cansa ate poder ser recolhido com a mao vazia.
 * Bater nele continua possivel, custa sessenta de vida atras de armadura 4, e
 * ele foge ao primeiro golpe -- e chato DE PROPOSITO. Quem "consertar" isso
 * transformando o encontro numa briga agradavel apaga o mob inteiro sem que
 * nada de erro.</p>
 *
 * <p>A ISCA E O ANZOL DE VERDADE. Este mob nao inventa um sistema de pesca
 * proprio: ele procura {@link FishingHook} na agua, como o jogo ja tem, e usa o
 * DONO do anzol como a outra ponta da linha. Um "modo de pesca" paralelo seria
 * a segunda autoridade sobre a mesma mecanica, e a divergencia entre as duas
 * apareceria como uma vara que as vezes funciona.</p>
 *
 * <p>SAIDA UNICA: existe UM metodo que solta a linha ({@link #soltarLinha()}),
 * e TODOS os caminhos passam por ele -- arrebentar, cansar, captura, morte do
 * pescador, morte do peixe, remocao da entidade e troca de dimensao. Um caminho
 * que limpasse "por conta propria" nao daria erro nenhum; daria um jogador
 * sendo puxado por um peixe que ja nao existe, ate o servidor reiniciar.</p>
 *
 * <p>Tudo que decide -- se a isca interessa, quanta tensao a linha levou,
 * quando ela arrebenta, quando o bicho cansa e se a captura vale -- roda no
 * SERVIDOR. Os quatro campos sincronizados carregam apenas FISGADO, CANSADO,
 * ESCAPANDO e a FASE da bocada, que e o que o cliente precisa para escolher o
 * clipe. O cliente nunca informa fisgada nem captura.</p>
 *
 * <p>PRIMEIRO MOB AQUATICO DO REPOSITORIO. O molde e o {@code Dolphin} do
 * vanilla: {@link WaterBoundPathNavigation}, {@link SmoothSwimmingMoveControl},
 * malus de agua zerado e {@code checkSpawnObstruction} que ACEITA liquido. As
 * tres primeiras sao infraestrutura reutilizada (ADR-017 item 4); a quarta e
 * obrigatoria -- o padrao do {@code Mob} recusa spawn dentro de agua, e sem ela
 * o mob nunca nasceria e nada acusaria.</p>
 */
public final class MasterOfTheSwampEntity extends BaseHxHMob implements GeoEntity {

    // ------------------------------------------------------ estado publicado
    /** Linha presa; o cliente precisa saber para tocar o debate-se. */
    private static final EntityDataAccessor<Boolean> FISGADO =
            SynchedEntityData.defineId(MasterOfTheSwampEntity.class, EntityDataSerializers.BOOLEAN);
    /** Esgotado; e o unico estado em que a captura funciona. */
    private static final EntityDataAccessor<Boolean> CANSADO =
            SynchedEntityData.defineId(MasterOfTheSwampEntity.class, EntityDataSerializers.BOOLEAN);
    /**
     * Mergulhando para sumir.
     *
     * <p>Existe como campo PROPRIO, e nao como uma fase de ataque reaproveitada,
     * porque a fuga e a recuperacao da bocada acontecem em momentos diferentes e
     * pedem clipes diferentes. Empilhar as duas em {@code RECOVERY} faria o
     * peixe tocar "escape" toda vez que fechasse a boca -- sem erro nenhum no
     * log, so uma animacao que mente sobre o que o servidor esta fazendo.</p>
     */
    private static final EntityDataAccessor<Boolean> ESCAPANDO =
            SynchedEntityData.defineId(MasterOfTheSwampEntity.class, EntityDataSerializers.BOOLEAN);
    /** Fase da bocada; o unico estado de combate que o cliente precisa conhecer. */
    private static final EntityDataAccessor<Integer> FASE_DE_ATAQUE =
            SynchedEntityData.defineId(MasterOfTheSwampEntity.class, EntityDataSerializers.INT);

    private static final AttackDefinition MORDIDA = HunterExamProfiles.masterOfTheSwampBite();
    private static final RegrasDeFisgada FISGADA = HunterExamProfiles.masterOfTheSwampFishing();

    // ------------------------------------------------------------ constantes
    /** Quanto tempo o peixe lembra de quem estava segurando a vara. */
    private static final int MEMORIA_DE_ALVO_TICKS = 100;
    /** Metade da vida: abaixo disso o cerebro pode decidir FLEE. */
    private static final float FRACAO_DE_VIDA_CRITICA = 0.5F;
    /** Folga, alem do meio-corpo, para a boca alcancar o anzol. */
    private static final double ALCANCE_DA_MORDIDA = 1.2D;
    /**
     * Tres blocos de puxao por ponto de dano.
     *
     * <p>Nao e um segundo caminho de tensao: e a CONVERSAO que faz o golpe
     * passar pela mesma {@link RegrasDeFisgada#tensao} que o puxao usa. Somar
     * dano direto no campo criaria uma segunda formula de tensao, e a divergencia
     * entre as duas so apareceria como uma linha que arrebenta em horas que
     * ninguem consegue reproduzir.</p>
     */
    private static final double AFASTAMENTO_POR_DANO = 3.0D;

    /**
     * Ritmo do cabo de guerra: 20 ticks de arranco, 20 de descanso.
     *
     * <p>Nao e botao de balanceamento, e LEGIBILIDADE. Um peixe que puxa em
     * todo tick ganharia sempre -- a tensao subiria mais rapido do que qualquer
     * jogador consegue acompanhar, e o encontro viraria "a linha sempre
     * arrebenta". O intervalo e o que da ao jogador a janela de recuperar
     * distancia, e e nele que o cabo de guerra existe.</p>
     */
    private static final int TICKS_DE_ARRANCO = 20;
    private static final int TICKS_DE_DESCANSO = 20;
    /**
     * Impulso do arranco no PROPRIO peixe, para longe de quem segura a vara.
     *
     * <p>O numero e pequeno porque ele e aplicado TODO TICK do arranco e a agua
     * so freia 20% por tick: a velocidade de regime e cerca de quatro vezes o
     * impulso, ou seja ~0.24 bloco por tick. Isso e pouco mais rapido do que um
     * jogador nadando, que e exatamente o ponto -- o peixe ganha distancia
     * devagar, e o jogador que corre pela margem recupera. Um impulso "obvio"
     * como 0.35 daria 1.4 bloco por tick e mandaria o peixe para fora do alcance
     * da vara em meio segundo: a linha arrebentaria SEMPRE, e o cabo de guerra
     * nunca teria existido.</p>
     */
    private static final double FORCA_DO_ARRANCO = 0.06D;
    /**
     * Impulso do arranco NO PESCADOR, na direcao do peixe. E ele que se sente na mao.
     *
     * <p>Um quinto do impulso do peixe, de proposito: ele precisa ser SENTIDO e
     * nao pode COMPENSAR. Igualado ao do peixe, o jogador seria arrastado junto,
     * a distancia entre os dois nunca mudaria, a tensao ficaria congelada e o
     * encontro inteiro viraria uma espera de dez segundos sem decisao nenhuma --
     * sem erro nenhum no log.</p>
     */
    private static final double FORCA_DO_PUXAO_NO_PESCADOR = 0.012D;

    /** Quanto tempo o mergulho de fuga dura; depois dele o peixe volta a nadar. */
    private static final int TICKS_DE_ESCAPE = 60;
    /** Dez segundos sem olhar para isca nenhuma depois de soltar a linha. */
    private static final int TICKS_DE_ESPERA_APOS_SOLTAR = 200;
    /**
     * Dez segundos de peixe esgotado boiando.
     *
     * <p>E A JANELA DE CAPTURA, e e o que impede "cansou" de virar "cansado para
     * sempre". Sem ela o bicho ficaria recolhivel ate alguem passar por ali no
     * dia seguinte, e o cabo de guerra que o esgotou nao teria valido nada.</p>
     */
    private static final int TICKS_DE_RECUPERACAO = 200;
    /** Espera curta entre bocadas que nao fisgaram nada. */
    private static final int TICKS_ENTRE_MORDIDAS = 60;

    /**
     * Componente horizontal do mergulho de fuga.
     *
     * <p>Quase o dobro do arranco, e isso e o desenho: resistir na linha e um
     * empurra-empurra que o jogador pode ganhar; FUGIR nao. Com a mesma conta de
     * regime (~4x o impulso), a fuga sai a ~0.4 bloco por tick e cobre umas duas
     * dezenas de blocos nos tres segundos da janela -- longe o bastante para o
     * encontro ter acabado de verdade.</p>
     */
    private static final double AFASTAMENTO_DO_ESCAPE = 0.10D;
    /** Componente vertical do mergulho de fuga: ele SOME para baixo. */
    private static final double MERGULHO_DO_ESCAPE = 0.05D;
    /** Giro por tick, em graus, a partir do qual o corpo toca o clipe de virada. */
    private static final float LIMITE_DE_GIRO_EM_GRAUS = 6.0F;

    // ------------------------------------------------------------- animacao
    // Os nomes abaixo sao um CONTRATO com master_of_the_swamp.animation.json.
    // Errar um deles nao da erro: o GeckoLib simplesmente nao acha o clipe e
    // deixa o osso parado. E o tipo de falha que so aparece na tela de quem joga.
    private static final RawAnimation SWIM = RawAnimation.begin()
            .then("animation.master_of_the_swamp.swim", Animation.LoopType.DEFAULT);
    private static final RawAnimation TURN = RawAnimation.begin()
            .then("animation.master_of_the_swamp.turn", Animation.LoopType.DEFAULT);
    private static final RawAnimation BITE = RawAnimation.begin()
            .then("animation.master_of_the_swamp.bite", Animation.LoopType.DEFAULT);
    private static final RawAnimation THRASH = RawAnimation.begin()
            .then("animation.master_of_the_swamp.thrash", Animation.LoopType.DEFAULT);
    private static final RawAnimation CAUGHT = RawAnimation.begin()
            .then("animation.master_of_the_swamp.caught", Animation.LoopType.DEFAULT);
    private static final RawAnimation ESCAPE = RawAnimation.begin()
            .then("animation.master_of_the_swamp.escape", Animation.LoopType.DEFAULT);

    /** Nome do unico controller; quem registrar um segundo clipe reusa esta constante. */
    private static final String CONTROLLER_DO_CORPO = "corpo";
    /**
     * Ticks de mistura entre um clipe e o proximo.
     *
     * <p>Quatro, e nao mais: a janela ACTIVE da bocada dura 4 ticks. Uma
     * transicao mais longa do que a fase que ela atravessa comeria a mordida
     * inteira em mistura, e quem perdeu a isca nunca veria a boca fechar.</p>
     */
    private static final int TRANSICAO_EM_TICKS = 4;

    /** Cache por INSTANCIA. Um cache estatico faria todos os peixes dividirem um clipe. */
    private final AnimatableInstanceCache cacheDeAnimacao = GeckoLibUtil.createInstanceCache(this);

    // -------------------------------------------------------- estado runtime
    // Estado DA INSTANCIA. Guardar qualquer um destes numa Goal (ou num static)
    // faria todos os peixes do mundo dividirem o mesmo pescador -- a Goal e uma
    // por entidade, e as classes aninhadas abaixo sao static justamente para nao
    // deixar essa dependencia escondida.
    //
    // Nada disto e persistido, DE PROPOSITO: fisgada e estado de runtime
    // (ADR-002). Um save no meio do cabo de guerra devolve um peixe livre e uma
    // vara vazia, que e o comportamento certo -- o anzol tambem nao sobrevive ao
    // reload.
    private AttackTimeline linhaDoTempoDaMordida = new AttackTimeline();
    private boolean mordidaEmAndamento;

    /** Anzol que o peixe esta investigando; vira {@link #anzolFisgado} na bocada. */
    private FishingHook anzolAlvo;
    /** Anzol preso na boca. Nao nulo se e somente se a linha esta presa. */
    private FishingHook anzolFisgado;
    /** Dono do anzol preso -- a outra ponta da linha. */
    private Player pescador;
    private double tensao;
    private int ticksFisgado;
    private double distanciaAnteriorDoPescador = Double.NaN;
    private int ticksDeCicloDeArranco;

    private int escapeRestante;
    private int esperaRestante;
    private int recuperacaoRestante;

    // Sensores do cerebro.
    private int memoriaDeAlvo;
    private double distanciaAnteriorDoAlvo = Double.NaN;

    public MasterOfTheSwampEntity(EntityType<? extends MasterOfTheSwampEntity> type, Level level) {
        super(type, level, HunterExamProfiles.masterOfTheSwamp().metadata(),
                // O cerebro passa de WARN para ENGAGE exatamente no tempo do aviso da
                // bocada: um segundo numero aqui divergiria do perfil sem dar erro, e o
                // peixe "avisaria" num relogio e morderia em outro.
                new AwarenessTuning(MORDIDA.windupTicks(), MEMORIA_DE_ALVO_TICKS));
        this.moveControl = new SmoothSwimmingMoveControl(this, 85, 10, 0.6F, 0.1F, true);
        this.lookControl = new SmoothSwimmingLookControl(this, 10);
        // Agua deixa de ser penalidade e passa a ser o caminho normal. Sem esta
        // linha o pathfinder trata cada bloco de agua como custo 8 e o peixe fica
        // parado dentro do proprio bioma, sem erro nenhum no log.
        setPathfindingMalus(PathType.WATER, 0.0F);
        setPathfindingMalus(PathType.WATER_BORDER, 0.0F);
    }

    public static AttributeSupplier.Builder createAttributes() {
        var attrs = HunterExamProfiles.masterOfTheSwamp().attributes();
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, attrs.maxHealth())
                .add(Attributes.MOVEMENT_SPEED, attrs.movementSpeed())
                .add(Attributes.ATTACK_DAMAGE, attrs.attackDamage())
                .add(Attributes.ARMOR, attrs.armor())
                .add(Attributes.FOLLOW_RANGE, attrs.followRange())
                .add(Attributes.KNOCKBACK_RESISTANCE, attrs.knockbackResistance());
    }

    public static EntityType<MasterOfTheSwampEntity> registeredType() {
        return EnemyEntityTypes.MASTER_OF_THE_SWAMP.get();
    }

    // ------------------------------------------------------------- aquatico

    @Override
    protected PathNavigation createNavigation(Level nivel) {
        return new WaterBoundPathNavigation(this, nivel);
    }

    /**
     * ACEITA liquido no lugar do spawn.
     *
     * <p>O padrao do {@code Mob} e {@code !containsAnyLiquid(...) &&
     * isUnobstructed(...)}, e ele recusaria TODO spawn deste mob. Nao daria erro
     * nenhum: apareceria como um pantano vazio, exatamente o relato de bug que
     * ninguem consegue reproduzir. E a mesma troca que o {@code WaterAnimal} do
     * vanilla faz, e e infraestrutura, nao identidade emprestada.</p>
     */
    @Override
    public boolean checkSpawnObstruction(LevelReader nivel) {
        return nivel.isUnobstructed(this);
    }

    /**
     * Fora d'agua ele sufoca; dentro, o ar nunca acaba.
     *
     * <p>{@code canBreatheUnderwater()} e FINAL em {@code LivingEntity} -- ele le
     * uma tag de tipo de entidade, e nao um metodo. Sobrescrever nao e opcao, e
     * depender de um arquivo de tag colocaria a respiracao do mob a distancia de
     * um datapack de terceiro. Este e o caminho do {@code WaterAnimal}: o ar e
     * reposto DEPOIS do baseTick, que e quem o teria gasto.</p>
     */
    @Override
    public void baseTick() {
        int arAntes = getAirSupply();
        super.baseTick();
        if (isAlive() && !isInWaterOrBubble()) {
            setAirSupply(arAntes - 1);
            if (getAirSupply() == -20) {
                setAirSupply(0);
                hurt(damageSources().drown(), 2.0F);
            }
        } else {
            setAirSupply(getMaxAirSupply());
        }
    }

    /** Corrente de agua nao empurra um bicho deste tamanho. */
    @Override
    public boolean isPushedByFluid() { return false; }

    /** Ninguem poe coleira no senhor do pantano. */
    @Override
    public boolean canBeLeashed() { return false; }

    // -------------------------------------------------------- dados sincronizados

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(FISGADO, Boolean.FALSE);
        builder.define(CANSADO, Boolean.FALSE);
        builder.define(ESCAPANDO, Boolean.FALSE);
        builder.define(FASE_DE_ATAQUE, AttackPhase.IDLE.ordinal());
    }

    /** Linha presa; vale nos dois lados, porque vem do SynchedEntityData. */
    public boolean estaFisgado() { return this.entityData.get(FISGADO); }

    /** Esgotado e recolhivel; vale nos dois lados, pelo mesmo motivo. */
    public boolean estaCansado() { return this.entityData.get(CANSADO); }

    /** Mergulhando para sumir; vale nos dois lados, pelo mesmo motivo. */
    public boolean estaEscapando() { return this.entityData.get(ESCAPANDO); }

    /** Fase corrente da bocada; vale nos dois lados, pelo mesmo motivo. */
    public AttackPhase faseDeAtaque() {
        int ordinal = this.entityData.get(FASE_DE_ATAQUE);
        AttackPhase[] fases = AttackPhase.values();
        return ordinal >= 0 && ordinal < fases.length ? fases[ordinal] : AttackPhase.IDLE;
    }

    // ------------------------------------------------------------------ tick

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (level().isClientSide) return;

        if (esperaRestante > 0) esperaRestante--;
        if (recuperacaoRestante > 0 && --recuperacaoRestante == 0) descansar();
        if (escapeRestante > 0) {
            mergulharParaLonge();
            if (--escapeRestante == 0) this.entityData.set(ESCAPANDO, Boolean.FALSE);
        }

        // A bocada e a linha sao tickadas AQUI, e nao dentro das Goals, porque uma
        // Goal pode ser preemptada por outra de prioridade maior. Se o relogio da
        // linha dependesse da Goal viva, a tensao congelaria no meio do cabo de
        // guerra -- sem erro nenhum no log, e com o jogador ainda sendo puxado.
        if (mordidaEmAndamento) tickDaMordida();
        tickDaLinha();
        if (estaCansado()) boiarExausto();

        procurarIsca();

        EnemyAwarenessState consciencia = enemyBrain().tick(lerSensores());
        alinharEstadoDeCombate(consciencia);
    }

    // ------------------------------------------------------------------ isca

    /**
     * Acha o anzol mais proximo que {@link RegrasDeFisgada#investigaIsca} aceita.
     *
     * <p>Quem decide e a regra pura; este metodo so mede e filtra. O peixe
     * ocupado -- mordendo, fisgado, cansado ou fugindo -- nao olha para isca
     * nenhuma, e isso e o que impede duas fisgadas de se sobreporem.</p>
     */
    private void procurarIsca() {
        if (estaFisgado() || estaCansado() || escapeRestante > 0) {
            anzolAlvo = null;
            return;
        }
        // A BOCADA EM CURSO E DONA DO ALVO, e por isso ela nao pode ser varrida
        // aqui: este metodo roda DEPOIS do tick da bocada, e limpar o anzol no meio
        // dela faria a fase ACTIVE do tick seguinte nao achar mais o que fisgar.
        // Nao daria erro nenhum -- daria um peixe que abre a boca e nunca pega nada.
        if (mordidaEmAndamento) return;
        double raio = FISGADA.raioDaIsca();
        AABB zona = getBoundingBox().inflate(raio);
        FishingHook melhor = null;
        double menorDistancia = Double.MAX_VALUE;
        for (FishingHook anzol : level().getEntitiesOfClass(FishingHook.class, zona, this::iscaValida)) {
            double distancia = distanceTo(anzol);
            if (!FISGADA.investigaIsca(anzol.isInWater(), distancia, esperaRestante)) continue;
            if (distancia < menorDistancia) {
                menorDistancia = distancia;
                melhor = anzol;
            }
        }
        anzolAlvo = melhor;
    }

    /**
     * Isca legitima: existe, esta na agua e tem um dono vivo que nao e
     * espectador.
     *
     * <p>O filtro de espectador esta aqui e nao no puxao: fisgar a vara de quem
     * so estava assistindo nao daria erro, so puxaria uma camera pelo pantano.</p>
     */
    private boolean iscaValida(FishingHook anzol) {
        if (anzol == null || anzol.isRemoved() || !anzol.isInWater()) return false;
        Player dono = anzol.getPlayerOwner();
        return dono != null && dono.isAlive() && !dono.isSpectator() && dono.level() == level();
    }

    // ---------------------------------------------------------------- bocada

    /** Comeca o aviso; a linha do tempo e NOVA para nunca herdar fase presa. */
    private void iniciarMordida() {
        mordidaEmAndamento = true;
        linhaDoTempoDaMordida = new AttackTimeline();
        linhaDoTempoDaMordida.start(MORDIDA);
        getNavigation().stop();
        combatState(EnemyCombatState.WINDUP);
        publicarFase(AttackPhase.WINDUP);
    }

    /**
     * WINDUP e a boca abrindo -- o unico aviso que o jogador recebe, e quem
     * recolhe a linha nele escapa da fisgada inteira. ACTIVE e a bocada, que
     * fisga o anzol e machuca quem estiver dentro dela. RECOVERY e a boca
     * fechando.
     */
    private void tickDaMordida() {
        AttackPhase fase = linhaDoTempoDaMordida.phase();
        getNavigation().stop();
        FishingHook anzol = anzolAlvo;
        switch (fase) {
            case WINDUP -> {
                combatState(EnemyCombatState.WINDUP);
                if (anzol != null && !anzol.isRemoved()) {
                    getLookControl().setLookAt(anzol.getX(), anzol.getY(), anzol.getZ(), 30.0F, 30.0F);
                }
            }
            case ACTIVE -> {
                combatState(EnemyCombatState.ACTIVE);
                aplicarDanoDaMordida();
                tentarFisgar();
            }
            case RECOVERY -> combatState(EnemyCombatState.RECOVERY);
            default -> { }
        }
        linhaDoTempoDaMordida.tick();
        AttackPhase proxima = linhaDoTempoDaMordida.phase();
        if (proxima != AttackPhase.COMPLETE) {
            publicarFase(proxima);
            return;
        }
        mordidaEmAndamento = false;
        publicarFase(AttackPhase.IDLE);
        if (!estaFisgado()) {
            // Bocada no vazio: espera curta, para o peixe nao ficar abrindo a boca
            // em todo tick em cima da mesma boia.
            anzolAlvo = null;
            esperaRestante = Math.max(esperaRestante, TICKS_ENTRE_MORDIDAS);
            combatState(EnemyCombatState.IDLE);
        }
    }

    /**
     * A bocada machuca quem estiver DENTRO dela -- tipicamente quem nadou ate a
     * propria boia.
     *
     * <p>Ela nao persegue ninguem: este mob nao caca jogador. O dano existe para
     * a bocada ser uma bocada, e nao um efeito visual.</p>
     */
    private void aplicarDanoDaMordida() {
        AABB boca = getBoundingBox().inflate(ALCANCE_DA_MORDIDA);
        for (LivingEntity vitima : level().getEntitiesOfClass(LivingEntity.class, boca, this::presaValida)) {
            if (!vitima.hurt(damageSources().mobAttack(this), MORDIDA.damage())) continue;
            vitima.knockback(MORDIDA.knockback(), getX() - vitima.getX(), getZ() - vitima.getZ());
        }
    }

    private boolean presaValida(LivingEntity candidato) {
        if (candidato == this || candidato == null) return false;
        if (candidato.getType() == getType()) return false;
        if (!candidato.isAlive() || !candidato.isAttackable() || isAlliedTo(candidato)) return false;
        return !(candidato instanceof Player jogador && (jogador.isCreative() || jogador.isSpectator()));
    }

    /** A bocada prende o anzol; daqui em diante quem manda e o cabo de guerra. */
    private void tentarFisgar() {
        if (estaFisgado()) return;
        FishingHook anzol = anzolAlvo;
        if (anzol == null || !iscaValida(anzol)) return;
        if (distanceTo(anzol) > getBbWidth() * 0.5D + ALCANCE_DA_MORDIDA) return;
        // UM ANZOL, UM PEIXE. Dois bichos na mesma boia nao dariam erro nenhum:
        // dariam duas tensoes contando a mesma linha, dois arrancos somados no mesmo
        // jogador e um anzol teleportando entre duas bocas. O encontro e raro e o
        // limite de grupo e 1, mas "raro" nao e "impossivel" -- e o dia em que
        // acontecesse ninguem conseguiria reproduzir.
        if (jaFisgadoPorOutro(anzol)) return;

        Player dono = anzol.getPlayerOwner();
        if (dono == null) return;
        anzolFisgado = anzol;
        pescador = dono;
        tensao = 0.0D;
        ticksFisgado = 0;
        ticksDeCicloDeArranco = 0;
        // NaN de proposito: o primeiro tick nao tem medida anterior, e a regra pura
        // devolve a tensao intacta para variacao nao finita. Comecar em zero aqui
        // faria a distancia inteira ate o jogador contar como um puxao unico.
        distanciaAnteriorDoPescador = Double.NaN;
        this.entityData.set(FISGADO, Boolean.TRUE);
        this.entityData.set(CANSADO, Boolean.FALSE);
        combatState(EnemyCombatState.AGGRO);
    }

    /** Algum outro senhor do pantano ja esta com esta linha na boca. */
    private boolean jaFisgadoPorOutro(FishingHook anzol) {
        AABB perto = anzol.getBoundingBox().inflate(FISGADA.raioDaIsca());
        for (MasterOfTheSwampEntity outro
                : level().getEntitiesOfClass(MasterOfTheSwampEntity.class, perto)) {
            if (outro != this && outro.anzolFisgado == anzol) return true;
        }
        return false;
    }

    // ------------------------------------------------------------ cabo de guerra

    /**
     * O tick da linha presa: mede, cobra tensao, cansa ou arrebenta.
     *
     * <p>Quem decide as tres coisas e {@link RegrasDeFisgada}. Este metodo so
     * fornece a VARIACAO de distancia entre o peixe e quem segura a vara -- a
     * mesma medida que o puxao e o golpe alimentam.</p>
     */
    private void tickDaLinha() {
        if (!estaFisgado()) return;
        FishingHook anzol = anzolFisgado;
        Player quem = pescador;
        // RELOAD, logout, /kill, troca de vara: o anzol e o pescador sao referencias
        // de runtime e podem sumir por fora do nosso ciclo. Sem esta checagem o peixe
        // ficaria "fisgado" num fantasma e continuaria puxando um jogador que ja nao
        // esta ali -- sem erro nenhum no log.
        if (anzol == null || quem == null || anzol.isRemoved() || quem.isRemoved()
                || !quem.isAlive() || quem.level() != level() || quem.fishing != anzol) {
            soltarLinha();
            return;
        }

        // O anzol acompanha a boca. E o mesmo que o vanilla faz com HOOKED_IN_ENTITY,
        // e e o que faz a linha desenhada na tela contar a mesma historia que o
        // servidor esta calculando.
        anzol.setDeltaMovement(Vec3.ZERO);
        anzol.setPos(getX(), getY(0.6D), getZ());

        double distancia = distanceTo(quem);
        double variacao = Double.isNaN(distanciaAnteriorDoPescador)
                ? Double.NaN
                : distancia - distanciaAnteriorDoPescador;
        distanciaAnteriorDoPescador = distancia;
        tensao = FISGADA.tensao(tensao, variacao);
        if (FISGADA.arrebenta(tensao)) {
            escapar();
            return;
        }

        ticksFisgado++;
        if (FISGADA.cansou(ticksFisgado)) {
            cansar();
            return;
        }

        resistir(quem);
    }

    /**
     * O arranco: o peixe foge de quem segura a vara, e leva o jogador junto.
     *
     * <p>{@code hurtMarked} e o que faz o empurrao CHEGAR no cliente do
     * pescador. Sem ele o servidor moveria o jogador e o cliente devolveria a
     * posicao antiga no tick seguinte: o cabo de guerra existiria so nos
     * numeros, e ninguem sentiria nada na mao.</p>
     */
    private void resistir(Player quem) {
        getNavigation().stop();
        ticksDeCicloDeArranco = (ticksDeCicloDeArranco + 1) % (TICKS_DE_ARRANCO + TICKS_DE_DESCANSO);
        if (ticksDeCicloDeArranco >= TICKS_DE_ARRANCO) return;

        Vec3 fuga = position().subtract(quem.position());
        if (fuga.lengthSqr() < 1.0E-6D) fuga = new Vec3(1.0D, 0.0D, 0.0D);
        fuga = fuga.normalize();
        setDeltaMovement(getDeltaMovement().add(fuga.scale(FORCA_DO_ARRANCO)));
        hasImpulse = true;
        combatState(EnemyCombatState.ACTIVE);

        if (quem.isCreative() || quem.isSpectator()) return;
        quem.setDeltaMovement(quem.getDeltaMovement().add(fuga.scale(FORCA_DO_PUXAO_NO_PESCADOR)));
        quem.hurtMarked = true;
    }

    /**
     * O peixe esgotou: solta a linha e boia por uma janela contada.
     *
     * <p>SOLTAR AQUI E DELIBERADO. O cabo de guerra acabou, e manter a vara
     * presa a um bicho que ja nao resiste so deixaria o jogador sem poder agir.
     * A recompensa vem de nadar ate ele com a mao vazia dentro da janela --
     * {@link #TICKS_DE_RECUPERACAO} --, e nao de continuar segurando a vara.</p>
     */
    private void cansar() {
        this.entityData.set(CANSADO, Boolean.TRUE);
        recuperacaoRestante = TICKS_DE_RECUPERACAO;
        combatState(EnemyCombatState.STAGGERED);
        soltarLinha();
    }

    /** Exausto, ele nao navega nem nada: boia, e e por isso que da para pegar. */
    private void boiarExausto() {
        getNavigation().stop();
        setDeltaMovement(getDeltaMovement().multiply(0.6D, 1.0D, 0.6D));
    }

    /** A janela fechou e ninguem recolheu: o bicho volta a ser um peixe livre. */
    private void descansar() {
        this.entityData.set(CANSADO, Boolean.FALSE);
        if (combatState() != EnemyCombatState.DYING) combatState(EnemyCombatState.IDLE);
    }

    // --------------------------------------------------------------- captura

    /**
     * A CAPTURA: mao vazia, peixe esgotado, encontro encerrado do jeito bom.
     *
     * <p>E a unica forma "boa" de terminar. Ela e deliberadamente exigente --
     * mao principal VAZIA e peixe CANSADO -- para nao acontecer por acidente no
     * meio do cabo de guerra.</p>
     *
     * <p>A RECUSA TEM MOTIVO VISIVEL, e nao mensagem: enquanto ele nao cansou,
     * o peixe esta se debatendo na tela e puxando a vara na mao de quem tentou.
     * PONTO CEGO DECLARADO: quem tentar pegar um peixe que nao esta cansado nao
     * recebe texto nenhum; a leitura depende da animacao estar tocando. Uma
     * chave de traducao para esta recusa ainda nao existe, e ela e de outra
     * frente.</p>
     */
    @Override
    protected InteractionResult mobInteract(Player jogador, InteractionHand mao) {
        if (mao != InteractionHand.MAIN_HAND || !jogador.getItemInHand(mao).isEmpty()
                || !estaCansado()) {
            return super.mobInteract(jogador, mao);
        }
        if (level().isClientSide) return InteractionResult.SUCCESS;
        recolher(jogador);
        return InteractionResult.CONSUME;
    }

    /**
     * Recolher deixa a recompensa e tira o peixe do mundo.
     *
     * <p>Ele sai por {@code remove}, e nao por {@code die}: capturar nao e
     * matar, e a diferenca importa para quem for pendurar bestiario, conquista
     * ou quest neste ponto. PONTO CEGO DECLARADO: hoje a recompensa e a MESMA
     * tabela de loot que cai de quem mata a pancada -- o que separa os dois
     * caminhos e o trabalho, nao o premio. Amarrar bestiario e conquista a
     * captura e trabalho de outra frente, e esta anotado.</p>
     */
    private void recolher(Player jogador) {
        soltarLinha();
        this.lastHurtByPlayer = jogador;
        this.lastHurtByPlayerTime = 100;
        dropFromLootTable(damageSources().playerAttack(jogador), true);
        // TODO: PLACEHOLDER -- som de vara do VANILLA (FISHING_BOBBER_RETRIEVE) no
        // recolhimento. Sai quando o mob ganhar banco de sons proprio; a secao 10 da
        // diretriz permite o emprestimo durante o desenvolvimento, e hoje ele e
        // obrigatorio: identidade sonora esta bloqueada por FERRAMENTA -- o Minecraft
        // so toca .ogg vorbis e nao ha encoder nesta maquina.
        //
        // A palavra PLACEHOLDER aqui nao e enfeite: PlaceholderDeclaradoTest varre a
        // fonte por ela. Este arquivo ja dizia "SOM EMPRESTADO (ADR-017)" e o portao
        // reprovou mesmo assim -- declaracao que cada um escreve com suas palavras nao
        // e encontravel por grep, e o que nao se encontra nao se paga.
        playSound(SoundEvents.FISHING_BOBBER_RETRIEVE, 1.0F, 0.7F);
        remove(Entity.RemovalReason.KILLED);
    }

    // ----------------------------------------------------------------- fuga

    /**
     * ESCAPE: mergulha e some. Entra aqui a linha arrebentada e o golpe levado.
     *
     * <p>Ele sempre passa por {@link #soltarLinha()}. Um caminho de fuga que
     * limpasse o proprio estado sem passar por ali deixaria o anzol preso a um
     * peixe que ja esta a trinta blocos dali.</p>
     */
    private void escapar() {
        escapeRestante = TICKS_DE_ESCAPE;
        recuperacaoRestante = 0;
        this.entityData.set(ESCAPANDO, Boolean.TRUE);
        this.entityData.set(CANSADO, Boolean.FALSE);
        mordidaEmAndamento = false;
        linhaDoTempoDaMordida = new AttackTimeline();
        publicarFase(AttackPhase.IDLE);
        soltarLinha();
        if (combatState() != EnemyCombatState.DYING) combatState(EnemyCombatState.RETREAT);
    }

    /**
     * O mergulho da fuga e IMPULSO, nao rota.
     *
     * <p>Pedir uma rota ao pathfinder no meio da fuga custaria uma busca por
     * tick e falharia justamente perto da margem, que e onde a fuga acontece. O
     * impulso para baixo e para longe do jogador mais proximo sempre existe, e o
     * peixe volta a navegar assim que a janela fecha.</p>
     */
    private void mergulharParaLonge() {
        getNavigation().stop();
        Player perto = level().getNearestPlayer(this, 32.0D);
        Vec3 fuga = perto == null ? getLookAngle() : position().subtract(perto.position());
        fuga = new Vec3(fuga.x, 0.0D, fuga.z);
        if (fuga.lengthSqr() < 1.0E-6D) fuga = new Vec3(1.0D, 0.0D, 0.0D);
        fuga = fuga.normalize().scale(AFASTAMENTO_DO_ESCAPE);
        setDeltaMovement(getDeltaMovement().add(fuga.x, -MERGULHO_DO_ESCAPE, fuga.z));
        hasImpulse = true;
    }

    /**
     * SAIDA UNICA da linha presa.
     *
     * <p>Arrebentar, cansar, capturar, o pescador morrer ou sair, o peixe
     * morrer, a entidade ser removida e a troca de dimensao terminam TODOS
     * aqui. E aqui que a tensao zera, o anzol e devolvido, a espera e armada e o
     * cliente para de ver o debate-se. Se um caminho deixar de passar por aqui,
     * o jogador continua sendo puxado por um peixe que ja nao esta fisgado --
     * e isso nao aparece como erro, aparece como um relato de bug impossivel de
     * reproduzir.</p>
     *
     * <p>O anzol e DESCARTADO, e nao apenas esquecido. Deixa-lo boiando junto do
     * peixe manteria a vara ocupada e a linha desenhada para um cabo de guerra
     * que ja acabou; descartar devolve a vara ao jogador no mesmo tick, que e o
     * que "a linha arrebentou" quer dizer.</p>
     */
    private void soltarLinha() {
        FishingHook anzol = anzolFisgado;
        anzolFisgado = null;
        pescador = null;
        anzolAlvo = null;
        tensao = 0.0D;
        ticksFisgado = 0;
        ticksDeCicloDeArranco = 0;
        distanciaAnteriorDoPescador = Double.NaN;
        esperaRestante = Math.max(esperaRestante, TICKS_DE_ESPERA_APOS_SOLTAR);
        this.entityData.set(FISGADO, Boolean.FALSE);
        if (anzol != null && !anzol.isRemoved()) anzol.discard();
    }

    // -------------------------------------------------------- ciclo de vida

    /**
     * Levar dano SOBE a tensao e empurra para a fuga.
     *
     * <p>Este mob nao troca golpes. O golpe entra na MESMA formula do puxao --
     * convertido por {@link #AFASTAMENTO_POR_DANO} -- e um acerto de arma de
     * verdade arrebenta a linha sozinho. Quem bater no peixe fisgado perde o
     * peixe, quem bater no peixe cansado o ve acordar e sumir, e quem insistir
     * em mata-lo vai persegui-lo pelo pantano inteiro. Tudo isso e o desenho, e
     * nao um efeito colateral.</p>
     *
     * <p>O que conta e o dano PEDIDO, antes de armadura e resistencia: o preco
     * de perder a linha e o esforco de quem bate, nao a defesa do peixe.</p>
     */
    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean estavaFisgado = estaFisgado();
        boolean aplicou = super.hurt(source, amount);
        if (!aplicou || level().isClientSide || !isAlive()) return aplicou;
        if (estavaFisgado && Float.isFinite(amount) && amount > 0.0F) {
            tensao = FISGADA.tensao(tensao, amount * AFASTAMENTO_POR_DANO);
            if (!FISGADA.arrebenta(tensao)) return aplicou;
        }
        escapar();
        return aplicou;
    }

    @Override
    public void die(DamageSource source) {
        // Quem liga, desliga: morrer com a linha presa nao pode deixar o pescador
        // amarrado a um cadaver.
        if (!level().isClientSide) {
            combatState(EnemyCombatState.DYING);
            soltarLinha();
            this.entityData.set(CANSADO, Boolean.FALSE);
            this.entityData.set(ESCAPANDO, Boolean.FALSE);
            publicarFase(AttackPhase.IDLE);
        }
        super.die(source);
    }

    @Override
    public void remove(Entity.RemovalReason reason) {
        if (!level().isClientSide) soltarLinha();
        super.remove(reason);
    }

    /**
     * Troca de dimensao solta ANTES de viajar.
     *
     * <p>O anzol nao atravessa portal com o peixe: o vanilla ja trata anzol em
     * outra dimensao como anzol perdido. Sem soltar aqui, este lado ficaria com
     * uma referencia a um anzol de outro mundo e continuaria medindo distancia
     * contra ele. Este e mais um ponto de saida, e ele passa pelo mesmo
     * {@link #soltarLinha()}.</p>
     */
    @Override
    public Entity changeDimension(DimensionTransition transition) {
        if (!level().isClientSide) soltarLinha();
        return super.changeDimension(transition);
    }

    private void publicarFase(AttackPhase fase) {
        this.entityData.set(FASE_DE_ATAQUE, fase.ordinal());
    }

    // ------------------------------------------------------------- animacao

    /**
     * O CLIENTE NAO DECIDE NADA. Ele le o que o servidor ja publica --
     * {@link #estaEscapando()}, {@link #estaCansado()}, {@link #estaFisgado()} e
     * {@link #faseDeAtaque()}, os quatro vindos do SynchedEntityData -- e
     * escolhe o clipe correspondente. Nao existe aqui nenhum timer, nenhuma
     * heuristica de "parece que vai morder" e nenhuma copia da regra de fisgada.
     * Se a animacao e o servidor discordarem, quem esta errado e o arquivo de
     * animacao.
     *
     * <p>Ordem de precedencia, da mais especifica para a menos: fuga, exaustao,
     * debate-se, bocada, virada, nado. A fuga vem primeiro porque ela pode
     * comecar no mesmo tick em que a linha arrebenta, e o debate-se nao pode
     * sobreviver a ela.</p>
     *
     * <p>A VIRADA E A UNICA LIDA DE ROTACAO, e isso nao e o cliente decidindo: a
     * rotacao do corpo ja e sincronizada pelo vanilla, e comparar o angulo deste
     * tick com o do anterior e leitura, nao regra. Nenhum estado de jogo depende
     * dela.</p>
     */
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<MasterOfTheSwampEntity>(this, CONTROLLER_DO_CORPO,
                TRANSICAO_EM_TICKS, this::clipeDoCorpo));
    }

    private PlayState clipeDoCorpo(AnimationState<MasterOfTheSwampEntity> estado) {
        if (estaEscapando()) return estado.setAndContinue(ESCAPE);
        if (estaCansado()) return estado.setAndContinue(CAUGHT);
        if (estaFisgado()) return estado.setAndContinue(THRASH);
        AttackPhase fase = faseDeAtaque();
        if (fase == AttackPhase.WINDUP || fase == AttackPhase.ACTIVE || fase == AttackPhase.RECOVERY) {
            return estado.setAndContinue(BITE);
        }
        if (girandoForte()) return estado.setAndContinue(TURN);
        return estado.setAndContinue(SWIM);
    }

    /** Giro do corpo neste tick, em graus; {@code yRotO} e vanilla e vale nos dois lados. */
    private boolean girandoForte() {
        return Math.abs(Mth.wrapDegrees(getYRot() - yRotO)) >= LIMITE_DE_GIRO_EM_GRAUS;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() { return cacheDeAnimacao; }

    @Override
    public double getTick(Object entidade) { return this.tickCount; }

    // -------------------------------------------------------------- sensores

    /** Leitura de mundo do tick: e isto que faz awarenessState() parar de ser decorativo. */
    private AwarenessInput lerSensores() {
        // O "alvo" deste mob e quem esta do outro lado da linha -- nunca uma presa
        // escolhida. Ele nao caca ninguem, e por isso nao tem targetSelector.
        LivingEntity alvo = pescador;
        boolean alvoVivo = alvo != null && alvo.isAlive();
        boolean visivel = alvoVivo && hasLineOfSight(alvo);
        double distancia = alvoVivo ? distanceTo(alvo) : Double.NaN;

        if (visivel) memoriaDeAlvo = MEMORIA_DE_ALVO_TICKS;
        else if (memoriaDeAlvo > 0) memoriaDeAlvo--;

        boolean recuando = visivel && !Double.isNaN(distanciaAnteriorDoAlvo)
                && distancia > distanciaAnteriorDoAlvo;
        distanciaAnteriorDoAlvo = distancia;

        // A isca percebida e o que o vanilla chamaria de "ouvi alguma coisa": o peixe
        // sabe que ha algo ali antes de ver quem jogou.
        boolean audivel = !visivel && (anzolAlvo != null || memoriaDeAlvo > 0);
        // "No territorio" aqui e literal: o jogador esta amarrado a ele pela linha.
        boolean noTerritorio = estaFisgado();
        boolean vidaCritica = getHealth() <= getMaxHealth() * FRACAO_DE_VIDA_CRITICA;
        boolean emboscando = anzolAlvo != null && !estaFisgado();

        return new AwarenessInput(visivel, audivel, noTerritorio, recuando, !alvoVivo,
                memoriaDeAlvo, emboscando, vidaCritica);
    }

    /**
     * A linha, a bocada e a fuga mandam; fora delas quem manda e a consciencia.
     *
     * <p>O RESET DO CEREBRO NAO E ENFEITE. O {@code EnemyBrain} so sai de AMBUSH
     * quando o alvo fica VISIVEL, e este mob nunca "ve" um alvo enquanto so
     * existe uma boia na agua. Sem o reset, um peixe que percebeu uma isca e viu
     * o jogador recolher a vara ficaria emboscando para sempre uma isca que nao
     * existe mais -- sem erro nenhum no log, e com toda ferramenta de
     * diagnostico repetindo a mentira.</p>
     */
    private void alinharEstadoDeCombate(EnemyAwarenessState consciencia) {
        if (consciencia == EnemyAwarenessState.AMBUSH && anzolAlvo == null && !estaFisgado()) {
            enemyBrain().reset();
        }
        if (mordidaEmAndamento || estaFisgado() || estaCansado() || escapeRestante > 0
                || combatState() == EnemyCombatState.DYING) {
            return;
        }
        if (consciencia == EnemyAwarenessState.FLEE) {
            combatState(EnemyCombatState.RETREAT);
            return;
        }
        combatState(anzolAlvo != null ? EnemyCombatState.AGGRO : EnemyCombatState.IDLE);
    }

    /** Fisgado, cansado, mordendo ou fugindo, o corpo nao e livre para passear. */
    private boolean corpoLivre() {
        return !estaFisgado() && !estaCansado() && !mordidaEmAndamento && escapeRestante == 0;
    }

    // ------------------------------------------------------------------ goals

    @Override
    protected void registerGoals() {
        // SEM FloatGoal: ele NAO quer boiar. E sem targetSelector nenhum, de
        // proposito -- este mob nao adota agressor e nao caca jogador. Quem bate
        // nele ve o peixe sumir, nao virar. A regra mora em hurt(), escrita, e nao
        // escondida numa Goal do vanilla que faria o contrario.
        goalSelector.addGoal(0, new CacarIscaGoal(this));
        goalSelector.addGoal(1, new SegurarALinhaGoal(this));
        goalSelector.addGoal(5, new NadoTranquiloGoal(this));
        goalSelector.addGoal(7, new EncararDeLongeGoal(this));
    }

    /**
     * Nada ate a isca e abre a boca ao alcance.
     *
     * <p>E static de proposito: todo estado que ela le e escreve mora na
     * ENTIDADE, e o prefixo {@code peixe.} deixa isso impossivel de esquecer. Um
     * campo aqui seria dividido por todos os peixes do mundo.</p>
     *
     * <p>Ela NAO ticka a bocada: quem faz isso e o {@code customServerAiStep},
     * para que uma Goal de prioridade maior nao congele a fase ao preemptar.</p>
     */
    private static final class CacarIscaGoal extends Goal {
        private final MasterOfTheSwampEntity peixe;

        private CacarIscaGoal(MasterOfTheSwampEntity peixe) {
            this.peixe = peixe;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override public boolean canUse() {
            if (peixe.level().isClientSide) return false;
            if (!peixe.corpoLivre()) return false;
            return peixe.anzolAlvo != null;
        }

        @Override public boolean canContinueToUse() {
            return peixe.mordidaEmAndamento
                    || (peixe.corpoLivre() && peixe.anzolAlvo != null);
        }

        @Override public boolean requiresUpdateEveryTick() { return true; }

        @Override public void stop() { peixe.getNavigation().stop(); }

        @Override public void tick() {
            FishingHook anzol = peixe.anzolAlvo;
            if (anzol == null || peixe.mordidaEmAndamento) return;
            peixe.getLookControl().setLookAt(anzol.getX(), anzol.getY(), anzol.getZ(), 30.0F, 30.0F);
            if (peixe.distanceTo(anzol) <= peixe.getBbWidth() * 0.5D + ALCANCE_DA_MORDIDA) {
                peixe.iniciarMordida();
                return;
            }
            peixe.getNavigation().moveTo(anzol.getX(), anzol.getY(), anzol.getZ(), 1.0D);
        }
    }

    /**
     * Segura MOVE e LOOK enquanto a linha esta presa ou o peixe esta esgotado.
     *
     * <p>Ela nao faz nada: existe para as Goals de passeio nao disputarem o
     * controle no meio do cabo de guerra. Quem move o peixe nesse periodo e o
     * arranco, no tick da entidade.</p>
     */
    private static final class SegurarALinhaGoal extends Goal {
        private final MasterOfTheSwampEntity peixe;

        private SegurarALinhaGoal(MasterOfTheSwampEntity peixe) {
            this.peixe = peixe;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override public boolean canUse() {
            return !peixe.level().isClientSide
                    && (peixe.estaFisgado() || peixe.estaCansado() || peixe.escapeRestante > 0);
        }

        @Override public boolean canContinueToUse() { return canUse(); }

        @Override public boolean requiresUpdateEveryTick() { return true; }

        @Override public void tick() {
            Player quem = peixe.pescador;
            if (quem != null) peixe.getLookControl().setLookAt(quem, 30.0F, 30.0F);
        }
    }

    /** Nado a toa; calado enquanto a isca, a linha ou a fuga mandam. */
    private static final class NadoTranquiloGoal extends RandomSwimmingGoal {
        private final MasterOfTheSwampEntity peixe;

        private NadoTranquiloGoal(MasterOfTheSwampEntity peixe) {
            super(peixe, 1.0D, 20);
            this.peixe = peixe;
        }

        @Override public boolean canUse() { return peixe.corpoLivre() && super.canUse(); }

        @Override public boolean canContinueToUse() {
            return peixe.corpoLivre() && super.canContinueToUse();
        }
    }

    /** Encarar so vale com o corpo livre; fisgado ele olha para quem o fisgou. */
    private static final class EncararDeLongeGoal extends LookAtPlayerGoal {
        private final MasterOfTheSwampEntity peixe;

        private EncararDeLongeGoal(MasterOfTheSwampEntity peixe) {
            super(peixe, Player.class, 10.0F);
            this.peixe = peixe;
        }

        @Override public boolean canUse() { return peixe.corpoLivre() && super.canUse(); }

        @Override public boolean canContinueToUse() {
            return peixe.corpoLivre() && super.canContinueToUse();
        }
    }
}
