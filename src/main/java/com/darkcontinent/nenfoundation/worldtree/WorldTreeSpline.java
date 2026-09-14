package com.darkcontinent.nenfoundation.worldtree;

import java.util.List;
import java.util.Objects;

/** Spline cubica deterministica usada para raizes e galhos principais. */
public record WorldTreeSpline(List<WorldTreePoint> controlPoints, double startRadius, double endRadius) {
    public WorldTreeSpline {
        controlPoints = List.copyOf(Objects.requireNonNull(controlPoints, "controlPoints"));
        if (controlPoints.size() != 4) {
            throw new IllegalArgumentException("World Tree splines precisam de quatro pontos de controle");
        }
        if (!Double.isFinite(startRadius) || !Double.isFinite(endRadius)
                || startRadius <= 0.0 || endRadius <= 0.0 || endRadius > startRadius) {
            throw new IllegalArgumentException("raios de spline invalidos");
        }
    }

    /**
     * O ponto da curva em {@code t}.
     *
     * <p>A CONTA MUDOU DE CASA, e o motivo importa. Ela morava em
     * {@code WorldTreeRootGenerator}, que importa {@code ChunkAccess} -- ou
     * seja, quem quisesse RACIOCINAR sobre a geometria da arvore sem escrever
     * bloco nenhum precisava arrastar o Minecraft junto. O plano de folhagem
     * precisa exatamente disso: decidir onde ha copa antes de existir chunk.
     *
     * <p>O gerador continua chamando {@code bezier(spline, t)}; aquele metodo
     * agora delega para este. Duas implementacoes da mesma curva divergiriam, e
     * a divergencia apareceria como folha ao lado do galho -- que e o defeito
     * que este pacote acabou de consertar.
     */
    public WorldTreePoint pointAt(double t) {
        double inverse = 1.0 - t;
        WorldTreePoint p0 = controlPoints.get(0);
        WorldTreePoint p1 = controlPoints.get(1);
        WorldTreePoint p2 = controlPoints.get(2);
        WorldTreePoint p3 = controlPoints.get(3);
        double a = inverse * inverse * inverse;
        double b = 3 * inverse * inverse * t;
        double c = 3 * inverse * t * t;
        double d = t * t * t;
        return new WorldTreePoint(
                a * p0.x() + b * p1.x() + c * p2.x() + d * p3.x(),
                a * p0.y() + b * p1.y() + c * p2.y() + d * p3.y(),
                a * p0.z() + b * p1.z() + c * p2.z() + d * p3.z());
    }

    /** O raio do galho em {@code t}: linear entre as duas pontas. */
    public double radiusAt(double t) {
        return startRadius + (endRadius - startRadius) * t;
    }

    /** O rumo horizontal da curva, do primeiro ao ultimo ponto de controle. */
    public double headingRadians() {
        WorldTreePoint first = controlPoints.get(0);
        WorldTreePoint last = controlPoints.get(3);
        return Math.atan2(last.z() - first.z(), last.x() - first.x());
    }
}
