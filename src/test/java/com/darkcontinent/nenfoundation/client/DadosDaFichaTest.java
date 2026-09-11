package com.darkcontinent.nenfoundation.client;

import static org.junit.jupiter.api.Assertions.*;

import com.darkcontinent.nenfoundation.client.screen.DadosDaFicha;
import com.darkcontinent.nenfoundation.client.screen.DadosDaFicha.Tipo;
import com.darkcontinent.nenfoundation.nen.category.NenCategory;
import com.darkcontinent.nenfoundation.network.payload.SnapshotDePerfilS2C;
import java.util.List;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

/** Prova a projecao usada pela tela, nao o desenho nem um socket de Minecraft. */
class DadosDaFichaTest {
    private final NenClientCache cache = new NenClientCache(() -> 0L, () -> false);
    private final DadosDaFicha ficha = new DadosDaFicha(this.cache);

    @Test
    void ausenciaDePerfilNaoEPerfilVazioRecebido() {
        assertTrue(this.ficha.perfil().isEmpty());
        assertTrue(this.ficha.entradas().isEmpty());
        this.cache.aoReceberSnapshot(perfil(Set.of(), Set.of(), Set.of()));
        assertTrue(this.ficha.perfil().isPresent());
        assertTrue(this.ficha.entradas().isEmpty());
        assertSame(NenCategory.UNDETERMINED, this.ficha.perfil().orElseThrow().categoriaVisivel());
    }

    @Test
    void listaTudoIncluindoIdDeDebugSemInventarRegistro() {
        var idLongo = id("debug/uma_habilidade_que_ainda_nao_tem_motor");
        this.cache.aoReceberSnapshot(perfil(Set.of(id("zetsu"), id("ten")), Set.of(idLongo), Set.of(id("despertar"))));
        assertEquals(List.of(Tipo.TECNICA, Tipo.TECNICA, Tipo.HABILIDADE, Tipo.MARCO),
                this.ficha.entradas().stream().map(DadosDaFicha.Entrada::tipo).toList());
        assertEquals(List.of(id("ten"), id("zetsu"), idLongo, id("despertar")),
                this.ficha.entradas().stream().map(DadosDaFicha.Entrada::id).toList());
        assertThrows(UnsupportedOperationException.class, () -> this.ficha.entradas().clear());
    }

    @Test
    void mesmaFichaAcompanhaUnlockLockResetSemReabrir() {
        this.cache.aoReceberSnapshot(perfil(Set.of(id("ten")), Set.of(), Set.of()));
        assertEquals(1, this.ficha.entradas().size());
        this.cache.aoReceberSnapshot(perfil(Set.of(id("ten"), id("ren")), Set.of(id("teste")), Set.of()));
        assertEquals(3, this.ficha.entradas().size());
        this.cache.aoReceberSnapshot(perfil(Set.of(id("ren")), Set.of(), Set.of()));
        assertEquals(List.of(id("ren")), this.ficha.entradas().stream().map(DadosDaFicha.Entrada::id).toList());
        this.cache.aoReceberSnapshot(perfil(Set.of(), Set.of(), Set.of()));
        assertTrue(this.ficha.entradas().isEmpty());
    }

    @Test
    void logoutDescartaListaMesmoQueContagemRecomeceNaNovaSessao() {
        this.cache.aoReceberSnapshot(perfil(Set.of(id("ten")), Set.of(), Set.of()));
        assertEquals(1, this.ficha.entradas().size());
        this.cache.limpar();
        assertTrue(this.ficha.perfil().isEmpty());
        assertTrue(this.ficha.entradas().isEmpty());
        this.cache.aoReceberSnapshot(perfil(Set.of(), Set.of(id("outro_mundo")), Set.of()));
        assertEquals(List.of(id("outro_mundo")), this.ficha.entradas().stream().map(DadosDaFicha.Entrada::id).toList());
    }

    private static ResourceLocation id(String nome) { return ResourceLocation.fromNamespaceAndPath("nenfoundation", nome); }
    private static SnapshotDePerfilS2C perfil(Set<ResourceLocation> tecnicas, Set<ResourceLocation> habilidades, Set<ResourceLocation> marcos) {
        return new SnapshotDePerfilS2C(NenCategory.UNDETERMINED, tecnicas, habilidades, marcos);
    }
}
