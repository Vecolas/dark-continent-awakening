package com.darkcontinent.nenfoundation.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.darkcontinent.nenfoundation.nen.profile.NenProfileMigrator;
import com.darkcontinent.nenfoundation.nen.profile.PersistentNenData;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class NenProfileServiceTest {

    private static final ResourceLocation MARCO =
            ResourceLocation.fromNamespaceAndPath("nenfoundation", "teste_m1");

    @Test
    @DisplayName("jogador novo recebe perfil neutro sem escrita desnecessaria")
    void jogadorNovoRecebePerfilNeutro() {
        Memoria memoria = new Memoria(PersistentNenData.NAO_DESPERTADO);

        PersistentNenData lido = NenProfileService.ler(memoria);

        assertSame(PersistentNenData.NAO_DESPERTADO, lido);
        assertEquals(0, memoria.escritas);
    }

    @Test
    @DisplayName("leitura recusa schema futuro antes de expor o perfil")
    void leituraPassaPeloMigrador() {
        PersistentNenData futuro = copiarCom(
                PersistentNenData.NAO_DESPERTADO,
                PersistentNenData.SCHEMA_ATUAL + 1,
                Set.of());
        Memoria memoria = new Memoria(futuro);

        assertThrows(NenProfileMigrator.SchemaDoFuturoException.class,
                () -> NenProfileService.ler(memoria));
        assertEquals(0, memoria.escritas,
                "Dado futuro nao pode ser regravado por um JAR antigo.");
    }

    @Test
    @DisplayName("mutacao grava somente no perfil do jogador recebido")
    void mutacaoNaoVazaEntreJogadores() {
        Memoria primeiro = new Memoria(PersistentNenData.NAO_DESPERTADO);
        Memoria segundo = new Memoria(PersistentNenData.NAO_DESPERTADO);

        PersistentNenData alterado = NenProfileService.atualizar(
                primeiro,
                atual -> copiarCom(atual, atual.schemaVersion(), Set.of(MARCO)));

        assertEquals(Set.of(MARCO), alterado.progressionFlags());
        assertEquals(Set.of(MARCO), primeiro.perfil.progressionFlags());
        assertEquals(Set.of(), segundo.perfil.progressionFlags());
        assertEquals(1, primeiro.escritas);
        assertEquals(0, segundo.escritas);
    }

    @Test
    @DisplayName("mutacao sem mudanca nao marca o attachment para escrita")
    void mutacaoIdenticaNaoEscreve() {
        Memoria memoria = new Memoria(PersistentNenData.NAO_DESPERTADO);

        PersistentNenData resultado = NenProfileService.atualizar(memoria, atual -> atual);

        assertSame(PersistentNenData.NAO_DESPERTADO, resultado);
        assertEquals(0, memoria.escritas);
    }

    @Test
    @DisplayName("mutacao nula falha sem apagar o perfil atual")
    void mutacaoNulaERecusada() {
        Memoria memoria = new Memoria(PersistentNenData.NAO_DESPERTADO);

        assertThrows(NullPointerException.class,
                () -> NenProfileService.atualizar(memoria, atual -> null));
        assertSame(PersistentNenData.NAO_DESPERTADO, memoria.perfil);
        assertEquals(0, memoria.escritas);
    }

    private static PersistentNenData copiarCom(
            PersistentNenData base, int schema, Set<ResourceLocation> marcos) {
        return new PersistentNenData(
                schema,
                base.awakened(),
                base.category(),
                base.categoryRevealed(),
                base.auraPotential(),
                base.control(),
                base.output(),
                Map.copyOf(base.techniqueProficiency()),
                Set.copyOf(base.unlockedTechniques()),
                Set.copyOf(base.unlockedAbilities()),
                new HashSet<>(marcos));
    }

    private static final class Memoria implements NenProfileService.Armazenamento {
        private PersistentNenData perfil;
        private int escritas;

        private Memoria(PersistentNenData perfil) {
            this.perfil = perfil;
        }

        @Override
        public PersistentNenData ler() {
            return this.perfil;
        }

        @Override
        public void gravar(PersistentNenData novoPerfil) {
            this.perfil = novoPerfil;
            this.escritas++;
        }
    }
}
