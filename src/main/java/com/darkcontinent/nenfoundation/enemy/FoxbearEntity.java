package com.darkcontinent.nenfoundation.enemy;

import com.darkcontinent.nenfoundation.registry.EnemyEntityTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import java.util.EnumSet;

/** Primeiro mob territorial; alvo, estado e dano são sempre decididos no servidor. */
public final class FoxbearEntity extends Animal {
    private static final TargetingConditions PLAYER_TARGET = TargetingConditions.forCombat()
            .range(FoxbearTerritory.TERRITORY_RADIUS);
    private FoxbearState state = FoxbearState.ROAM;
    private double warningDistance = Double.NaN;
    private int warningTicks;
    /** Quanto tempo o aviso dura antes de o Foxbear desistir e voltar. */
    private static final int TICKS_MAXIMOS_DE_AVISO = 80;
    public FoxbearEntity(EntityType<? extends FoxbearEntity> type, Level level) { super(type, level); }
    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 44.0D)
                .add(Attributes.ATTACK_DAMAGE, 6.0D).add(Attributes.MOVEMENT_SPEED, 0.25D)
                .add(Attributes.ARMOR, 2.0D).add(Attributes.FOLLOW_RANGE, FoxbearTerritory.TERRITORY_RADIUS);
    }
    @Override protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this)); goalSelector.addGoal(1, new TerritorialGoal(this));
        goalSelector.addGoal(2, new FoxbearAttackGoal(this));
        goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.8D));
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 8.0F));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
    }
    @Override public boolean isFood(net.minecraft.world.item.ItemStack stack) { return false; }
    @Override public @Nullable AgeableMob getBreedOffspring(ServerLevel level, AgeableMob parent) {
        return EnemyEntityTypes.FOXBEAR.get().create(level);
    }
    public FoxbearState state() { return state; }
    public boolean isWarning() { return state == FoxbearState.WARN; }
    private void setState(FoxbearState next) {
        if (state == next) return; state = next;
        if (next != FoxbearState.WARN) { warningDistance = Double.NaN; warningTicks = 0; }
    }
    private static final class TerritorialGoal extends Goal {
        private final FoxbearEntity foxbear; private Player player;
        private TerritorialGoal(FoxbearEntity foxbear) { this.foxbear = foxbear; setFlags(EnumSet.of(Flag.LOOK)); }
        @Override public boolean canUse() {
            if (foxbear.level().isClientSide || foxbear.state == FoxbearState.ENGAGE) return false;
            player = foxbear.level().getNearestPlayer(PLAYER_TARGET, foxbear); return player != null;
        }
        @Override public boolean canContinueToUse() {
            return player != null && player.isAlive() && foxbear.distanceToSqr(player)
                    <= FoxbearTerritory.TERRITORY_RADIUS * FoxbearTerritory.TERRITORY_RADIUS;
        }
        @Override public void start() {
            // `foxbear.` E OBRIGATORIO: esta Goal e static, e os campos de
            // aviso sao da ENTIDADE -- e tem de ser, porque a implementacao
            // seria compartilhada entre todos os Foxbears se morasse aqui.
            foxbear.warningDistance = foxbear.distanceTo(player);
            foxbear.setState(FoxbearState.WARN);
        }
        @Override public void tick() {
            if (player == null) return; double distance = foxbear.distanceTo(player);
            foxbear.getLookControl().setLookAt(player, 30.0F, 30.0F); foxbear.warningTicks++;
            boolean advanced = distance + FoxbearTerritory.ADVANCE_TOLERANCE < foxbear.warningDistance;
            boolean retreated = distance > foxbear.warningDistance + 1.0D
                    || foxbear.warningTicks > TICKS_MAXIMOS_DE_AVISO;
            FoxbearState next = FoxbearTerritory.stateFor(distance <= FoxbearTerritory.TERRITORY_RADIUS,
                    advanced, retreated, true); foxbear.setState(next);
            if (next == FoxbearState.ENGAGE) foxbear.setTarget(player);
            if (next == FoxbearState.RETURN_HOME) foxbear.setTarget(null);
        }
        @Override public void stop() { player = null; }
    }
    private static final class FoxbearAttackGoal extends MeleeAttackGoal {
        private final FoxbearEntity foxbear;
        private FoxbearAttackGoal(FoxbearEntity foxbear) { super(foxbear, 1.15D, true); this.foxbear = foxbear; }
        @Override public boolean canUse() { return foxbear.state == FoxbearState.ENGAGE && super.canUse(); }
        @Override public boolean canContinueToUse() { return foxbear.state == FoxbearState.ENGAGE && super.canContinueToUse(); }
    }
}
