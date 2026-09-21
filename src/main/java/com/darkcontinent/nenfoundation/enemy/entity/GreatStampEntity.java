package com.darkcontinent.nenfoundation.enemy.entity;

import com.darkcontinent.nenfoundation.enemy.ai.AwarenessInput;
import com.darkcontinent.nenfoundation.enemy.ai.AwarenessTuning;
import com.darkcontinent.nenfoundation.enemy.api.EnemyAwarenessState;
import com.darkcontinent.nenfoundation.enemy.api.EnemyCombatState;
import com.darkcontinent.nenfoundation.enemy.base.BaseHxHMob;
import com.darkcontinent.nenfoundation.enemy.combat.AttackDefinition;
import com.darkcontinent.nenfoundation.enemy.combat.AttackPhase;
import com.darkcontinent.nenfoundation.enemy.combat.AttackTimeline;
import com.darkcontinent.nenfoundation.enemy.combat.ChargeRules;
import com.darkcontinent.nenfoundation.enemy.combat.WeakPointRegistry;
import com.darkcontinent.nenfoundation.enemy.combat.WeakPointResolver;
import com.darkcontinent.nenfoundation.enemy.content.HunterExamProfiles;
import com.darkcontinent.nenfoundation.enemy.registry.EnemyEntityTypes;
import java.util.EnumSet;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
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
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
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
 * Great stamp: a ameaca e a carga telegrafada, e a resposta do jogador e
 * desviar e acertar a TESTA.
 *
 * <p>Tudo que decide -- se a carga comeca, para onde ela vai, quem ela
 * machuca e qual regiao o jogador acertou -- roda no servidor. O campo
 * sincronizado carrega apenas a FASE do ataque, para o cliente conseguir
 * desenhar a cabeca baixa e a corrida. O cliente nunca informa acerto.</p>
 *
 * <p>CORPO PROPRIO (ADR-017). Ate aqui o great stamp vestia geometria e textura
 * do hoglin vanilla. Agora ele e um {@link GeoEntity}: modelo, esqueleto,
 * animacoes e textura sao autorais, e o andaime saiu. O comportamento nao mudou
 * uma linha -- carga, testa e manada sao os mesmos que os gametests medem.</p>
 */
public final class GreatStampEntity extends BaseHxHMob implements GeoEntity {
    /** Fase do ataque; o unico estado de combate que o cliente precisa conhecer. */
    private static final EntityDataAccessor<Integer> FASE_DE_ATAQUE =
            SynchedEntityData.defineId(GreatStampEntity.class, EntityDataSerializers.INT);

    private static final AttackDefinition CARGA = HunterExamProfiles.greatStampCharge();
    private static final ChargeRules REGRAS = HunterExamProfiles.greatStampChargeRules();
    private static final WeakPointRegistry PONTOS_FRACOS = HunterExamProfiles.greatStampWeakPoints();
    private static final WeakPointResolver GEOMETRIA_DO_PONTO_FRACO = HunterExamProfiles.greatStampWeakPoint();

    /** Quanto tempo o stamp lembra de um alvo que saiu da linha de visao. */
    private static final int MEMORIA_DE_ALVO_TICKS = 160;
    /** Quantos ticks de aviso antes de o cerebro passar de WARN para ENGAGE. */
    private static final int TICKS_DE_AVISO = 30;
    /** Dentro deste raio o intruso e considerado dentro do territorio da manada. */
    private static final double RAIO_DE_TERRITORIO = 12.0D;
    /** Afastar-se mais do que isto num tick conta como recuo. */
    private static final double TOLERANCIA_DE_RECUO = 0.35D;
    /** Fracao da vida abaixo da qual o cerebro pode decidir fugir. */
    private static final float FRACAO_DE_VIDA_CRITICA = 0.2F;
    /** Alcance do traco que procura o ponto de impacto na caixa do stamp. */
    private static final double ALCANCE_DO_TRACO = 8.0D;
    /** Folga da caixa de colisao usada para achar quem esta no caminho da carga. */
    private static final double FOLGA_DA_CARGA = 0.35D;

