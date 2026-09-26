package com.darkcontinent.nenfoundation.enemy.greedisland.navigation;

import com.darkcontinent.nenfoundation.enemy.greedisland.GreedIslandConstants;
import com.darkcontinent.nenfoundation.enemy.greedisland.city.DefinicaoDeCidade;
import com.darkcontinent.nenfoundation.enemy.greedisland.city.RegistroDeCidades;
import com.darkcontinent.nenfoundation.enemy.greedisland.landmark.Landmark;
import com.darkcontinent.nenfoundation.enemy.greedisland.landmark.RegistroDeLandmarks;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * A ponte entre a geografia e as Spell Cards. Secoes 72-76, fase G11.
 *
 * <p><b>O DOCUMENTO E CATEGORICO: worldgen, Spell Cards e progressao NAO sao
 * tres sistemas. Sao um.</b> A ilha e enorme para que distancia custe; as
 * cartas existem para que conhecimento compre distancia de volta. Separar os
 * dois faria a escala virar castigo.
 *
 * <p>O arco que isso produz:
 *
 * <pre>
 *   comeco:  o mundo parece enorme
 *   meio:    o jogador comeca a entende-lo
 *   fim:     ele navega Greed Island como quem dominou suas regras
 * </pre>
 *
 * <p><b>O SERVICO E DE CONSULTA, e nao de teleporte.</b> Ele responde "que
 * destinos existem", "este jogador ja conhece aquele lugar", "quanto custa ir
 * daqui ate la". QUEM MOVE o jogador e a carta, no servidor, com as validacoes
 * dela -- e misturar as duas coisas poria uma regra de teleporte dentro de uma
 * classe de geografia, que e onde ninguem procuraria por ela.
 *
 * <p>SEM MINECRAFT: o destino e um id e um ponto. O que faz o jogador andar
 * fica na camada de cima, e assim a regra de alcance da carta e provavel sem
 * subir o jogo.
 */
public final class ServicoDeNavegacao {

    private ServicoDeNavegacao() {
    }

    /** Um lugar para onde uma carta pode levar. */
    public record Destino(String id, String nome, int x, int z, Especie especie) {

        /** O que o destino e. A carta decide quais especies ela aceita. */
        public enum Especie {
            /** Uma das oito cidades. */
            CIDADE,
            /** Um landmark grande. */
            LANDMARK
        }
    }

    /**
     * Todos os destinos possiveis da ilha.
     *
     * <p>CIDADES E LANDMARKS, e nao coordenada arbitraria: uma carta que leva a
     * qualquer ponto transforma a ilha num menu, e a geografia deixa de valer.
     * O documento poe a carta a servico da escala, e nao no lugar dela.
     */
    public static List<Destino> todos() {
        List<Destino> lista = new java.util.ArrayList<>();
        for (DefinicaoDeCidade c : RegistroDeCidades.todas()) {
            lista.add(new Destino(c.id(), c.nome(), c.ancora().x(), c.ancora().z(),
                    Destino.Especie.CIDADE));
        }
        for (Landmark l : RegistroDeLandmarks.todos()) {
            lista.add(new Destino(l.id(), l.id(), l.ancora().x(), l.ancora().z(),
                    Destino.Especie.LANDMARK));
        }
        return List.copyOf(lista);
    }

    /** Um destino pelo id. */
    public static Optional<Destino> destino(String id) {
        return todos().stream().filter(d -> d.id().equals(id)).findFirst();
    }

    /**
     * Os destinos que ESTE jogador pode usar.
     *
     * <p><b>O CONHECIMENTO E A MOEDA.</b> A carta nao leva a qualquer lugar --
     * ela leva aonde o jogador ja esteve. E isso que faz explorar valer a pena
     * depois de se ter cartas: sem a regra, a primeira carta de transporte
     * tornaria a ilha inteira acessivel e a exploracao vira opcional no
     * primeiro dia.
     *
     * @param descobertos os ids que o jogador ja visitou
     */
    public static List<Destino> disponiveisPara(Set<String> descobertos) {
        return todos().stream()
                .filter(d -> descobertos.contains(d.id())
                        // O HUB INICIAL E SEMPRE ALCANCAVEL. Um jogador que
                        // perdeu o mapa e nunca gravou nada ficaria preso sem
                        // esta excecao -- e ficar preso nao e dificuldade.
                        || d.id().equals(GreedIslandConstants.CIDADE_INICIAL))
                .toList();
    }

    /**
     * O que o jogador descobre por estar aqui.
     *
     * <p>Devolve vazio quando nao ha nada perto, e isso e o caso comum: a ilha
     * e 60-70% de natureza aberta por decisao da secao 111.
     */
    public static Optional<String> descobertaEm(double x, double z) {
        Optional<DefinicaoDeCidade> cidade = RegistroDeCidades.em(x, z);
        if (cidade.isPresent()) {
            return cidade.map(DefinicaoDeCidade::id);
        }
        return RegistroDeLandmarks.todos().stream()
                .filter(l -> l.descobertoDe(x, z))
                .map(Landmark::id)
                .findFirst();
    }

    /**
     * A distancia em linha reta ate um destino, em blocos.
     *
     * <p>EM LINHA RETA, e nao pela estrada: a carta NAO anda, ela salta. O
     * custo dela, se um dia depender de distancia, depende da distancia que
     * ela poupa -- e essa e a reta.
     */
    public static double distanciaAte(double x, double z, Destino destino) {
        return Math.hypot(x - destino.x(), z - destino.z());
    }

    /**
     * O destino conhecido mais proximo: para onde uma carta de retorno leva.
     *
     * <p>E A CARTA {@code Return} da secao 75. Ela nao pede alvo -- ela devolve
     * o jogador ao lugar seguro mais perto, e "mais perto" so faz sentido
     * entre os que ele ja conhece.
     */
    public static Optional<Destino> retornoPara(double x, double z,
            Set<String> descobertos) {
        return disponiveisPara(descobertos).stream()
                .min((a, b) -> Double.compare(distanciaAte(x, z, a), distanciaAte(x, z, b)));
    }
}
