package com.darkcontinent.nenfoundation.enemy.ai;

import com.darkcontinent.nenfoundation.config.NenConfig;
import com.darkcontinent.nenfoundation.enemy.api.EnemyFaction;
import com.darkcontinent.nenfoundation.enemy.api.EnemyMetadata;
import com.darkcontinent.nenfoundation.enemy.api.HxHEnemy;
import com.darkcontinent.nenfoundation.enemy.faction.FactionRelations;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Sensor server-side comum: cone + LOS, ruído por evento, memoria expirada e
 * seleção limitada. O serviço não decide dano, aura ou estado de combate.
 */
public final class EnemyPerceptionService {
    private final PerceptionBudget budget = new PerceptionBudget();
    private final Map<UUID, HeardTarget> memory = new HashMap<>();
    private final FactionRelations relations = FactionRelations.padrao();
    private final AuraPerceptionExtension auraExtension;
    private EnemyPerceptionSnapshot snapshot = EnemyPerceptionSnapshot.empty();
    private long lastSignalTick = Long.MIN_VALUE;

    public EnemyPerceptionService() {
        this(AuraPerceptionExtension.inerte());
    }

    public EnemyPerceptionService(AuraPerceptionExtension auraExtension) {
        this.auraExtension = auraExtension == null ? AuraPerceptionExtension.inerte() : auraExtension;
    }

    public EnemyPerceptionSnapshot tick(Mob observer, EnemyMetadata metadata, Vec3 territoryCenter) {
        if (observer.level().isClientSide) return snapshot;
        PerceptionTuning tuning = NenConfig.enemyPerceptionTuning();
        long now = observer.level().getGameTime();
        if (!budget.due(now, tuning.scanIntervalTicks())) {
            return new EnemyPerceptionSnapshot(snapshot.target(), snapshot.targetVisible(),
                    snapshot.targetAudible(), snapshot.targetInTerritory(), snapshot.targetLost(),
                    snapshot.memoryTicksRemaining(), snapshot.audibleNoise(), false);
        }

        absorbSignals(observer.level(), observer.position(), tuning, now);
        memory.entrySet().removeIf(entry -> now - entry.getValue().lastRelevantTick()
                > tuning.memoryTicks());
        double searchRange = Math.max(tuning.visionRange(), tuning.hearingRange());
        AABB searchBox = observer.getBoundingBox().inflate(searchRange);
        var candidates = observer.level().getEntitiesOfClass(LivingEntity.class, searchBox,
                target -> target != observer && target.isAlive() && !target.isSpectator());
        LivingEntity selected = null;
        boolean selectedVisible = false;
        boolean selectedAudible = false;
        boolean selectedTerritory = false;
        double selectedDistance = Double.MAX_VALUE;
        for (LivingEntity candidate : PerceptionBudget.nearest(candidates, observer,
                tuning.maxCandidates())) {
            if (!auraExtension.canDetect(observer, candidate)) continue;
            double distance = observer.distanceTo(candidate);
            boolean sameDimension = observer.level().dimension().equals(candidate.level().dimension());
            boolean inTerritory = territoryCenter != null
                    && territoryCenter.distanceToSqr(candidate.position())
                    <= tuning.territoryRadius() * tuning.territoryRadius();
            EnemyFaction faction = factionOf(candidate);
            TargetEvaluation evaluation = TargetEvaluator.evaluate(metadata, faction, distance,
                    searchRange, sameDimension, inTerritory, relations);
            if (!evaluation.accepted()) continue;
            boolean visible = PerceptionGeometry.insideCone(observer.getEyePosition(),
                    observer.getLookAngle(), candidate.getEyePosition(), tuning.visionRange(),
                    tuning.visionHalfAngleDegrees()) && observer.hasLineOfSight(candidate);
            HeardTarget remembered = memory.get(candidate.getUUID());
            boolean audible = remembered != null && remembered.lastHeardTick() >= remembered.lastSeenTick();
            if (visible) {
                memory.merge(candidate.getUUID(), new HeardTarget(candidate.position(), now, -1),
                        (old, current) -> new HeardTarget(candidate.position(), now, old.lastHeardTick()));
                remembered = memory.get(candidate.getUUID());
            }
            if (!visible && !audible) continue;
            if (distance < selectedDistance || (visible && !selectedVisible)) {
                selected = candidate;
                selectedVisible = visible;
                selectedAudible = audible;
                selectedTerritory = inTerritory;
                selectedDistance = distance;
            }
        }
        HeardTarget heard = selected == null ? null : memory.get(selected.getUUID());
        int remaining = heard == null ? 0
                : Math.max(0, tuning.memoryTicks() - (int) (now - heard.lastRelevantTick()));
        Vec3 activeNoise = latestNoiseTick >= 0 && now - latestNoiseTick <= tuning.memoryTicks()
                ? latestNoise : null;
        snapshot = new EnemyPerceptionSnapshot(selected, selectedVisible, selectedAudible,
                selectedTerritory, selected == null, remaining,
                heard == null ? activeNoise : heard.position(), true);
        return snapshot;
    }

    private Vec3 latestNoise;
    private long latestNoiseTick = Long.MIN_VALUE;

    private void absorbSignals(Level level, Vec3 observerPosition, PerceptionTuning tuning, long now) {
        for (EnemyHearingSignal signal : EnemyHearingBus.recent(level, lastSignalTick + 1)) {
            lastSignalTick = Math.max(lastSignalTick, signal.gameTime());
            double range = Math.min(tuning.hearingRange(), signal.radius());
            if (observerPosition.distanceToSqr(signal.position()) > range * range) continue;
            latestNoise = signal.position();
            latestNoiseTick = now;
            if (signal.sourceId() != null) {
                memory.merge(signal.sourceId(), new HeardTarget(signal.position(), -1, now),
                        (old, current) -> new HeardTarget(signal.position(), old.lastSeenTick(), now));
            }
        }
    }

    private static EnemyFaction factionOf(LivingEntity target) {
        if (target instanceof HxHEnemy enemy) return enemy.enemyMetadata().faction();
        // Iron Golems are the dedicated non-player combat proxy used by the
        // server/GameTests and represent Hunter protection, not wildlife prey.
        return target instanceof Player || target instanceof IronGolem
                ? EnemyFaction.HUNTER_ASSOCIATION : EnemyFaction.CIVILIAN;
    }

    private record HeardTarget(Vec3 position, long lastSeenTick, long lastHeardTick) {
        private long lastRelevantTick() {
            return Math.max(lastSeenTick, lastHeardTick);
        }
    }
}
