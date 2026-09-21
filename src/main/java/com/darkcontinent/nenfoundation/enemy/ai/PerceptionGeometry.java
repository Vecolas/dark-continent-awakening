package com.darkcontinent.nenfoundation.enemy.ai;

import net.minecraft.world.phys.Vec3;

/** Geometria pura do cone; linha de visao continua sendo validada pelo servidor. */
public final class PerceptionGeometry {
    private PerceptionGeometry() { }

    public static boolean insideCone(Vec3 observerEye, Vec3 lookDirection,
            Vec3 targetCenter, double maxDistance, double halfAngleDegrees) {
        if (observerEye == null || lookDirection == null || targetCenter == null
                || !Double.isFinite(maxDistance) || maxDistance <= 0.0
                || !Double.isFinite(halfAngleDegrees) || halfAngleDegrees <= 0.0
                || halfAngleDegrees > 180.0) return false;
        Vec3 toTarget = targetCenter.subtract(observerEye);
        double distanceSqr = toTarget.lengthSqr();
        if (!Double.isFinite(distanceSqr) || distanceSqr <= 1.0E-8
                || distanceSqr > maxDistance * maxDistance
                || lookDirection.lengthSqr() <= 1.0E-8) return false;
        double cosine = lookDirection.normalize().dot(toTarget.normalize());
        return cosine >= Math.cos(Math.toRadians(halfAngleDegrees));
    }
}
