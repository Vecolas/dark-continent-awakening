package com.darkcontinent.nenfoundation.worldtree.checkpoint;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.darkcontinent.nenfoundation.Repo;
import com.darkcontinent.nenfoundation.worldtree.WorldTreeBranchNode;
import com.darkcontinent.nenfoundation.worldtree.WorldTreeFoliagePlan;
import com.darkcontinent.nenfoundation.worldtree.WorldTreeFoliageShelf;
import com.darkcontinent.nenfoundation.worldtree.WorldTreeClimbingPost;
import com.darkcontinent.nenfoundation.worldtree.WorldTreeLayout;
import com.darkcontinent.nenfoundation.worldtree.WorldTreeLayoutGenerator;
import com.darkcontinent.nenfoundation.worldtree.WorldTreePoint;
import com.darkcontinent.nenfoundation.worldtree.WorldTreeSpline;
import com.darkcontinent.nenfoundation.worldtree.WorldTreeTrunkSurface;
import com.darkcontinent.nenfoundation.worldtree.generation.WorldTreeBaseGenerator;
import com.darkcontinent.nenfoundation.worldtree.generation.WorldTreeBranchNetwork;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * A CABANA de cada checkpoint: onde ela fica, e por que ela nao pode boiar.
 *
 * <p><b>POR QUE ESTE PORTAO NAO EXISTIA.</b> Havia dois testes para
 * {@code anchorPosition} e os dois perguntavam a mesma coisa: em que CHUNK a
 * ancora cai, e se a conta bate com ela mesma. Nenhum perguntava a unica coisa
 * que o jogador ve -- se ela esta apoiada em alguma coisa. Ficaram verdes durante
 * todo o tempo em que a ancora nasceu flutuando.
 *
 * <p>A rodada anterior consertou a POSICAO -- ela media o raio nominal do tronco
 * quando a casca real fica ate 6,5 blocos longe dele -- e o resultado em jogo
 * continuou ruim: uma tabua nua saindo da casca. A prateleira virou CABANA, e as
 * reguas mudaram junto.
 */
class CabanaDeCheckpointTest {

    private static final int SEEDS = 20;

    private static long seedAt(int index) {
        return 1_000L + index * 7_919L;
    }

    @Test
    @DisplayName("mesma seed, mesma cabana -- senao ela sai partida na fronteira de chunk")
    void deterministico() {
        // O SUBSTITUTO DE `WorldTreeCheckpointGeneratorTest`, que foi apagado.
        //
        // Aquele arquivo tinha dois casos, e os dois comparavam a conta com ela
        // mesma: em que chunk a ancora cai, e se `ceil(raio) + 2` da `ceil(raio)
        // + 2`. Ficaram verdes durante todo o tempo em que a ancora nasceu
        // flutuando. O que valia a pena guardar era so o DETERMINISMO, e ele
        // importa mais agora: a cabana tem 7x7 e cruza fronteira de chunk quase
        // sempre. Se dois chunks escolhessem galhos diferentes, cada um
        // construiria metade de uma cabana diferente.
        for (int index = 0; index < SEEDS; index++) {
            long seed = seedAt(index);
            WorldTreeLayout primeiro = WorldTreeLayoutGenerator.generate(seed, 0, 0);
            WorldTreeLayout segundo = WorldTreeLayoutGenerator.generate(seed, 0, 0);
            for (WorldTreeCheckpoint checkpoint : WorldTreeCheckpoint.values()) {
                // O CACHE E POR SEED: pedir duas vezes devolveria o mesmo objeto e
                // nao provaria nada. `forCheckpoint` direto refaz a conta.
                WorldTreeFoliagePlan copaA = WorldTreeFoliagePlan.of(primeiro,
                        WorldTreeBranchNetwork.secondaryAndTertiary(primeiro));
                WorldTreeFoliagePlan copaB = WorldTreeFoliagePlan.of(segundo,
                        WorldTreeBranchNetwork.secondaryAndTertiary(segundo));
                assertEquals(
                        WorldTreeClimbingPost.forCheckpoint(primeiro, checkpoint.y(), copaA),
                        WorldTreeClimbingPost.forCheckpoint(segundo, checkpoint.y(), copaB),
                        "seed " + seed + ", " + checkpoint + ": duas consultas deram"
                                + " cabanas diferentes.");
            }
        }
    }

