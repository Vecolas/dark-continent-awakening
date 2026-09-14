package com.darkcontinent.nenfoundation.worldtree.generation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * ESTE ARQUIVO EXISTIA DUAS VEZES, e a suite inteira estava vermelha por causa
 * disso.
 *
 * <p>Havia um {@code WorldTreeTrunkGeneratorTest} em
 * {@code worldtree/generation/} e OUTRO em {@code worldtree/}, este segundo com
 * a pasta errada mas o mesmo {@code package} -- ou seja, duas classes com o
 * mesmo nome qualificado. {@code compileTestJava} reprovava com
 * "duplicate class", e como reprova na COMPILACAO, nenhum teste do repositorio
 * rodava. Os dois conjuntos de asserçoes estao aqui, fundidos.
 */
class WorldTreeTrunkGeneratorTest {

    @Test
    @DisplayName("a base comeca logo acima da bedrock")
    void baseComecaImediatamenteAcimaDaBedrock() {
        assertEquals(-63, WorldTreeTrunkGenerator.overworldBottomY(-64));
        assertEquals(1, WorldTreeTrunkGenerator.overworldBottomY(0));
    }

    @Test
    @DisplayName("chunk distante sai antes do laco vertical")
    void chunksDistantesSaoDescartadosAntesDoLoopVertical() {
        assertFalse(WorldTreeTrunkGenerator.chunkIntersectsTrunk(160, 176, 0, 16));
        assertFalse(WorldTreeTrunkGenerator.chunkIntersectsTrunk(0, 16, -176, -160));
        assertTrue(WorldTreeTrunkGenerator.chunkIntersectsTrunk(-16, 0, -16, 0));
    }

    @Test
    @DisplayName("o raio irregular e reproduzivel por coordenada")
    void raioIrregularEReproduzivelPorCoordenada() {
        double primeiro = WorldTreeTrunkGenerator.irregularRadius(30.0, 12, 400, -7, 99L);
        double segundo = WorldTreeTrunkGenerator.irregularRadius(30.0, 12, 400, -7, 99L);
        double diferente = WorldTreeTrunkGenerator.irregularRadius(30.0, 13, 400, -7, 99L);

        assertEquals(primeiro, segundo);
        assertNotEquals(primeiro, diferente);
        assertTrue(primeiro > 0.0);
    }
}
