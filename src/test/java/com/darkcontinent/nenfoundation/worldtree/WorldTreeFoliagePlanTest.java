package com.darkcontinent.nenfoundation.worldtree;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.darkcontinent.nenfoundation.worldtree.generation.WorldTreeBranchNetwork;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * O portao da copa, em vinte seeds.
 *
 * <p>CADA ITEM AQUI EXISTE POR CAUSA DE UM DEFEITO QUE FOI VISTO EM JOGO, e a
 * lista esta em {@code docs/worldtree/copa-e-folhagem.md} secao 1. Nenhum deles
 * dava erro: a copa simplesmente sumia, ou virava confete, ou virava luva.
 *
 * <p><b>O QUE ESTE ARQUIVO NAO PODE FAZER:</b> julgar aparencia. Ele mede
 * geometria -- cobertura, camadas, sobreposicao, ancoragem, volume. A aprovacao
 * final continua sendo captura comparada com {@code docs/insp/arvoremundo.png},
 * por gente olhando. Um verde aqui e condicao necessaria, e nunca suficiente.
 */
class WorldTreeFoliagePlanTest {

    private static final int SEEDS = 20;

    private static WorldTreeFoliagePlan plan(long seed) {
        WorldTreeLayout layout = WorldTreeLayoutGenerator.generate(seed, 0, 0);
        return WorldTreeFoliagePlan.of(layout, WorldTreeBranchNetwork.secondaryAndTertiary(layout));
    }

    private static long seedAt(int index) {
        return 1_000L + index * 7_919L;
    }

    // ------------------------------------------------------- determinismo

    @Test
    @DisplayName("mesma seed, mesmo plano -- senao a copa corta na fronteira de chunk")
    void deterministico() {
        // UM CHUNK NAO SABE O QUE O VIZINHO GEROU. A unica coisa que mantem a
        // copa continua atraves da fronteira e o plano ser funcao pura da seed.
        // Um sorteio nao semeado aqui apareceria como folha cortada em linha
        // reta a cada 16 blocos -- e ninguem atribuiria isso ao planejador.
        WorldTreeFoliagePlan first = plan(4242L);
        WorldTreeFoliagePlan second = plan(4242L);
        assertEquals(first.shelves(), second.shelves());
        assertEquals(first.vines(), second.vines());
    }

    // ------------------------------------------------- a copa existe mesmo

    @Test
    @DisplayName("a copa cobre o disco dela -- contra a copa rala")
    void coberturaProjetada() {
        // O DEFEITO 1.1 E 1.2: prateleira dentro da madeira mais ruido branco a
        // 38% davam uma copa que, vista de cima, era quase toda buraco.
        //
        // O LIMITE DESCEU DE 0,55 PARA 0,45, e afrouxar portao exige motivo
        // escrito. O motivo e que o 0,55 media outra arvore:
        //
        // 1. A COROA ESTAVA CARREGANDO ESTE PORTAO. As dezesseis prateleiras do
        //    lider tinham raio ate 185 -- um disco de 107 mil blocos de area
        //    dentro de um disco de copa de 188 mil. Sozinhas, elas cobriam 57%
        //    do que este teste mede. O verde vinha da laje que o jogador reclamou
        //    de ver no tronco, e nao da copa.
        //
        // 2. A COPA ENCOLHEU DE PROPOSITO, duas vezes e a pedido: 64% menos folha
        //    para a madeira dos galhos aparecer, e agora a coroa em linha com o
        //    resto. Uma copa concentrada na ponta dos galhos DEIXA ceu entre os
        //    tufos -- e esse ceu e o pedido, nao o defeito.
        //
        // O que 0,45 ainda reprova e o que este portao nasceu para pegar: copa
        // que, vista de cima, e quase toda buraco. O defeito original media 38%
        // de ruido branco no volume inteiro, e produzia cobertura bem abaixo
        // disto.
        //
        // Medido hoje: 0,513 no pior caso das vinte seeds.
        for (int index = 0; index < SEEDS; index++) {
            long seed = seedAt(index);
            double cobertura = coberturaProjetada(plan(seed));
            assertTrue(cobertura >= 0.45,
                    "seed " + seed + ": a copa cobre so " + String.format("%.0f%%", cobertura * 100)
                            + " do proprio disco. Vista de cima, isso e peneira, e nao arvore.");
        }
    }

