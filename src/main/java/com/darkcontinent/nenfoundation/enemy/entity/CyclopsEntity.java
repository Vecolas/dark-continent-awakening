package com.darkcontinent.nenfoundation.enemy.entity;

import com.darkcontinent.nenfoundation.enemy.ai.AwarenessTuning;
import com.darkcontinent.nenfoundation.enemy.ai.EnemyBrain;
import com.darkcontinent.nenfoundation.enemy.ai.SingleEyeRules;
import com.darkcontinent.nenfoundation.enemy.api.EnemyAwarenessState;
import com.darkcontinent.nenfoundation.enemy.api.EnemyCombatState;
import com.darkcontinent.nenfoundation.enemy.api.EnemyFaction;
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
import com.darkcontinent.nenfoundation.enemy.content.CyclopsTuning;
import com.darkcontinent.nenfoundation.enemy.content.GreedIslandProfiles;
import com.darkcontinent.nenfoundation.enemy.faction.FactionRelations;
import com.darkcontinent.nenfoundation.enemy.perception.HearingEvent;
import com.darkcontinent.nenfoundation.enemy.perception.PercepcaoDeAura;
import com.darkcontinent.nenfoundation.enemy.perception.PerceptionBudget;
import com.darkcontinent.nenfoundation.enemy.perception.PerceptionController;
import com.darkcontinent.nenfoundation.enemy.perception.PerceptionSnapshot;
import com.darkcontinent.nenfoundation.enemy.perception.TargetCandidate;
import com.darkcontinent.nenfoundation.enemy.perception.TargetEvaluator;
import com.darkcontinent.nenfoundation.enemy.perception.ThreatMemory;
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
 * Cyclops -- o gigante de UM olho so, de Greed Island.
 *
 * <p><b>O que ele faz, numa frase:</b> ele enxerga METADE do que um mob comum
 * enxerga, e o unico lugar que paga critico e exatamente o lugar de onde ele te
 * ve chegar.</p>
 *
 * <p><b>O ponto cego tem consequencia, e ela e observavel.</b> O cone e de 60
 * graus de meia-abertura -- o boneco de treino usa 75, e a fauna fica perto
 * disso. Quem entra pelo flanco NAO e percebido: o {@link PerceptionController}
 * filtra o candidato pelo cone DEPOIS de o {@link TargetEvaluator} ja te-lo
 * escolhido, entao nem "este aqui me feriu" atravessa a cegueira. Ferir o
 * gigante pelas costas nao o faz virar; ele continua vagando, e so volta a achar
 * o agressor quando a propria perambulacao o girar. Alem disso ele RECUSA
 * comecar o golpe contra um alvo fora do campo de visao, mesmo enquanto a
 * memoria ainda o lembra. Circular por fora funciona; correr de frente nao.</p>
 *
 * <p><b>O golpe e uma tora, e ele e lento.</b> Trinta ticks de aviso, cinco de
 * janela, vinte e cinco de recuperacao. Ele mira apenas nos doze primeiros ticks
 * do aviso e depois TRAVA a direcao -- sem isso o porrete viraria mira-laser e o
 * telegrafo de um segundo e meio nao serviria para nada. A caixa de dano e larga
 * (quatro blocos de arco) porque tora varre em vez de acertar um ponto, e ela
 * nao chega mais longe do que o porrete DESENHADO, que e o que impede o jogador
 * de apanhar de uma arma que, na tela, parou antes dele.</p>
 *
 * <p><b>O olho e o sensor E o ponto fraco, e isso e uma coisa so.</b> As duas
 * aberturas vivem juntas em {@link SingleEyeRules}, que recusa um arco de olho
 * mais largo que o arco de visao -- pagar critico a quem ele nao enxerga seria
 * dano de graca sem risco, e apagaria o encontro sem nenhum erro no log. A
 * regiao atingida e resolvida com a geometria QUE O SERVIDOR TEM: nenhum pacote
 * de cliente diz "acertei o olho".</p>
 *
 * <p>Ela usa {@link EnemyRuntime} por inteiro -- percepcao com orcamento,
 * cerebro, ataque e stagger -- e publica ao cliente so a fase e o cambaleio, que
 * e do que o {@link AnimationController} precisa para escolher o clipe.</p>
 */