    @Test
    @DisplayName("a ancora fica na altura do CHECKPOINT -- senao ela nao e reconhecida")
    void ancoraDentroDaJanelaDoCheckpoint() {
        // A TRAVA QUE DECIDE O DESENHO INTEIRO, e ela nao e visual: e de codigo.
        // `WorldTreeCheckpoint.nearest(y)` mapeia a altura da ancora de volta para
        // o checkpoint com tolerancia de 24 blocos. Uma cabana posta na altura do
        // GALHO -- que e o obvio, e foi a primeira ideia -- sai dessa janela, e o
        // jogador clica na ancora e recebe "este anchor nao pertence a rota".
        //
        // Nao ha erro em lugar nenhum: a cabana esta la, bonita, e o checkpoint
        // nao funciona.
        for (int index = 0; index < SEEDS; index++) {
            long seed = seedAt(index);
            WorldTreeLayout layout = WorldTreeLayoutGenerator.generate(seed, 0, 0);
            for (WorldTreeCheckpoint checkpoint : WorldTreeCheckpoint.values()) {
                WorldTreeClimbingPost post =
                        WorldTreeClimbingPosts.of(layout).get(checkpoint.ordinal());
                assertEquals(checkpoint, WorldTreeCheckpoint.nearest(post.anchorY()),
                        "seed " + seed + ", " + checkpoint + ": a ancora em y="
                                + post.anchorY() + " e reconhecida como "
                                + WorldTreeCheckpoint.nearest(post.anchorY())
                                + ". O checkpoint deixa de funcionar, sem erro nenhum.");
            }
        }
    }

    @Test
    @DisplayName("o poco limpa TUDO do piso para cima -- e ele nao e enfeite")
    void aCabanaNaoNasceEnterrada() {
        // O RELATO: "nasceu dentro da folhagem da arvore... fica impossivel um
        // jogador encontrar". Medido antes do poco, nas vinte seeds: 54% das
        // cabanas nasciam DENTRO da massa de folha e 95% tinham folha por cima.
        //
        // ESTE CASO TEM DUAS METADES, e a segunda e a que importa.
        //
        // A primeira e barata: o corte do poco fica ABAIXO do piso, entao nada
        // sobrevive da cabana para cima. Isso e quase construcao -- e a regra
        // conferindo a si mesma.
        //
        // A segunda pergunta se a regra ainda faz alguma coisa: quantas cabanas
        // TERIAM folha por cima se o poco nao existisse. Se esse numero cair para
        // zero -- porque alguem mudou onde as cabanas nascem, ou a copa encolheu
        // de novo --, o poco virou codigo morto e este teste passa a mentir
        // dizendo que protege algo. A regua avisa em vez de ficar verde a toa.
        int comFolhaPorCimaSemPoco = 0;
        int total = 0;
        for (int index = 0; index < SEEDS; index++) {
            long seed = seedAt(index);
            WorldTreeLayout layout = WorldTreeLayoutGenerator.generate(seed, 0, 0);
            WorldTreeFoliagePlan plano = WorldTreeFoliagePlan.of(layout,
                    WorldTreeBranchNetwork.secondaryAndTertiary(layout));
            for (WorldTreeCheckpoint checkpoint : WorldTreeCheckpoint.values()) {
                WorldTreeClimbingPost post =
                        WorldTreeClimbingPosts.of(layout).get(checkpoint.ordinal());
                total++;

                int corte = post.baseDoPoco();
                assertTrue(corte < post.floorY(),
                        "seed " + seed + ", " + checkpoint + ": o poco so limpa a partir"
                                + " de y=" + corte + " e o piso esta em " + post.floorY()
                                + ". A folha entraria na cabana.");

                for (WorldTreeFoliageShelf shelf : plano.shelves()) {
                    double dx = post.centerX() - shelf.centerX();
                    double dz = post.centerZ() - shelf.centerZ();
                    if (dx * dx + dz * dz <= shelf.radius() * shelf.radius()
                            && shelf.maxY() > post.roofY()) {
                        comFolhaPorCimaSemPoco++;
                        break;
                    }
                }
            }
        }
        assertTrue(comFolhaPorCimaSemPoco > total / 4,
                "sem o poco, so " + comFolhaPorCimaSemPoco + " de " + total
                        + " cabanas teriam folha por cima. O poco deixou de proteger"
                        + " alguma coisa -- ou as cabanas mudaram de lugar, ou a copa"
                        + " encolheu. Confira se ele ainda precisa existir antes de"
                        + " afrouxar este numero.");
    }

