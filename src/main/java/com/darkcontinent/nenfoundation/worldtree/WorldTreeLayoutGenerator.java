package com.darkcontinent.nenfoundation.worldtree;

import java.util.ArrayList;
import java.util.List;
import java.util.SplittableRandom;

/** Gera o contrato de layout sem estado global ou dependencia de chunks. */
public final class WorldTreeLayoutGenerator {
    public static final int CURRENT_GENERATION_VERSION = 1;

    private WorldTreeLayoutGenerator() {
    }

    public static WorldTreeLayout generate(long seed, int overworldOriginX, int overworldOriginZ) {
        return generate(seed, CURRENT_GENERATION_VERSION, overworldOriginX, overworldOriginZ);
    }

    public static WorldTreeLayout generate(long seed, int generationVersion,
            int overworldOriginX, int overworldOriginZ) {
        if (generationVersion != CURRENT_GENERATION_VERSION) {
            throw new IllegalArgumentException("versao de layout nao suportada: " + generationVersion);
        }

        SplittableRandom random = new SplittableRandom(mix(seed, generationVersion));
        WorldTreeTrunkProfile trunk = new WorldTreeTrunkProfile(48, 1200, 30.0, 14.0);
        List<WorldTreeSpline> roots = generateRoots(random);
        List<WorldTreeSpline> branches = generateBranches(random);
        List<WorldTreeHollow> hollows = generateHollows(random);
        List<WorldTreeLandmark> landmarks = List.of(
                new WorldTreeLandmark("base", 48, 0.0, 0.0),
                new WorldTreeLandmark("cloud", 400, 0.0, 0.0),
                new WorldTreeLandmark("canopy", 900, 0.0, 0.0),
                new WorldTreeLandmark("crown", 1250, 0.0, 0.0),
                new WorldTreeLandmark("summit", 1450, 0.0, 0.0));
        return new WorldTreeLayout(seed, generationVersion, overworldOriginX, overworldOriginZ,
                trunk, roots, branches, hollows, landmarks);
    }

    private static List<WorldTreeSpline> generateRoots(SplittableRandom random) {
        int count = 8 + random.nextInt(7);
        List<WorldTreeSpline> roots = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            double angle = (Math.PI * 2.0 * i / count) + random.nextDouble(-0.16, 0.16);
            double length = random.nextDouble(80.0, 141.0);
            double dx = Math.cos(angle);
            double dz = Math.sin(angle);
            roots.add(new WorldTreeSpline(List.of(
                    new WorldTreePoint(dx * 24.0, 48.0, dz * 24.0),
                    new WorldTreePoint(dx * 50.0, 43.0, dz * 50.0),
                    new WorldTreePoint(dx * length * 0.78, 39.0 + random.nextDouble(8.0),
                            dz * length * 0.78),
                    new WorldTreePoint(dx * length, 42.0 + random.nextDouble(10.0), dz * length)),
                    random.nextDouble(10.0, 22.0), random.nextDouble(2.0, 5.0)));
        }
        return roots;
    }

    private static List<WorldTreeSpline> generateBranches(SplittableRandom random) {
        WorldTreeZone[] branchZones = {
                WorldTreeZone.CLOUD_SEA,
                WorldTreeZone.MID_BOUGHS,
                WorldTreeZone.HIGH_CANOPY,
                WorldTreeZone.CROWN,
                WorldTreeZone.SUMMIT
        };
        List<WorldTreeSpline> branches = new ArrayList<>(branchZones.length * 5);
        for (WorldTreeZone zone : branchZones) {
            int count = 3 + random.nextInt(5);
            for (int i = 0; i < count; i++) {
                double angle = Math.PI * 2.0 * i / count + random.nextDouble(-0.25, 0.25);
                double y = random.nextDouble(zone.minY() + 20.0, zone.maxYExclusive() - 20.0);
            double length = random.nextDouble(80.0, 201.0);
            double dx = Math.cos(angle);
            double dz = Math.sin(angle);
                double startDistance = zone == WorldTreeZone.SUMMIT ? 0.0 : 12.0;
                branches.add(new WorldTreeSpline(List.of(
                        new WorldTreePoint(dx * startDistance, y, dz * startDistance),
                        new WorldTreePoint(dx * 35.0, y + random.nextDouble(-12.0, 18.0), dz * 35.0),
                        new WorldTreePoint(dx * length * 0.65, y + random.nextDouble(8.0, 42.0),
                                dz * length * 0.65),
                        new WorldTreePoint(dx * length, y + random.nextDouble(12.0, 55.0), dz * length)),
                        random.nextDouble(12.0, 30.0), random.nextDouble(4.0, 9.0)));
            }
        }
        return branches;
    }

    private static List<WorldTreeHollow> generateHollows(SplittableRandom random) {
        int count = 8 + random.nextInt(5);
        List<WorldTreeHollow> hollows = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            int y = 110 + i * 90 + random.nextInt(40);
            hollows.add(new WorldTreeHollow(y, random.nextDouble(-7.0, 7.0),
                    random.nextDouble(-7.0, 7.0), random.nextDouble(5.0, 9.0),
                    random.nextDouble(3.0, 7.0), random.nextDouble(5.0, 9.0)));
        }
        return hollows;
    }

    private static long mix(long seed, int version) {
        long value = seed ^ (0x9E3779B97F4A7C15L * version);
        value = (value ^ (value >>> 30)) * 0xBF58476D1CE4E5B9L;
        value = (value ^ (value >>> 27)) * 0x94D049BB133111EBL;
        return value ^ (value >>> 31);
    }
}
