package com.darkcontinent.nenfoundation.server;

import static org.junit.jupiter.api.Assertions.*;
import com.darkcontinent.nenfoundation.nen.profile.RuntimeNenState;
import com.darkcontinent.nenfoundation.network.payload.DeltaDeRuntimeS2C;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

/** Mede o caminho que o transporte usa, incluindo recusa e duas sessoes. */
class ControleDeSyncTest {
    @Test void cemTicksLimposNaoEnviamERegeneracaoRespeitaCadencia() {
        var estado = new RuntimeNenState(100);
        var controle = new ControleDeSync();
        List<DeltaDeRuntimeS2C> pacotes = new ArrayList<>();
        assertTrue(controle.enviar(estado, 0, 5, pacotes::add));
        for (int tick = 1; tick <= 100; tick++) controle.enviar(estado, tick, 5, pacotes::add);
        assertEquals(1, pacotes.size());
        estado.gastarAura(10);
        assertTrue(controle.enviar(estado, 101, 5, pacotes::add));
        for (int tick = 102; tick <= 201; tick++) {
            estado.recuperarAura(0.01);
            controle.enviar(estado, tick, 5, pacotes::add);
        }
        assertEquals(22, pacotes.size(), "um inicial, um gasto, vinte em cem ticks alterados");
    }

    @Test void canalIndisponivelOuExcecaoNaoConfirmaNemConta() {
        var estado = new RuntimeNenState(10);
        var controle = new ControleDeSync();
        assertFalse(controle.enviar(estado, 0, 5, p -> false));
        assertTrue(estado.auraSuja());
        assertThrows(IllegalStateException.class, () -> controle.enviar(estado, 0, 5, p -> {
            throw new IllegalStateException("transporte falhou");
        }));
        assertTrue(estado.auraSuja());
        assertTrue(controle.enviar(estado, 0, 5, p -> true));
        assertFalse(estado.auraSuja());
    }

    @Test void setterTecnicaECooldownNotificamEFloatIgualNaoEnvia() {
        var estado = new RuntimeNenState(100);
        var controle = new ControleDeSync();
        var tecnica = ResourceLocation.fromNamespaceAndPath("nenfoundation", "ten");
        assertTrue(controle.enviar(estado, 0, 1, p -> true));
        estado.definirAuraAtual(Math.nextDown(100.0));
        assertFalse(controle.enviar(estado, 1, 1, p -> true));
        estado.definirAuraAtual(50);
        assertTrue(controle.enviar(estado, 2, 1, p -> p.aura() == 50));
        estado.ativarTecnica(tecnica);
        assertTrue(controle.enviar(estado, 3, 1, p -> p.tecnicasAtivas().contains(tecnica)));
        estado.definirCooldown(tecnica, 3);
        assertTrue(controle.enviar(estado, 4, 1, p -> p.cooldowns().get(tecnica) == 3));
        estado.removerCooldown(tecnica);
        assertTrue(controle.enviar(estado, 5, 1, p -> p.cooldowns().isEmpty()));
    }

    @Test void envioNaoApagaMutacaoReentranteENovaSessaoEnviaMesmoValor() {
        var estado = new RuntimeNenState(10);
        var controle = new ControleDeSync();
        assertTrue(controle.enviar(estado, 0, 1, p -> { estado.gastarAura(1); return true; }));
        assertTrue(estado.auraSuja());
        assertTrue(controle.enviar(estado, 1, 1, p -> p.aura() == 9));
        var outraSessao = new ControleDeSync();
        assertTrue(outraSessao.enviar(new RuntimeNenState(9), 0, 1, p -> true));
    }
}
