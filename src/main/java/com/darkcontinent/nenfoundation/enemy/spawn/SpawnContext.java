package com.darkcontinent.nenfoundation.enemy.spawn;

/** Snapshot ambiental fornecido pelo servidor ao validador, sem acesso a mundo no record. */
public record SpawnContext(String biomeTag, String dimension, int light, boolean onGround,
        boolean inWater, boolean skyVisible, int nearbySameFaction) {
    public SpawnContext {
        if (biomeTag == null || biomeTag.isBlank() || dimension == null || dimension.isBlank()
                || light < 0 || light > 15 || nearbySameFaction < 0) {
            throw new IllegalArgumentException("contexto de spawn invalido");
        }
    }
}