    @Test
    @DisplayName("a copa e MUITO maior que o tronco -- senao nao ha copa, ha um poste")
    void copaMaiorQueOTronco() {
        for (int index = 0; index < SEEDS; index++) {
            long seed = seedAt(index);
            WorldTreeLayout layout = WorldTreeLayoutGenerator.generate(seed, 0, 0);
            double raioDaCopa = raioDaCopa(plan(seed));
            double raioDoTronco = layout.trunk().baseRadius();
            assertTrue(raioDaCopa >= raioDoTronco * 3.0,
                    "seed " + seed + ": copa de raio " + (int) raioDaCopa
                            + " sobre tronco de raio " + (int) raioDoTronco
                            + ". A referencia tem copa varias vezes mais larga que o tronco.");
        }
    }

    @Test
    @DisplayName("a copa tem varios andares -- a referencia empilha, nao e um chapeu")
    void multiplasCamadas() {
        for (int index = 0; index < SEEDS; index++) {
            long seed = seedAt(index);
            // Faixas de 60 blocos: duas prateleiras dentro da mesma faixa leem
            // como um andar so.
            TreeSet<Integer> andares = new TreeSet<>();
            for (WorldTreeFoliageShelf shelf : plan(seed).shelves()) {
                andares.add((int) (shelf.centerY() / 60.0));
            }
            assertTrue(andares.size() >= 6,
                    "seed " + seed + ": so " + andares.size() + " andares de copa.");
        }
    }

    @Test
    @DisplayName("o LIDER CENTRAL tem folha -- ele era madeira macica ate o topo")
    void aCoroaTemFolha() {
        // A PRIMEIRA VERSAO DESTE TESTE NAO PEGAVA NADA, e so a quebra
        // deliberada revelou: ela pedia "alguma folha acima de y=1300", e os
        // galhos da zona SUMMIT nascem em y=1450 e satisfaziam sozinhos. Dava
        // para apagar a copa da coroa inteira com o portao verde.
        //
        // O que importa e folha PERTO DO EIXO la em cima: o lider central e
        // vertical em x=0,z=0, e sao os discos dele que impedem o topo de ser um
        // poste. Os galhos de SUMMIT ficam longe do eixo e nao substituem isso.
        for (int index = 0; index < SEEDS; index++) {
            long seed = seedAt(index);
            long noEixo = plan(seed).shelves().stream()
                    .filter(shelf -> shelf.centerY() >= 1300.0)
                    .filter(shelf -> Math.hypot(shelf.centerX(), shelf.centerZ()) < 40.0)
                    .count();
            assertTrue(noEixo >= 2,
                    "seed " + seed + ": so " + noEixo + " prateleiras perto do eixo acima"
                            + " de y=1300. O lider central desenha madeira de 1100 a 1450;"
                            + " sem copa ali, o topo da arvore e um poste.");
        }
    }

    @Test
    @DisplayName("a folha aparece POR FORA da madeira -- o defeito original")
    void aFolhaEscapaDaMadeira() {
        // ESTE PORTAO NASCEU DE UMA QUEBRA QUE PASSOU. Ao alimentar as reguas
        // com o defeito original -- prateleira centrada no eixo do galho --,
        // TODAS passaram: o disco continuava ancorado, chato, com as duas faces
        // e sobreposto ao vizinho. So que ficava DENTRO da madeira, e como folha
        // nao sobrescreve madeira, nao virava um bloco sequer.
        //
        // O buraco era que toda regua olhava a prateleira SOZINHA. Esta olha ela
        // contra a madeira que ela veste.
        for (int index = 0; index < SEEDS; index++) {
            long seed = seedAt(index);
            for (WorldTreeFoliageShelf shelf : plan(seed).shelves()) {
                assertTrue(shelf.escapesWood(),
                        "seed " + seed + ", suporte " + shelf.suporte() + ": disco de raio "
                                + (int) shelf.radius() + " e massa de " + (int) shelf.minY()
                                + " a " + (int) shelf.maxY() + " sobre madeira de raio "
                                + (int) shelf.branchRadius() + " ancorada em "
                                + (int) shelf.anchorY() + ". Folha dentro de madeira nao"
                                + " vira bloco -- e o defeito que fazia a copa sumir.");
            }
        }
    }

    // ---------------------------------------------------------- a luva

