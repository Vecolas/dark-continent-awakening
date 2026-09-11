package com.darkcontinent.nenfoundation.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.darkcontinent.nenfoundation.nen.aura.AuraPool;
import com.darkcontinent.nenfoundation.nen.category.NenCategory;
import com.darkcontinent.nenfoundation.nen.profile.PersistentNenData;
import com.darkcontinent.nenfoundation.nen.profile.RuntimeNenState;
import com.darkcontinent.nenfoundation.network.payload.SnapshotDePerfilS2C;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class NenSyncServiceTest {

    private static final ResourceLocation TEN = id("ten");
    private static final ResourceLocation DISPARO = id("disparo_de_aura");

    @Test
    @DisplayName("snapshot usa categoria visivel e nunca revela categoria oculta")
    void snapshotRespeitaRevelacao() {
        PersistentNenData oculto = perfil(NenCategory.SPECIALIZATION, false);
        PersistentNenData revelado = perfil(NenCategory.SPECIALIZATION, true);

        SnapshotDePerfilS2C snapshotOculto = NenSyncService.criarSnapshot(oculto);
        SnapshotDePerfilS2C snapshotRevelado = NenSyncService.criarSnapshot(revelado);

        assertSame(NenCategory.UNDETERMINED, snapshotOculto.categoriaVisivel());
        assertSame(NenCategory.SPECIALIZATION, snapshotRevelado.categoriaVisivel());
        assertEquals(Set.of(TEN), snapshotRevelado.tecnicasDesbloqueadas());
    }

    /** Parametros de teste sem config carregada: base 100, vigor desabilitado. */
    private static final AuraPool.Parametros P = AuraPool.Parametros.de(100.0D, 1.0D, 0.10D);

    @Test
    @DisplayName("delta copia o runtime no instante do envio")
    void deltaNaoFicaVivoComEstadoMutavel() {
        PersistentNenData pf = perfil(NenCategory.ENHANCEMENT, true);
        RuntimeNenState estado = new RuntimeNenState();
        // configura aura: resetar para max (110 = 100*(1+0.1 de potencial 10)) e gastar ate 12.5
        // Como auraPotential=10 => max = 100*(1+10) = 1100 neste perfil, mas usamos P.base = 100
        // => max = 100*(1+10) = 1100. Gastamos 1100 - 12.5 para ficar em 12.5
        estado.pool().resetarParaMaximo(pf, P);
        estado.pool().gastarAura(AuraPool.auraMaxima(pf, P) - 12.5D, pf, P);
        estado.ativarTecnica(TEN);
        estado.definirCooldown(DISPARO, 40);

        var delta = NenSyncService.criarDelta(estado, pf, P);
        // mutacoes apos o snapshot nao devem afetar o delta ja criado
        estado.pool().gastarAura(12.5D, pf, P);
        estado.desativarTecnica(TEN);
        estado.removerCooldown(DISPARO);

        assertEquals(12.5F, delta.aura(), 0.01F);
        assertEquals((float) AuraPool.auraMaxima(pf, P), delta.auraMaxima(), 0.01F);
        assertEquals(1.0F, delta.outputPercent(), 0.01F);
        assertEquals(Set.of(TEN), delta.tecnicasAtivas());
        assertEquals(Map.of(DISPARO, 40), delta.cooldowns());
    }

    @Test
    @DisplayName("entrega S2C chama o transporte uma vez e somente para o dono")
    void entregaTemUmDestino() {
        List<String> destinos = new ArrayList<>();
        SnapshotDePerfilS2C payload = NenSyncService.criarSnapshot(
                perfil(NenCategory.ENHANCEMENT, true));

        NenSyncService.entregarAoDono("jogador-a", payload,
                (destino, ignorado) -> destinos.add(destino));

        assertEquals(List.of("jogador-a"), destinos);
    }

    private static PersistentNenData perfil(NenCategory categoria, boolean revelada) {
        return new PersistentNenData(
                PersistentNenData.SCHEMA_ATUAL,
                true,
                categoria,
                revelada,
                10.0D,
                2.0D,
                3.0D,
                Map.of(),
                Set.of(TEN),
                Set.of(),
                Set.of());
    }

    private static ResourceLocation id(String caminho) {
        return ResourceLocation.fromNamespaceAndPath("nenfoundation", caminho);
    }
}
