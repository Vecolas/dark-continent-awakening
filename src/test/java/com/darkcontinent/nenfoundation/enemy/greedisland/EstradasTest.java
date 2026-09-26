package com.darkcontinent.nenfoundation.enemy.greedisland;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.darkcontinent.nenfoundation.enemy.greedisland.GreedIslandConstants.Ponto;
import com.darkcontinent.nenfoundation.enemy.greedisland.layout.GreedIslandElevationField;
import com.darkcontinent.nenfoundation.enemy.greedisland.layout.GreedIslandMask;
import com.darkcontinent.nenfoundation.enemy.greedisland.road.RoteadorDeEstradas;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * O portao da fase G5: as nove estradas, e o terreno mandando nelas.
 *
 * <p>O nao-negociavel numero 7 e "Roads respect terrain". Uma estrada reta
 * entre duas cidades nao levanta excecao -- ela so atravessa cordilheira e
 * pantano como se nao existissem, e o jogador percebe na hora que aquilo foi
 * desenhado por cima do mapa em vez de a partir dele.
 */
class EstradasTest {

    /** As nove rotas, roteadas uma vez e reusadas: o A* nao e barato. */
    private static final List<List<Ponto>> ROTAS = rotearTudo();

    private static List<List<Ponto>> rotearTudo() {
        return GreedIslandConstants.ESTRADAS.stream()
                .map(l -> RoteadorDeEstradas.rotear(
                        GreedIslandConstants.cidade(l.de()).map(
                                c -> new Ponto(c.x(), c.z())).orElseThrow(),
                        GreedIslandConstants.cidade(l.para()).map(
                                c -> new Ponto(c.x(), c.z())).orElseThrow()))
                .toList();
    }

    @Test
    @DisplayName("as nove ligacoes da secao 56 estao declaradas, e ligam cidades reais")
    void oGrafoEOhDoDocumento() {
        assertEquals(9, GreedIslandConstants.ESTRADAS.size());
        for (var l : GreedIslandConstants.ESTRADAS) {
            assertTrue(GreedIslandConstants.cidade(l.de()).isPresent(),
                    "estrada saindo de cidade inexistente: " + l.de());
            assertTrue(GreedIslandConstants.cidade(l.para()).isPresent(),
                    "estrada chegando em cidade inexistente: " + l.para());
        }
    }

    @Test
    @DisplayName("TODA estrada encontra caminho")
    void nenhumaRotaFalha() {
        for (int i = 0; i < ROTAS.size(); i++) {
            assertTrue(!ROTAS.get(i).isEmpty(),
                    "sem rota para " + GreedIslandConstants.ESTRADAS.get(i)
                            + ": as duas cidades ficam desligadas, e nada acusa isso");
        }
    }

    @Test
    @DisplayName("nenhuma estrada passa pelo MAR")
    void aEstradaNaoNada() {
        for (int i = 0; i < ROTAS.size(); i++) {
            for (Ponto p : ROTAS.get(i)) {
                assertTrue(GreedIslandMask.terra(p.x(), p.z()),
                        GreedIslandConstants.ESTRADAS.get(i) + " passa pela agua em ("
                                + p.x() + "," + p.z() + ")");
            }
        }
    }

    @Test
    @DisplayName("a estrada NAO e uma reta -- ela contorna o terreno")
    void oCaminhoRespeitaORelevo() {
        // Uma rota que empata com a reta e uma reta. O desvio nao precisa ser
        // grande em toda rota -- terreno plano nao exige desvio --, mas pelo
        // menos uma das nove tem de contornar alguma coisa de verdade.
        double maiorDesvio = 1.0D;
        for (int i = 0; i < ROTAS.size(); i++) {
            var rota = ROTAS.get(i);
            if (rota.size() < 2) {
                continue;
            }
            double reta = Math.hypot(
                    rota.get(rota.size() - 1).x() - rota.get(0).x(),
                    rota.get(rota.size() - 1).z() - rota.get(0).z());
            double andado = 0.0D;
            for (int j = 1; j < rota.size(); j++) {
                andado += Math.hypot(rota.get(j).x() - rota.get(j - 1).x(),
                        rota.get(j).z() - rota.get(j - 1).z());
            }
            maiorDesvio = Math.max(maiorDesvio, andado / Math.max(1.0D, reta));
        }
        assertTrue(maiorDesvio > 1.06D,
                "a rota mais tortuosa anda so " + String.format("%.2f", maiorDesvio)
                        + "x a distancia em linha reta: as nove estradas sao retas, e o"
                        + " nao-negociavel 7 e 'Roads respect terrain'");
    }

