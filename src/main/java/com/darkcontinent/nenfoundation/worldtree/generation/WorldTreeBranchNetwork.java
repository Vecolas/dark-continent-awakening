package com.darkcontinent.nenfoundation.worldtree.generation;

import com.darkcontinent.nenfoundation.worldtree.WorldTreeBranchNode;
import com.darkcontinent.nenfoundation.worldtree.WorldTreePoint;
import com.darkcontinent.nenfoundation.worldtree.WorldTreeSpline;
import com.darkcontinent.nenfoundation.worldtree.WorldTreeTrunkProfile;
import java.util.ArrayList;
import java.util.List;

/** Builds and validates the deterministic parent-child branch graph. */
public final class WorldTreeBranchNetwork {
    private static final double GOLDEN_ANGLE = Math.toRadians(137.5);
    /** Keeps the final voxelized branch visibly thicker than a one-block twig. */
    public static final double MIN_TIP_RADIUS = 3.0;

    private WorldTreeBranchNetwork() {
    }

    public static List<WorldTreeBranchNode> generateGraph(long seed,
            WorldTreeTrunkProfile trunk, List<WorldTreeSpline> primaryBranches) {
        List<MutableNode> mutable = new ArrayList<>();
        for (int i = 0; i < primaryBranches.size(); i++) {
            WorldTreeSpline spline = primaryBranches.get(i);
            WorldTreePoint start = spline.controlPoints().get(0);
            mutable.add(new MutableNode(i, -1, 1,
                    attachmentT(trunk, start.y()), start, tangent(spline, 0.0), spline,
                    spline.startRadius(), spline.endRadius()));
        }

        int primaryCount = mutable.size();
        for (int primaryId = 0; primaryId < primaryCount; primaryId++) {
            createChildren(mutable, mutable.get(primaryId), seed, 2, 5);
        }
        int orderTwoEnd = mutable.size();
        for (int id = primaryCount; id < orderTwoEnd; id++) {
            createChildren(mutable, mutable.get(id), seed, 3, 2);
        }
        int orderThreeEnd = mutable.size();
        for (int id = orderTwoEnd; id < orderThreeEnd; id++) {
            // Terminal branches are sparse and small; they are silhouette supports,
            // not a second layer of large tubes.
            if (Math.floorMod(mix(seed, id, 91), 3) != 0) {
                createChildren(mutable, mutable.get(id), seed, 4, 1);
            }
        }

        List<WorldTreeBranchNode> result = new ArrayList<>(mutable.size());
        for (MutableNode node : mutable) {
            result.add(node.freeze());
        }
        validate(result);
        return List.copyOf(result);
    }

    /** Compatibility view used by older generators; the graph remains authoritative. */
    public static List<WorldTreeSpline> secondaryAndTertiary(
            com.darkcontinent.nenfoundation.worldtree.WorldTreeLayout layout) {
        return layout.branchNodes().stream()
                .filter(node -> !node.isPrimary())
                .map(WorldTreeBranchNode::spline)
                .toList();
    }

    private static void createChildren(List<MutableNode> nodes, MutableNode parent,
            long seed, int order, int requestedCount) {
        double firstT = order == 2 ? 0.32 : order == 3 ? 0.50 : 0.72;
        double step = order == 2 ? 0.105 : order == 3 ? 0.27 : 0.0;
        for (int childIndex = 0; childIndex < requestedCount; childIndex++) {
            double t = Math.min(0.91, firstT + childIndex * step
                    + signed(mix(seed, parent.id, childIndex) >>> 17) * (order == 2 ? 0.025 : 0.04));
            WorldTreePoint attachment = bezier(parent.spline, t);
            WorldTreePoint parentTangent = tangent(parent.spline, t);
            WorldTreePoint direction = childDirection(parentTangent, attachment,
                    seed, parent.id, childIndex, order);
            double parentRadius = radiusAt(parent.spline, t);
            double ratio = radiusRatio(seed, parent.id, childIndex, order);
            double startRadius = Math.max(0.8, parentRadius * ratio);
            if (startRadius <= MIN_TIP_RADIUS) {
                continue;
            }
            double endRadius = Math.max(MIN_TIP_RADIUS,
                    startRadius * (0.18 + unit(mix(seed, parent.id, childIndex) >>> 41) * 0.14));
            double length = branchLength(seed, parent.id, childIndex, order, startRadius);
            WorldTreeSpline spline = childSpline(attachment, direction, length,
                    startRadius, endRadius, order);
            MutableNode candidate = new MutableNode(nodes.size(), parent.id, order, t,
                    attachment, direction, spline, startRadius,
                    endRadius);
            if (!validCandidate(candidate, parent, nodes)) {
                continue;
            }
            nodes.add(candidate);
            parent.children.add(candidate.id);
            if (parent.mainChildId < 0 || childIndex == 0) {
                parent.mainChildId = candidate.id;
            }
        }
    }

