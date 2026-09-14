package com.darkcontinent.nenfoundation.enemy.entity;

import com.darkcontinent.nenfoundation.enemy.ai.AwarenessTuning;
import com.darkcontinent.nenfoundation.enemy.ai.EnemyBrain;
import com.darkcontinent.nenfoundation.enemy.api.EnemyAwarenessState;
import com.darkcontinent.nenfoundation.enemy.api.EnemyCombatState;
import com.darkcontinent.nenfoundation.enemy.base.BaseHxHMob;
import com.darkcontinent.nenfoundation.enemy.base.EnemyRuntime;
import com.darkcontinent.nenfoundation.enemy.combat.AttackController;
import com.darkcontinent.nenfoundation.enemy.combat.AttackHit;
import com.darkcontinent.nenfoundation.enemy.combat.AttackHitbox;
import com.darkcontinent.nenfoundation.enemy.combat.AttackPhase;
import com.darkcontinent.nenfoundation.enemy.combat.StaggerResult;
import com.darkcontinent.nenfoundation.enemy.combat.StaggerState;
import com.darkcontinent.nenfoundation.enemy.combat.WeakPointRegistry;
import com.darkcontinent.nenfoundation.enemy.combat.WeakPointResolver;
import com.darkcontinent.nenfoundation.enemy.content.HunterExamProfiles;
import com.darkcontinent.nenfoundation.enemy.faction.FactionRelations;
import com.darkcontinent.nenfoundation.enemy.perception.HearingEvent;
import com.darkcontinent.nenfoundation.enemy.perception.PercepcaoDeAura;
import com.darkcontinent.nenfoundation.enemy.perception.PerceptionBudget;
import com.darkcontinent.nenfoundation.enemy.perception.PerceptionController;
import com.darkcontinent.nenfoundation.enemy.perception.PerceptionSnapshot;
import com.darkcontinent.nenfoundation.enemy.perception.SensorDeVisao;
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
 * Boneco de Treino: a entidade DESCARTAVEL que prova a fundacao inteira (#138).
 *
 * <p><b>Ele nao e conteudo, e a silhueta garante isso.</b> Um saco de estopa num
 * poste com uma cruz de madeira nao pertence ao bestiario de Hunter x Hunter; no
 * dia em que alguem pensar em promove-lo, a aparencia responde antes da
 * discussao. O risco que isso fecha e real: um dummy bonito vira mob de
 * conteudo que ninguem projetou.</p>
 *
 * <p><b>Por que ele existe depois de sete mobs prontos.</b> Os sete nasceram
 * antes da fundacao e cada um trouxe os proprios sensores. As pecas novas --
 * {@link PerceptionController} com orcamento, {@link StaggerState},
 * {@link AttackController} e {@link EnemyRuntime} -- precisam de UMA entidade que
 * as use por inteiro, do spawn a morte, antes de qualquer mob de conteudo
 * depender delas. Provar isso migrando um dos sete misturaria "a fundacao
 * funciona?" com "eu quebrei o great stamp?".</p>
 *
 * <p><b>Ele nao nasce sozinho.</b> O perfil e
 * {@link com.darkcontinent.nenfoundation.enemy.spawn.SpawnProfile#ENCOUNTER_ONLY}:
 * sem isso o boneco entraria na lista de bioma e o mundo ficaria salpicado de
 * ferramenta de teste -- sem erro nenhum, porque cada um seria uma entidade
 * legitima. Ele chega por comando de dev e por arena de teste.</p>
 */
public final class DummyEnemyEntity extends BaseHxHMob implements GeoEntity {

    /** Fase do ataque: o unico estado de combate que o cliente precisa conhecer. */
    private static final EntityDataAccessor<Integer> FASE_DE_ATAQUE =
            SynchedEntityData.defineId(DummyEnemyEntity.class, EntityDataSerializers.INT);

    /**
     * Cambaleando: sincronizado de PROPOSITO, ao contrario de combatState().
     *
     * <p>O great stamp declarou o ponto cego de escolher o clipe de stagger a
     * partir de um campo que so existe no servidor -- no cliente ele le IDLE
     * sempre, e o ramo nunca dispara. Aqui o boneco tem stagger de verdade e a
     * interrupcao PRECISA ser vista, senao o sistema inteiro vira um numero que
     * so o servidor conhece. Um booleano por mob interrompido e barato; a
     * alternativa e um stagger invisivel.</p>
     */
    private static final EntityDataAccessor<Boolean> CAMBALEANDO =
            SynchedEntityData.defineId(DummyEnemyEntity.class, EntityDataSerializers.BOOLEAN);

    // ------------------------------------------------------------- contrato
    /** Ticks de memoria de alvo; e tambem o prazo do {@link ThreatMemory}. */
    private static final int MEMORIA_DE_ALVO_TICKS = 120;
    /** Ticks de aviso antes de o cerebro passar de WARN para ENGAGE. */
    private static final int TICKS_DE_AVISO = 20;
    /** Meia-abertura do cone de visao, em graus. */
    private static final double ABERTURA_DA_VISAO = 75.0D;
    /** Alcance de audicao em blocos, para som de intensidade 1. */
    private static final double ALCANCE_DE_AUDICAO = 14.0D;
    /** Distancia em que o boneco decide golpear. */
    private static final double ALCANCE_DO_GOLPE = 2.6D;
    /** Recarga entre golpes, em ticks. */
    private static final int RECARGA_DO_GOLPE = 30;
    /** Recarga imposta a quem foi interrompido -- interromper nao pode premiar. */
    private static final int RECARGA_APOS_INTERRUPCAO = 40;
    /** Fracao da vida abaixo da qual o cerebro pode decidir fugir. */
    private static final float FRACAO_DE_VIDA_CRITICA = 0.25F;
    /** Alcance do traco que procura o ponto de impacto na caixa do boneco. */
    private static final double ALCANCE_DO_TRACO = 6.0D;

    /**
     * Caixa do golpe, em coordenadas LOCAIS. A FRENTE E +Z.
     *
     * <p>A primeira versao deste campo escrevia z de -2.6 a -0.4, "porque a frente
     * do bicho e -Z". Isso vale para a GEOMETRIA do modelo (no formato Bedrock -Z
     * aponta para a frente), e nao para a transformacao de {@link AttackHitbox},
     * que e matematica de MUNDO: com yaw 0 o olhar vanilla aponta para +Z, e a
     * conta de {@code noMundo} segue essa convencao. Misturar as duas colocava a
     * caixa ATRAS do boneco -- ele atacava, animava, e nao encostava em quem
     * estava na frente; quem estivesse pelas costas e que apanhava.</p>
     *
     * <p>Nao havia erro nenhum: fase certa, cooldown certo, log limpo. Quem achou
     * foi o gametest, medindo dano num golem parado na frente. O great stamp usa a
     * mesma convencao ({@code z} de -0.5 a 2.1), e a coerencia entre os dois e o
     * que impede a proxima pessoa de inverter de novo.</p>
     */
    private static final AttackHitbox CAIXA_DO_GOLPE =
            new AttackHitbox(-1.0D, 0.4D, 0.4D, 1.0D, 1.8D, 2.6D);

    private static final WeakPointRegistry PONTOS_FRACOS = HunterExamProfiles.dummyEnemyWeakPoints();
    private static final WeakPointResolver GEOMETRIA_DO_PONTO_FRACO =
            HunterExamProfiles.dummyEnemyWeakPoint();

    // ------------------------------------------------------------- animacao
    // Os nomes abaixo sao um CONTRATO com dummy_enemy.animation.json. Errar um
    // deles nao da erro: o GeckoLib nao acha o clipe e deixa o osso parado.
    //
    // LoopType.DEFAULT em TODOS, e isso nao e descuido: no GeckoLib 4.8.3 o tipo
    // de repeticao declarado aqui VENCE o do arquivo. Cravar o tipo em Java
    // transformaria o dicionario LOOPS do gerador em documentacao que discorda do
    // comportamento -- e quem fosse corrigir a repeticao da morte mexeria no
    // .json e nada mudaria em jogo. DEFAULT e o unico valor que devolve a decisao
    // ao arquivo, e o portao CoerenciaDeGeckoLibTest cobra isso varrendo a FONTE.
    // Por isso nem os atalhos proibidos aparecem escritos neste comentario: o
    // portao le texto, e citar o que ele procura o faz reprovar a explicacao.
    private static final RawAnimation IDLE = RawAnimation.begin().then("animation.dummy_enemy.idle", Animation.LoopType.DEFAULT);
    private static final RawAnimation WALK = RawAnimation.begin().then("animation.dummy_enemy.walk", Animation.LoopType.DEFAULT);
    private static final RawAnimation WINDUP = RawAnimation.begin().then("animation.dummy_enemy.windup", Animation.LoopType.DEFAULT);
    private static final RawAnimation STRIKE = RawAnimation.begin().then("animation.dummy_enemy.strike", Animation.LoopType.DEFAULT);
    private static final RawAnimation RECOVERY = RawAnimation.begin().then("animation.dummy_enemy.recovery", Animation.LoopType.DEFAULT);
    private static final RawAnimation STAGGER = RawAnimation.begin().then("animation.dummy_enemy.stagger", Animation.LoopType.DEFAULT);
    private static final RawAnimation DEATH = RawAnimation.begin().then("animation.dummy_enemy.death", Animation.LoopType.DEFAULT);

    private static final String CONTROLLER_DO_CORPO = "corpo";
    private static final int TRANSICAO_EM_TICKS = 4;
    /** Deslocamento por tick acima do qual o clipe passa de idle para walk. */
    private static final double LIMIAR_DE_CAMINHADA = 0.02D;

    /** Cache por INSTANCIA; um cache estatico faria toda a arena compartilhar um clipe. */
    private final AnimatableInstanceCache cacheDeAnimacao = GeckoLibUtil.createInstanceCache(this);

    public DummyEnemyEntity(EntityType<? extends DummyEnemyEntity> type, Level level) {
        super(type, level, HunterExamProfiles.dummyEnemy().metadata(),
                new AwarenessTuning(TICKS_DE_AVISO, MEMORIA_DE_ALVO_TICKS));
        instalarRuntime(montarRuntime());
    }

    /**
     * Monta as quatro pecas do inimigo.
     *
     * <p>A desfasagem sai do hashCode do proprio uuid: sem ela, uma arena com
     * vinte bonecos varreria o mundo TODA no mesmo tick, e o custo viraria uma
     * travada periodica em vez de um gasto espalhado. Travada periodica e pior de
     * diagnosticar, porque se parece com rede.</p>
     */
    private EnemyRuntime montarRuntime() {
        var perfil = HunterExamProfiles.dummyEnemy();
        PerceptionController percepcao = new PerceptionController(
                PerceptionBudget.padrao(),
                VisionCone.deGraus(perfil.attributes().followRange(), ABERTURA_DA_VISAO),
                new ThreatMemory(MEMORIA_DE_ALVO_TICKS),
                new TargetEvaluator(FactionRelations.padrao(), perfil.metadata().faction(),
                        perfil.attributes().followRange(), perfil.metadata().territorial()),
                PercepcaoDeAura.NENHUMA,
                ALCANCE_DE_AUDICAO,
                getUUID().hashCode());
        return new EnemyRuntime(percepcao, new EnemyBrain(new AwarenessTuning(TICKS_DE_AVISO,
                MEMORIA_DE_ALVO_TICKS)), new AttackController(RECARGA_DO_GOLPE),
                new StaggerState(HunterExamProfiles.dummyEnemyStagger()));
    }

    public static AttributeSupplier.Builder createAttributes() {
        var attrs = HunterExamProfiles.dummyEnemy().attributes();
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, attrs.maxHealth())
                .add(Attributes.MOVEMENT_SPEED, attrs.movementSpeed())
                .add(Attributes.ATTACK_DAMAGE, attrs.attackDamage())
                .add(Attributes.ARMOR, attrs.armor())
                .add(Attributes.FOLLOW_RANGE, attrs.followRange())
                .add(Attributes.KNOCKBACK_RESISTANCE, attrs.knockbackResistance());
    }

    public static EntityType<DummyEnemyEntity> registeredType() { return EnemyEntityTypes.DUMMY_ENEMY.get(); }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(FASE_DE_ATAQUE, AttackPhase.IDLE.ordinal());
        builder.define(CAMBALEANDO, false);
    }

    /** Fase corrente do golpe; vale nos dois lados, porque vem do SynchedEntityData. */
    public AttackPhase faseDeAtaque() {
        int ordinal = this.entityData.get(FASE_DE_ATAQUE);
        AttackPhase[] fases = AttackPhase.values();
        return ordinal >= 0 && ordinal < fases.length ? fases[ordinal] : AttackPhase.IDLE;
    }

    public boolean cambaleando() { return this.entityData.get(CAMBALEANDO); }

    @Override
    protected void registerGoals() {
        // GoalSelector cuida de LOCOMOCAO. A intencao -- quem e o alvo, quando o
        // golpe sai -- e do runtime. Misturar as duas coisas foi o que a issue
        // #109 proibiu: regra de combate dentro de tick() de entidade.
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new GolpeGoal(this));
        goalSelector.addGoal(2, new PerseguirGoal(this));
        goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.8D));
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 10.0F));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        // NENHUM target goal vanilla: quem escolhe alvo aqui e o TargetEvaluator,
        // por FACCAO. Um NearestAttackableTargetGoal ao lado seria uma segunda
        // autoridade sobre a mesma decisao, e as duas discordariam em silencio.
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (level().isClientSide) return;

        EnemyRuntime runtime = runtimeExigido();
        PerceptionSnapshot snapshot = runtime.tick(tickCount, this::varrerCandidatos,
                getHealth() <= getMaxHealth() * FRACAO_DE_VIDA_CRITICA, false);

        aplicarAlvo(snapshot);
        tickDoGolpe(runtime);
        publicarEstado(runtime);
    }

    /**
     * Traduz o alvo LEMBRADO em {@code setTarget}.
     *
     * <p>O uuid e resolvido para entidade AQUI, e a cada uso -- nunca guardado.
     * Guardar a entidade seria segurar uma referencia viva num campo de mob: nao
     * da erro, so impede o objeto de morrer. Reconstruir do servidor devolve
     * vazio quando o alvo deixou de existir, que e a resposta certa.</p>
     */
    private void aplicarAlvo(PerceptionSnapshot snapshot) {
        Optional<UUID> lembrado = snapshot.alvoOpcional();
        if (lembrado.isEmpty()) {
            if (getTarget() != null) setTarget(null);
            return;
        }
        Entity encontrado = ((ServerLevel) level()).getEntity(lembrado.get());
        if (encontrado instanceof LivingEntity vivo && vivo.isAlive()) {
            if (getTarget() != vivo) setTarget(vivo);
        } else if (getTarget() != null) {
            setTarget(null);
        }
    }

    /**
     * Varredura de mundo -- a UNICA, e so quando o orcamento autoriza.
     *
     * <p>O {@link PerceptionController} chama isto a cada seis ticks. Chamar todo
     * tick funcionaria perfeitamente com um boneco e derrubaria o TPS com vinte,
     * sem nenhum erro no log.</p>
     */
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

    /** 1 quando o alvo esta bem na frente, -1 quando esta atras. */
    private double cossenoDoOlharAte(Entity alvo) {
        Vec3 olhar = getLookAngle();
        Vec3 horizontal = new Vec3(olhar.x, 0.0D, olhar.z);
        Vec3 ateOAlvo = new Vec3(alvo.getX() - getX(), 0.0D, alvo.getZ() - getZ());
        if (horizontal.lengthSqr() < 1.0E-6D || ateOAlvo.lengthSqr() < 1.0E-6D) return 0.0D;
        return Mth.clamp(horizontal.normalize().dot(ateOAlvo.normalize()), -1.0D, 1.0D);
    }

    /** A janela ACTIVE consulta a caixa do golpe; fora dela o golpe nao existe. */
    private void tickDoGolpe(EnemyRuntime runtime) {
        if (runtime.ataques().phase() != AttackPhase.ACTIVE) return;
        LivingEntity alvo = getTarget();
        if (alvo == null || !alvo.isAlive()) return;

        Optional<AttackHit> acerto = runtime.ataques().tryHit(alvo.getId(), alvo.getBoundingBox(),
                CAIXA_DO_GOLPE, position(), getYRot(), "body");
        acerto.ifPresent(hit -> {
            alvo.hurt(damageSources().mobAttack(this), hit.damage());
            alvo.knockback(0.4D, getX() - alvo.getX(), getZ() - alvo.getZ());
        });
    }

    /** Publica ao cliente SO o que ele precisa para escolher o clipe. */
    private void publicarEstado(EnemyRuntime runtime) {
        int fase = runtime.ataques().phase().ordinal();
        if (this.entityData.get(FASE_DE_ATAQUE) != fase) this.entityData.set(FASE_DE_ATAQUE, fase);

        boolean cambaleando = runtime.stagger().cambaleando();
        if (this.entityData.get(CAMBALEANDO) != cambaleando) {
            this.entityData.set(CAMBALEANDO, cambaleando);
        }

        combatState(estadoDeCombate(runtime, cambaleando));
    }

    private EnemyCombatState estadoDeCombate(EnemyRuntime runtime, boolean cambaleando) {
        if (combatState() == EnemyCombatState.DYING) return EnemyCombatState.DYING;
        if (cambaleando) return EnemyCombatState.STAGGERED;
        if (runtime.consciencia() == EnemyAwarenessState.FLEE) return EnemyCombatState.RETREAT;
        return switch (runtime.ataques().phase()) {
            case WINDUP -> EnemyCombatState.WINDUP;
            case ACTIVE -> EnemyCombatState.ACTIVE;
            case RECOVERY -> EnemyCombatState.RECOVERY;
            default -> getTarget() != null ? EnemyCombatState.AGGRO : EnemyCombatState.IDLE;
        };
    }

    /**
     * UNICO lugar que aplica o multiplicador do alvo, e o UNICO que alimenta o stagger.
     *
     * <p>Dano multiplicado em dois handlers e o bug silencioso que este projeto ja
     * sabe que comete: o numero final fica plausivel demais para alguem notar sem
     * medir. A regiao e resolvida com a geometria QUE O SERVIDOR TEM; o cliente
     * nao participa. Dano sem atacante (fogo, queda) nao tem angulo: cai na
     * regiao padrao e passa sem multiplicador, e tambem sem stagger -- cambalear
     * por queimadura transformaria fogo em interrupcao permanente.</p>
     */
    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (level().isClientSide || !(source.getEntity() instanceof LivingEntity atacante)
                || !Float.isFinite(amount) || amount <= 0.0F) {
            return super.hurt(source, amount);
        }
        Vec3 impacto = pontoDeImpacto(source, atacante);
        double alturaRelativa = Mth.clamp((impacto.y - getY()) / getBbHeight(), 0.0D, 1.0D);
        String regiao = GEOMETRIA_DO_PONTO_FRACO.resolver(alturaRelativa, cossenoDoOlharAte(atacante));
        float danoFinal = PONTOS_FRACOS.damage(amount, regiao);

        boolean levou = super.hurt(source, danoFinal);
        if (!levou) return false;

        // O stagger le o dano REAL ja multiplicado: acertar o alvo interrompe mais
        // depressa, que e a resposta que o boneco ensina. Ler o dano bruto faria o
        // ponto fraco valer so para a barra de vida, e a leitura visual mentiria.
        EnemyRuntime runtime = runtimeExigido();
        StaggerResult resultado = runtime.sofrerStagger(runtime.ataques().attackInstanceId(),
                danoFinal, RECARGA_APOS_INTERRUPCAO);
        if (resultado == StaggerResult.DISPAROU) {
            getNavigation().stop();
            this.entityData.set(CAMBALEANDO, true);
            this.entityData.set(FASE_DE_ATAQUE, AttackPhase.IDLE.ordinal());
        }
        return true;
    }

    /**
     * Ponto de impacto medido NO SERVIDOR: traco do olho do atacante contra a caixa.
     *
     * <p>Sem intersecao, cai para a posicao do dano (ou o olho do atacante) --
     * nunca para algo que o cliente afirmou.</p>
     */
    private Vec3 pontoDeImpacto(DamageSource source, LivingEntity atacante) {
        Vec3 olho = atacante.getEyePosition();
        Vec3 fim = olho.add(atacante.getLookAngle().scale(ALCANCE_DO_TRACO));
        Vec3 reserva = source.getSourcePosition() != null ? source.getSourcePosition() : olho;
        return getBoundingBox().clip(olho, fim).orElse(reserva);
    }

    // ------------------------------------------------------------- animacao

    /**
     * O CLIENTE NAO DECIDE NADA: ele le a fase e o cambaleio que o servidor publica.
     *
     * <p>Ordem de precedencia, da mais especifica para a menos: morte, stagger,
     * fases do golpe, locomocao, ocio. Morte vem primeiro porque um boneco que
     * morre no meio do golpe nao pode terminar o clipe de ataque -- e stagger vem
     * antes do golpe porque interromper e, por definicao, o que ganha do golpe.</p>
     */
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<DummyEnemyEntity>(this, CONTROLLER_DO_CORPO,
                TRANSICAO_EM_TICKS, this::clipeDoCorpo));
    }

    private PlayState clipeDoCorpo(AnimationState<DummyEnemyEntity> estado) {
        if (this.deathTime > 0) return estado.setAndContinue(DEATH);
        if (cambaleando()) return estado.setAndContinue(STAGGER);
        switch (faseDeAtaque()) {
            case WINDUP -> { return estado.setAndContinue(WINDUP); }
            case ACTIVE -> { return estado.setAndContinue(STRIKE); }
            case RECOVERY -> { return estado.setAndContinue(RECOVERY); }
            default -> { }
        }
        return estado.setAndContinue(velocidadeHorizontal() >= LIMIAR_DE_CAMINHADA ? WALK : IDLE);
    }

    /**
     * Blocos andados no ultimo tick, medidos por POSICAO.
     *
     * <p>Nao usa {@code getDeltaMovement()}: no cliente o delta de uma entidade
     * remota so e escrito quando chega um pacote de velocidade, entao ele fica
     * zerado na maior parte dos ticks e o boneco andaria sempre no clipe de ocio.
     * {@code xo}/{@code zo} sao atualizados todo tick nos DOIS lados.</p>
     */
    private double velocidadeHorizontal() {
        double dx = getX() - this.xo;
        double dz = getZ() - this.zo;
        return Math.sqrt(dx * dx + dz * dz);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() { return cacheDeAnimacao; }

    @Override
    public double getTick(Object entidade) { return this.tickCount; }

    /** Som de mundo chega ao boneco como EVENTO -- e assim que a audicao existe. */
    public void ouvir(HearingEvent evento) {
        if (!level().isClientSide) runtimeExigido().percepcao().ouvir(evento);
    }

    // ------------------------------------------------------------- ciclo de vida

    /**
     * Morrer PUBLICA o repouso, e nao apenas limpa o servidor.
     *
     * <p>{@code BaseHxHMob.die} limpa o runtime, mas o campo sincronizado nao
     * sabe disso: depois de morto {@code isImmobile()} e verdadeiro,
     * {@code customServerAiStep} nao roda mais e {@link #publicarEstado} nunca
     * mais acontece. Morrer no meio de um golpe deixava a fase congelada em
     * WINDUP ou ACTIVE no CLIENTE, e o boneco morria com os bracos erguidos.</p>
     *
     * <p>A morte por golpe escapava disso por acidente -- o ramo de stagger em
     * {@link #hurt} grava IDLE de passagem. Morte SEM atacante (fogo, queda,
     * {@code /kill}) nao passa por ali, e era exatamente o caso que ninguem
     * testaria a mao. Quem achou foi o gametest.</p>
     */
    @Override
    public void die(DamageSource source) {
        if (!level().isClientSide) publicarRepouso();
        super.die(source);
    }

    /** Remocao -- unload, dimensao, comando -- passa pelo MESMO ponto. */
    @Override
    public void remove(Entity.RemovalReason reason) {
        if (!level().isClientSide) publicarRepouso();
        super.remove(reason);
    }

    private void publicarRepouso() {
        this.entityData.set(FASE_DE_ATAQUE, AttackPhase.IDLE.ordinal());
        this.entityData.set(CAMBALEANDO, false);
    }

    // ------------------------------------------------------------------ goals

    /**
     * Anda ate o alvo enquanto ele estiver longe demais para o golpe.
     *
     * <p>{@code static} de proposito: todo estado que ela le mora na ENTIDADE. Um
     * campo aqui seria compartilhado por todos os bonecos da arena, e o sintoma
     * seria um deles perseguindo o alvo de outro.</p>
     */
    private static final class PerseguirGoal extends Goal {
        private final DummyEnemyEntity boneco;

        private PerseguirGoal(DummyEnemyEntity boneco) {
            this.boneco = boneco;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override public boolean canUse() {
            LivingEntity alvo = boneco.getTarget();
            return alvo != null && alvo.isAlive()
                    && !boneco.runtimeExigido().stagger().cambaleando()
                    && boneco.runtimeExigido().ataques().phase() == AttackPhase.IDLE
                    && boneco.distanceTo(alvo) > ALCANCE_DO_GOLPE;
        }

        @Override public boolean canContinueToUse() { return canUse(); }

        @Override public boolean requiresUpdateEveryTick() { return true; }

        @Override public void tick() {
            LivingEntity alvo = boneco.getTarget();
            if (alvo == null) return;
            boneco.getLookControl().setLookAt(alvo, 30.0F, 30.0F);
            boneco.getNavigation().moveTo(alvo, 1.0D);
        }

        @Override public void stop() { boneco.getNavigation().stop(); }
    }

    /** Dispara e conduz o golpe; quem mede a janela e o {@link AttackController}. */
    private static final class GolpeGoal extends Goal {
        private final DummyEnemyEntity boneco;

        private GolpeGoal(DummyEnemyEntity boneco) {
            this.boneco = boneco;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override public boolean canUse() {
            if (boneco.level().isClientSide) return false;
            EnemyRuntime runtime = boneco.runtimeExigido();
            if (runtime.stagger().cambaleando()) return false;
            if (runtime.consciencia() == EnemyAwarenessState.FLEE) return false;
            LivingEntity alvo = boneco.getTarget();
            if (alvo == null || !alvo.isAlive()) return false;
            return boneco.distanceTo(alvo) <= ALCANCE_DO_GOLPE
                    && boneco.hasLineOfSight(alvo)
                    && runtime.ataques().canStart();
        }

        @Override public boolean canContinueToUse() {
            EnemyRuntime runtime = boneco.runtimeExigido();
            return runtime.ataques().phase() != AttackPhase.IDLE
                    && runtime.ataques().phase() != AttackPhase.COMPLETE
                    && !runtime.stagger().cambaleando();
        }

        @Override public boolean requiresUpdateEveryTick() { return true; }

        @Override public void start() {
            boneco.getNavigation().stop();
            boneco.runtimeExigido().ataques().start(HunterExamProfiles.dummyEnemyStrike());
        }

        @Override public void tick() {
            LivingEntity alvo = boneco.getTarget();
            // Encarar o alvo so ate o fim do WINDUP: a partir da janela ACTIVE a
            // direcao esta TRAVADA. Sem travar, o golpe viraria mira-laser -- o
            // boneco giraria junto com quem desvia e o desvio deixaria de existir.
            if (alvo != null && boneco.runtimeExigido().ataques().phase() == AttackPhase.WINDUP) {
                boneco.getLookControl().setLookAt(alvo, 30.0F, 30.0F);
            }
            boneco.setDeltaMovement(boneco.getDeltaMovement().multiply(0.2D, 1.0D, 0.2D));
        }

        @Override public void stop() { boneco.getNavigation().stop(); }
    }
}