    @Test
    @DisplayName("prateleiras vizinhas se SOBREPOEM -- este e o teste da luva")
    void semDedos() {
        // AS "COBERTURAS QUE PARECIAM LUVAS" vinham disto: clusters de raio
        // parecido, enfileirados ao longo do galho, SEM se tocarem. Cada um lia
        // como um dedo. A cura nao e diminuir o espaco entre eles no olho: e
        // exigir sobreposicao, e medir.
        for (int index = 0; index < SEEDS; index++) {
            long seed = seedAt(index);
            Map<Integer, List<WorldTreeFoliageShelf>> porGalho = agruparPorGalho(plan(seed));
            for (Map.Entry<Integer, List<WorldTreeFoliageShelf>> entrada : porGalho.entrySet()) {
                List<WorldTreeFoliageShelf> fila = entrada.getValue();
                for (int i = 1; i < fila.size(); i++) {
                    WorldTreeFoliageShelf anterior = fila.get(i - 1);
                    WorldTreeFoliageShelf atual = fila.get(i);
                    double distancia = Math.hypot(atual.centerX() - anterior.centerX(),
                            atual.centerZ() - anterior.centerZ());
                    double soma = anterior.radius() + atual.radius();
                    double sobreposicao = 1.0 - distancia / soma;
                    assertTrue(sobreposicao >= WorldTreeFoliagePlan.SOBREPOSICAO_MINIMA,
                            "seed " + seed + ", galho " + entrada.getKey() + ", prateleiras "
                                    + (i - 1) + " e " + i + ": sobreposicao de "
                                    + String.format("%.2f", sobreposicao)
                                    + ". Discos que nao se tocam leem como DEDOS.");
                }
            }
        }
    }

    @Test
    @DisplayName("o raio decresce em direcao a ponta -- senao a copa vira haltere")
    void afunilaNaPonta() {
        for (int index = 0; index < SEEDS; index++) {
            long seed = seedAt(index);
            for (Map.Entry<Integer, List<WorldTreeFoliageShelf>> entrada
                    : agruparPorGalho(plan(seed)).entrySet()) {
                List<WorldTreeFoliageShelf> fila = entrada.getValue();
                if (fila.size() < 2) {
                    continue;
                }
                WorldTreeFoliageShelf primeira = fila.get(0);
                WorldTreeFoliageShelf ultima = fila.get(fila.size() - 1);
                assertTrue(ultima.radius() < primeira.radius(),
                        "seed " + seed + ", galho " + entrada.getKey()
                                + ": a prateleira da ponta (" + (int) ultima.radius()
                                + ") nao e menor que a da base (" + (int) primeira.radius()
                                + "). Discos do mesmo tamanho ate a ponta leem como haltere.");
            }
        }
    }

    // ------------------------------------------------------ ilha flutuante

    @Test
    @DisplayName("nenhuma prateleira solta: toda uma ancorada em madeira alcancavel")
    void semIlhaFlutuante() {
        for (int index = 0; index < SEEDS; index++) {
            long seed = seedAt(index);
            for (WorldTreeFoliageShelf shelf : plan(seed).shelves()) {
                // O centro do disco nao pode estar longe do eixo do galho que o
                // sustenta: se estiver, a massa aparece sem nada embaixo.
                assertTrue(shelf.anchorOffset() <= shelf.radius() * 0.35 + 1.0,
                        "seed " + seed + ": prateleira a " + (int) shelf.anchorOffset()
                                + " blocos do eixo do galho, com raio "
                                + (int) shelf.radius() + ". Isso aparece como ilha.");
                // E a ancora tem de estar DENTRO da massa verticalmente, senao a
                // folha flutua acima do galho com um vao no meio.
                assertTrue(shelf.anchorY() >= shelf.minY() - 2.0
                                && shelf.anchorY() <= shelf.maxY(),
                        "seed " + seed + ": o galho passa fora da prateleira em Y."
                                + " Ancora em " + (int) shelf.anchorY() + ", massa de "
                                + (int) shelf.minY() + " a " + (int) shelf.maxY() + ".");
            }
        }
    }

    @Test
    @DisplayName("nenhuma folha alem de onde a MADEIRA e desenhada")
    void copaCabeNoAlcanceDaMadeira() {
        // ESTE E UM CONTRATO ENTRE DOIS ARQUIVOS, e do tipo que se quebra sozinho.
        // `WorldTreeBranchGenerator` descarta o chunk inteiro alem de
        // MAX_BRANCH_REACH -- nenhuma madeira e escrita la. Se a copa crescer
        // para fora desse raio, a folha aparece pendurada em NADA, longe do
        // tronco, e ninguem liga o defeito ao numero que o causou.
        //
        // Aumentar o raio das prateleiras, alongar os galhos ou acrescentar um
        // nivel de subgalho sao tres mudancas plausiveis que estouram isto.
        double limite = com.darkcontinent.nenfoundation.worldtree.generation
                .WorldTreeBranchGenerator.MAX_BRANCH_REACH;
        for (int index = 0; index < SEEDS; index++) {
            long seed = seedAt(index);
            for (WorldTreeFoliageShelf shelf : plan(seed).shelves()) {
                double alcance = Math.hypot(shelf.centerX(), shelf.centerZ()) + shelf.radius();
                assertTrue(alcance <= limite,
                        "seed " + seed + ": copa chega a " + (int) alcance
                                + " blocos, e a madeira para em " + (int) limite
                                + ". A folha de la aparece pendurada em nada.");
            }
        }
    }