    private static WorldTreePoint childDirection(WorldTreePoint parentTangent,
            WorldTreePoint attachment, long seed, int parentId, int childIndex, int order) {
        double tangentX = parentTangent.x();
        double tangentY = parentTangent.y();
        double tangentZ = parentTangent.z();
        double horizontal = Math.max(0.001, Math.hypot(tangentX, tangentZ));
        double outwardX = attachment.x() / Math.max(1.0, Math.hypot(attachment.x(), attachment.z()));
        double outwardZ = attachment.z() / Math.max(1.0, Math.hypot(attachment.x(), attachment.z()));
        double sideX = -outwardZ;
        double sideZ = outwardX;
        double azimuth = childIndex * GOLDEN_ANGLE
                + signed(mix(seed, parentId, childIndex) >>> 7) * Math.toRadians(15.0);
        double side = Math.cos(azimuth) * 0.32;
        double forward = childIndex == 0 ? 0.62 : 0.28;
        double upward = 0.16 + Math.min(0.56, Math.max(0.0, attachment.y() / 1536.0) * 0.56)
                + (order - 2) * 0.04;
        double x = tangentX / horizontal * forward + outwardX * 0.30 + sideX * side;
        double z = tangentZ / horizontal * forward + outwardZ * 0.30 + sideZ * side;
        double y = tangentY * 0.15 + upward;
        return normalize(x, y, z);
    }

    private static WorldTreeSpline childSpline(WorldTreePoint attachment,
            WorldTreePoint direction, double length, double startRadius, double endRadius,
            int order) {
        double collar = startRadius * 0.35;
        WorldTreePoint start = plus(attachment, scale(direction, -collar));
        double sag = length * startRadius * 0.0025 * (order <= 2 ? 1.0 : 0.65);
        WorldTreePoint p1 = plus(start, scale(direction, length * 0.28));
        WorldTreePoint p2 = plus(start, scale(direction, length * 0.66));
        WorldTreePoint p3 = plus(start, scale(direction, length));
        p2 = new WorldTreePoint(p2.x(), p2.y() - sag, p2.z());
        p3 = new WorldTreePoint(p3.x(), p3.y() - sag * 1.5, p3.z());
        return new WorldTreeSpline(List.of(start, p1, p2, p3), startRadius, endRadius);
    }

    private static boolean validCandidate(MutableNode candidate, MutableNode parent,
            List<MutableNode> existing) {
        if (candidate.startRadius > radiusAt(parent.spline, candidate.attachmentT) * 0.65) {
            return false;
        }
        if (candidate.endRadius >= candidate.startRadius) {
            return false;
        }
        double outward = candidate.attachmentPosition.x() * candidate.attachmentDirection.x()
                + candidate.attachmentPosition.z() * candidate.attachmentDirection.z();
        if (outward < -0.15) {
            return false;
        }
        for (MutableNode other : existing) {
            if (other.id == parent.id) {
                continue;
            }
            if (near(candidate.spline, other.spline,
                    (candidate.startRadius + other.startRadius) * 0.42)) {
                return false;
            }
        }
        return true;
    }

    private static boolean near(WorldTreeSpline first, WorldTreeSpline second, double minimum) {
        double minimumSquared = minimum * minimum;
        for (int i = 0; i <= 8; i++) {
            WorldTreePoint a = bezier(first, i / 8.0);
            for (int j = 0; j <= 8; j++) {
                WorldTreePoint b = bezier(second, j / 8.0);
                double dx = a.x() - b.x();
                double dy = a.y() - b.y();
                double dz = a.z() - b.z();
                if (dx * dx + dy * dy + dz * dz < minimumSquared) {
                    return true;
                }
            }
        }
        return false;
    }

    private static void validate(List<WorldTreeBranchNode> nodes) {
        for (WorldTreeBranchNode node : nodes) {
            if (node.isPrimary()) {
                continue;
            }
            WorldTreeBranchNode parent = nodes.get(node.parentId());
            if (node.startRadius() > radiusAt(parent.spline(), node.attachmentT()) * 0.65) {
                throw new IllegalStateException("galho filho mais grosso que o pai");
            }
            if (!parent.children().contains(node.id())) {
                throw new IllegalStateException("galho fora do indice de filhos");
            }
        }
    }