    @Test
    @DisplayName("a TORRE emerge da folhagem local -- e e por isso que ela existe")
    void aTorreEmerge() {
        // O PEDIDO: "extenda um pouco a cabana para ela ficar naturalmente acima
        // das folhas tambem, possuir estrutura la".
        //
        // A cabana sozinha nao resolve: ela fica na altura do checkpoint, que e
        // onde o galho esta -- dentro do andar de folhagem daquele galho. Mesmo
        // com o poco aberto, ela e uma caixa no fundo de um buraco.
        //
        // Este caso mede contra o plano de copa REAL: nenhuma prateleira do andar
        // local pode terminar acima do mirante.
        for (int index = 0; index < SEEDS; index++) {
            long seed = seedAt(index);
            WorldTreeLayout layout = WorldTreeLayoutGenerator.generate(seed, 0, 0);
            WorldTreeFoliagePlan plano = WorldTreeFoliagePlan.of(layout,
                    WorldTreeBranchNetwork.secondaryAndTertiary(layout));
            for (WorldTreeCheckpoint checkpoint : WorldTreeCheckpoint.values()) {
                WorldTreeClimbingPost post =
                        WorldTreeClimbingPosts.of(layout).get(checkpoint.ordinal());
                int limiteLocal = post.roofY() + WorldTreeClimbingPost.ALCANCE_DO_POCO;
                for (WorldTreeFoliageShelf shelf : plano.shelves()) {
                    double dx = post.centerX() - shelf.centerX();
                    double dz = post.centerZ() - shelf.centerZ();
                    double alcance = shelf.radius() + WorldTreeClimbingPost.RAIO_DA_CLAREIRA;
                    if (dx * dx + dz * dz > alcance * alcance) {
                        continue;
                    }
                    if (shelf.maxY() > limiteLocal) {
                        // Folha de OUTRO andar: deliberadamente fora do alcance.
                        continue;
                    }
                    assertTrue(shelf.maxY() <= post.topoDaTorre(),
                            "seed " + seed + ", " + checkpoint + ": folha do andar local"
                                    + " sobe ate y=" + (int) shelf.maxY() + " e o mirante"
                                    + " para em " + post.topoDaTorre()
                                    + ". A torre nasce enterrada.");
                }
                assertTrue(post.topoDaTorre() > post.roofY(),
                        "seed " + seed + ", " + checkpoint + ": torre de altura zero.");
            }
        }
    }