    @Test
    @DisplayName("toda vinha nasce SOB uma prateleira -- vinha orfa le como bug")
    void semVinhaOrfa() {
        for (int index = 0; index < SEEDS; index++) {
            long seed = seedAt(index);
            WorldTreeFoliagePlan plano = plan(seed);
            for (WorldTreeVineStrand vine : plano.vines()) {
                assertTrue(vine.shelfIndex() < plano.shelves().size(),
                        "seed " + seed + ": vinha apontando para prateleira inexistente");
                WorldTreeFoliageShelf shelf = plano.shelves().get(vine.shelfIndex());
                double distancia = Math.hypot(vine.originX() - shelf.centerX(),
                        vine.originZ() - shelf.centerZ());
                assertTrue(distancia <= shelf.radius() + 0.5,
                        "seed " + seed + ": vinha nasce a " + (int) distancia
                                + " blocos do centro de uma prateleira de raio "
                                + (int) shelf.radius() + " -- fora dela, pendurada em nada.");
                assertTrue(vine.originY() <= shelf.centerY(),
                        "seed " + seed + ": vinha nascendo ACIMA do centro da prateleira");
            }
        }
    }

    // ------------------------------------------------------ folha em baixo

    @Test
    @DisplayName("toda prateleira tem massa em cima E embaixo")
    void folhaEmCimaEEmbaixo() {
        for (int index = 0; index < SEEDS; index++) {
            long seed = seedAt(index);
            for (WorldTreeFoliageShelf shelf : plan(seed).shelves()) {
                assertTrue(shelf.topThickness() > 0.0 && shelf.bottomThickness() > 0.0,
                        "seed " + seed + ": prateleira sem uma das duas faces");
                // A de baixo e MENOR, mas nao pode ser desprezivel: e ela que
                // impede a copa de parecer um guarda-sol visto do chao.
                assertTrue(shelf.bottomThickness() >= shelf.topThickness() * 0.4,
                        "seed " + seed + ": a saia inferior ficou fina demais ("
                                + String.format("%.1f", shelf.bottomThickness()) + " contra "
                                + String.format("%.1f", shelf.topThickness()) + " em cima)");
            }
        }
    }

    @Test
    @DisplayName("a prateleira e CHATA: larga em XZ e fina em Y")
    void chataEmVezDeBolha() {
        for (int index = 0; index < SEEDS; index++) {
            long seed = seedAt(index);
            for (WorldTreeFoliageShelf shelf : plan(seed).shelves()) {
                double altura = shelf.topThickness() + shelf.bottomThickness();
                assertTrue(altura < shelf.radius(),
                        "seed " + seed + ": prateleira de raio " + (int) shelf.radius()
                                + " e altura " + (int) altura
                                + ". Isso e uma bolha, e bolha em fila vira brocolis.");
            }
        }
    }

    // ------------------------------------------------------------- custo

    @Test
    @DisplayName("o custo tem teto, e ele nao e um botao")
    void tetoDeCusto() {
        for (int index = 0; index < SEEDS; index++) {
            long seed = seedAt(index);
            WorldTreeFoliagePlan plano = plan(seed);
            assertTrue(plano.shelves().size() <= WorldTreeFoliagePlan.TETO_DE_PRATELEIRAS,
                    "seed " + seed + ": " + plano.shelves().size() + " prateleiras");
            assertTrue(plano.vines().size() <= WorldTreeFoliagePlan.TETO_DE_VINHAS,
                    "seed " + seed + ": " + plano.vines().size() + " vinhas");
            // O TETO NAO PODE MORDER DE VERDADE. Se ele mordesse, cortaria a
            // CAUDA da lista em silencio, e ninguem procuraria a copa que sumiu
            // num limite de seguranca. Ele existe para o caso patologico.
            assertTrue(plano.shelves().size() < WorldTreeFoliagePlan.TETO_DE_PRATELEIRAS,
                    "seed " + seed + ": o teto de seguranca esta MORDENDO. Ou o plano"
                            + " cresceu demais, ou o teto virou parametro de arte.");
            assertTrue(plano.vines().size() < WorldTreeFoliagePlan.TETO_DE_VINHAS,
                    "seed " + seed + ": o teto de vinhas esta mordendo");
        }
    }

