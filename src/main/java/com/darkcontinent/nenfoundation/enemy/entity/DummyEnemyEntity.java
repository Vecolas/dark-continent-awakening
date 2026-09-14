package com.darkcontinent.nenfoundation.enemy.entity;

import com.darkcontinent.nenfoundation.enemy.ai.AwarenessInput;
import com.darkcontinent.nenfoundation.enemy.ai.AwarenessTuning;
import com.darkcontinent.nenfoundation.enemy.ai.PerceptionBudget;
import com.darkcontinent.nenfoundation.enemy.ai.PerceptionController;
import com.darkcontinent.nenfoundation.enemy.ai.PerceptionSnapshot;
import com.darkcontinent.nenfoundation.enemy.api.EnemyAwarenessState;
import com.darkcontinent.nenfoundation.enemy.api.EnemyCombatState;
import com.darkcontinent.nenfoundation.enemy.api.EnemyFaction;
import com.darkcontinent.nenfoundation.enemy.api.FactionRelation;
import com.darkcontinent.nenfoundation.enemy.base.BaseHxHMob;
import com.darkcontinent.nenfoundation.enemy.combat.AttackController;
import com.darkcontinent.nenfoundation.enemy.combat.AttackDefinition;
import com.darkcontinent.nenfoundation.enemy.combat.AttackHitbox;
import com.darkcontinent.nenfoundation.enemy.combat.AttackPhase;
import com.darkcontinent.nenfoundation.enemy.combat.StaggerController;
import com.darkcontinent.nenfoundation.enemy.combat.StaggerDefinition;
import com.darkcontinent.nenfoundation.enemy.combat.StaggerResult;
import com.darkcontinent.nenfoundation.enemy.combat.WeakPointRegistry;
import com.darkcontinent.nenfoundation.enemy.combat.WeakPointResolver;
import com.darkcontinent.nenfoundation.enemy.content.HunterExamProfiles;
import com.darkcontinent.nenfoundation.enemy.faction.FactionRelations;
import com.darkcontinent.nenfoundation.enemy.registry.EnemyEntityTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.util.Mth;
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

/** Mob de contrato do EN1: pequeno, determinista e completo o bastante para o primeiro GameTest. */
public final class DummyEnemyEntity extends BaseHxHMob implements GeoEntity {
    private static final EntityDataAccessor<Integer> FASE =
            SynchedEntityData.defineId(DummyEnemyEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> ACAO =
            SynchedEntityData.defineId(DummyEnemyEntity.class, EntityDataSerializers.INT);
    private static final AttackDefinition GOLPE = HunterExamProfiles.dummyEnemyStrike();
    private static final AttackHitbox HITBOX = new AttackHitbox(-0.55D, 0.0D, 0.0D,
            0.55D, 1.4D, 1.8D);
    private static final WeakPointRegistry WEAK_POINTS = HunterExamProfiles.dummyEnemyWeakPoints();
    private static final WeakPointResolver WEAK_POINT_RESOLVER = HunterExamProfiles.dummyEnemyWeakPoint();
    private static final double WEAK_POINT_TRACE = 6.0D;
    private static final int MEMORY_TICKS = 40;
    private static final double RANGE = 2.4D;
    private static final RawAnimation IDLE = RawAnimation.begin().then("animation.dummy_enemy.idle", Animation.LoopType.DEFAULT);
    private static final RawAnimation WALK = RawAnimation.begin().then("animation.dummy_enemy.walk", Animation.LoopType.DEFAULT);
    private static final RawAnimation WINDUP = RawAnimation.begin().then("animation.dummy_enemy.windup", Animation.LoopType.DEFAULT);
    private static final RawAnimation ACTIVE = RawAnimation.begin().then("animation.dummy_enemy.active", Animation.LoopType.DEFAULT);
    private static final RawAnimation RECOVERY = RawAnimation.begin().then("animation.dummy_enemy.recovery", Animation.LoopType.DEFAULT);
    private static final RawAnimation STAGGER = RawAnimation.begin().then("animation.dummy_enemy.stagger", Animation.LoopType.DEFAULT);
    private static final RawAnimation DEATH = RawAnimation.begin().then("animation.dummy_enemy.death", Animation.LoopType.PLAY_ONCE);

    private final AttackController attack = new AttackController(10);
    private final StaggerController stagger = new StaggerController(new StaggerDefinition(3.0F, 0.5F, 0.25F, 20));
    private final PerceptionController perception = new PerceptionController(
            new com.darkcontinent.nenfoundation.enemy.ai.TargetEvaluator(EnemyFaction.CUSTOM,
                    "minecraft:overworld", 16.0D, 90.0D,
                    new FactionRelations(java.util.Map.of(EnemyFaction.CUSTOM,
                            java.util.Map.of(EnemyFaction.CIVILIAN, FactionRelation.HOSTILE)))),
            new PerceptionBudget(5, 20), MEMORY_TICKS);
    private final AnimatableInstanceCache animationCache = GeckoLibUtil.createInstanceCache(this);

    public DummyEnemyEntity(EntityType<? extends DummyEnemyEntity> type, Level level) {
        super(type, level, HunterExamProfiles.dummyEnemy().metadata(), new AwarenessTuning(4, MEMORY_TICKS));
    }

    public static AttributeSupplier.Builder createAttributes() {
        var a = HunterExamProfiles.dummyEnemy().attributes();
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, a.maxHealth())
                .add(Attributes.MOVEMENT_SPEED, a.movementSpeed()).add(Attributes.ATTACK_DAMAGE, a.attackDamage())
                .add(Attributes.ARMOR, a.armor()).add(Attributes.FOLLOW_RANGE, a.followRange())
                .add(Attributes.KNOCKBACK_RESISTANCE, a.knockbackResistance());
    }

