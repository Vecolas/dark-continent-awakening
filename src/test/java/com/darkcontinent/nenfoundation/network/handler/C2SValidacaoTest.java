package com.darkcontinent.nenfoundation.network.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.darkcontinent.nenfoundation.nen.category.NenCategory;
import com.darkcontinent.nenfoundation.nen.profile.PersistentNenData;
import com.darkcontinent.nenfoundation.nen.profile.RuntimeNenState;
import java.util.Map;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

class C2SValidacaoTest {

    private static final ResourceLocation TECNICA = ResourceLocation.fromNamespaceAndPath(
            "nenfoundation", "ren");
    private static final ResourceLocation HABILIDADE = ResourceLocation.fromNamespaceAndPath(
            "nenfoundation", "disparo");

    @Test
    void recusaTecnicaSemDespertarOuUnlock() {
        PersistentNenData adormecido = perfil(false, Set.of(), Set.of());
        assertEquals("nen.error.not_awakened",
                C2SValidacao.tecnica(adormecido, new RuntimeNenState(), TECNICA));

        PersistentNenData semUnlock = perfil(true, Set.of(), Set.of());
        assertEquals("nen.error.technique_locked",
                C2SValidacao.tecnica(semUnlock, new RuntimeNenState(), TECNICA));
    }

    @Test
    void aceitaPedidoDeTecnicaDesbloqueadaEIdValido() {
        PersistentNenData perfil = perfil(true, Set.of(TECNICA), Set.of());
        assertNull(C2SValidacao.tecnica(perfil, new RuntimeNenState(), TECNICA));
        assertEquals("nen.error.invalid_request",
                C2SValidacao.tecnica(perfil, new RuntimeNenState(), null));
    }

    @Test
    void slotForaDaBarraERecusadoEAlvoNaoViraEstado() {
        PersistentNenData perfil = perfil(true, Set.of(), Set.of(HABILIDADE));
        RuntimeNenState estado = new RuntimeNenState();
        assertEquals("nen.error.invalid_request",
                C2SValidacao.habilidade(perfil, estado, HABILIDADE, 9));
        assertNull(C2SValidacao.habilidade(perfil, estado, HABILIDADE, 0));
        assertEquals(Set.of(), estado.tecnicasAtivas());
    }

    private static PersistentNenData perfil(boolean despertado,
            Set<ResourceLocation> tecnicas, Set<ResourceLocation> habilidades) {
        return new PersistentNenData(1, despertado, NenCategory.UNDETERMINED, false,
                0.0, 0.0, 0.0, Map.of(), tecnicas, habilidades, Set.of());
    }
}
