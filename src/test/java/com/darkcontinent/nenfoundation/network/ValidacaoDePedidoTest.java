package com.darkcontinent.nenfoundation.network;

import com.darkcontinent.nenfoundation.network.handler.ValidacaoDePedido;
import com.darkcontinent.nenfoundation.network.handler.ValidacaoDePedido.Motivo;
import com.darkcontinent.nenfoundation.nen.profile.PersistentNenData;
import com.darkcontinent.nenfoundation.nen.profile.RuntimeNenState;
import com.darkcontinent.nenfoundation.nen.category.NenCategory;
import com.darkcontinent.nenfoundation.Repo;
import com.google.gson.JsonParser;
import java.util.Map;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Contrastes de estado, unlock e existencia; catalogo simulado nao e registro M4. */
class ValidacaoDePedidoTest {
    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("nenfoundation", "teste");
    private final RuntimeNenState runtime = new RuntimeNenState();

    private PersistentNenData perfil(boolean unlock) {
        return new PersistentNenData(1, true, NenCategory.EMISSION, false, 0, 0, 0,
                Map.of(), unlock ? Set.of(ID) : Set.of(), unlock ? Set.of(ID) : Set.of(), Set.of());
    }

    @Test
    void recusaNaoDespertoEIdDesconhecidoMesmoDesbloqueado() {
        assertEquals(Motivo.NAO_DESPERTO, ValidacaoDePedido.validar(ID, false, true, true,
                PersistentNenData.NAO_DESPERTADO, runtime));
        assertEquals(Motivo.ID_INEXISTENTE, ValidacaoDePedido.validar(ID, false, false, true,
                perfil(true), runtime));
        // TECNICA VALIDA DEIXOU DE SER INDISPONIVEL, e isso e mudanca
        // deliberada do M4, nao regressao.
        //
        // Ate aqui o metodo devolvia INDISPONIVEL sempre, porque nao havia
        // motor para executar nada -- fabricar ativacao sem motor seria dizer
        // ao jogador que algo aconteceu quando nada aconteceu. O motor de
        // TECNICA existe desde a issue #85; `null` significa "pedido valido, o
        // chamador que execute".
        assertNull(ValidacaoDePedido.validar(ID, false, true, true,
                perfil(true), runtime),
                "tecnica existente, desbloqueada e com estado valido devia passar");

        // HABILIDADE continua sem motor ate o M5, e por isso continua
        // INDISPONIVEL. O que mudou foi o mundo, e nao a regra.
        assertEquals(Motivo.INDISPONIVEL, ValidacaoDePedido.validar(ID, true, true, true,
                perfil(true), runtime),
                "habilidade nao tem motor ate o M5; fabricar ativacao aqui seria mentir");
    }

    @Test
    void validaEstadoUnlockECooldownSemMutar() {
        assertEquals(Motivo.ESTADO_INVALIDO, ValidacaoDePedido.validar(ID, false, true, false,
                perfil(true), runtime));
        for (boolean habilidade : new boolean[]{false, true}) {
            assertEquals(Motivo.NAO_DESBLOQUEADO, ValidacaoDePedido.validar(ID, habilidade, true, true,
                    perfil(false), runtime));
        }
        runtime.definirCooldown(ID, 4);
        assertEquals(Motivo.EM_RECARGA, ValidacaoDePedido.validar(ID, true, true, true,
                perfil(true), runtime));
        assertEquals(4, runtime.cooldowns().get(ID));
        assertTrue(runtime.tecnicasAtivas().isEmpty());
    }

    @Test
    void todaRecusaTemTraducaoNosDoisIdiomas() {
        for (String idioma : new String[]{"pt_br", "en_us"}) {
            var json = JsonParser.parseString(Repo.texto("src/main/resources/assets/nenfoundation/lang/"
                    + idioma + ".json")).getAsJsonObject();
            for (Motivo motivo : Motivo.values()) {
                assertTrue(json.has(motivo.chave()), motivo.chave());
                assertFalse(json.get(motivo.chave()).getAsString().isBlank());
            }
        }
    }
}