public final class CyclopsEntity extends BaseHxHMob implements GeoEntity {

    /** Fase do ataque: o unico estado de combate que o cliente precisa conhecer. */
    private static final EntityDataAccessor<Integer> FASE_DE_ATAQUE =
            SynchedEntityData.defineId(CyclopsEntity.class, EntityDataSerializers.INT);
    /** Cambaleando: sincronizado porque interrupcao invisivel e stagger que so o servidor conhece. */
    private static final EntityDataAccessor<Boolean> CAMBALEANDO =
            SynchedEntityData.defineId(CyclopsEntity.class, EntityDataSerializers.BOOLEAN);

    /**
     * As pecas imutaveis da ficha, resolvidas uma vez.
     *
     * <p>Estaticas porque NENHUMA delas guarda estado de jogador -- sao records e
     * catalogos imutaveis. Um campo estatico com estado seria o erro classico
     * deste projeto: dois gigantes decidindo com a mesma memoria.</p>
     */
    private static final SingleEyeRules OLHO = CyclopsTuning.olhoUnico();
    private static final WeakPointRegistry PONTOS_FRACOS = CyclopsTuning.pontosFracos();
    private static final WeakPointResolver GEOMETRIA_DO_OLHO = CyclopsTuning.olho();
    private static final AttackHitbox CAIXA_DO_PORRETE = CyclopsTuning.caixaDoPorrete();

    /** Cache por INSTANCIA; um cache estatico faria todos os ciclopes dividirem um clipe. */
    private final AnimatableInstanceCache cacheDeAnimacao = GeckoLibUtil.createInstanceCache(this);

    public CyclopsEntity(EntityType<? extends CyclopsEntity> type, Level level) {
        super(type, level, GreedIslandProfiles.cyclops().metadata(),
                new AwarenessTuning(CyclopsTuning.TICKS_DE_AVISO,
                        CyclopsTuning.MEMORIA_DE_ALVO_TICKS));
        instalarRuntime(montarRuntime());
    }

    /**
     * Monta as quatro pecas do inimigo.
     *
     * <p>O cone sai de {@link CyclopsTuning#coneDeVisao(double)}, e nao de um
     * {@code deGraus(..., 60)} escrito aqui: com dois lugares declarando a mesma
     * abertura, estreitar o olho sem mexer neste construtor daria um gigante que
     * enxerga mais longe do que o proprio olho enxerga -- sem erro, e com a licao
     * do encontro invertida.</p>
     *
     * <p>A desfasagem sai do hashCode do uuid, para que um grupo nao varra o mundo
     * todo no mesmo tick. Travada periodica e pior de diagnosticar do que
     * lentidao constante, porque se parece com rede.</p>
     */
    private EnemyRuntime montarRuntime() {
        var perfil = GreedIslandProfiles.cyclops();
        PerceptionController percepcao = new PerceptionController(
                PerceptionBudget.padrao(),
                CyclopsTuning.coneDeVisao(perfil.attributes().followRange()),
                new ThreatMemory(CyclopsTuning.MEMORIA_DE_ALVO_TICKS),
                new TargetEvaluator(FactionRelations.padrao(), perfil.metadata().faction(),
                        perfil.attributes().followRange(), perfil.metadata().territorial()),
                PercepcaoDeAura.NENHUMA, CyclopsTuning.ALCANCE_DE_AUDICAO, getUUID().hashCode());
        return new EnemyRuntime(percepcao,
                new EnemyBrain(new AwarenessTuning(CyclopsTuning.TICKS_DE_AVISO,
                        CyclopsTuning.MEMORIA_DE_ALVO_TICKS)),
                new AttackController(GreedIslandProfiles.cyclopsRecarga()),
                new StaggerState(GreedIslandProfiles.cyclopsStagger()));
    }