    @Test
    @DisplayName("o poco tem TETO -- ele furava a copa inteira, de baixo a cima")
    void oPocoNaoAtravessaACopa() {
        // O DEFEITO RELATADO: "atualmente ela abre um buraco ate o bloco mais
        // alto, ou seja uma cabana em 500 mantem o buraco ate um galho em 1000 ou
        // 1500; deve influenciar apenas no seu proprio galho".
        //
        // O poco nao tinha teto: limpava do piso para cima, sem limite. Sete
        // cabanas viravam sete tubos atravessando a copa de baixo a cima, levando
        // junto a folhagem de galhos que nao tem nada a ver com elas.
        int maior = 0;
        for (int index = 0; index < SEEDS; index++) {
            long seed = seedAt(index);
            WorldTreeLayout layout = WorldTreeLayoutGenerator.generate(seed, 0, 0);
            for (WorldTreeCheckpoint checkpoint : WorldTreeCheckpoint.values()) {
                WorldTreeClimbingPost post =
                        WorldTreeClimbingPosts.of(layout).get(checkpoint.ordinal());
                int altura = post.topoDoPoco() - post.baseDoPoco();
                maior = Math.max(maior, altura);
                assertTrue(post.topoDoPoco() < post.floorY()
                                + WorldTreeClimbingPost.ALCANCE_DO_POCO + 20,
                        "seed " + seed + ", " + checkpoint + ": o poco tem " + altura
                                + " blocos de altura. Ele so pode alcancar o andar de"
                                + " folhagem do PROPRIO galho.");
            }
        }
        // E ele tem de alcancar alguma coisa: um poco de altura minima em todas as
        // cabanas significa que ele parou de limpar folha.
        assertTrue(maior > 20,
                "o maior poco tem " + maior + " blocos. Se ele encolheu ate isto,"
                        + " ele parou de abrir a folhagem local -- confira antes de"
                        + " afrouxar o limite acima.");
    }

    @Test
    @DisplayName("o poco cobre a cabana INTEIRA, e nao so o centro")
    void pocoCobreACabanaInteira() {
        // ALIMENTAR O PORTAO COM O DEFEITO: encolher RAIO_DA_CLAREIRA para menos
        // que RAIO deixa os cantos da cabana de fora do poco -- a folha encosta
        // nas paredes e a clareira deixa de ler como clareira.
        for (int index = 0; index < SEEDS; index += 5) {
            long seed = seedAt(index);
            WorldTreeLayout layout = WorldTreeLayoutGenerator.generate(seed, 0, 0);
            for (WorldTreeCheckpoint checkpoint : WorldTreeCheckpoint.values()) {
                WorldTreeClimbingPost post =
                        WorldTreeClimbingPosts.of(layout).get(checkpoint.ordinal());
                for (int dx = -WorldTreeClimbingPost.RAIO;
                        dx <= WorldTreeClimbingPost.RAIO; dx++) {
                    for (int dz = -WorldTreeClimbingPost.RAIO;
                            dz <= WorldTreeClimbingPost.RAIO; dz++) {
                        assertTrue(post.colunaNoPoco(post.centerX() + dx, post.centerZ() + dz),
                                "seed " + seed + ", " + checkpoint + ": a coluna ("
                                        + dx + "," + dz + ") da cabana esta FORA do poco."
                                        + " A folha encosta na parede.");
                    }
                }
                // E o poco acaba: ele nao pode virar uma cratera na copa.
                assertTrue(!post.colunaNoPoco(post.centerX() + 40, post.centerZ()),
                        "o poco alcanca 40 blocos de lado -- isso e cratera, e nao"
                                + " clareira.");
            }
        }
    }

    @Test
    @DisplayName("nenhuma cabana nasce ABAIXO do pe da arvore -- la e vazio")
    void cabanaAcimaDoPeDaArvore() {
        // O CASO QUE ESCAPOU DE TODOS OS OUTROS PORTOES DAQUI. BASE mora em y=48
        // e o tronco COMECA em y=48: o piso em `checkpoint.y() - 1` caia em 47.
        // Abaixo do pe a dimensao e vazio -- sem chao, sem tronco, sem nada em
        // que encostar --, entao a cabana do primeiro checkpoint da rota nascia
        // boiando no nada.
        //
        // Os outros casos nao viam: `cabanaApoiada` compara a parede com a casca
        // calculada no y CLAMPADO do tronco, e a conta fechava; `aMaioriaFicaEmGalho`
        // so olha qual suporte foi escolhido.
        for (int index = 0; index < SEEDS; index++) {
            long seed = seedAt(index);
            WorldTreeLayout layout = WorldTreeLayoutGenerator.generate(seed, 0, 0);
            for (WorldTreeCheckpoint checkpoint : WorldTreeCheckpoint.values()) {
                WorldTreeClimbingPost post =
                        WorldTreeClimbingPosts.of(layout).get(checkpoint.ordinal());
                assertTrue(post.floorY() > layout.trunk().baseY(),
                        "seed " + seed + ", " + checkpoint + ": piso em y="
                                + post.floorY() + " e a arvore comeca em y="
                                + layout.trunk().baseY() + ". Abaixo disso e vazio.");
            }
        }
    }

