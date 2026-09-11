package com.darkcontinent.nenfoundation.nen.profile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.darkcontinent.nenfoundation.api.ability.ActiveAbility;
import com.darkcontinent.nenfoundation.nen.aura.AuraPool;
import com.darkcontinent.nenfoundation.nen.category.NenCategory;
import com.darkcontinent.nenfoundation.nen.profile.PersistentNenData;
import java.util.Map;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RuntimeNenStateTest {

    private static final ResourceLocation TEN = id("ten");
    private static final ResourceLocation HABILIDADE = id("habilidade_teste");

    /** Perfil neutro para usar nos testes que precisam de um contexto de perfil. */
    private static final PersistentNenData PERFIL = new PersistentNenData(
            1, false, NenCategory.UNDETERMINED, false,
            0.0D, 0.0D, 0.0D, Map.of(), Set.of(), Set.of(), Set.of());

    /** Parametros de teste: base 100, sem vigor. */
    private static final AuraPool.Parametros P = AuraPool.Parametros.de(100.0D, 1.0D, 0.10D);

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

        // pool vem zerado; resetar para maximo garante aura != 0 para o teste
        estado.pool().resetarParaMaximo(PERFIL, P);
        estado.pool().gastarAura(100.0D - 12.5D, PERFIL, P); // => 12.5 restam
        assertTrue(estado.ativarTecnica(TEN));
        assertFalse(estado.ativarTecnica(TEN), "ativacao repetida deve ser idempotente");
        estado.definirCooldown(HABILIDADE, 20);
        estado.iniciarCanalizacao(canalizacao);

        assertEquals(12.5D, estado.auraAtual(), 1e-9);
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