    public static AttributeSupplier.Builder createAttributes() {
        var attrs = GreedIslandProfiles.cyclops().attributes();
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, attrs.maxHealth())
                .add(Attributes.MOVEMENT_SPEED, attrs.movementSpeed())
                .add(Attributes.ATTACK_DAMAGE, attrs.attackDamage())
                .add(Attributes.ARMOR, attrs.armor())
                .add(Attributes.FOLLOW_RANGE, attrs.followRange())
                .add(Attributes.KNOCKBACK_RESISTANCE, attrs.knockbackResistance());
    }

    public static EntityType<CyclopsEntity> registeredType() { return EnemyEntityTypes.CYCLOPS.get(); }

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
        // golpe sai -- e do runtime.
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new PorreteGoal(this));
        goalSelector.addGoal(2, new PerseguirGoal(this));
        goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.8D));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        // NENHUM LookAtPlayerGoal, e a ausencia e deliberada. Ele encara o jogador
        // MAIS PROXIMO sem consultar cone nenhum: a cabeca do gigante passaria a
        // acompanhar quem esta pelas costas, e a tela mostraria que ele te viu
        // enquanto a regra diz que nao. Ponto cego que o proprio modelo desmente
        // e pior do que nao ter ponto cego, porque ensina o jogador a nao
        // acreditar no que ve -- e nada nisso da erro.
        //
        // NENHUM target goal vanilla, pelo motivo de sempre: quem escolhe alvo
        // aqui e o TargetEvaluator, por faccao. Um NearestAttackableTargetGoal ao
        // lado seria uma segunda autoridade sobre a mesma decisao, e as duas
        // discordariam em silencio.
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (level().isClientSide) return;
        EnemyRuntime runtime = runtimeExigido();
        PerceptionSnapshot snapshot = runtime.tick(tickCount, this::varrerCandidatos,
                getHealth() <= getMaxHealth() * CyclopsTuning.FRACAO_DE_VIDA_CRITICA, false);
        aplicarAlvo(snapshot);
        tickDoGolpe(runtime);
        publicarEstado(runtime);
    }

    /**
     * Traduz o alvo LEMBRADO em {@code setTarget}.
     *
     * <p>O uuid e resolvido para entidade a cada uso, nunca guardado: um campo com
     * a entidade seria uma referencia viva presa num mob, e isso nao da erro --
     * so impede o objeto de morrer.</p>
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
     * <p>Ela entrega candidatos MEDIDOS e nao filtra por cone: quem aplica o cone
     * e o {@link PerceptionController}, depois da avaliacao por faccao. A ordem
     * importa, e e ela que faz o ponto cego valer tambem para quem acabou de
     * ferir o gigante -- filtrar aqui deixaria o candidato agressor passar por
     * cima da cegueira sem que nada acusasse.</p>
     */
    private List<TargetCandidate> varrerCandidatos() {
        double alcance = getAttributeValue(Attributes.FOLLOW_RANGE);
        AABB area = getBoundingBox().inflate(alcance);
        List<TargetCandidate> candidatos = new ArrayList<>();
        for (Player jogador : level().getEntitiesOfClass(Player.class, area,
                p -> p.isAlive() && !p.isSpectator() && !p.isCreative())) {
            candidatos.add(new TargetCandidate(jogador.getUUID(), EnemyFaction.HUNTER_ASSOCIATION,
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

    /** A janela ACTIVE consulta a caixa do porrete; fora dela o golpe nao existe. */
    private void tickDoGolpe(EnemyRuntime runtime) {
        if (runtime.ataques().phase() != AttackPhase.ACTIVE) return;
        LivingEntity alvo = getTarget();
        if (alvo == null || !alvo.isAlive()) return;

        // A regiao passada aqui e a COMUM, e nao a do olho: este e o golpe que ele
        // DA, e ponto fraco e coisa do golpe que ele RECEBE. Passar a regiao do
        // olho nao daria erro nenhum -- daria o multiplicador aplicado no lado
        // errado do combate, e o dano final ficaria plausivel demais para alguem
        // notar sem medir.
        Optional<AttackHit> acerto = runtime.ataques().tryHit(alvo.getId(), alvo.getBoundingBox(),
                CAIXA_DO_PORRETE, position(), getYRot(), CyclopsTuning.REGIAO_COMUM);
        acerto.ifPresent(hit -> {
            alvo.hurt(damageSources().mobAttack(this), hit.damage());
            alvo.knockback(CyclopsTuning.EMPURRAO_DO_PORRETE,
                    getX() - alvo.getX(), getZ() - alvo.getZ());
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
     * UNICO lugar que aplica o multiplicador do olho, e o UNICO que alimenta o stagger.
     *
     * <p>Dano multiplicado em dois handlers e o bug silencioso que este projeto ja
     * sabe que comete: o numero final fica plausivel demais para alguem notar sem
     * medir. A regiao e resolvida com a geometria QUE O SERVIDOR TEM -- altura do
     * impacto e angulo do atacante -- e o cliente nao participa de nada disso. Se
     * a regiao viesse de um pacote, todo golpe viraria critico, e o sintoma seria
     * um chefe que morre rapido demais sem uma linha de log.</p>
     *
     * <p>Dano sem atacante (fogo, queda, {@code /kill}) nao tem angulo: cai na
     * regiao comum, passa sem multiplicador e tambem sem stagger -- cambalear por
     * queimadura transformaria fogo numa interrupcao permanente.</p>
     */
    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (level().isClientSide || !(source.getEntity() instanceof LivingEntity atacante)
                || !Float.isFinite(amount) || amount <= 0.0F) {
            return super.hurt(source, amount);
        }
        Vec3 impacto = pontoDeImpacto(source, atacante);
        double alturaRelativa = Mth.clamp((impacto.y - getY()) / getBbHeight(), 0.0D, 1.0D);
        String regiao = GEOMETRIA_DO_OLHO.resolver(alturaRelativa, cossenoDoOlharAte(atacante));
        float danoFinal = PONTOS_FRACOS.damage(amount, regiao);

        boolean levou = super.hurt(source, danoFinal);
        if (!levou) return false;

        // O stagger le o dano REAL, ja multiplicado: acertar o olho interrompe o
        // gigante em dois golpes, e bater na perna precisaria de sete. Ler o dano
        // bruto faria o ponto fraco valer so para a barra de vida, e o cambaleio
        // -- que e a leitura visual da recompensa -- mentiria sobre onde mirar.
        EnemyRuntime runtime = runtimeExigido();
        StaggerResult resultado = runtime.sofrerStagger(runtime.ataques().attackInstanceId(),
                danoFinal, CyclopsTuning.RECARGA_APOS_INTERRUPCAO);
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
        Vec3 fim = olho.add(atacante.getLookAngle().scale(CyclopsTuning.ALCANCE_DO_TRACO));
        Vec3 reserva = source.getSourcePosition() != null ? source.getSourcePosition() : olho;
        return getBoundingBox().clip(olho, fim).orElse(reserva);
    }

    /** Som de mundo chega ao gigante como EVENTO -- e assim que a audicao existe. */
    public void ouvir(HearingEvent evento) {
        if (!level().isClientSide) runtimeExigido().percepcao().ouvir(evento);
    }

    // ------------------------------------------------------------- ciclo de vida

    /**
     * Morrer PUBLICA o repouso.
     *
     * <p>Depois de morto o passo de IA nao roda mais, e o campo sincronizado fica
     * onde estava: o bicho morreria com a pose do golpe travada no cliente. Nao
     * ha erro nisso -- so um cadaver de porrete erguido.</p>
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
     * Anda ate o alvo enquanto ele estiver longe demais para o porrete.
     *
     * <p>{@code static} de proposito: todo estado que ela le mora na ENTIDADE. Um
     * campo aqui seria compartilhado por todos os ciclopes do mundo, e o sintoma
     * seria um deles perseguindo o alvo de outro.</p>
     */
    private static final class PerseguirGoal extends Goal {
        private final CyclopsEntity gigante;

        private PerseguirGoal(CyclopsEntity gigante) {
            this.gigante = gigante;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override public boolean canUse() {
            LivingEntity alvo = gigante.getTarget();
            return alvo != null && alvo.isAlive()
                    && !gigante.runtimeExigido().stagger().cambaleando()
                    && gigante.runtimeExigido().ataques().phase() == AttackPhase.IDLE
                    && gigante.distanceTo(alvo) > CyclopsTuning.ALCANCE_DO_GOLPE;
        }

        @Override public boolean canContinueToUse() { return canUse(); }

        @Override public boolean requiresUpdateEveryTick() { return true; }

        @Override public void tick() {
            LivingEntity alvo = gigante.getTarget();
            if (alvo == null) return;
            gigante.getLookControl().setLookAt(alvo, 30.0F, 30.0F);
            gigante.getNavigation().moveTo(alvo, 1.0D);
        }

        @Override public void stop() { gigante.getNavigation().stop(); }
    }

    /**
     * Arma e desce o porrete; quem mede a janela e o {@link AttackController}.
     *
     * <p><b>Ela RECUSA o golpe contra quem esta no ponto cego, e a recusa tem
     * motivo.</b> A memoria de ameaca dura 120 ticks e sobrevive ao alvo sair do
     * cone -- sem esta condicao, o gigante continuaria golpeando com precisao
     * alguem que ele deixou de enxergar, e o flanco cego viraria enfeite de
     * documento. Quem circula VE o gigante parar de atacar, e e essa a prova em
     * tela de que a regra existe.</p>
     */
    private static final class PorreteGoal extends Goal {
        private final CyclopsEntity gigante;

        private PorreteGoal(CyclopsEntity gigante) {
            this.gigante = gigante;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override public boolean canUse() {
            if (gigante.level().isClientSide) return false;
            EnemyRuntime runtime = gigante.runtimeExigido();
            if (runtime.stagger().cambaleando()) return false;
            if (runtime.consciencia() == EnemyAwarenessState.FLEE) return false;
            LivingEntity alvo = gigante.getTarget();
            if (alvo == null || !alvo.isAlive()) return false;
            if (OLHO.noPontoCego(gigante.cossenoDoOlharAte(alvo))) return false;
            return gigante.distanceTo(alvo) <= CyclopsTuning.ALCANCE_DO_GOLPE
                    && gigante.hasLineOfSight(alvo)
                    && runtime.ataques().canStart();
        }

        @Override public boolean canContinueToUse() {
            EnemyRuntime runtime = gigante.runtimeExigido();
            // A CONTINUACAO NAO RECONSULTA O CONE, de proposito. Depois que a tora
            // subiu, o golpe acontece: cancelar no meio porque o alvo saiu do campo
            // de visao faria o gigante desarmar em silencio um ataque que o jogador
            // ja viu comecar, e o telegrafo passaria a mentir. Quem sai do cone
            // ganha o golpe ERRANDO, e nao o golpe desaparecendo.
            return runtime.ataques().phase() != AttackPhase.IDLE
                    && runtime.ataques().phase() != AttackPhase.COMPLETE
                    && !runtime.stagger().cambaleando();
        }

        @Override public boolean requiresUpdateEveryTick() { return true; }

        @Override public void start() {
            gigante.getNavigation().stop();
            gigante.runtimeExigido().ataques().start(CyclopsTuning.porrete());
        }

        @Override public void tick() {
            EnemyRuntime runtime = gigante.runtimeExigido();
            LivingEntity alvo = gigante.getTarget();
            if (alvo != null && runtime.ataques().phase() == AttackPhase.WINDUP
                    && ticksDeAvisoJaGastos(runtime) < CyclopsTuning.TICKS_DE_MIRA_NO_WINDUP) {
                gigante.getLookControl().setLookAt(alvo, 30.0F, 30.0F);
            }
            // Um gigante nao desliza enquanto bate. Sem isto ele chegaria ao fim do
            // aviso ainda carregando a inercia da perseguicao, e o golpe sairia de
            // um lugar diferente daquele onde ele foi armado -- sem erro nenhum, e
            // com o telegrafo apontando para o lugar errado.
            gigante.setDeltaMovement(gigante.getDeltaMovement().multiply(0.2D, 1.0D, 0.2D));
        }

        /** Quantos ticks do aviso ja passaram; o relogio e o do servidor, nunca o do clipe. */
        private static int ticksDeAvisoJaGastos(EnemyRuntime runtime) {
            return CyclopsTuning.WINDUP_DO_PORRETE - runtime.ataques().remainingTicks();
        }

        @Override public void stop() { gigante.getNavigation().stop(); }
    }

    // ------------------------------------------------------------------ animacao

    /**
     * O CLIENTE NAO DECIDE NADA: ele le a fase e o cambaleio que o servidor publica.
     *
     * <p>Ordem de precedencia, da mais especifica para a menos: morte, stagger,
     * fases do golpe, locomocao, ocio. Morte vem primeiro porque um gigante que
     * morre no meio do golpe nao pode terminar o clipe de ataque -- e stagger vem
     * antes do golpe porque interromper e, por definicao, o que ganha do golpe.</p>
     */
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<CyclopsEntity>(this, CONTROLLER_DO_CORPO,
                TRANSICAO_EM_TICKS, this::clipeDoCorpo));
    }

    private PlayState clipeDoCorpo(AnimationState<CyclopsEntity> estado) {
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

    // Os nomes sao um CONTRATO com cyclops.animation.json. Errar um deles nao da
    // erro: o GeckoLib nao acha o clipe e deixa o osso parado. LoopType.DEFAULT em
    // todos -- o tipo de repeticao mora no arquivo, e cravar em Java o faria vencer
    // o JSON, transformando o dicionario LOOPS do gerador em documentacao que
    // discorda do comportamento.
    private static final RawAnimation IDLE =
            RawAnimation.begin().then("animation.cyclops.idle", Animation.LoopType.DEFAULT);
    private static final RawAnimation WALK =
            RawAnimation.begin().then("animation.cyclops.walk", Animation.LoopType.DEFAULT);
    private static final RawAnimation WINDUP =
            RawAnimation.begin().then("animation.cyclops.windup", Animation.LoopType.DEFAULT);
    private static final RawAnimation STRIKE =
            RawAnimation.begin().then("animation.cyclops.strike", Animation.LoopType.DEFAULT);
    private static final RawAnimation RECOVERY =
            RawAnimation.begin().then("animation.cyclops.recovery", Animation.LoopType.DEFAULT);
    private static final RawAnimation STAGGER =
            RawAnimation.begin().then("animation.cyclops.stagger", Animation.LoopType.DEFAULT);
    private static final RawAnimation DEATH =
            RawAnimation.begin().then("animation.cyclops.death", Animation.LoopType.DEFAULT);

    private static final String CONTROLLER_DO_CORPO = "corpo";
    private static final int TRANSICAO_EM_TICKS = 4;

    /**
     * Deslocamento por tick acima do qual o clipe passa de ocio para caminhada.
     *
     * <p>Limite de LEITURA, e nao botao de balanceamento: ele separa "parado" de
     * "andando" na tela e nao muda nada no servidor.</p>
     */
    private static final double LIMIAR_DE_CAMINHADA = 0.02D;

    /**
     * Blocos andados no ultimo tick, medidos por POSICAO.
     *
     * <p>Nao usa {@code getDeltaMovement()}: no cliente o delta de uma entidade
     * remota so e escrito quando chega um pacote de velocidade, entao ele fica
     * zerado na maior parte dos ticks e o gigante andaria sempre no clipe de
     * ocio. {@code xo}/{@code zo} sao atualizados todo tick nos DOIS lados.</p>
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
}
