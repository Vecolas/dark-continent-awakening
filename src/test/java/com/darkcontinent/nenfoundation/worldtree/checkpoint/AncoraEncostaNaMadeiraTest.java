package com.darkcontinent.nenfoundation.worldtree.checkpoint;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.darkcontinent.nenfoundation.Repo;
import com.darkcontinent.nenfoundation.worldtree.WorldTreeLayout;
import com.darkcontinent.nenfoundation.worldtree.WorldTreeLayoutGenerator;
import com.darkcontinent.nenfoundation.worldtree.WorldTreeAnchorPlatform;
import com.darkcontinent.nenfoundation.worldtree.WorldTreeTrunkSurface;
import com.darkcontinent.nenfoundation.worldtree.generation.WorldTreeBaseGenerator;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * A ancora de escalada encosta na madeira -- em todas as alturas, em vinte seeds.
 *
 * <p><b>POR QUE ESTE PORTAO NAO EXISTIA, e o que ele teria pego.</b> Havia dois
 * testes para {@code anchorPosition}, e os dois perguntavam a mesma coisa: em que
 * CHUNK a ancora cai, e se a conta bate com ela mesma. Nenhum perguntava a unica
 * coisa que o jogador ve -- se ela esta encostada no tronco.
 *
 * <p>E ela nao estava. Duas causas somadas, nenhuma com erro:
 *
 * <ol>
 *   <li>um {@code Math.max(18, ...)} cravado, sem relacao com a arvore: onde o
 *       tronco tinha raio 10, a ancora ia para x=20;</li>
 *   <li>o raio NOMINAL do perfil no lugar da casca de verdade, que tem lobos de
 *       ate 4,5 blocos e ruido de ate 2 -- erro de ate 6,5 para cada lado.</li>
 * </ol>
 *
 * <p>O resultado em jogo era "flutuando de qualquer jeito": as vezes metros
 * longe da casca, as vezes enterrada dentro dela. Os dois testes antigos
 * continuavam verdes, porque mediam a aritmetica e nao a arvore.
 */
class AncoraEncostaNaMadeiraTest {

    private static final int SEEDS = 20;

    private static long seedAt(int index) {
        return 1_000L + index * 7_919L;
    }

    /*
     * O ALCANCE VEM DO CODIGO DE PRODUCAO, e este arquivo ja teve uma copia dele.
     *
     * Com a copia, encurtar o laco do gerador de volta para 6 nao reprovava nada:
     * o teste continuava medindo o proprio 9 contra a arvore, e a varanda em jogo
     * voltava a nascer solta. Regua com a copia do numero que ela vigia nao vigia
     * coisa nenhuma -- e o erro numero 7 do CLAUDE.md, na forma mais barata de
     * cometer.
     */

    @Test
    @DisplayName("a ancora do tronco fica ENCOSTADA na casca, e nao no raio nominal")
    void ancoraDoTroncoEncosta() {
        for (int index = 0; index < SEEDS; index++) {
            long seed = seedAt(index);
            WorldTreeLayout layout = WorldTreeLayoutGenerator.generate(seed, 0, 0);
            for (WorldTreeCheckpoint checkpoint : WorldTreeCheckpoint.values()) {
                if (checkpoint.y() > layout.trunk().topY()) {
                    continue;
                }
                int y = Math.min(checkpoint.y(), layout.trunk().topY());
                double casca = WorldTreeTrunkSurface.naDirecaoX(
                        layout.trunk().radiusAt(y), y, seed);
                int x = WorldTreeCheckpointGenerator
                        .anchorPosition(layout, checkpoint).getX();

                // FORA DA MADEIRA: enterrada, o jogador nao acha a ancora.
                assertTrue(x > casca,
                        "seed " + seed + ", " + checkpoint + ": ancora em x=" + x
                                + " com a casca em " + String.format("%.1f", casca)
                                + " -- ela nasce ENTERRADA na madeira.");
                // E COLADA: cada bloco de ar aqui e um bloco de "flutuando".
                assertTrue(x - casca <= 2.5,
                        "seed " + seed + ", " + checkpoint + ": ancora em x=" + x
                                + " com a casca em " + String.format("%.1f", casca)
                                + " -- " + String.format("%.1f", x - casca)
                                + " blocos de ar entre as duas. Isso e a ancora"
                                + " FLUTUANDO, que foi o relato de jogo.");
            }
        }
    }

