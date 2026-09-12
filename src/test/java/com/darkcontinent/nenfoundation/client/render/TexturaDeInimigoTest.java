package com.darkcontinent.nenfoundation.client.render;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.darkcontinent.nenfoundation.Repo;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Portao da textura que veste GEOMETRIA EMPRESTADA.
 *
 * <p>POR QUE ELE EXISTE, e o custo que ele cobra: enquanto nao ha modelo
 * autoral, os inimigos deste repositorio renderizam com camadas vanilla, e cada
 * camada tem uma UV de tamanho fixo. Uma textura com PROPORCAO ERRADA nao
 * levanta excecao nenhuma -- o jogo estica a imagem sobre os UVs e entrega um
 * bicho manchado. E uma textura que aponta para o arquivo vanilla entrega um
 * Foxbear BRANCO, que foi exatamente o relato que abriu esta issue: "o bear nao
 * possui nenhuma textura".
 *
 * <p>Por isso o portao morde dos dois lados: a variante emprestada tem de
 * existir com a dimensao da UV emprestada, E o renderer tem de continuar
 * apontando para ela. Trocar o alvo para {@code foxbear.png} -- o atlas autoral
 * 1254x1254, feito para um modelo GeckoLib que ainda nao existe -- compila,
 * carrega e destroi a aparencia do mob em silencio.
 */
class TexturaDeInimigoTest {

    private static final String TEXTURA_EMPRESTADA =
            "src/main/resources/assets/nenfoundation/textures/entity/foxbear/emprestado.png";

    /** {@code ModelLayers.POLAR_BEAR} e {@code LayerDefinition.create(mesh, 128, 64)}. */
    private static final int UV_DO_URSO_LARGURA = 128;
    private static final int UV_DO_URSO_ALTURA = 64;

    @Test
    @DisplayName("a textura emprestada do Foxbear tem a dimensao da UV que ela veste")
    void texturaEmprestadaCabeNaUvDoUrsoPolar() {
        var arquivo = Repo.arquivo(TEXTURA_EMPRESTADA);
        assertTrue(Files.isRegularFile(arquivo),
                "faltou " + TEXTURA_EMPRESTADA + ". Sem ela o renderer volta a vestir o mob com a"
                        + " imagem do urso polar, e o Foxbear aparece branco.");
        try {
            var imagem = ImageIO.read(arquivo.toFile());
            assertNotNull(imagem, "emprestado.png precisa ser um PNG legivel");
            assertEquals(UV_DO_URSO_LARGURA, imagem.getWidth(),
                    "a camada ModelLayers.POLAR_BEAR tem UV 128x64; largura diferente estica a"
                            + " imagem sobre os UVs e produz um bicho manchado, sem erro nenhum");
            assertEquals(UV_DO_URSO_ALTURA, imagem.getHeight(),
                    "a camada ModelLayers.POLAR_BEAR tem UV 128x64; altura diferente estica a"
                            + " imagem sobre os UVs e produz um bicho manchado, sem erro nenhum");
        } catch (IOException erro) {
            throw new UncheckedIOException(erro);
        }
    }

    @Test
    @DisplayName("o renderer do Foxbear nao veste o atlas do modelo que ainda nao existe")
    void rendererApontaParaAVarianteEmprestada() {
        String fonte = ler("src/main/java/com/darkcontinent/nenfoundation/client/render/"
                + "FoxbearRenderer.java");

        assertTrue(fonte.contains("textures/entity/foxbear/emprestado.png"),
                "FoxbearRenderer parou de apontar para a variante emprestada.");
        assertTrue(!fonte.contains("\"textures/entity/bear/polarbear.png\""),
                "FoxbearRenderer voltou a vestir o mob com a textura do URSO POLAR vanilla: em"
                        + " jogo isso e um bicho branco, e o relato que chega e 'esse mob nao tem"
                        + " textura'.");
        assertTrue(!fonte.contains("foxbear/foxbear.png"),
                "FoxbearRenderer passou a apontar para foxbear.png, o atlas 1254x1254 feito para"
                        + " um modelo GeckoLib que este repositorio ainda nao tem. A UV da camada"
                        + " emprestada e 128x64: isso compila, carrega e entrega um bicho"
                        + " manchado. Quando o modelo proprio chegar, e o renderer INTEIRO que"
                        + " muda -- camada e textura juntas -- e este portao sai com ele.");
    }

    private static String ler(String caminho) {
        try {
            return Files.readString(Repo.arquivo(caminho));
        } catch (IOException erro) {
            throw new UncheckedIOException(erro);
        }
    }
}
