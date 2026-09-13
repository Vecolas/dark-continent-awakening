package com.darkcontinent.nenfoundation.enemy.combat;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Hitbox local de ataque; a transformação para o mundo é responsabilidade do servidor. */
public record AttackHitbox(double minX, double minY, double minZ,
        double maxX, double maxY, double maxZ) {
    public AttackHitbox {
        if (!Double.isFinite(minX) || !Double.isFinite(minY) || !Double.isFinite(minZ)
                || !Double.isFinite(maxX) || !Double.isFinite(maxY) || !Double.isFinite(maxZ)
                || minX > maxX || minY > maxY || minZ > maxZ) {
            throw new IllegalArgumentException("hitbox de ataque invalida");
        }
    }

    /** Converte os oito vértices locais para uma AABB conservadora no mundo. */
    public AABB noMundo(Vec3 origem, float yawGraus) {
        if (origem == null || !Float.isFinite(yawGraus)) {
            throw new IllegalArgumentException("transformacao de hitbox invalida");
        }
        double angulo = Math.toRadians(yawGraus);
        double seno = Math.sin(angulo);
        double cosseno = Math.cos(angulo);
        double minX = Double.POSITIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY;
        double minZ = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY;
        double maxZ = Double.NEGATIVE_INFINITY;
        for (double x : new double[] { minX(), maxX() }) {
            for (double y : new double[] { minY(), maxY() }) {
                for (double z : new double[] { minZ(), maxZ() }) {
                    // Minecraft yaw 0 aponta para +Z; yaw positivo gira para -X.
                    double mundoX = origem.x + cosseno * x - seno * z;
                    double mundoZ = origem.z + seno * x + cosseno * z;
                    minX = Math.min(minX, mundoX);
                    maxX = Math.max(maxX, mundoX);
                    minY = Math.min(minY, origem.y + y);
                    maxY = Math.max(maxY, origem.y + y);
                    minZ = Math.min(minZ, mundoZ);
                    maxZ = Math.max(maxZ, mundoZ);
                }
            }
        }
        return new AABB(minX, minY, minZ, maxX, maxY, maxZ);
    }
}
