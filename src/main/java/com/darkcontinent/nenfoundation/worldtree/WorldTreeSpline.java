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
}
