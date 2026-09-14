package com.darkcontinent.nenfoundation.worldtree;

import java.util.ArrayList;
import java.util.List;
import java.util.SplittableRandom;
import com.darkcontinent.nenfoundation.worldtree.generation.WorldTreeBranchNetwork;
import com.darkcontinent.nenfoundation.worldtree.generation.WorldTreeFoliageAnchorGenerator;

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
        List<WorldTreeBranchNode> branchNodes = WorldTreeBranchNetwork.generateGraph(seed, trunk, branches);
        List<WorldTreeFoliageAnchor> foliageAnchors = WorldTreeFoliageAnchorGenerator.generate(
                seed, branchNodes);
        List<WorldTreeHollow> hollows = generateHollows(random);
        List<WorldTreeLandmark> landmarks = List.of(
                new WorldTreeLandmark("base", 48, 0.0, 0.0),
                new WorldTreeLandmark("cloud", 400, 0.0, 0.0),
                new WorldTreeLandmark("canopy", 900, 0.0, 0.0),
                new WorldTreeLandmark("crown", 1250, 0.0, 0.0),
                new WorldTreeLandmark("summit", 1450, 0.0, 0.0));
        return new WorldTreeLayout(seed, generationVersion, overworldOriginX, overworldOriginZ,
                trunk, roots, branches, branchNodes, foliageAnchors, hollows, landmarks);
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
                // A ALTURA E ESTRATIFICADA DENTRO DA ZONA, e nao sorteada livre.
                //
                // ISTO FOI UM DEFEITO REAL, achado pelo portao da copa. Com
                // sorteio livre, os galhos de uma zona se amontoam: na seed 1000
                // a zona MID_BOUGHS punha os seus entre y=576 e y=697 e a
                // HIGH_CANOPY comecava em y=957 -- 260 blocos de tronco sem UM
                // galho, e a copa com um vao de 179 blocos no meio, que le como
                // duas arvores empilhadas.
                //
                // Estratificar da a cada galho uma FATIA da zona e sorteia dentro
                // dela: a distribuicao continua irregular, e deixa de ter buraco.
                double y;
                if (zone == WorldTreeZone.SUMMIT) {
                    y = 1450.0;
                } else {
                    double baixo = zone.minY() + 20.0;
                    double alto = zone.maxYExclusive() - 20.0;
                    double fatia = (alto - baixo) / count;
                    y = baixo + fatia * i + random.nextDouble(fatia * 0.15, fatia * 0.85);
                }
                double length = random.nextDouble(80.0, 201.0);
                double dx = Math.cos(angle);
                double dz = Math.sin(angle);
                double localTrunkRadius = trunkRadiusAt(y);
                double startDistance = Math.max(0.0, localTrunkRadius * 0.45);
                double startRadius = localTrunkRadius * (zone == WorldTreeZone.SUMMIT
                        ? 0.48 : 0.56);
                double endRadius = Math.max(WorldTreeBranchNetwork.MIN_TIP_RADIUS,
                        startRadius * random.nextDouble(0.18, 0.32));
                branches.add(new WorldTreeSpline(List.of(
                        new WorldTreePoint(dx * startDistance, y, dz * startDistance),
                        new WorldTreePoint(dx * (startDistance + length * 0.26), y + random.nextDouble(-8.0, 12.0), dz * (startDistance + length * 0.26)),
                        new WorldTreePoint(dx * (startDistance + length * 0.62), y + random.nextDouble(8.0, 34.0),
                                dz * (startDistance + length * 0.62)),
                        new WorldTreePoint(dx * (startDistance + length), y + random.nextDouble(12.0, 48.0), dz * (startDistance + length))),
                        startRadius, endRadius));
            }
        }
        return branches;
    }

    private static double trunkRadiusAt(double y) {
        double t = Math.max(0.0, Math.min(1.0, (y - 48.0) / (1200.0 - 48.0)));
        return 30.0 + (14.0 - 30.0) * t;
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