    @Test
    @DisplayName("a plataforma ATRAVESSA a casca -- e ela que segura a varanda")
    void plataformaAtravessaAMadeira() {
        // ALIMENTAR O PORTAO COM O DEFEITO, e o que isso ensinou: encurtar o
        // alcance da varanda de 6 para menos NAO reprova aqui, e esta certo nao
        // reprovar -- com a ancora no lugar, seis blocos ja entram cinco casca
        // adentro. Cheguei a alargar a elipse para 9 "por seguranca" e desfiz.
        //
        // O que ESTE caso pega e a combinacao que existia de verdade: com a
        // ancora em `nominal + 2` (mais o max(18)), a ponta interna da plataforma
        // parava FORA da casca -- a varanda inteira nascia no ar. Com a quebra da
        // ancora reintroduzida, este caso reprova junto com o de cima.
        for (int index = 0; index < SEEDS; index++) {
            long seed = seedAt(index);
            WorldTreeLayout layout = WorldTreeLayoutGenerator.generate(seed, 0, 0);
            for (WorldTreeCheckpoint checkpoint : WorldTreeCheckpoint.values()) {
                if (checkpoint.y() > layout.trunk().topY()) {
                    continue;
                }
                int y = Math.min(checkpoint.y(), layout.trunk().topY());
                double casca = WorldTreeTrunkSurface.naDirecaoX(
                        layout.trunk().radiusAt(y), y, seed);
                int pontaInterna = WorldTreeCheckpointGenerator
                        .anchorPosition(layout, checkpoint).getX();
                pontaInterna = WorldTreeAnchorPlatform.pontaInterna(pontaInterna);
                assertTrue(pontaInterna < casca - 2.0,
                        "seed " + seed + ", " + checkpoint + ": a ponta interna da"
                                + " plataforma para em x=" + pontaInterna + " e a casca"
                                + " esta em " + String.format("%.1f", casca)
                                + ". A varanda nao encosta no tronco.");
            }
        }
    }

    @Test
    @DisplayName("a ancora do Overworld encosta no fuste -- ela estava 16 blocos no ar")
    void ancoraDoOverworldEncosta() {
        // O QUE HAVIA: (origemX + 49, 64 + 16, origemZ), tres numeros cravados,
        // com UM bloco de lenho embaixo. O 49 nao consulta o fuste e o +16 nao
        // consulta nada: a ancora ficava pendurada no ar, sempre.
        for (int index = 0; index < SEEDS; index++) {
            long seed = seedAt(index);
            WorldTreeLayout layout = WorldTreeLayoutGenerator.generate(seed, 0, 0);
            int y = 66;
            double nominal = WorldTreeBaseGenerator.raioNominalEm(y);
            double casca = WorldTreeTrunkSurface.naDirecaoX(nominal, y, seed);
            int x = (int) Math.ceil(casca) + 1;

            assertTrue(x > casca, "seed " + seed + ": ancora do Overworld enterrada");
            assertTrue(x - casca <= 2.0,
                    "seed " + seed + ": " + String.format("%.1f", x - casca)
                            + " blocos de ar entre a ancora do Overworld e o fuste.");
            assertTrue(WorldTreeAnchorPlatform.pontaInterna(x) < casca - 2.0,
                    "seed " + seed + ": a varanda do Overworld nao atravessa a casca.");
        }
    }