    public static EntityType<DummyEnemyEntity> registeredType() { return EnemyEntityTypes.DUMMY_ENEMY.get(); }
    public AttackPhase attackPhase() { return phaseFrom(this.entityData.get(FASE)); }
    public int actionId() { return this.entityData.get(ACAO); }
    public boolean staggered() { return stagger.active(); }

    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(FASE, AttackPhase.IDLE.ordinal());
        builder.define(ACAO, 0);
    }

    @Override protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.75D));
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 8.0F));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
    }

    @Override protected void customServerAiStep() {
        super.customServerAiStep();
        if (level().isClientSide) return;
        if (stagger.active()) {
            stagger.tick();
            getNavigation().stop();
            setDeltaMovement(Vec3.ZERO);
            if (!stagger.active()) combatState(getTarget() == null ? EnemyCombatState.IDLE : EnemyCombatState.AGGRO);
            return;
        }
        LivingEntity target = getTarget();
        if (target == null || !target.isAlive() || target.level() != level()
                || distanceToSqr(target) > 100.0D) {
            target = level().getNearestPlayer(TargetingConditions.forCombat().range(10.0D), this);
            setTarget(target);
        }
        LivingEntity candidate = target;
        var perceptionResult = perception.tick(tickCount, () -> snapshot(candidate));
        boolean targetAccepted = perceptionResult.targetId().isPresent()
                && candidate != null && perceptionResult.targetId().get().equals(candidate.getUUID());
        AwarenessInput awareness = perceptionResult.awareness();
        enemyBrain().tick(new AwarenessInput(awareness.targetVisible(), awareness.targetAudible(),
                awareness.targetInTerritory(), awareness.targetRetreating(), !targetAccepted,
                awareness.memoryTicksRemaining(), awareness.ambushOpportunity(),
                getHealth() <= getMaxHealth() * 0.2F));
        if (!targetAccepted) target = null;
        if (target != null && target.isAlive()) {
            getLookControl().setLookAt(target, 30.0F, 30.0F);
            if (attackPhase() == AttackPhase.IDLE || attackPhase() == AttackPhase.COMPLETE) {
                if (distanceTo(target) <= RANGE && attack.canStart()) startAttack();
                else getNavigation().moveTo(target, 0.8D);
            } else tickAttack(target);
        } else if (attackPhase() != AttackPhase.IDLE) {
            attack.reset(); syncPhase(); combatState(EnemyCombatState.IDLE);
        }
    }

    private void startAttack() {
        long id = attack.start(GOLPE);
        entityData.set(ACAO, (int) id);
        combatState(EnemyCombatState.WINDUP);
        syncPhase();
    }

    private void tickAttack(LivingEntity target) {
        if (attack.phase() == AttackPhase.ACTIVE) {
            combatState(EnemyCombatState.ACTIVE);
            var hit = attack.tryHit(target.getId(), target.getBoundingBox(), HITBOX, position(), getYRot(), "body");
            hit.ifPresent(h -> target.hurt(damageSources().mobAttack(this), h.damage()));
        } else if (attack.phase() == AttackPhase.WINDUP) combatState(EnemyCombatState.WINDUP);
        else if (attack.phase() == AttackPhase.RECOVERY) combatState(EnemyCombatState.RECOVERY);
        attack.tick();
        syncPhase();
    }

    @Override public boolean hurt(DamageSource source, float amount) {
        float dano = danoDoServidor(source, amount);
        if (!level().isClientSide && Float.isFinite(dano) && dano > 0.0F) {
            StaggerResult result = stagger.apply(dano);
            if (result.outcome() == StaggerResult.Outcome.APPLIED) {
                attack.reset(); syncPhase(); combatState(EnemyCombatState.STAGGERED);
            }
        }
        return super.hurt(source, dano);
    }

    @Override public void die(DamageSource source) {
        attack.reset(); stagger.reset(); syncPhase(); combatState(EnemyCombatState.DYING);
        super.die(source);
    }

    @Override public void remove(Entity.RemovalReason reason) {
        attack.reset(); stagger.reset(); syncPhase();
        super.remove(reason);
    }

    @Override public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<DummyEnemyEntity>(this, "body", 3, this::clip));
    }

    private PlayState clip(AnimationState<DummyEnemyEntity> state) {
        if (!isAlive()) return state.setAndContinue(DEATH);
        if (stagger.active()) return state.setAndContinue(STAGGER);
        return switch (attackPhase()) {
            case WINDUP -> state.setAndContinue(WINDUP);
            case ACTIVE -> state.setAndContinue(ACTIVE);
            case RECOVERY -> state.setAndContinue(RECOVERY);
            default -> state.isMoving() ? state.setAndContinue(WALK) : state.setAndContinue(IDLE);
        };
    }

    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return animationCache; }
    @Override public double getTick(Object entity) { return tickCount; }

    private void syncPhase() { entityData.set(FASE, attack.phase().ordinal()); }

    private float danoDoServidor(DamageSource source, float amount) {
        if (level().isClientSide || !(source.getEntity() instanceof LivingEntity atacante)
                || !Float.isFinite(amount) || amount <= 0.0F) return amount;
        Vec3 olho = atacante.getEyePosition();
        Vec3 fim = olho.add(atacante.getLookAngle().scale(WEAK_POINT_TRACE));
        Vec3 impacto = getBoundingBox().clip(olho, fim).orElse(source.getSourcePosition() != null
                ? source.getSourcePosition() : olho);
        double altura = Mth.clamp((impacto.y - getY()) / getBbHeight(), 0.0D, 1.0D);
        Vec3 olhar = getLookAngle();
        Vec3 olharHorizontal = new Vec3(olhar.x, 0.0D, olhar.z);
        Vec3 ateAtacante = new Vec3(atacante.getX() - getX(), 0.0D, atacante.getZ() - getZ());
        double frente = olharHorizontal.lengthSqr() < 1.0E-6D || ateAtacante.lengthSqr() < 1.0E-6D
                ? 0.0D : olharHorizontal.normalize().dot(ateAtacante.normalize());
        String regiao = WEAK_POINT_RESOLVER.resolver(altura, Mth.clamp(frente, -1.0D, 1.0D));
        return WEAK_POINTS.damage(amount, regiao);
    }

    private PerceptionSnapshot snapshot(LivingEntity target) {
        if (target == null) return null;
        Vec3 toTarget = target.position().subtract(position());
        Vec3 horizontal = new Vec3(toTarget.x, 0.0D, toTarget.z);
        Vec3 look = getLookAngle();
        Vec3 lookHorizontal = new Vec3(look.x, 0.0D, look.z);
        double dot = horizontal.lengthSqr() < 1.0E-6D || lookHorizontal.lengthSqr() < 1.0E-6D
                ? 0.0D : lookHorizontal.normalize().dot(horizontal.normalize());
        return new PerceptionSnapshot(target.getUUID(), level().dimension().location().toString(),
                distanceTo(target), dot, hasLineOfSight(target), false, false, target.isAlive(),
                distanceTo(target) <= RANGE, EnemyFaction.CIVILIAN);
    }
    private static AttackPhase phaseFrom(int ordinal) {
        AttackPhase[] phases = AttackPhase.values();
        return ordinal >= 0 && ordinal < phases.length ? phases[ordinal] : AttackPhase.IDLE;
    }
}