    static double radiusRatio(long seed, int parentId, int childId, int order) {
        double min = order == 2 ? 0.45 : order == 3 ? 0.30 : 0.20;
        double max = order == 2 ? 0.60 : order == 3 ? 0.50 : 0.40;
        return min + unit(mix(seed, parentId, childId)) * (max - min);
    }

    private static double branchLength(long seed, int parentId, int childId,
            int order, double radius) {
        double min = order == 2 ? 45.0 : order == 3 ? 22.0 : 12.0;
        double max = order == 2 ? 90.0 : order == 3 ? 48.0 : 30.0;
        return min + unit(mix(seed, parentId, childId) >>> 23) * (max - min)
                + radius * 0.5;
    }

    private static double attachmentT(WorldTreeTrunkProfile trunk, double y) {
        return Math.max(0.0, Math.min(1.0, (y - trunk.baseY())
                / (double) (trunk.topY() - trunk.baseY())));
    }

    // AS DUAS CHAMAM A PROPRIA SPLINE, e nao o gerador de raizes.
    //
    // `WorldTreeRootGenerator` importa ChunkAccess. Enquanto a rede de galhos
    // passava por ele, ESTA classe -- que e matematica de layout, sem um bloco
    // sequer -- so podia ser executada com o Minecraft de pe. O sintoma
    // concreto: o desenhador de silhueta, que le o layout para conferir a forma
    // da arvore antes de subir o jogo, morria com NoClassDefFoundError.
    static WorldTreePoint bezier(WorldTreeSpline spline, double t) {
        return spline.pointAt(t);
    }

    static double radiusAt(WorldTreeSpline spline, double t) {
        return spline.radiusAt(t);
    }

    public static WorldTreePoint tangentAt(WorldTreeSpline spline, double t) {
        return tangent(spline, t);
    }

    private static WorldTreePoint tangent(WorldTreeSpline spline, double t) {
        List<WorldTreePoint> p = spline.controlPoints();
        double u = 1.0 - t;
        return normalize(
                3.0 * u * u * (p.get(1).x() - p.get(0).x())
                        + 6.0 * u * t * (p.get(2).x() - p.get(1).x())
                        + 3.0 * t * t * (p.get(3).x() - p.get(2).x()),
                3.0 * u * u * (p.get(1).y() - p.get(0).y())
                        + 6.0 * u * t * (p.get(2).y() - p.get(1).y())
                        + 3.0 * t * t * (p.get(3).y() - p.get(2).y()),
                3.0 * u * u * (p.get(1).z() - p.get(0).z())
                        + 6.0 * u * t * (p.get(2).z() - p.get(1).z())
                        + 3.0 * t * t * (p.get(3).z() - p.get(2).z()));
    }

    private static WorldTreePoint normalize(double x, double y, double z) {
        double length = Math.sqrt(x * x + y * y + z * z);
        return new WorldTreePoint(x / length, y / length, z / length);
    }

    private static WorldTreePoint plus(WorldTreePoint a, WorldTreePoint b) {
        return new WorldTreePoint(a.x() + b.x(), a.y() + b.y(), a.z() + b.z());
    }

    private static WorldTreePoint scale(WorldTreePoint point, double amount) {
        return new WorldTreePoint(point.x() * amount, point.y() * amount, point.z() * amount);
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

    private static final class MutableNode {
        private final int id;
        private final int parentId;
        private final int order;
        private final double attachmentT;
        private final WorldTreePoint attachmentPosition;
        private final WorldTreePoint attachmentDirection;
        private final WorldTreeSpline spline;
        private final double startRadius;
        private final double endRadius;
        private final List<Integer> children = new ArrayList<>();
        private int mainChildId = -1;

        private MutableNode(int id, int parentId, int order, double attachmentT,
                WorldTreePoint attachmentPosition, WorldTreePoint attachmentDirection,
                WorldTreeSpline spline, double startRadius, double endRadius) {
            this.id = id;
            this.parentId = parentId;
            this.order = order;
            this.attachmentT = attachmentT;
            this.attachmentPosition = attachmentPosition;
            this.attachmentDirection = attachmentDirection;
            this.spline = spline;
            this.startRadius = startRadius;
            this.endRadius = endRadius;
        }

        private WorldTreeBranchNode freeze() {
            return new WorldTreeBranchNode(id, parentId, order, attachmentT,
                    attachmentPosition, attachmentDirection, spline,
                    startRadius, endRadius, children, mainChildId);
        }
    }
}
