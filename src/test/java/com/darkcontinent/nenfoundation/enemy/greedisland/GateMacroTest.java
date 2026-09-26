package com.darkcontinent.nenfoundation.enemy.greedisland;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.darkcontinent.nenfoundation.enemy.greedisland.city.RegistroDeCidades;
import com.darkcontinent.nenfoundation.enemy.greedisland.ecology.HabitatDaIlha;
import com.darkcontinent.nenfoundation.enemy.greedisland.layout.GreedIslandElevationField;
import com.darkcontinent.nenfoundation.enemy.greedisland.layout.GreedIslandHydrologyField;
import com.darkcontinent.nenfoundation.enemy.greedisland.layout.GreedIslandMask;
import com.darkcontinent.nenfoundation.enemy.greedisland.layout.GreedIslandRidgeField;
import com.darkcontinent.nenfoundation.enemy.greedisland.navigation.ServicoDeNavegacao;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * A fase G12: a secao 102 do documento, virada em regua.
 *
 * <p>A secao lista OITO condicoes de reprovacao do gate macro. Seis delas sao
 * medidas -- e este arquivo as mede. As outras duas ("road graph feels theme
 * park", "cards feel unnecessary") sao julgamento humano sobre o mapa
 * exportado, e estao declaradas em {@code o-que-nao-provamos.md} em vez de
 * fingidas aqui.
 *
 * <p><b>ELE EXISTE PORQUE UM PORTAO VERDE JA APROVOU UM BLOB.</b> Numa versao
 * anterior desta trilha, todos os testes de layout passavam e o PNG mostrava
 * uma massa arredondada sem peninsula nenhuma. A licao virou este arquivo: as
 * condicoes de reprovacao do documento sao a regua, e nao as que eu inventaria.
 */
class GateMacroTest {

    @Test
    @DisplayName("FAIL 1 — duas cidades grandes visiveis juntas")
    void nenhumParDeHubsSeEnxerga() {
        // "two major cities visible together". Render distance maxima do
        // vanilla e 32 chunks = 512 blocos; o dobro e a margem.
        var lista = RegistroDeCidades.todas();
        for (int i = 0; i < lista.size(); i++) {
            for (int j = i + 1; j < lista.size(); j++) {
                double d = Math.hypot(lista.get(i).ancora().x() - lista.get(j).ancora().x(),
                        lista.get(i).ancora().z() - lista.get(j).ancora().z());
                assertTrue(d > 1_024,
                        lista.get(i).id() + " e " + lista.get(j).id() + " a " + (int) d);
            }
        }
    }

    @Test
    @DisplayName("FAIL 2 — todos os hubs a menos de 5.000 um do outro")
    void aEscalaDeViagemNaoEDeParque() {
        // "hubs all <5k". Basta UM salto longo para a ilha nao ser um parque;
        // mas se o maior salto entre vizinhos for curto, ela e.
        double maior = 0.0D;
        for (var l : GreedIslandConstants.ESTRADAS) {
            var a = GreedIslandConstants.cidade(l.de()).orElseThrow();
            var b = GreedIslandConstants.cidade(l.para()).orElseThrow();
            maior = Math.max(maior, Math.hypot(a.x() - b.x(), a.z() - b.z()));
        }
        assertTrue(maior > 15_000,
                "o maior trecho de estrada tem so " + (int) maior + " blocos: a ilha"
                        + " inteira se atravessa a pe sem esforco, e as cartas nao"
                        + " compram nada");
    }

    @Test
    @DisplayName("FAIL 3 — wilderness ausente")
    void haVazioDeVerdadeEntreOsLugares() {
        // "wilderness absent". Mede quanta terra esta LONGE de tudo: cidade,
        // estrada e landmark. A secao 111 pede 60-70% de natureza aberta.
        int longe = 0;
        int total = 0;
        for (int x = -40_000; x <= 40_000; x += 1_200) {
            for (int z = -36_000; z <= 36_000; z += 1_200) {
                if (!GreedIslandMask.terra(x, z)) {
                    continue;
                }
                total++;
                final int px = x;
                final int pz = z;
                boolean pertoDeAlgo = HabitatDaIlha.perturbadoPorGente(px, pz)
                        || com.darkcontinent.nenfoundation.enemy.greedisland.landmark
                                .RegistroDeLandmarks.todos().stream()
                                .anyMatch(l -> Math.hypot(px - l.ancora().x(),
                                        pz - l.ancora().z()) < 2_000);
                if (!pertoDeAlgo) {
                    longe++;
                }
            }
        }
        double fatia = longe / (double) total;
        assertTrue(fatia > 0.55D,
                "so " + String.format("%.0f%%", fatia * 100) + " da ilha esta longe de"
                        + " cidade, estrada e landmark. A sensacao de distancia E parte"
                        + " do mundo, e sem ela a ilha vira um parque tematico.");
    }

    @Test
    @DisplayName("FAIL 4 — a ilha e quase um circulo")
    void aFormaNaoEDisco() {
        // "island near-circle". Mesmo criterio do gate de G1, repetido aqui
        // porque a secao 102 o lista: um teste que existe em dois lugares por
        // duas razoes diferentes nao e duplicata.
        double[] raios = new double[96];
        double soma = 0.0D;
        for (int i = 0; i < raios.length; i++) {
            double ang = i * 2.0D * Math.PI / raios.length;
            double ultimo = 0.0D;
            for (int r = 0; r <= 46_000; r += 500) {
                if (GreedIslandMask.terra(Math.cos(ang) * r, Math.sin(ang) * r)) {
                    ultimo = r;
                }
            }
            raios[i] = ultimo;
            soma += ultimo;
        }
        double media = soma / raios.length;
        double v = 0.0D;
        for (double r : raios) {
            v += (r - media) * (r - media);
        }
        assertTrue(Math.sqrt(v / raios.length) / media >= 0.07D,
                "a costa e quase circular");
    }

    @Test
    @DisplayName("FAIL 5 — as montanhas sao manchas aleatorias")
    void asCadeiasTemEixo() {
        // "mountains random blobs". A prova de que ha EIXO: seguindo a crista,
        // a altura se mantem; saindo dela, cai. Blob nao tem essa propriedade.
        for (var c : GreedIslandConstants.CORDILHEIRAS) {
            int naCrista = 0;
            int amostras = 0;
            int foraDela = 0;
            for (var no : c.nos()) {
                naCrista += GreedIslandElevationField.alturaEm(no.x(), no.z());
                foraDela += GreedIslandElevationField.alturaEm(
                        no.x() + c.larguraBase(), no.z() + c.larguraBase());
                amostras++;
            }
            double mediaCrista = naCrista / (double) amostras;
            double mediaFora = foraDela / (double) amostras;
            assertTrue(mediaCrista > mediaFora + 50,
                    c.id() + ": media na crista " + (int) mediaCrista + ", fora "
                            + (int) mediaFora + ". Sem queda ao sair do eixo, isso e"
                            + " mancha alta e nao cordilheira.");
        }
    }

    @Test
    @DisplayName("FAIL 6 — os rios sao arbitrarios")
    void osRiosTemHidrologia() {
        // "rivers arbitrary". Um rio nao-arbitrario nasce alto, desce, e
        // termina no mar. Os tres ja tem portao proprio; aqui se cobra a
        // propriedade que os liga: TODOS nascem acima da media da ilha.
        for (var rio : GreedIslandConstants.RIOS) {
            var nascente = rio.curso().get(0);
            int y = GreedIslandElevationField.alturaEm(nascente.x(), nascente.z());
            assertTrue(y > GreedIslandConstants.TOPO_DA_PLANICIE,
                    "o rio " + rio.id() + " nasce a Y=" + y + ", na planicie: rio que"
                            + " nao vem da serra e uma linha desenhada no mapa");
        }
    }

    // ------------------------------------------------------------------
    // TRAVEL MEASUREMENTS (secao 126)
    // ------------------------------------------------------------------

    @Test
    @DisplayName("a primeira caminhada cabe numa sessao, e o resto nao")
    void oTempoDeViagemEoDoDocumento() {
        // A pe, ~4,3 blocos/s andando. A secao 109 quer a primeira hora
        // confortavel; a secao 38 quer os extremos realmente distantes.
        double shisoAntokiba = distancia("shiso_tree", "antokiba");
        double minutosPrimeira = shisoAntokiba / 4.3D / 60.0D;
        assertTrue(minutosPrimeira < 30,
                "Shiso -> Antokiba leva " + (int) minutosPrimeira + " min a pe: a"
                        + " primeira caminhada do jogador nao pode ser meia sessao");

        double extremo = distancia("shiso_tree", "soufrabi");
        double minutosExtremo = extremo / 4.3D / 60.0D;
        assertTrue(minutosExtremo > 120,
                "atravessar a ilha leva so " + (int) minutosExtremo + " min: se der"
                        + " para andar de ponta a ponta numa sessao, a carta de"
                        + " transporte nao compra nada");
    }

    @Test
    @DisplayName("a carta VALE -- ela poupa horas, e nao minutos")
    void asCartasTemValor() {
        // "cards feel unnecessary" e a oitava condicao de reprovacao. O que da
        // para medir e a economia: quanto tempo a carta poupa.
        var destinos = ServicoDeNavegacao.disponiveisPara(
                Set.of("masadora", "soufrabi", "limeiro", "dorias"));
        var soufrabi = ServicoDeNavegacao.destino("soufrabi").orElseThrow();
        var shiso = GreedIslandConstants.cidade("shiso_tree").orElseThrow();

        double poupado = ServicoDeNavegacao.distanciaAte(shiso.x(), shiso.z(), soufrabi)
                / 4.3D / 3600.0D;
        assertTrue(poupado > 2.5D,
                "a carta poupa so " + String.format("%.1f", poupado) + " h de caminhada:"
                        + " nesse patamar o jogador anda e guarda a carta");
        assertTrue(destinos.size() >= 5, "poucos destinos conhecidos para comparar");
    }

    // ------------------------------------------------------------------
    // PERFORMANCE (secao 104)
    // ------------------------------------------------------------------

    @Test
    @DisplayName("o caminho REAL da geracao cabe no tick -- inclusive a cota de cidade")
    void oCustoDoChunkDeCidadeCabeNoTick() {
        // ESTE TESTE NASCEU DE UM SERVIDOR MORTO. O teste abaixo media
        // `alturaEm` e passava, e eu concluí que o layout era barato. A
        // feature chamava `PlantaDeCidade.cotaDe` POR BLOCO -- e cotaDe varre
        // a pegada inteira da cidade amostrando elevacao. O watchdog matou o
        // servidor com um tick de 60 segundos.
        //
        // MEDIR A FUNCAO BARATA E CONCLUIR QUE O SISTEMA E BARATO e a forma
        // mais comum de falso verde em performance. Este mede o que a geracao
        // de um chunk de cidade REALMENTE faz.
        var cidade = RegistroDeCidades.porId("limeiro").orElseThrow();
        com.darkcontinent.nenfoundation.enemy.greedisland.city.PlantaDeCidade
                .cotaDe(cidade);

        long inicio = System.nanoTime();
        for (int dx = 0; dx < 16; dx++) {
            for (int dz = 0; dz < 16; dz++) {
                int x = cidade.ancora().x() + dx;
                int z = cidade.ancora().z() + dz;
                RegistroDeCidades.em(x, z);
                com.darkcontinent.nenfoundation.enemy.greedisland.city.PlantaDeCidade
                        .usoEm(cidade, x, z);
                com.darkcontinent.nenfoundation.enemy.greedisland.city.PlantaDeCidade
                        .cotaDe(cidade);
            }
        }
        double ms = (System.nanoTime() - inicio) / 1_000_000.0D;
        assertTrue(ms < 25.0D,
                String.format("%.1f", ms) + " ms para um chunk de cidade. O tick tem 50 ms"
                        + " para TUDO, e a geracao concorre com o resto do servidor.");
    }

    @Test
    @DisplayName("consultar o layout e barato o bastante para o tick")
    void oCustoPorColunaCabeNoTick() {
        // O gerador chama isto por coluna de chunk: 256 por chunk. Se uma
        // consulta custar mais que dezenas de microssegundos, a geracao
        // engasga -- e o sintoma nao e erro, e o servidor parando por segundos
        // quando alguem explora.
        long inicio = System.nanoTime();
        int n = 0;
        for (int x = 0; x < 16 * 40; x += 16) {
            for (int z = 0; z < 16 * 40; z += 16) {
                GreedIslandElevationField.alturaEm(x, z);
                n++;
            }
        }
        double microsPorConsulta = (System.nanoTime() - inicio) / 1_000.0D / n;
        assertTrue(microsPorConsulta < 120.0D,
                String.format("%.1f", microsPorConsulta) + " us por consulta de altura."
                        + " Com 256 colunas por chunk, isso e "
                        + String.format("%.0f", microsPorConsulta * 256 / 1000.0D)
                        + " ms por chunk so de layout.");
    }

    @Test
    @DisplayName("o layout e DETERMINISTICO entre chamadas -- e portanto entre servidores")
    void oMesmoPontoDaOMesmoMundo() {
        for (int i = 0; i < 400; i++) {
            int x = (i * 7919) % 70_000 - 35_000;
            int z = (i * 6271) % 60_000 - 30_000;
            double a = GreedIslandMask.distanciaComSinal(x, z);
            int b = GreedIslandElevationField.alturaEm(x, z);
            boolean c = GreedIslandHydrologyField.molhado(x, z);
            double d = GreedIslandRidgeField.contribuicao(x, z);

            assertTrue(a == GreedIslandMask.distanciaComSinal(x, z)
                            && b == GreedIslandElevationField.alturaEm(x, z)
                            && c == GreedIslandHydrologyField.molhado(x, z)
                            && d == GreedIslandRidgeField.contribuicao(x, z),
                    "o layout mudou entre duas chamadas em (" + x + "," + z + "): a ilha"
                            + " seria diferente em dois servidores, e o guia de Greed"
                            + " Island deixaria de valer");
        }
    }

    private static double distancia(String a, String b) {
        var ca = GreedIslandConstants.cidade(a).orElseThrow();
        var cb = GreedIslandConstants.cidade(b).orElseThrow();
        return Math.hypot(ca.x() - cb.x(), ca.z() - cb.z());
    }
}
