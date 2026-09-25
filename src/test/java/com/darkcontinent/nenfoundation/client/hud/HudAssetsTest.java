package com.darkcontinent.nenfoundation.client.hud;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.darkcontinent.nenfoundation.Repo;
import java.nio.file.Files;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * O portao dos assets da HUD, invertido em 2026-09-25.
 *
 * <p><b>ELE COBRAVA PRESENCA; AGORA COBRA AUSENCIA.</b> Ate o redesenho existiam
 * tres PNGs -- a moldura de 512x128 e dois gradientes de preenchimento -- e este
 * arquivo conferia dimensao e transparencia de cada um. A HUD passou a ser
 * desenhada em codigo, e os tres sairam.
 *
 * <p>O QUE ELE IMPEDE AGORA e a volta silenciosa: alguem gera uma textura de
 * HUD, comita, e ela fica no repositorio sem nenhum {@code blit} apontando para
 * ela. Textura orfa nao da erro, nao aparece em jogo, e sobrevive a varias
 * entregas -- e a mesma familia do erro numero 7 do CLAUDE.md, config declarada
 * sem consumidor.
 *
 * <p>Se um dia a HUD voltar a usar arquivo, este portao tem de ser reescrito no
 * mesmo commit que reintroduz o primeiro {@code blit}. Reprovar aqui e o
 * lembrete.
 */
class HudAssetsTest {

    private static final String DIRETORIO =
            "src/main/resources/assets/nenfoundation/textures/gui/nen_hud";

    @Test
    @DisplayName("a HUD e desenhada em codigo -- textura orfa em nen_hud reprova")
    void nenhumaTexturaDeHudSobrouSemConsumidor() {
        var pasta = Repo.raiz().resolve(DIRETORIO);
        if (!Files.isDirectory(pasta)) {
            return;
        }
        List<String> encontradas = Repo.varrer(DIRETORIO, ".png").stream()
                .map(caminho -> caminho.getFileName().toString())
                .sorted()
                .toList();

        assertTrue(encontradas.isEmpty(),
                "A HUD nao blita textura nenhuma desde 2026-09-25. Os arquivos abaixo "
                        + "nao tem consumidor, e um PNG que ninguem desenha e peso morto "
                        + "que ninguem remove: " + encontradas);
    }

    @Test
    @DisplayName("nenhum codigo de HUD aponta para uma textura que nao existe mais")
    void nenhumaReferenciaAsTexturasRemovidas() {
        List<String> removidas = List.of("nen_hud/frame.png", "nen_hud/aura_pool_fill.png",
                "nen_hud/aura_output_fill.png", "textures/gui/nen_hud/");
        for (var fonte : Repo.varrer("src/main/java", ".java")) {
            String texto;
            try {
                texto = Files.readString(fonte);
            } catch (java.io.IOException e) {
                throw new java.io.UncheckedIOException(e);
            }
            for (String alvo : removidas) {
                assertTrue(!texto.contains(alvo),
                        fonte + " aponta para " + alvo + ", que foi removida no redesenho");
            }
        }
    }
}