    @Test
    @DisplayName("a estrada EVITA a serra, ou passa por um passo")
    void aSerraSoSeAtravessaPeloPasso() {
        int emSerraForaDePasso = 0;
        int total = 0;
        for (var rota : ROTAS) {
            for (Ponto p : rota) {
                total++;
                int y = GreedIslandElevationField.alturaEm(p.x(), p.z());
                if (y >= GreedIslandConstants.TOPO_DO_PLANALTO
                        && !RoteadorDeEstradas.dentroDeUmPasso(p.x(), p.z())) {
                    emSerraForaDePasso++;
                }
            }
        }
        double fracao = emSerraForaDePasso / (double) total;
        assertTrue(fracao < 0.04D,
                String.format("%.1f%%", fracao * 100) + " do traçado corre acima de Y="
                        + GreedIslandConstants.TOPO_DO_PLANALTO + " fora de um passo."
                        + " A estrada esta escalando a cordilheira.");
    }

    @Test
    @DisplayName("os quatro passos da secao 58 sao usados de verdade")
    void osPassosNaoSaoDecoracao() {
        Set<String> usados = new HashSet<>();
        for (var rota : ROTAS) {
            for (Ponto p : rota) {
                for (Ponto passo : GreedIslandConstants.PASSOS) {
                    if (Math.hypot(p.x() - passo.x(), p.z() - passo.z()) < 3_500) {
                        usados.add(passo.x() + "," + passo.z());
                    }
                }
            }
        }
        assertTrue(!usados.isEmpty(),
                "nenhuma das nove estradas passa perto de um dos quatro passos: eles sao"
                        + " coordenada declarada sem consumidor -- a familia do erro no 7"
                        + " do CLAUDE.md, com outra roupa");
    }

    @Test
    @DisplayName("a rota liga as pontas: comeca na origem e termina no destino")
    void oCaminhoChegaOndeDeveria() {
        for (int i = 0; i < ROTAS.size(); i++) {
            var l = GreedIslandConstants.ESTRADAS.get(i);
            var rota = ROTAS.get(i);
            var origem = GreedIslandConstants.cidade(l.de()).orElseThrow();
            var destino = GreedIslandConstants.cidade(l.para()).orElseThrow();

            assertTrue(Math.hypot(rota.get(0).x() - origem.x(),
                            rota.get(0).z() - origem.z()) < RoteadorDeEstradas.GRADE * 1.5,
                    l + ": a rota comeca longe da origem");
            var fim = rota.get(rota.size() - 1);
            assertTrue(Math.hypot(fim.x() - destino.x(), fim.z() - destino.z())
                            < RoteadorDeEstradas.GRADE * 1.5,
                    l + ": a rota termina longe do destino");
        }
    }

    @Test
    @DisplayName("o grafo NAO e completo -- senao as cartas perdem sentido")
    void nemTodaCidadeSeLigaATodas() {
        int cidades = GreedIslandConstants.CIDADES.size();
        int completo = cidades * (cidades - 1) / 2;
        assertTrue(GreedIslandConstants.ESTRADAS.size() < completo / 2,
                "com " + GreedIslandConstants.ESTRADAS.size() + " de " + completo
                        + " ligacoes possiveis, quase toda viagem vira direta -- e a"
                        + " secao 72 poe a escala a servico das Spell Cards");
    }
}