    // ------------------------------------------------------------- animacao
    // Os nomes abaixo sao um CONTRATO com great_stamp.animation.json. Errar um
    // deles nao da erro: o GeckoLib simplesmente nao acha o clipe e deixa o osso
    // parado. E o tipo de falha que so aparece na tela de quem joga.
    private static final RawAnimation IDLE = RawAnimation.begin().then("animation.great_stamp.idle", Animation.LoopType.DEFAULT);
    private static final RawAnimation WALK = RawAnimation.begin().then("animation.great_stamp.walk", Animation.LoopType.DEFAULT);
    private static final RawAnimation RUN = RawAnimation.begin().then("animation.great_stamp.run", Animation.LoopType.DEFAULT);
    private static final RawAnimation WINDUP = RawAnimation.begin().then("animation.great_stamp.windup", Animation.LoopType.DEFAULT);
    private static final RawAnimation CHARGE = RawAnimation.begin().then("animation.great_stamp.charge", Animation.LoopType.DEFAULT);
    private static final RawAnimation STAGGER = RawAnimation.begin().then("animation.great_stamp.stagger", Animation.LoopType.DEFAULT);

    /** Nome do unico controller; quem registrar um segundo clipe reusa esta constante. */
    private static final String CONTROLLER_DO_CORPO = "corpo";
    /** Ticks de mistura entre um clipe e o proximo -- o suficiente para nao ter salto. */
    private static final int TRANSICAO_EM_TICKS = 5;
    /**
     * Deslocamento horizontal por tick acima do qual o clipe passa de walk para run.
     *
     * <p>NAO e botao de balanceamento e por isso nao vai para config: e o ponto de
     * troca entre duas animacoes, medido em blocos/tick. O passeio do stamp anda a
     * 0.8x e a perseguicao a 1.0x da MOVEMENT_SPEED; o limiar fica entre os dois.</p>
     */
    private static final double LIMIAR_DE_CORRIDA = 0.13D;

    /** Cache por INSTANCIA. Um cache estatico faria a manada inteira compartilhar um clipe. */
    private final AnimatableInstanceCache cacheDeAnimacao = GeckoLibUtil.createInstanceCache(this);

    // Estado de carga: DA INSTANCIA. Guardar isto na Goal faria todos os stamps do
    // mundo compartilharem a mesma carga -- a Goal e uma por entidade, mas a classe
    // aninhada abaixo e static justamente para nao esconder essa dependencia.
    private AttackTimeline linhaDoTempoDaCarga = new AttackTimeline();
    private boolean cargaEmAndamento;
    private boolean jaAcertouNestaCarga;
    private Vec3 direcaoDaCarga = Vec3.ZERO;
    private int esperaRestante;
    private int atordoamentoRestante;

    // Sensores do cerebro.
    private int memoriaDeAlvo;
    private double distanciaAnteriorDoAlvo = Double.NaN;

    public GreatStampEntity(EntityType<? extends GreatStampEntity> type, Level level) {
        super(type, level, HunterExamProfiles.greatStamp().metadata(),
                new AwarenessTuning(TICKS_DE_AVISO, MEMORIA_DE_ALVO_TICKS));
    }

