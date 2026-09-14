package com.darkcontinent.nenfoundation.enemy.encounter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.darkcontinent.nenfoundation.enemy.greedisland.DefeatResult;
import com.darkcontinent.nenfoundation.enemy.greedisland.GreedIslandRegion;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

/** Regressao do save em disco para desfecho, trava e contagem de cards. */
class EncounterSavedDataTest {
    @Test
    void encontroCardsETravasSobrevivemAoSave() {
        EncounterSavedData original = new EncounterSavedData();
        UUID encontroId = UUID.randomUUID();
        UUID entidadeId = UUID.randomUUID();
        UUID jogadorId = UUID.randomUUID();
        EncounterInstance instancia = new EncounterInstance(encontroId, "nenfoundation:cyclops",
                GreedIslandRegion.DIMENSAO, new BlockPos(4, 70, -3));
        instancia.estado(EncounterState.ARMED);
        instancia.estado(EncounterState.ACTIVE);
        instancia.entrar(jogadorId);
        instancia.registrarEntidade(entidadeId);
        instancia.registrarDesfecho(entidadeId, DefeatResult.CAPTURADO, jogadorId);
        instancia.esquecerEntidade(entidadeId);
        instancia.estado(EncounterState.COMPLETED);
        original.registrar(instancia);
        original.ledger().travar(encontroId, "gi_card/nenfoundation:cyclops/" + entidadeId);
        ResourceLocation card = ResourceLocation.fromNamespaceAndPath("nenfoundation", "cyclops");
        original.registrarCardsEmitidos(Map.of(card, 1));

        CompoundTag salvo = original.save(new CompoundTag(), null);
        EncounterSavedData carregado = EncounterSavedData.carregar(salvo, null);
        EncounterInstance restaurado = carregado.instancias().get(encontroId);

        assertEquals(EncounterState.COMPLETED, restaurado.estado());
        assertEquals(Map.of(entidadeId, DefeatResult.CAPTURADO), restaurado.desfechos());
        assertEquals(Map.of(entidadeId, jogadorId), restaurado.autoresDosDesfechos());
        assertTrue(carregado.ledger().jaPago(encontroId,
                "gi_card/nenfoundation:cyclops/" + entidadeId));
        assertEquals(Map.of(card, 1), carregado.cardsEmitidos());
    }
}