    @Test
    @DisplayName("a cabana tem MADEIRA embaixo -- galho ou tronco, nunca ar")
    void cabanaApoiada() {
        // O DEFEITO DO RELATO, medido: "o lugar spawna flutuando de qualquer
        // jeito". Uma cabana e uma prateleira tem o mesmo problema se nada as
        // segura.
        for (int index = 0; index < SEEDS; index++) {
            long seed = seedAt(index);
            WorldTreeLayout layout = WorldTreeLayoutGenerator.generate(seed, 0, 0);
            for (WorldTreeCheckpoint checkpoint : WorldTreeCheckpoint.values()) {
                WorldTreeClimbingPost post =
                        WorldTreeClimbingPosts.of(layout).get(checkpoint.ordinal());
                if (post.suporte() == WorldTreeClimbingPost.Suporte.GALHO) {
                    // IGUAL AO PISO E VALIDO: o galho raspa a cabana e o pilar
                    // tem comprimento zero. Acima do piso nao e: o laco do pilar
                    // escreveria dentro do comodo.
                    assertTrue(post.topoDoSuporte() <= post.floorY(),
                            "seed " + seed + ", " + checkpoint + ": o topo do apoio"
                                    + " esta em y=" + post.topoDoSuporte() + ", ACIMA do"
                                    + " piso em " + post.floorY() + " -- o pilar"
                                    + " escreveria dentro do comodo.");
                    assertTrue(post.floorY() - post.topoDoSuporte()
                                    <= WorldTreeClimbingPost.DESVIO_MAXIMO_DO_GALHO,
                            "seed " + seed + ", " + checkpoint + ": pilar de "
                                    + (post.floorY() - post.topoDoSuporte())
                                    + " blocos. Acima do limite isso vira poste, e nao"
                                    + " apoio.");
                    assertTrue(apoiadaEmGalho(layout, post),
                            "seed " + seed + ", " + checkpoint + ": o ponto escolhido"
                                    + " nao corresponde a galho nenhum do layout.");
                } else {
                    // O EIXO CENTRAL NAO E SEMPRE O TRONCO, e este teste ja errou
                    // por ignorar isso: o tronco acaba em y=1200, e do 1100 ao
                    // 1450 quem sobe e o LIDER, com outro eixo e outro raio.
                    // Comparar a cabana do SUMMIT com o raio do tronco reprovava
                    // uma cabana CORRETA -- a regua tem de ler a mesma verdade que
                    // o codigo.
                    double casca;
                    double eixoX;
                    if (post.floorY() > layout.trunk().topY()) {
                        WorldTreePoint eixo = WorldTreeTrunkSurface.leaderCenter(
                                post.floorY(), seed);
                        casca = WorldTreeTrunkSurface.leaderRadius(post.floorY());
                        eixoX = eixo.x();
                    } else {
                        int y = Math.max(layout.trunk().baseY(), post.floorY());
                        casca = WorldTreeTrunkSurface.naDirecaoX(
                                layout.trunk().radiusAt(y), y, seed);
                        eixoX = 0.0;
                    }
                    int paredeDeDentro = post.centerX() - WorldTreeClimbingPost.RAIO;
                    assertTrue(Math.abs(paredeDeDentro - (eixoX + casca)) <= 1.5,
                            "seed " + seed + ", " + checkpoint + ": a parede de dentro"
                                    + " esta em x=" + paredeDeDentro + " e a casca em "
                                    + String.format("%.1f", eixoX + casca)
                                    + ". A cabana nao encosta no eixo central.");
                }
            }
        }
    }