    @Test
    @DisplayName("nenhum chunk paga por prateleiras demais -- este e o custo real")
    void custoPorChunk() {
        // A REGUA MUDOU DE GRANDEZA, e vale dizer por que. Ela somava o volume
        // aproximado de cada prateleira -- e a copa e feita de SOBREPOSICAO de
        // proposito, entao aquela soma contava o mesmo bloco varias vezes e
        // media algo que o gerador nunca executa. O que ele executa e varrer, por
        // chunk, a caixa de cada prateleira que o toca.
        long pior = 0;
        int piorPrateleiras = 0;
        for (int index = 0; index < SEEDS; index++) {
            WorldTreeFoliagePlan plano = plan(seedAt(index));
            for (int chunkX = -24; chunkX <= 24; chunkX += 3) {
                for (int chunkZ = -24; chunkZ <= 24; chunkZ += 3) {
                    pior = Math.max(pior, plano.estimatedVisitsForChunk(chunkX, chunkZ));
                    piorPrateleiras = Math.max(piorPrateleiras,
                            plano.shelvesTouchingChunk(chunkX, chunkZ));
                }
            }
        }
        // ESTE NUMERO NAO FOI MEDIDO EM JOGO, e isso esta dito em voz alta aqui e
        // em o-que-nao-provamos.md. Ele e o custo que a copa da REFERENCIA pede
        // no pior chunk -- o que fica sobre o tronco, onde todos os galhos
        // convergem -- mais uma folga de um terco.
        //
        // O que ele protege e regressao de ORDEM DE GRANDEZA: alguem dobrar o
        // numero de prateleiras ou a espessura e nao perceber. O que ele NAO faz
        // e afirmar que o custo atual e aceitavel; isso depende do tempo de
        // geracao real, e a medida sai de um runServer voando pela copa.
        // O TETO DESCEU DE 2.000.000 PARA 800.000, e descer um teto exige o mesmo
        // cuidado que subir: um teto que nao acompanha a realidade para de
        // proteger. Medido depois de a copa migrar para o terco externo dos
        // galhos, o pior chunk das vinte seeds visita 408.834 blocos -- era
        // 1.061.281. O teto e cerca do dobro do medido, e continua sendo uma
        // protecao contra regressao de ORDEM DE GRANDEZA, e nao uma afirmacao de
        // que 408 mil e um custo aceitavel. Isso so sai de um runServer voando
        // pela copa, e esta em o-que-nao-provamos.md.
        assertTrue(pior <= 800_000L,
                "o pior chunk visitaria " + pior + " blocos so de copa (em "
                        + piorPrateleiras + " prateleiras). O limite existe porque este"
                        + " custo aparece como engasgo ao voar, e nao como erro.");
    }

    // ------------------------------------------------ a madeira tem de aparecer

    @Test
    @DisplayName("boa parte de cada galho fica com a MADEIRA a mostra")
    void madeiraDoGalhoAparece() {
        // ESTA REGUA NASCEU DE UM PEDIDO, e o pedido descrevia um defeito que
        // nenhum portao daqui media: a copa tapava o galho inteiro. Todos os
        // portoes olhavam a folha -- cobertura, sobreposicao, ancoragem,
        // espessura --, e nenhum perguntava o que sobra da MADEIRA.
        //
        // Medido antes da mudanca: 0,146 do eixo de um galho sem folha por cima,
        // e 708 de 766 galhos com menos de 30%. A arvore nao tinha estrutura
        // visivel; era uma massa verde.
        //
        // O que a conta faz: anda pelo eixo do galho, sobe ate a superficie da
        // madeira e pergunta se algum disco DAQUELE galho contem aquele ponto.
        // Ela nao afirma que o jogador VE a madeira -- isso depende de oclusao,
        // luz e distancia, e esta em o-que-nao-provamos.md. Ela afirma que existe
        // madeira descoberta, que e a condicao necessaria.
        double somaNua = 0.0;
        int galhos = 0;
        int apertados = 0;
        for (int index = 0; index < 8; index++) {
            long seed = seedAt(index);
            WorldTreeLayout layout = WorldTreeLayoutGenerator.generate(seed, 0, 0);
            List<WorldTreeSpline> derivados = WorldTreeBranchNetwork.secondaryAndTertiary(layout);
            WorldTreeFoliagePlan plano = WorldTreeFoliagePlan.of(layout, derivados);
            Map<Integer, List<WorldTreeFoliageShelf>> porGalho = agruparPorGalho(plano);

            List<WorldTreeSpline> todos = new ArrayList<>(layout.branches());
            todos.addAll(derivados);
            for (int b = 0; b < todos.size(); b++) {
                // O branchId comeca em 1: o 0 e a coroa, que nao e galho e nao
                // tem madeira horizontal para mostrar.
                List<WorldTreeFoliageShelf> fila = porGalho.get(b + 1);
                if (fila == null) {
                    continue;
                }
                WorldTreeSpline spline = todos.get(b);
                int amostras = 200;
                int coberto = 0;
                for (int i = 0; i < amostras; i++) {
                    double t = (i + 0.5) / amostras;
                    WorldTreePoint ponto = spline.pointAt(t);
                    for (WorldTreeFoliageShelf shelf : fila) {
                        if (shelf.contains(ponto.x(),
                                ponto.y() + spline.radiusAt(t), ponto.z())) {
                            coberto++;
                            break;
                        }
                    }
                }
                double nua = 1.0 - (double) coberto / amostras;
                somaNua += nua;
                galhos++;
                if (nua < 0.30) {
                    apertados++;
                }
            }
        }
        double media = somaNua / galhos;
        assertTrue(media >= 0.40,
                "so " + String.format("%.1f%%", media * 100) + " do eixo de um galho"
                        + " fica sem folha por cima, na media. Abaixo disso a arvore"
                        + " volta a ser uma massa verde sem estrutura visivel.");
        assertTrue(apertados <= galhos / 10,
                apertados + " de " + galhos + " galhos tem menos de 30% de madeira a"
                        + " mostra. A copa precisa deixar a maior parte dos galhos"
                        + " legivel, e nao so a media fechar.");
    }

