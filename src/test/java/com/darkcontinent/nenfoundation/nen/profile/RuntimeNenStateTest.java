package com.darkcontinent.nenfoundation.nen.profile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.darkcontinent.nenfoundation.api.ability.ActiveAbility;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RuntimeNenStateTest {

    private static final ResourceLocation TEN = id("ten");
    private static final ResourceLocation HABILIDADE = id("habilidade_teste");

    @Test
    @DisplayName("estado novo e neutro e nao expoe colecoes mutaveis")
    void estadoNovoENeutro() {
        RuntimeNenState estado = new RuntimeNenState();

        assertEquals(0.0D, estado.auraAtual());
        assertTrue(estado.tecnicasAtivas().isEmpty());
        assertTrue(estado.cooldowns().isEmpty());
        assertTrue(estado.canalizacao().isEmpty());
        assertThrows(UnsupportedOperationException.class,
                () -> estado.tecnicasAtivas().add(TEN));
        assertThrows(UnsupportedOperationException.class,
                () -> estado.cooldowns().put(HABILIDADE, 1));
    }

    @Test
    @DisplayName("mutacoes de runtime passam pela propria fronteira")
    void mutacoesControladas() {
        RuntimeNenState estado = new RuntimeNenState();
        ActiveAbility canalizacao = new HabilidadeAtiva(HABILIDADE, 42L);

        estado.definirAuraMaxima(20.0D);
        estado.definirAuraAtual(12.5D);
        assertTrue(estado.ativarTecnica(TEN));
        assertFalse(estado.ativarTecnica(TEN), "ativacao repetida deve ser idempotente");
        estado.definirCooldown(HABILIDADE, 20);
        estado.iniciarCanalizacao(canalizacao);

        assertEquals(12.5D, estado.auraAtual());
        assertEquals(1, estado.tecnicasAtivas().size());
        assertEquals(20, estado.cooldowns().get(HABILIDADE));
        assertSame(canalizacao, estado.canalizacao().orElseThrow());

        assertTrue(estado.desativarTecnica(TEN));
        estado.definirCooldown(HABILIDADE, 0);
        estado.encerrarCanalizacao();
        assertTrue(estado.tecnicasAtivas().isEmpty());
        assertTrue(estado.cooldowns().isEmpty());
        assertTrue(estado.canalizacao().isEmpty());
    }

    @Test
    @DisplayName("cooldown negativo nao entra no estado")
    void cooldownNegativoERecusado() {
        RuntimeNenState estado = new RuntimeNenState();

        assertThrows(IllegalArgumentException.class,
                () -> estado.definirCooldown(HABILIDADE, -1));
        assertTrue(estado.cooldowns().isEmpty());
    }

    @Test
    @DisplayName("aura suja so muda quando o valor autoritativo muda")
    void auraSujaTemCicloExplicito() {
        RuntimeNenState estado = new RuntimeNenState(10.0D);

        assertTrue(estado.auraSuja());
        estado.marcarAuraSincronizada();
        assertFalse(estado.auraSuja());
        assertFalse(estado.gastarAura(11.0D));
        assertFalse(estado.auraSuja());
        assertTrue(estado.gastarAura(2.0D));
        assertTrue(estado.auraSuja());
        estado.marcarAuraSincronizada();
        assertEquals(0.0D, estado.recuperarAura(0.0D));
        assertFalse(estado.auraSuja());
    }

    private static ResourceLocation id(String caminho) {
        return ResourceLocation.fromNamespaceAndPath("nenfoundation", caminho);
    }

    private record HabilidadeAtiva(ResourceLocation abilityId, long tickDeInicio)
            implements ActiveAbility {
        @Override
        public boolean viva() {
            return true;
        }
    }
}
