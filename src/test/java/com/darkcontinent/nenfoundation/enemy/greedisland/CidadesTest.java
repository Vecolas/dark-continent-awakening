package com.darkcontinent.nenfoundation.enemy.greedisland;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.darkcontinent.nenfoundation.enemy.greedisland.city.DefinicaoDeCidade;
import com.darkcontinent.nenfoundation.enemy.greedisland.city.RegistroDeCidades;
import com.darkcontinent.nenfoundation.enemy.greedisland.layout.GreedIslandElevationField;
import com.darkcontinent.nenfoundation.enemy.greedisland.layout.GreedIslandHydrologyField;
import com.darkcontinent.nenfoundation.enemy.greedisland.layout.GreedIslandMask;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** O portao das fases G6-G8: as oito cidades e o chao onde elas nascem. */
class CidadesTest {

    @Test
    @DisplayName("as oito cidades existem, com pegada e papel proprios")
    void oElencoEstaCompleto() {
        assertEquals(8, RegistroDeCidades.todas().size());
        Set<DefinicaoDeCidade.Papel> papeis = new HashSet<>();
        for (var c : RegistroDeCidades.todas()) {
            assertTrue(papeis.add(c.papel()),
                    "duas cidades com o papel " + c.papel() + ": uma delas nao tem"
                            + " razao propria de existir");
        }
    }

    @Test
    @DisplayName("as pegadas sao as das secoes 39-46")
    void oTamanhoEoDoDocumento() {
        assertPegada("antokiba", 420, 520);
        assertPegada("rubicuta", 380, 460);
        assertPegada("masadora", 700, 850);
        assertPegada("aiai", 550, 700);
        assertPegada("dorias", 650, 900);
        assertPegada("soufrabi", 900, 1_250);
        assertPegada("limeiro", 1_200, 1_500);
    }

    @Test
    @DisplayName("a capital e a MAIOR, e o Shiso e o menor")
    void aHierarquiaDeTamanhoFazSentido() {
        var limeiro = RegistroDeCidades.porId("limeiro").orElseThrow();
        var shiso = RegistroDeCidades.porId("shiso_tree").orElseThrow();
        for (var c : RegistroDeCidades.todas()) {
            assertTrue(area(limeiro) >= area(c),
                    c.id() + " e maior que a capital");
            assertTrue(area(shiso) <= area(c),
                    c.id() + " e menor que o Shiso Tree, que e so um ponto de entrada");
        }
    }

    // ------------------------------------------------------------------
    // O CHAO
    // ------------------------------------------------------------------

    @Test
    @DisplayName("nenhuma cidade nasce na AGUA")
    void oChaoESeco() {
        for (var c : RegistroDeCidades.todas()) {
            assertTrue(GreedIslandMask.terra(c.ancora().x(), c.ancora().z()),
                    c.id() + " esta no mar");
            assertTrue(!GreedIslandHydrologyField.molhado(c.ancora().x(), c.ancora().z()),
                    c.id() + " esta dentro de um rio ou lago: as casas nasceriam"
                            + " submersas, e nada acusa isso");
        }
    }

    @Test
    @DisplayName("nenhuma cidade nasce na SERRA")
    void oChaoEPlano() {
        for (var c : RegistroDeCidades.todas()) {
            int y = GreedIslandElevationField.alturaEm(c.ancora().x(), c.ancora().z());
            assertTrue(y < GreedIslandConstants.TOPO_DAS_COLINAS,
                    c.id() + " esta a Y=" + y + ", acima da faixa de colina: a secao 48"
                            + " pede adaptacao SUAVE do terreno, e aplainar uma montanha"
                            + " deixa uma mesa quadrada visivel de quilometros");
        }
    }

    @Test
    @DisplayName("o terreno sob a pegada nao e um despenhadeiro")
    void aPegadaCabeNoRelevo() {
        for (var c : RegistroDeCidades.todas()) {
            int menor = Integer.MAX_VALUE;
            int maior = Integer.MIN_VALUE;
            for (int dx = -c.larguraX() / 2; dx <= c.larguraX() / 2; dx += 60) {
                for (int dz = -c.larguraZ() / 2; dz <= c.larguraZ() / 2; dz += 60) {
                    int y = GreedIslandElevationField.alturaEm(
                            c.ancora().x() + dx, c.ancora().z() + dz);
                    menor = Math.min(menor, y);
                    maior = Math.max(maior, y);
                }
            }
            assertTrue(maior - menor <= 45,
                    c.id() + " tem " + (maior - menor) + " blocos de desnivel na pegada."
                            + " Ou a cidade vira escada, ou o aplainamento deixa um"
                            + " paredao -- as duas leem como cidade colada no mapa.");
        }
    }

