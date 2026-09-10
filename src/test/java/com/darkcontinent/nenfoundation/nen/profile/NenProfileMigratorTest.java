package com.darkcontinent.nenfoundation.nen.profile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.darkcontinent.nenfoundation.nen.category.NenCategory;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Portao do migrador.
 *
 * <p>Ele existe agora, com um unico schema publicado e nada para migrar, para
 * que a PRIMEIRA migracao real ja nasca com um portao que reprova. Escrever o
 * teste depois de existirem saves de jogadores significa escreve-lo sob pressao,
 * contra dados que ninguem pode perder.
 */
class NenProfileMigratorTest {

    private static PersistentNenData naVersao(int versao) {
        return new PersistentNenData(
                versao, true, NenCategory.EMISSION, true,
                10.0D, 0.5D, 0.5D, Map.of(), Set.of(), Set.of(), Set.of());
    }

    @Test
    @DisplayName("dado ja na versao atual atravessa sem copia")
    void versaoAtualPassaDireto() {
        PersistentNenData atual = naVersao(PersistentNenData.SCHEMA_ATUAL);
        assertSame(atual, NenProfileMigrator.migrar(atual));
    }

    @Test
    @DisplayName("save de uma versao FUTURA e recusado alto")
    void versaoFuturaRecusada() {
        PersistentNenData doFuturo = naVersao(PersistentNenData.SCHEMA_ATUAL + 1);
        NenProfileMigrator.SchemaDoFuturoException e = assertThrows(
                NenProfileMigrator.SchemaDoFuturoException.class,
                () -> NenProfileMigrator.migrar(doFuturo));
        assertEquals(true, e.getMessage().contains("Atualize o mod"),
                "A mensagem tem de dizer o que fazer. Abrir o mundo com um JAR"
                        + " antigo apaga os campos que ele nao conhece, em silencio.");
    }

    @Test
    @DisplayName("versao de schema que nunca existiu e recusada")
    void versaoZeroRecusada() {
        assertThrows(NenProfileMigrator.SchemaInvalidoException.class,
                () -> NenProfileMigrator.migrar(naVersao(0)));
        assertThrows(NenProfileMigrator.SchemaInvalidoException.class,
                () -> NenProfileMigrator.migrar(naVersao(-1)));
    }
}
