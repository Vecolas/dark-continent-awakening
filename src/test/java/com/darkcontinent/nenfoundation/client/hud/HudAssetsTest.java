package com.darkcontinent.nenfoundation.client.hud;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.darkcontinent.nenfoundation.Repo;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Map;
import java.util.Set;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

/** Portao dos assets modulares: faltando e sobrando reprovam. */
class HudAssetsTest {
    private static final String DIRETORIO =
            "src/main/resources/assets/nenfoundation/textures/gui/nen_hud";
    private static final Map<String, Dimensao> ESPERADOS = Map.of(
            "frame.png", new Dimensao(256, 80),
            "portrait_frame.png", new Dimensao(64, 64),
            "badge_frame.png", new Dimensao(32, 32),
            "aura_pool_fill.png", new Dimensao(256, 16),
            "aura_output_fill.png", new Dimensao(256, 16));

    @Test
    void conjuntoESeparadoTransparenteETemDimensoesCongeladas() {
        Set<String> encontrados = Repo.varrer(DIRETORIO, ".png").stream()
                .map(caminho -> caminho.getFileName().toString())
                .collect(java.util.stream.Collectors.toSet());
        assertEquals(ESPERADOS.keySet(), encontrados,
                "Asset faltando ou textura gigante/sobrando precisa reprovar.");

        ESPERADOS.forEach((nome, dimensao) -> {
            try {
                var imagem = ImageIO.read(Repo.arquivo(DIRETORIO + "/" + nome).toFile());
                assertNotNull(imagem, nome + " precisa ser um PNG legivel");
                assertEquals(dimensao.largura(), imagem.getWidth(), nome);
                assertEquals(dimensao.altura(), imagem.getHeight(), nome);
                assertTrue(imagem.getColorModel().hasAlpha(), nome + " precisa preservar transparencia");
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
    }

    private record Dimensao(int largura, int altura) {
    }
}