    @Test
    @DisplayName("a casca do tronco e a MESMA conta em toda parte")
    void umaFonteSoParaACasca() {
        // O DEFEITO QUE ORIGINOU TUDO ISTO foi a forma da casca existir so dentro
        // do laco que escreve blocos. Este caso amarra o ponto fixo: o raio
        // devolvido por `naDirecaoX` tem de ser um raio que `irregularRadius`
        // confirma naquela coordenada -- senao a "casca" e um numero que o
        // escritor nao reconhece.
        for (int index = 0; index < SEEDS; index++) {
            long seed = seedAt(index);
            WorldTreeLayout layout = WorldTreeLayoutGenerator.generate(seed, 0, 0);
            for (int y = layout.trunk().baseY(); y < layout.trunk().topY(); y += 97) {
                double nominal = layout.trunk().radiusAt(y);
                double casca = WorldTreeTrunkSurface.naDirecaoX(nominal, y, seed);
                double confirmacao = WorldTreeTrunkSurface.irregularRadius(
                        nominal, (int) Math.round(casca), y, 0, seed);
                assertTrue(Math.abs(casca - confirmacao) < 0.5,
                        "seed " + seed + ", y=" + y + ": o ponto fixo nao convergiu ("
                                + String.format("%.2f vs %.2f", casca, confirmacao)
                                + "). A ancora encostaria num raio que o escritor do"
                                + " tronco nao reconhece.");
            }
        }
    }

    @Test
    @DisplayName("o CHECKPOINT e escrito antes da COPA -- senao a varanda vira peneira")
    void checkpointVemAntesDaCopa() throws IOException {
        // O TERCEIRO DEFEITO DO RELATO, e o unico que nao mora numa coordenada:
        // mora na ORDEM. A plataforma so preenche ar, e a copa tambem. Com a copa
        // primeiro, cada folha ja posta virava um buraco na varanda -- pior quanto
        // mais alto o checkpoint, porque CROWN e SUMMIT ficam debaixo dos discos
        // do lider central, de raio ate 168.
        //
        // Dois geradores educados, cada um respeitando o que o outro escreveu, e
        // nenhum erro em lugar nenhum.
        //
        // ISTO E UM PORTAO DE TEXTO, e nao de comportamento -- provar a ordem de
        // verdade exigiria um chunk, e portanto um gametest. Ele le a fonte e
        // compara duas posicoes. E pouco; e MUITO mais que o comentario que era
        // toda a protecao que existia.
        String fonte = Files.readString(Repo.raiz().resolve(
                "src/main/java/com/darkcontinent/nenfoundation/worldtree/"
                        + "WorldTreeChunkGenerator.java"), StandardCharsets.UTF_8);
        int checkpoint = fonte.indexOf("WorldTreeCheckpointGenerator.generate(chunk");
        int copa = fonte.indexOf("WorldTreeCanopyGenerator.generate(chunk");
        assertTrue(checkpoint >= 0 && copa >= 0,
                "nao achei as duas chamadas em buildSurface; o portao parou de"
                        + " vigiar o que devia.");
        assertTrue(checkpoint < copa,
                "a copa e escrita ANTES do checkpoint em buildSurface. A plataforma"
                        + " so preenche ar, entao ela sai furada em toda folha que a"
                        + " copa ja tiver posto -- a 'base bugada' do relato de jogo.");
    }

    @Test
    @DisplayName("a ancora do topo cai perto do lider, e nao do nominal do tronco")
    void ancoraAcimaDoTroncoSegueOLider() {
        for (int index = 0; index < SEEDS; index++) {
            long seed = seedAt(index);
            WorldTreeLayout layout = WorldTreeLayoutGenerator.generate(seed, 0, 0);
            for (WorldTreeCheckpoint checkpoint : WorldTreeCheckpoint.values()) {
                if (checkpoint.y() <= layout.trunk().topY()) {
                    continue;
                }
                BlockPos anchor = WorldTreeCheckpointGenerator
                        .anchorPosition(layout, checkpoint);
                double raioDoLider = com.darkcontinent.nenfoundation.worldtree.generation
                        .WorldTreeCrownGenerator.leaderRadius(checkpoint.y());
                var centro = com.darkcontinent.nenfoundation.worldtree.generation
                        .WorldTreeCrownGenerator.leaderCenter(checkpoint.y(), seed);
                double folga = anchor.getX() - (centro.x() + raioDoLider);
                assertTrue(folga > 0 && folga <= 2.5,
                        "seed " + seed + ", " + checkpoint + ": " + folga
                                + " blocos entre a ancora e o lider central.");
            }
        }
    }
}