    @Test
    @DisplayName("os checkpoints acima de y=320 ficam num GALHO -- era o pedido")
    void aMaioriaFicaEmGalho() {
        // O PEDIDO FOI "em um galho da propria arvore", e a medida diz quanto
        // disso e alcancavel: as cinco zonas de galho vao de y=320 a y=1480, e
        // BASE (48) e LOWER (260) ficam ABAIXO da primeira. Nas vinte seeds, o
        // galho mais proximo de BASE esta a 297 blocos e o de LOWER a 85.
        //
        // Este caso nao finge que os sete ficam em galho. Ele exige os CINCO que
        // podem, e reprova se algum deles cair para o tronco -- que seria a
        // regressao de verdade.
        for (int index = 0; index < SEEDS; index++) {
            long seed = seedAt(index);
            WorldTreeLayout layout = WorldTreeLayoutGenerator.generate(seed, 0, 0);
            for (WorldTreeCheckpoint checkpoint : WorldTreeCheckpoint.values()) {
                WorldTreeClimbingPost post =
                        WorldTreeClimbingPosts.of(layout).get(checkpoint.ordinal());
                if (checkpoint == WorldTreeCheckpoint.SUMMIT) {
                    // O SUMMIT E O UNICO QUE PODE IR PARA QUALQUER UM DOS DOIS, e
                    // a razao e geometrica: os galhos da zona do topo NASCEM em
                    // y=1450, que e a altura dele. Dependendo da seed, o galho mais
                    // proximo tem o topo oito blocos acima do piso -- alto demais
                    // para a cabana raspar sem furar o galho --, e ai o apoio certo
                    // e o LIDER central, que termina exatamente ali. Uma cabana no
                    // apice e o lugar de um mirante.
                    continue;
                }
                if (checkpoint.y() >= 400) {
                    assertEquals(WorldTreeClimbingPost.Suporte.GALHO, post.suporte(),
                            "seed " + seed + ", " + checkpoint + " (y=" + checkpoint.y()
                                    + "): caiu para o eixo central, e ha galho nessa faixa.");
                } else {
                    assertEquals(WorldTreeClimbingPost.Suporte.TRONCO, post.suporte(),
                            "seed " + seed + ", " + checkpoint + " (y=" + checkpoint.y()
                                    + "): achou galho abaixo de y=320. Se a arvore"
                                    + " mudou, esta expectativa precisa mudar junto.");
                }
            }
        }
    }

    @Test
    @DisplayName("o jogador pousa DENTRO da cabana, e nao em cima da ancora")
    void pousoDentroDaCabana() {
        for (int index = 0; index < SEEDS; index++) {
            long seed = seedAt(index);
            WorldTreeLayout layout = WorldTreeLayoutGenerator.generate(seed, 0, 0);
            for (WorldTreeCheckpoint checkpoint : WorldTreeCheckpoint.values()) {
                WorldTreeClimbingPost post =
                        WorldTreeClimbingPosts.of(layout).get(checkpoint.ordinal());
                int x = (int) Math.floor(post.spawnX());
                int z = (int) Math.floor(post.spawnZ());
                assertTrue(post.naPegada(x, z) && !post.naParede(x, z),
                        "seed " + seed + ", " + checkpoint + ": o ponto de pouso ("
                                + x + "," + z + ") cai na parede ou fora da cabana.");
                assertTrue(x != post.centerX() || z != post.centerZ(),
                        "seed " + seed + ", " + checkpoint + ": o pouso e em cima da"
                                + " propria ancora -- o motor empurra o jogador para"
                                + " fora pela primeira face livre.");
            }
        }
    }