    public static AttributeSupplier.Builder createAttributes() {
        var attrs = HunterExamProfiles.greatStamp().attributes();
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, attrs.maxHealth())
                .add(Attributes.MOVEMENT_SPEED, attrs.movementSpeed())
                .add(Attributes.ATTACK_DAMAGE, attrs.attackDamage())
                .add(Attributes.ARMOR, attrs.armor())
                .add(Attributes.FOLLOW_RANGE, attrs.followRange())
                .add(Attributes.KNOCKBACK_RESISTANCE, attrs.knockbackResistance());
    }

    public static EntityType<GreatStampEntity> registeredType() { return EnemyEntityTypes.GREAT_STAMP.get(); }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(FASE_DE_ATAQUE, AttackPhase.IDLE.ordinal());
    }

    /** Fase corrente da carga; vale nos dois lados, porque vem do SynchedEntityData. */
    public AttackPhase faseDeAtaque() {
        int ordinal = this.entityData.get(FASE_DE_ATAQUE);
        AttackPhase[] fases = AttackPhase.values();
        return ordinal >= 0 && ordinal < fases.length ? fases[ordinal] : AttackPhase.IDLE;
    }

    /** Verdadeiro so na fase que de fato machuca; o windup ainda nao e carga. */
    public boolean estaCarregando() { return faseDeAtaque() == AttackPhase.ACTIVE; }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new CargaGoal(this));
        goalSelector.addGoal(2, new AtaqueCorpoACorpoGoal(this));
        goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.8D));
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 10.0F));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (level().isClientSide) return;
        if (esperaRestante > 0) esperaRestante--;
        if (atordoamentoRestante > 0) tickDeAtordoamento();
        EnemyAwarenessState consciencia = enemyBrain().tick(lerSensores());
        alinharEstadoDeCombate(consciencia);
    }

    /**
     * UNICO lugar do mod que aplica o multiplicador da testa.
     *
     * <p>Dano multiplicado em dois handlers e o bug silencioso que este projeto
     * ja sabe que comete: o numero final fica plausivel demais para alguem notar
     * sem medir. Se um dia alguem precisar do multiplicador em outro ponto, o
     * certo e mover daqui, nunca somar um segundo ponto.</p>
     *
     * <p>A regiao e resolvida com a geometria QUE O SERVIDOR TEM; o cliente nao
     * participa. Dano sem atacante (fogo, queda, magia) nao tem golpe nem angulo:
     * cai na regiao padrao e passa sem multiplicador.</p>
     */
    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (level().isClientSide || !(source.getEntity() instanceof LivingEntity atacante)
                || !Float.isFinite(amount) || amount <= 0.0F) {
            return super.hurt(source, amount);
        }
        Vec3 impacto = pontoDeImpacto(source, atacante);
        double alturaRelativa = Mth.clamp((impacto.y - getY()) / getBbHeight(), 0.0D, 1.0D);
        String regiao = GEOMETRIA_DO_PONTO_FRACO.resolver(alturaRelativa, cossenoDeFrente(atacante));
        return super.hurt(source, PONTOS_FRACOS.damage(amount, regiao));
    }

    @Override
    public void die(DamageSource source) {
        // Quem liga, desliga: morrer no meio da carga nao pode deixar a fase presa em ACTIVE.
        if (!level().isClientSide) {
            encerrarCarga();
            combatState(EnemyCombatState.DYING);
        }
        super.die(source);
    }

    @Override
    public void remove(Entity.RemovalReason reason) {
        if (!level().isClientSide) encerrarCarga();
        super.remove(reason);
    }

    // ------------------------------------------------------------- animacao

    /**
     * O CLIENTE NAO DECIDE NADA. Ele le a fase que o servidor ja publica em
     * {@link #faseDeAtaque()} e escolhe o clipe correspondente -- nao existe aqui
     * nenhum timer, nenhuma heuristica de "parece que vai carregar" e nenhuma
     * copia da regra de combate. Se a animacao e a hitbox discordarem, quem esta
     * errado e o arquivo de animacao, nunca o servidor.
     *
     * <p>Ordem de precedencia, da mais especifica para a menos: telegrafo, corrida,
     * atordoamento, locomocao, ocio. O telegrafo vem primeiro de proposito -- e a
     * unica leitura que o jogador tem para saber que precisa desviar.</p>
     */
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<GreatStampEntity>(this, CONTROLLER_DO_CORPO,
                TRANSICAO_EM_TICKS, this::clipeDoCorpo));
    }

    private PlayState clipeDoCorpo(AnimationState<GreatStampEntity> estado) {
        AttackPhase fase = faseDeAtaque();
        if (fase == AttackPhase.WINDUP) return estado.setAndContinue(WINDUP);
        if (fase == AttackPhase.ACTIVE) return estado.setAndContinue(CHARGE);
        // PONTO CEGO DECLARADO: combatState() e campo de servidor, nao SynchedEntityData.
        // No cliente ele le IDLE sempre, entao este ramo NAO dispara em jogo hoje. Ele
        // fica escrito porque a regra e esta, e passa a valer no dia em que o estado de
        // combate for sincronizado -- sem isso, a proxima pessoa reescreveria a regra.
        if (combatState() == EnemyCombatState.STAGGERED) return estado.setAndContinue(STAGGER);
        if (estado.isMoving()) {
            return estado.setAndContinue(velocidadeHorizontal() >= LIMIAR_DE_CORRIDA ? RUN : WALK);
        }
        return estado.setAndContinue(IDLE);
    }

    /**
     * Blocos andados no ultimo tick, medidos por posicao.
     *
     * <p>Nao usa {@code getDeltaMovement()}: no cliente o delta de uma entidade
     * remota so e escrito quando chega um pacote de velocidade, entao ele fica
     * zerado na maior parte dos ticks e o stamp andaria sempre no clipe de walk.
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

    // ---------------------------------------------------------------- carga

    /** Comeca uma carga do zero; a linha do tempo e NOVA para nunca herdar fase presa. */
    private void iniciarCarga() {
        cargaEmAndamento = true;
        jaAcertouNestaCarga = false;
        direcaoDaCarga = Vec3.ZERO;
        linhaDoTempoDaCarga = new AttackTimeline();
        linhaDoTempoDaCarga.start(CARGA);
        getNavigation().stop();
        combatState(EnemyCombatState.WINDUP);
        sincronizarFase();
    }

    private void tickDaCarga() {
        if (level().isClientSide) return;
        AttackPhase fase = linhaDoTempoDaCarga.phase();
        LivingEntity alvo = getTarget();
        switch (fase) {
            case WINDUP -> {
                // Telegrafo: para de andar e encara o alvo. A cabeca abaixada e a fase
                // sincronizada; quem desenha e o cliente.
                combatState(EnemyCombatState.WINDUP);
                getNavigation().stop();
                setDeltaMovement(getDeltaMovement().multiply(0.2D, 1.0D, 0.2D));
                if (alvo != null) {
                    getLookControl().setLookAt(alvo, 30.0F, 30.0F);
                    // Reescrito a cada tick: o valor que sobra e o do ULTIMO tick do
                    // windup, que e exatamente a direcao travada no fim do telegrafo.
                    direcaoDaCarga = direcaoHorizontalAte(alvo);
                }
            }
            case ACTIVE -> {
                combatState(EnemyCombatState.ACTIVE);
                correrEmLinhaReta();
                aplicarDanoDaCarga(fase);
                if (REGRAS.atordoa(fase, this.horizontalCollision)) {
                    atordoar();
                    return;
                }
            }
            case RECOVERY -> {
                combatState(EnemyCombatState.RECOVERY);
                getNavigation().stop();
            }
            default -> { }
        }
        linhaDoTempoDaCarga.tick();
        sincronizarFase();
        if (linhaDoTempoDaCarga.finished()) cargaEmAndamento = false;
    }

    /**
     * Saida UNICA da carga. Toda forma de terminar -- alvo morto, atordoamento,
     * recovery, remocao da entidade -- passa por aqui, e e aqui que a espera e
     * armada. Limpeza espalhada pelos pontos de saida deixa a carga ligada para
     * sempre em algum deles.
     */
    private void encerrarCarga() {
        cargaEmAndamento = false;
        jaAcertouNestaCarga = false;
        direcaoDaCarga = Vec3.ZERO;
        esperaRestante = REGRAS.ticksDeEspera();
        linhaDoTempoDaCarga = new AttackTimeline();
        sincronizarFase();
        if (combatState() != EnemyCombatState.STAGGERED && combatState() != EnemyCombatState.DYING) {
            combatState(getTarget() != null ? EnemyCombatState.AGGRO : estadoOcioso());
        }
    }

    /** Bateu na parede correndo: esta e a janela em que o jogador alcanca a testa. */
    private void atordoar() {
        atordoamentoRestante = REGRAS.ticksDeAtordoamento();
        combatState(EnemyCombatState.STAGGERED);
        getNavigation().stop();
        setDeltaMovement(Vec3.ZERO);
        encerrarCarga();
    }

    private void tickDeAtordoamento() {
        atordoamentoRestante--;
        getNavigation().stop();
        setDeltaMovement(getDeltaMovement().multiply(0.0D, 1.0D, 0.0D));
        if (atordoamentoRestante == 0 && combatState() == EnemyCombatState.STAGGERED) {
            combatState(getTarget() != null ? EnemyCombatState.AGGRO : estadoOcioso());
        }
    }

    private void correrEmLinhaReta() {
        if (direcaoDaCarga.lengthSqr() < 1.0E-6D) return;
        double velocidade = getAttributeValue(Attributes.MOVEMENT_SPEED) * REGRAS.multiplicadorDeVelocidade();
        setDeltaMovement(direcaoDaCarga.x * velocidade, getDeltaMovement().y, direcaoDaCarga.z * velocidade);
        float rotacao = (float) (Mth.atan2(direcaoDaCarga.z, direcaoDaCarga.x) * (180.0D / Math.PI)) - 90.0F;
        setYRot(rotacao);
        this.yBodyRot = rotacao;
        this.hasImpulse = true;
    }

    /** A primeira vitima viva no caminho leva o dano da definicao; uma por carga. */
    private void aplicarDanoDaCarga(AttackPhase fase) {
        if (!REGRAS.janelaDeDano(fase, jaAcertouNestaCarga)) return;
        AABB caminho = getBoundingBox().inflate(FOLGA_DA_CARGA);
        // A MANADA NAO E ALVO. Great stamp nasce em grupo de 2 a 4; sem este filtro a
        // primeira carga acerta um irmao, o HurtByTargetGoal do irmao responde, e a
        // manada se mata sozinha antes de o jogador chegar perto.
        for (LivingEntity vitima : level().getEntitiesOfClass(LivingEntity.class, caminho,
                v -> v != this && v.getType() != getType() && v.isAlive()
                        && v.isAttackable() && !isAlliedTo(v))) {
            if (!vitima.hurt(damageSources().mobAttack(this), CARGA.damage())) continue;
            Vec3 empurrao = direcaoDaCarga.lengthSqr() < 1.0E-6D
                    ? vitima.position().subtract(position())
                    : direcaoDaCarga;
            // knockback empurra para LONGE de (x,z); negar manda a vitima na direcao da carga.
            vitima.knockback(CARGA.knockback(), -empurrao.x, -empurrao.z);
            jaAcertouNestaCarga = true;
            return;
        }
    }

    private Vec3 direcaoHorizontalAte(Entity alvo) {
        Vec3 delta = new Vec3(alvo.getX() - getX(), 0.0D, alvo.getZ() - getZ());
        return delta.lengthSqr() < 1.0E-6D ? Vec3.ZERO : delta.normalize();
    }

    private void sincronizarFase() {
        this.entityData.set(FASE_DE_ATAQUE, linhaDoTempoDaCarga.phase().ordinal());
    }

    // ------------------------------------------------------------ geometria

    /**
     * Ponto de impacto medido no servidor: traco do olho do atacante na direcao
     * do olhar contra a caixa do stamp. Sem intersecao, cai para a posicao do
     * dano (ou o olho do atacante) -- nunca para algo que o cliente afirmou.
     */
    private Vec3 pontoDeImpacto(DamageSource source, LivingEntity atacante) {
        Vec3 olho = atacante.getEyePosition();
        Vec3 fim = olho.add(atacante.getLookAngle().scale(ALCANCE_DO_TRACO));
        Vec3 reserva = source.getSourcePosition() != null ? source.getSourcePosition() : olho;
        return getBoundingBox().clip(olho, fim).orElse(reserva);
    }

    /** 1 quando o atacante esta bem de frente, -1 quando esta atras. */
    private double cossenoDeFrente(Entity atacante) {
        Vec3 olhar = getLookAngle();
        Vec3 olharHorizontal = new Vec3(olhar.x, 0.0D, olhar.z);
        Vec3 ateOAtacante = new Vec3(atacante.getX() - getX(), 0.0D, atacante.getZ() - getZ());
        if (olharHorizontal.lengthSqr() < 1.0E-6D || ateOAtacante.lengthSqr() < 1.0E-6D) return 0.0D;
        return Mth.clamp(olharHorizontal.normalize().dot(ateOAtacante.normalize()), -1.0D, 1.0D);
    }

    // -------------------------------------------------------------- sensores

    /** Leitura de mundo do tick: e isto que faz awarenessState() parar de ser decorativo. */
    private AwarenessInput lerSensores() {
        LivingEntity alvo = getTarget();
        boolean alvoVivo = alvo != null && alvo.isAlive();
        boolean visivel = alvoVivo && isPerceivedVisible(alvo);
        double distancia = alvoVivo ? distanceTo(alvo) : Double.NaN;

        if (visivel) memoriaDeAlvo = MEMORIA_DE_ALVO_TICKS;
        else if (memoriaDeAlvo > 0) memoriaDeAlvo--;

        boolean recuando = visivel && !Double.isNaN(distanciaAnteriorDoAlvo)
                && distancia > distanciaAnteriorDoAlvo + TOLERANCIA_DE_RECUO;
        distanciaAnteriorDoAlvo = distancia;

        boolean audivel = !visivel && (isPerceivedAudible(alvo) || memoriaDeAlvo > 0);
        boolean noTerritorio = visivel && distancia <= RAIO_DE_TERRITORIO;
        boolean vidaCritica = getHealth() <= getMaxHealth() * FRACAO_DE_VIDA_CRITICA;

        // Great stamp e manada, nao emboscador: nunca oferece oportunidade de ambush.
        return new AwarenessInput(visivel, audivel, noTerritorio, recuando, !alvoVivo,
                memoriaDeAlvo, false, vidaCritica);
    }

    /** A fase da carga manda; fora dela quem manda e a consciencia. */
    private void alinharEstadoDeCombate(EnemyAwarenessState consciencia) {
        if (cargaEmAndamento || atordoamentoRestante > 0 || combatState() == EnemyCombatState.DYING) return;
        if (consciencia == EnemyAwarenessState.FLEE) {
            combatState(EnemyCombatState.RETREAT);
            return;
        }
        combatState(getTarget() != null ? EnemyCombatState.AGGRO : estadoOcioso());
    }

    private EnemyCombatState estadoOcioso() {
        return enemyMetadata().social() ? EnemyCombatState.GROUP_ROAM : EnemyCombatState.IDLE;
    }

    // ------------------------------------------------------------------ goals

    /**
     * Dirige a carga. E static de proposito: todo estado que ela le e escreve
     * mora na ENTIDADE, e o prefixo {@code stamp.} deixa isso impossivel de
     * esquecer. Um campo aqui seria compartilhado por todos os great stamps.
     */
    private static final class CargaGoal extends Goal {
        private final GreatStampEntity stamp;

        private CargaGoal(GreatStampEntity stamp) {
            this.stamp = stamp;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override public boolean canUse() {
            if (stamp.level().isClientSide || stamp.atordoamentoRestante > 0) return false;
            if (stamp.awarenessState() == EnemyAwarenessState.FLEE) return false;
            LivingEntity alvo = stamp.getTarget();
            if (alvo == null || !alvo.isAlive()) return false;
            return REGRAS.podeIniciar(stamp.hasLineOfSight(alvo), stamp.distanceTo(alvo), stamp.esperaRestante);
        }

        @Override public boolean canContinueToUse() {
            LivingEntity alvo = stamp.getTarget();
            // Alvo morto interrompe, mas quem arma a espera continua sendo stop().
            return stamp.cargaEmAndamento && alvo != null && alvo.isAlive();
        }

        @Override public boolean requiresUpdateEveryTick() { return true; }

        @Override public void start() { stamp.iniciarCarga(); }

        @Override public void tick() { stamp.tickDaCarga(); }

        @Override public void stop() { stamp.encerrarCarga(); }
    }

    /** Corpo a corpo comum, calado enquanto a carga ou o atordoamento mandam no corpo. */
    private static final class AtaqueCorpoACorpoGoal extends MeleeAttackGoal {
        private final GreatStampEntity stamp;

        private AtaqueCorpoACorpoGoal(GreatStampEntity stamp) {
            super(stamp, 1.0D, true);
            this.stamp = stamp;
        }

        private boolean corpoLivre() {
            return !stamp.cargaEmAndamento && stamp.atordoamentoRestante == 0;
        }

        @Override public boolean canUse() { return corpoLivre() && super.canUse(); }

        @Override public boolean canContinueToUse() { return corpoLivre() && super.canContinueToUse(); }
    }
}