    @Test
    @DisplayName("chunk longe da arvore nao paga NADA -- nem por vinha")
    void chunkDistanteNaoPaga() {
        // ESTE PORTAO NASCEU DE UM ACHADO DE REVISAO, e o defeito era caro e
        // invisivel: o laco de vinhas nao tinha corte por chunk. Todo chunk da
        // dimensao percorria as ~6.000 cortinas passo a passo -- ~140.000
        // iteracoes, ~1,2 ms medidos -- mesmo a milhares de blocos do tronco.
        //
        // E a regua de custo era CEGA a isso: `estimatedVisitsForChunk` so
        // somava prateleiras, entao devolvia ZERO exatamente para os chunks que
        // pagavam o preco inteiro. Medir uma coisa enquanto o gerador paga outra
        // e o pior estado possivel para um portao.
        for (int index = 0; index < SEEDS; index++) {
            long seed = seedAt(index);
            WorldTreeFoliagePlan plano = plan(seed);
            for (int[] longe : new int[][] {{300, 300}, {-400, 120}, {0, 260}, {900, -900}}) {
                assertEquals(0L, plano.estimatedVisitsForChunk(longe[0], longe[1]),
                        "seed " + seed + ": o chunk (" + longe[0] + "," + longe[1]
                                + ") esta fora da copa e mesmo assim custa algo.");
            }
        }
    }

    @Test
    @DisplayName("a pilha do LIDER nao tem vao vertical -- o portao da luva e cego a ela")
    void semVaoNaPilhaDoLider() {
        // A REVISAO MOSTROU QUE `semDedos` E VAZIO PARA A COROA, e ela tem razao:
        // aquele teste mede `hypot(dx, dz)`, e os discos do lider central ficam
        // todos a menos de 11 blocos do eixo por construcao. A sobreposicao
        // horizontal deles da ~0,95 aconteca o que acontecer -- a assercao nao
        // pode reprovar.
        //
        // O espacamento real da coroa e VERTICAL, e ali havia de 16 a 23 blocos
        // de lider NU entre um degrau e o seguinte, em todas as vinte seeds, com
        // a suite verde. Um chapeu de degraus separados, que e exatamente o que
        // a copa da coroa existe para nao ser.
        for (int index = 0; index < SEEDS; index++) {
            long seed = seedAt(index);
            List<WorldTreeFoliageShelf> pilha = new ArrayList<>(plan(seed).shelves().stream()
                    .filter(shelf -> shelf.suporte() == WorldTreeFoliageShelf.Suporte.LIDER)
                    .toList());
            pilha.sort((a, b) -> Double.compare(a.centerY(), b.centerY()));
            assertTrue(pilha.size() >= 4, "seed " + seed + ": pilha do lider quase vazia");
            for (int i = 1; i < pilha.size(); i++) {
                double topoDoAnterior = pilha.get(i - 1).maxY();
                double baseDoAtual = pilha.get(i).minY();
                assertTrue(baseDoAtual <= topoDoAnterior,
                        "seed " + seed + ": " + (int) (baseDoAtual - topoDoAnterior)
                                + " blocos de lider nu entre o degrau em y="
                                + (int) pilha.get(i - 1).centerY() + " e o de y="
                                + (int) pilha.get(i).centerY()
                                + ". Degraus separados leem como chapeu, e nao como cupula.");
            }
        }
    }

