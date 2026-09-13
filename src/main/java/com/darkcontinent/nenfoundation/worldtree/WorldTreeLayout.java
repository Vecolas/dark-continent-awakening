package com.darkcontinent.nenfoundation.worldtree;

import java.util.List;
import java.util.Objects;

/**
 * Contrato imutavel do layout da World Tree.
 *
 * <p>Este objeto descreve geometria e marcos, nao blocos. A mesma instancia
 * conceitual pode ser consultada pelo Overworld e pela dimensao dedicada sem
 * depender da ordem em que chunks foram solicitados.
 */
public record WorldTreeLayout(
        long seed,
        int generationVersion,
        int overworldOriginX,
        int overworldOriginZ,
        WorldTreeTrunkProfile trunk,
        List<WorldTreeSpline> roots,
        List<WorldTreeSpline> branches,
        List<WorldTreeLandmark> landmarks) {

    public WorldTreeLayout {
        if (generationVersion <= 0) {
            throw new IllegalArgumentException("generationVersion deve ser positivo");
        }
        trunk = Objects.requireNonNull(trunk, "trunk");
        roots = List.copyOf(Objects.requireNonNull(roots, "roots"));
        branches = List.copyOf(Objects.requireNonNull(branches, "branches"));
        landmarks = List.copyOf(Objects.requireNonNull(landmarks, "landmarks"));
        if (roots.size() < 8 || roots.size() > 14) {
            throw new IllegalArgumentException("layout precisa de 8 a 14 raizes principais");
        }
        if (branches.size() < 3) {
            throw new IllegalArgumentException("layout precisa de galhos principais");
        }
    }
}