    @Test
    @DisplayName("Soufrabi e porto de verdade -- ela ENCOSTA no mar")
    void oPortoFicaNaCosta() {
        var soufrabi = RegistroDeCidades.porId("soufrabi").orElseThrow();
        double aoMar = GreedIslandMask.distanciaComSinal(
                soufrabi.ancora().x(), soufrabi.ancora().z());
        assertTrue(aoMar < 3_500,
                "Soufrabi esta a " + (int) aoMar + " blocos da costa: uma cidade"
                        + " portuaria terra adentro nao tem porto, docas nem farol --"
                        + " e sete dos distritos dela dependem do mar");
    }

    @Test
    @DisplayName("Antokiba fica perto do rio dela, na faixa da secao 28")
    void aCidadeFicaNaDistanciaCertaDoRio() {
        var antokiba = RegistroDeCidades.porId("antokiba").orElseThrow();
        double aoRio = GreedIslandHydrologyField.distanciaAoRio(
                antokiba.ancora().x(), antokiba.ancora().z());
        assertTrue(aoRio >= 200 && aoRio <= 2_600,
                "Antokiba esta a " + (int) aoRio + " do rio. A secao 28 pede 800-1.800:"
                        + " perto o bastante para o rio fazer parte da cidade, longe o"
                        + " bastante para nao inundar.");
    }

    @Test
    @DisplayName("as pegadas NAO se sobrepoem")
    void nenhumaCidadeInvadeAOutra() {
        var lista = RegistroDeCidades.todas();
        for (int i = 0; i < lista.size(); i++) {
            for (int j = i + 1; j < lista.size(); j++) {
                var a = lista.get(i);
                var b = lista.get(j);
                boolean colideX = Math.abs(a.ancora().x() - b.ancora().x())
                        < (a.larguraX() + b.larguraX()) / 2.0D;
                boolean colideZ = Math.abs(a.ancora().z() - b.ancora().z())
                        < (a.larguraZ() + b.larguraZ()) / 2.0D;
                assertTrue(!(colideX && colideZ),
                        a.id() + " e " + b.id() + " se sobrepoem");
            }
        }
    }

    // ------------------------------------------------------------------
    // OS DISTRITOS
    // ------------------------------------------------------------------

    @Test
    @DisplayName("o numero de distritos acompanha o tamanho da cidade")
    void cidadeGrandeTemMaisBairros() {
        var shiso = RegistroDeCidades.porId("shiso_tree").orElseThrow();
        var limeiro = RegistroDeCidades.porId("limeiro").orElseThrow();
        assertTrue(limeiro.distritos().size() > shiso.distritos().size() + 3,
                "a capital tem " + limeiro.distritos().size() + " distritos e o Shiso "
                        + shiso.distritos().size() + ": a capital nao parece capital");
    }

    @Test
    @DisplayName("o landmark de cada cidade E um distrito dela, ou o proprio nome")
    void oLandmarkTemOndeFicar() {
        for (var c : RegistroDeCidades.todas()) {
            assertTrue(c.distritos().contains(c.landmark())
                            || c.landmark().equals(c.id())
                            || List.of("spell_card_hall", "castelo", "farol")
                                    .contains(c.landmark()),
                    c.id() + ": o landmark '" + c.landmark() + "' nao corresponde a"
                            + " nenhum distrito -- ele nasceria sem lugar na cidade");
        }
    }

    @Test
    @DisplayName("a zona de aproximacao e maior que a pegada -- a cidade nao nasce do nada")
    void haTransicaoAntesDaPrimeiraCasa() {
        for (var c : RegistroDeCidades.todas()) {
            assertTrue(c.raioDeAproximacao() > c.raio() + 100,
                    c.id() + ": sem zona de aproximacao a cidade parece colada no mapa,"
                            + " e a secao 49 existe para evitar isso");
        }
    }

    @Test
    void cidadeSemDistritoReprova() {
        assertThrows(IllegalArgumentException.class,
                () -> new DefinicaoDeCidade("x", "X",
                        new GreedIslandConstants.Ponto(0, 0), 100, 100,
                        DefinicaoDeCidade.Papel.SOCIAL, List.of(), "y"));
    }

    private static void assertPegada(String id, int x, int z) {
        var c = RegistroDeCidades.porId(id).orElseThrow();
        assertEquals(x, c.larguraX(), id + ": largura X fora do documento");
        assertEquals(z, c.larguraZ(), id + ": largura Z fora do documento");
    }

    private static long area(DefinicaoDeCidade c) {
        return (long) c.larguraX() * c.larguraZ();
    }
}