    @Test
    @DisplayName("nao ha faixa longa de altitude sem uma folha, dentro da copa")
    void semFaixaVaziaDeAltitude() {
        // Outro achado de revisao. `multiplasCamadas` conta faixas de 60 blocos
        // que TEM folha, e nunca pergunta se alguma faixa ficou vazia -- um vao
        // de 200 blocos passa por ele sem reclamar.
        for (int index = 0; index < SEEDS; index++) {
            long seed = seedAt(index);
            List<WorldTreeFoliageShelf> shelves = plan(seed).shelves();
            double base = shelves.stream().mapToDouble(WorldTreeFoliageShelf::minY).min().orElse(0);
            double topo = shelves.stream().mapToDouble(WorldTreeFoliageShelf::maxY).max().orElse(0);
            int maiorVao = 0;
            int vaoAtual = 0;
            for (int y = (int) base; y <= (int) topo; y++) {
                final int altura = y;
                boolean temFolha = shelves.stream()
                        .anyMatch(shelf -> altura >= shelf.minY() && altura <= shelf.maxY());
                vaoAtual = temFolha ? 0 : vaoAtual + 1;
                maiorVao = Math.max(maiorVao, vaoAtual);
            }
            assertTrue(maiorVao <= 120,
                    "seed " + seed + ": " + maiorVao + " blocos de altitude sem uma folha,"
                            + " dentro da faixa que a copa ocupa.");
        }
    }

    @Test
    @DisplayName("a deriva da vinha e um sorteio em DUAS dimensoes")
    void derivaNaoEDiagonal() {
        // `unit` le os 16 bits baixos; os dois desvios saiam de `>>> 44` e
        // `>>> 48`, compartilhando doze deles. O resultado nao era um sorteio em
        // duas dimensoes, e sim quase uma diagonal -- cortinas caindo todas para
        // o mesmo lado. A correlacao mede isso.
        double somaX = 0;
        double somaZ = 0;
        double somaXZ = 0;
        double somaX2 = 0;
        double somaZ2 = 0;
        int n = 0;
        for (int index = 0; index < SEEDS; index++) {
            for (WorldTreeVineStrand vine : plan(seedAt(index)).vines()) {
                somaX += vine.driftX();
                somaZ += vine.driftZ();
                somaXZ += vine.driftX() * vine.driftZ();
                somaX2 += vine.driftX() * vine.driftX();
                somaZ2 += vine.driftZ() * vine.driftZ();
                n++;
            }
        }
        double cov = somaXZ / n - (somaX / n) * (somaZ / n);
        double desvio = Math.sqrt((somaX2 / n - Math.pow(somaX / n, 2.0))
                * (somaZ2 / n - Math.pow(somaZ / n, 2.0)));
        double correlacao = Math.abs(cov / desvio);
        assertTrue(correlacao < 0.15,
                "a deriva em X e em Z esta correlacionada em "
                        + String.format("%.2f", correlacao)
                        + ": as cortinas caem todas para o mesmo lado.");
    }

    // ------------------------------------------------------- invariantes

    @Test
    @DisplayName("a forma recusa o que nao e prateleira")
    void formaInvalidaERecusada() {
        assertThrows(IllegalArgumentException.class,
                () -> new WorldTreeFoliageShelf(0, 900, 0, 10, 3, 0, 0, 900, 0, 4,
                        WorldTreeFoliageShelf.Suporte.GALHO, 0, 0, 0),
                "espessura inferior zero e 'folha so por cima', e a copa vira guarda-sol");
        assertThrows(IllegalArgumentException.class,
                () -> new WorldTreeFoliageShelf(0, 900, 0, 0, 3, 2, 0, 900, 0, 4,
                        WorldTreeFoliageShelf.Suporte.GALHO, 0, 0, 0));
        assertThrows(IllegalArgumentException.class,
                () -> new WorldTreeVineStrand(0, 900, 0, 2, 0, 0, false, 0),
                "vinha de dois blocos nao le como cortina; le como bloco solto");
        assertThrows(IllegalArgumentException.class,
                () -> new WorldTreeVineStrand(0, 900, 0, 999, 0, 0, false, 0));
    }

