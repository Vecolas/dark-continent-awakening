package com.darkcontinent.nenfoundation.worldtree.generation;

import com.darkcontinent.nenfoundation.worldtree.WorldTreeLayout;
import com.darkcontinent.nenfoundation.worldtree.WorldTreePoint;
import com.darkcontinent.nenfoundation.worldtree.WorldTreeSpline;
import java.util.ArrayList;
import java.util.List;

/** Rede determinística de subgalhos e sub-subgalhos derivada dos galhos major. */
public final class WorldTreeBranchNetwork {
    private WorldTreeBranchNetwork() {
    }

    public static List<WorldTreeSpline> secondaryAndTertiary(WorldTreeLayout layout) {
        List<WorldTreeSpline> result = new ArrayList<>();
        for (int majorId = 0; majorId < layout.branches().size(); majorId++) {
            WorldTreeSpline major = layout.branches().get(majorId);
            List<WorldTreeSpline> secondary = deriveChildren(major, layout.seed(), majorId,
                    3, 0.34, 28.0, 78.0, 0.48, 5.0, 3.0);
            result.addAll(secondary);
            for (int secondaryId = 0; secondaryId < secondary.size(); secondaryId++) {
                result.addAll(deriveChildren(secondary.get(secondaryId), layout.seed(),
                        majorId * 17 + secondaryId, 2, 0.48, 16.0, 42.0,
                        0.44, 3.2, 1.8));
            }
        }
        return List.copyOf(result);
    }

    static List<WorldTreeSpline> deriveChildren(WorldTreeSpline parent, long seed, int id,
            int count, double firstT, double minLength, double maxLength,
            double radiusFactor, double minimumStartRadius, double endRadius) {
        List<WorldTreeSpline> children = new ArrayList<>(count);
        WorldTreePoint first = parent.controlPoints().get(0);
        WorldTreePoint last = parent.controlPoints().get(3);
        double parentAngle = Math.atan2(last.z() - first.z(), last.x() - first.x());
        for (int child = 0; child < count; child++) {
            double t = firstT + child * 0.19;
            // A CURVA VEM DA PROPRIA SPLINE, e nao do gerador de raizes.
            // Chamar `WorldTreeRootGenerator.bezier` aqui arrastava uma classe
            // que importa ChunkAccess para dentro do calculo de layout -- e com
            // isso a rede de galhos, que e matematica pura, so podia ser
            // executada com o Minecraft de pe.
            WorldTreePoint origin = parent.pointAt(t);
            long mixed = mix(seed, id, child);
            double angle = parentAngle + (-0.72 + child * 0.66)
                    + signed(mixed >>> 8) * 0.16;
            double length = minLength + unit(mixed) * (maxLength - minLength);
            double dx = Math.cos(angle);
            double dz = Math.sin(angle);
            double lift = 5.0 + unit(mixed >>> 16) * 18.0;
            children.add(new WorldTreeSpline(List.of(
                    origin,
                    new WorldTreePoint(origin.x() + dx * length * 0.28,
                            origin.y() + lift * 0.25, origin.z() + dz * length * 0.28),
                    new WorldTreePoint(origin.x() + dx * length * 0.66,
                            origin.y() + lift * 0.68, origin.z() + dz * length * 0.66),
                    new WorldTreePoint(origin.x() + dx * length,
                            origin.y() + lift, origin.z() + dz * length)),
                    Math.max(minimumStartRadius, parent.startRadius() * radiusFactor), endRadius));
        }
        return children;
    }

    private static double unit(long value) {
        return (value & 0xFFFFL) / 65535.0;
    }

    private static double signed(long value) {
        return unit(value) * 2.0 - 1.0;
    }

    private static long mix(long seed, int id, int child) {
        long value = seed ^ ((long) id * 0x9E3779B97F4A7C15L)
                ^ ((long) child * 0xBF58476D1CE4E5B9L);
        value = (value ^ (value >>> 30)) * 0xBF58476D1CE4E5B9L;
        value = (value ^ (value >>> 27)) * 0x94D049BB133111EBL;
        return value ^ (value >>> 31);
    }
}
