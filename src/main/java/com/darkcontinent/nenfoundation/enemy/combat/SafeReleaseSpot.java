package com.darkcontinent.nenfoundation.enemy.combat;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import net.minecraft.world.phys.Vec3;

/**
 * Onde a vitima cai quando o agarrao termina -- sem sufocar e sem atravessar bloco.
 *
 * <p><b>A falha que isto evita nao aparece em teste nenhum e nao da erro.</b> A
 * soltura obvia -- "coloca a vitima na frente do predador" -- funciona em campo
 * aberto e mata quem foi agarrado encostado numa parede: o jogador reaparece
 * dentro da pedra, leva dano de sufocamento continuo e nao entende de onde vem.
 * Pior, o caso so acontece quando o predador encurralou o alvo, que e
 * exatamente quando o agarrao e interessante.</p>
 *
 * <p><b>Ela nao consulta mundo.</b> O teste de "esta livre?" chega como
 * predicado, medido por quem tem o servidor na mao. Assim a ordem das
 * tentativas -- a regra de verdade -- e provavel sem servidor de pe, e nenhum
 * caminho de cliente consegue alimenta-la.</p>
 *
 * <p><b>Sem lugar livre, ela devolve vazio.</b> Nao inventa um ponto "menos
 * ruim": quem chamou mantem a vitima onde ela esta e tenta de novo no proximo
 * tick. Parado dentro do predador por um tick e recuperavel; dentro da pedra,
 * nao.</p>
 */
public final class SafeReleaseSpot {

    private SafeReleaseSpot() { }

    /**
     * Ordem das tentativas, e ela NAO e arbitraria.
     *
     * <p>Primeiro a frente do predador (a soltura natural, que e a que o jogador
     * espera), depois os lados, depois atras, e so entao para cima. "Para cima"
     * fica por ultimo porque teleportar a vitima para o teto e a soltura que
     * mais surpreende -- ela e a ultima carta, nao a primeira.</p>
     */
    private static List<Vec3> candidatos(Vec3 origem, Vec3 direcaoDeSaida, double distancia,
            double alturaDeEscape) {
        Vec3 frente = direcaoDeSaida.normalize().scale(distancia);
        Vec3 lado = new Vec3(-frente.z, 0.0D, frente.x);
        List<Vec3> lugares = new ArrayList<>(6);
        lugares.add(origem.add(frente));
        lugares.add(origem.add(lado));
        lugares.add(origem.subtract(lado));
        lugares.add(origem.subtract(frente));
        lugares.add(origem.add(0.0D, alturaDeEscape, 0.0D));
        // A propria posicao do predador entra por ultimo: ela e melhor do que a
        // pedra, e o empurrao natural das entidades resolve a sobreposicao no
        // tick seguinte. Coloca-la antes faria toda soltura acontecer dentro do
        // bicho, e o jogador nunca veria a boca abrir.
        lugares.add(origem);
        return lugares;
    }

    /**
     * Primeiro lugar livre, na ordem documentada acima.
     *
     * @param origem posicao do predador
     * @param direcaoDeSaida para onde a boca aponta; nao precisa vir normalizada
     * @param distancia quantos blocos a frente tentar primeiro
     * @param alturaDeEscape quanto subir na ultima tentativa
     * @param livre teste medido pelo servidor: cabe uma vitima AQUI?
     * @return o ponto escolhido, ou vazio quando nenhum serve
     */
    public static Optional<Vec3> escolher(Vec3 origem, Vec3 direcaoDeSaida, double distancia,
            double alturaDeEscape, Predicate<Vec3> livre) {
        Objects.requireNonNull(origem, "origem ausente");
        Objects.requireNonNull(direcaoDeSaida, "direcao de saida ausente");
        Objects.requireNonNull(livre, "teste de espaco livre ausente");
        if (!Double.isFinite(distancia) || distancia <= 0.0D
                || !Double.isFinite(alturaDeEscape) || alturaDeEscape <= 0.0D) {
            throw new IllegalArgumentException("geometria de soltura invalida");
        }
        if (direcaoDeSaida.lengthSqr() < 1.0E-6D) {
            throw new IllegalArgumentException("direcao de saida sem comprimento: normalizar um"
                    + " vetor nulo devolve NaN, e a vitima seria solta em lugar nenhum");
        }
        for (Vec3 candidato : candidatos(origem, direcaoDeSaida, distancia, alturaDeEscape)) {
            if (livre.test(candidato)) return Optional.of(candidato);
        }
        return Optional.empty();
    }
}
