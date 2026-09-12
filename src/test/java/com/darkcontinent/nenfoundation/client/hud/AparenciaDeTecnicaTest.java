package com.darkcontinent.nenfoundation.client.hud;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.darkcontinent.nenfoundation.nen.technique.Ren;
import com.darkcontinent.nenfoundation.nen.technique.Ten;
import com.darkcontinent.nenfoundation.nen.technique.Zetsu;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** A aparencia das tecnicas na tela: uma fonte so, e distinguivel sem ler. */
class AparenciaDeTecnicaTest {

    private static final List<ResourceLocation> AS_TRES =
            List.of(Ten.ID, Ren.ID, Zetsu.ID);

    @Test
    @DisplayName("nenhuma tecnica compartilha cor com outra")
    void coresDistintas() {
        Set<Integer> cores = new LinkedHashSet<>();
        for (ResourceLocation id : AS_TRES) {
            assertTrue(cores.add(AparenciaDeTecnica.de(id).cor()),
                    "Duas tecnicas usam a mesma cor; a segunda foi " + id
                            + ". O criterio da #89 e distinguir OLHANDO, e duas"
                            + " tecnicas da mesma cor so se separam lendo o nome.");
        }
    }

    @Test
    @DisplayName("nenhuma tecnica compartilha FORMA com outra")
    void formasDistintas() {
        // COR SOZINHA NAO BASTA, e esta e a metade do criterio que some com
        // facilidade: cerca de um em doze homens nao separa vermelho de verde,
        // e o indicador da HUD tem nove pixels. Se as tres virassem a mesma
        // forma, o teste de cor acima continuaria passando -- e a tela ficaria
        // ilegivel para quem nao distingue as cores.
        Set<AparenciaDeTecnica.Forma> formas = new LinkedHashSet<>();
        for (ResourceLocation id : AS_TRES) {
            assertTrue(formas.add(AparenciaDeTecnica.de(id).forma()),
                    "Duas tecnicas usam a mesma forma; a segunda foi " + id);
        }
    }

    @Test
    @DisplayName("nenhuma das conhecidas cai na forma NEUTRA")
    void conhecidasNaoSaoNeutras() {
        // A forma NEUTRA e a saida de emergencia para id desconhecido. Se uma
        // tecnica de verdade cair nela, e porque o mapa esqueceu dela -- e o
        // sintoma em jogo seria um disco sem significado, que nao parece erro.
        for (ResourceLocation id : AS_TRES) {
            assertTrue(AparenciaDeTecnica.de(id).forma() != AparenciaDeTecnica.Forma.NEUTRA,
                    id + " caiu na forma neutra; ela nao esta no mapa de conhecidas.");
        }
    }

    @Test
    @DisplayName("tecnica desconhecida nao quebra, e ganha cor estavel")
    void desconhecidaNaoQuebra() {
        ResourceLocation inventada = ResourceLocation.fromNamespaceAndPath("outromod", "hatsu_x");

        AparenciaDeTecnica.Aparencia primeira = AparenciaDeTecnica.de(inventada);
        assertNotNull(primeira, "id desconhecido devolveu nulo; a HUD cairia inteira.");
        assertEquals(AparenciaDeTecnica.Forma.NEUTRA, primeira.forma(),
                "id desconhecido devia cair na forma neutra.");

        // ESTAVEL ENTRE CHAMADAS, e o teste existe porque a alternativa obvia
        // -- sortear uma cor -- passaria em todo o resto deste arquivo. A cor
        // mudaria a cada entrada no mundo, e o jogador nunca aprenderia nada.
        assertEquals(primeira.cor(), AparenciaDeTecnica.de(inventada).cor(),
                "a cor derivada mudou entre duas chamadas com o mesmo id.");
    }

    @Test
    @DisplayName("a cor derivada nunca sai escura demais nem transparente")
    void corDerivadaELegivel() {
        // O indicador aparece SOBRE O MUNDO. Uma cor quase preta some contra
        // qualquer caverna, e alfa zero some contra tudo -- e nenhum dos dois
        // da erro: o indicador simplesmente nao aparece, e o relato vira "a
        // tecnica nao liga".
        for (String nome : List.of("a", "zzz", "hatsu_1", "tecnica_com_nome_longo", "")) {
            int cor = AparenciaDeTecnica.corDerivadaDe(
                    ResourceLocation.fromNamespaceAndPath("outromod", nome.isEmpty() ? "x" : nome));
            assertEquals(0xFF, (cor >>> 24) & 0xFF, "alfa nao ficou opaco para " + nome);
            for (int deslocamento : List.of(16, 8, 0)) {
                int canal = (cor >>> deslocamento) & 0xFF;
                assertTrue(canal >= 0x60,
                        "canal escuro demais (" + canal + ") para " + nome
                                + ": o indicador sumiria contra fundo escuro.");
            }
        }
    }
}