    @Test
    @DisplayName("a cabana do pe da arvore encosta no fuste -- ela estava 16 blocos no ar")
    void cabanaDoPeEncosta() {
        // O QUE HAVIA: (origem + 49, 64 + 16, origem), tres numeros cravados, com
        // UM bloco de lenho embaixo.
        for (int index = 0; index < SEEDS; index++) {
            long seed = seedAt(index);
            WorldTreeLayout layout = WorldTreeLayoutGenerator.generate(seed, 0, 0);
            WorldTreeClimbingPost post = WorldTreeBaseGenerator.postoDoPe(layout);
            double casca = WorldTreeTrunkSurface.naDirecaoX(
                    WorldTreeBaseGenerator.raioNominalEm(post.floorY()),
                    post.floorY(), seed);
            int paredeDeDentro = post.centerX() - layout.overworldOriginX()
                    - WorldTreeClimbingPost.RAIO;
            assertTrue(Math.abs(paredeDeDentro - casca) <= 1.5,
                    "seed " + seed + ": a parede de dentro esta a " + paredeDeDentro
                            + " do eixo e a casca em " + String.format("%.1f", casca));
        }
    }

    @Test
    @DisplayName("a casca do tronco e a MESMA conta em toda parte")
    void umaFonteSoParaACasca() {
        // O DEFEITO QUE ORIGINOU TUDO ISTO foi a forma da casca existir so dentro
        // do laco que escreve blocos. Este caso amarra o ponto fixo: o raio
        // devolvido por `naDirecaoX` tem de ser um raio que `irregularRadius`
        // confirma naquela coordenada.
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
                                + String.format("%.2f vs %.2f", casca, confirmacao) + ").");
            }
        }
    }

    @Test
    @DisplayName("o CHECKPOINT e escrito antes da COPA -- senao a cabana ganha folha dentro")
    void checkpointVemAntesDaCopa() throws IOException {
        // PORTAO DE TEXTO, e nao de comportamento -- provar a ordem de verdade
        // exigiria um chunk, e portanto um gametest. Ele le a fonte e compara duas
        // posicoes. E pouco; e MUITO mais que o comentario que era toda a protecao
        // que existia.
        //
        // A cabana sobrevive melhor que a prateleira antiga: ela ESCREVE por cima
        // do que houver, entao a copa nao a fura mais. Mas a copa depois dela
        // continua sendo o certo -- folha so entra em ar, e o interior esvaziado E
        // ar: invertida, a cabana ganharia folhagem por dentro.
        String fonte = Files.readString(Repo.raiz().resolve(
                "src/main/java/com/darkcontinent/nenfoundation/worldtree/"
                        + "WorldTreeChunkGenerator.java"), StandardCharsets.UTF_8);
        int checkpoint = fonte.indexOf("WorldTreeCheckpointGenerator.generate(chunk");
        int copa = fonte.indexOf("WorldTreeCanopyGenerator.generate(chunk");
        assertTrue(checkpoint >= 0 && copa >= 0,
                "nao achei as duas chamadas em buildSurface; o portao parou de"
                        + " vigiar o que devia.");
        assertTrue(checkpoint < copa,
                "a copa e escrita ANTES do checkpoint. O interior esvaziado da"
                        + " cabana e ar, e folha entra em ar: a cabana ganharia"
                        + " folhagem por dentro.");
    }

    /** Se o ponto de apoio escolhido corresponde mesmo a um galho do layout. */
    private static boolean apoiadaEmGalho(WorldTreeLayout layout, WorldTreeClimbingPost post) {
        for (WorldTreeBranchNode node : layout.branchNodes()) {
            WorldTreeSpline spline = node.spline();
            for (int amostra = 0; amostra <= 40; amostra++) {
                double t = (double) amostra / 40;
                WorldTreePoint ponto = spline.pointAt(t);
                if (Math.round(ponto.x()) == post.centerX()
                        && Math.round(ponto.z()) == post.centerZ()
                        && spline.radiusAt(t) >= WorldTreeClimbingPost.GALHO_MINIMO) {
                    return true;
                }
            }
        }
        return false;
    }
}