    @Test
    @DisplayName("o span e a forma concordam -- o expoente esta escrito em DOIS lugares")
    void spanConcordaComAForma() {
        // O EXPOENTE APARECE DUAS VEZES, e nenhuma delas e uma constante: em
        // `normalized`, como v*v*v*v; em `verticalSpanFactor`, como raiz da raiz.
        // Havia uma constante `EXPOENTE_VERTICAL` que parecia mandar nos dois e
        // nao mandava em nenhum -- trocar 4 por 2 nao mudava um bloco. So a
        // quebra deliberada mostrou.
        //
        // Se as duas encodificacoes divergirem, o laco de desenho corta a coluna
        // antes da borda da massa: folhagem com o topo raspado, sem erro nenhum.
        // Este teste amarra as duas.
        WorldTreeFoliageShelf shelf = new WorldTreeFoliageShelf(0, 900, 0, 20, 5, 3,
                0, 900, 0, 4, WorldTreeFoliageShelf.Suporte.GALHO, 0, 0, 0);
        for (int passo = 0; passo <= 18; passo++) {
            double h = passo * 20.0 / 19.0;
            double factor = shelf.verticalSpanFactor((h * h) / 400.0);
            // Exatamente no alcance previsto, o ponto tem de estar DENTRO.
            assertTrue(shelf.contains(h, 900.0 + factor * 5.0 * 0.999, 0.0),
                    "o span promete alcance que a forma nao tem, em h=" + h);
            // Um pouco alem, tem de estar FORA.
            assertTrue(!shelf.contains(h, 900.0 + factor * 5.0 * 1.05 + 0.01, 0.0),
                    "o span corta a massa antes da borda, em h=" + h
                            + " -- folhagem com o topo raspado");
        }
    }

    @Test
    @DisplayName("o expoente vertical achata: a 4, o topo e chato; a 2, seria bolha")
    void superelipseAchata() {
        WorldTreeFoliageShelf shelf =
                new WorldTreeFoliageShelf(0, 900, 0, 20, 5, 3, 0, 900, 0, 4,
                        WorldTreeFoliageShelf.Suporte.GALHO, 0, 0, 0);

        // O PONTO DE TESTE MUDOU, e a quebra deliberada e quem mostrou por que.
        // Com (19, 901.5) o teste passava TANTO com expoente 4 quanto com 2 --
        // ou seja, ele nao media a unica decisao de forma deste arquivo. O ponto
        // abaixo separa os dois: a 19 de distancia num raio de 20, o expoente 4
        // ainda alcanca 2,79 de altura, e o expoente 2 para em 1,56.
        assertTrue(shelf.contains(19.0, 902.4, 0.0),
                "a borda fechou cedo demais. Com expoente 2 isto reprova, e e o ponto:"
                        + " expoente 2 e um ELIPSOIDE, e elipsoide em fila vira brocolis.");
        assertTrue(!shelf.contains(0.0, 900.0 + 5.6, 0.0),
                "a prateleira nao pode passar da propria espessura");
        assertTrue(shelf.contains(0.0, 900.0 - 2.9, 0.0), "a face de baixo sumiu");
    }

    // --------------------------------------------------------- utilidades

    private static Map<Integer, List<WorldTreeFoliageShelf>> agruparPorGalho(
            WorldTreeFoliagePlan plano) {
        Map<Integer, List<WorldTreeFoliageShelf>> porGalho = new HashMap<>();
        for (WorldTreeFoliageShelf shelf : plano.shelves()) {
            porGalho.computeIfAbsent(shelf.branchId(), key -> new ArrayList<>()).add(shelf);
        }
        for (List<WorldTreeFoliageShelf> fila : porGalho.values()) {
            fila.sort((a, b) -> Integer.compare(a.order(), b.order()));
        }
        return porGalho;
    }

    /**
     * Fracao do disco da copa que tem folha por cima, vista de cima.
     *
     * <p>AMOSTRA EM GRADE, e nao bloco a bloco: o disco tem centenas de blocos de
     * raio, e uma varredura completa custaria mais que o resto da suite inteira.
     * A grade de 4 blocos e fina o suficiente para pegar peneira e grossa o
     * suficiente para rodar em milissegundos.
     */
    private static double coberturaProjetada(WorldTreeFoliagePlan plano) {
        double raio = raioDaCopa(plano);
        int passo = 4;
        int dentro = 0;
        int cobertos = 0;
        for (int x = (int) -raio; x <= raio; x += passo) {
            for (int z = (int) -raio; z <= raio; z += passo) {
                if (Math.hypot(x, z) > raio) {
                    continue;
                }
                dentro++;
                for (WorldTreeFoliageShelf shelf : plano.shelves()) {
                    double dx = x - shelf.centerX();
                    double dz = z - shelf.centerZ();
                    if (dx * dx + dz * dz <= shelf.radius() * shelf.radius()) {
                        cobertos++;
                        break;
                    }
                }
            }
        }
        return dentro == 0 ? 0.0 : (double) cobertos / dentro;
    }

    /** O maior alcance horizontal que a copa tem, a partir do eixo. */
    private static double raioDaCopa(WorldTreeFoliagePlan plano) {
        double maior = 0.0;
        for (WorldTreeFoliageShelf shelf : plano.shelves()) {
            maior = Math.max(maior,
                    Math.hypot(shelf.centerX(), shelf.centerZ()) + shelf.radius());
        }
        return maior;
    }
}
