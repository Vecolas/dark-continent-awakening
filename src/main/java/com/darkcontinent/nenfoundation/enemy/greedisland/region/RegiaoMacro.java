package com.darkcontinent.nenfoundation.enemy.greedisland.region;

import com.darkcontinent.nenfoundation.enemy.greedisland.GreedIslandConstants.Ponto;

/**
 * Uma das 24 macro-regioes da secao 60. Fase G4.
 *
 * <p><b>REGIAO NAO E BIOMA, e a secao 62 separa os dois com todas as letras:</b>
 *
 * <table border="1">
 *   <caption>A separacao</caption>
 *   <tr><td><b>REGIAO</b></td><td>papel geografico e de gameplay</td></tr>
 *   <tr><td><b>BIOMA</b></td><td>aparencia e ecologia</td></tr>
 * </table>
 *
 * <p>A armadilha que essa separacao evita e concreta: com 24 regioes, a saida
 * preguicosa e criar 24 biomas -- e aí cada ajuste de arvore vira um arquivo
 * novo, e duas regioes vizinhas com a mesma cara precisam de dois biomas
 * identicos com nomes diferentes. Aqui, nove biomas atendem as 24 regioes, e
 * quem escolhe qual e {@link BiomaDaIlha}, olhando regiao MAIS altitude,
 * umidade, distancia do rio e distancia da costa.
 *
 * <p>A REGIAO E UM CENTRO E UM RAIO, e nao um poligono. O documento pede
 * "polygon/mask", e poligono seria mais preciso -- mas exigiria desenhar 24
 * contornos a mao antes de haver uma unica regiao em jogo. Centro e raio,
 * resolvidos pelo mais proximo, dao fronteiras naturais entre vizinhas e
 * cobrem a ilha inteira sem buraco. Quando alguem precisar de uma fronteira
 * que o mais-proximo nao da -- um rio separando duas regioes, por exemplo --,
 * o campo vira poligono. <b>Isto esta declarado, e nao escondido.</b>
 *
 * @param dificuldade a faixa da secao 61, de 1 (inicio) a 8 (zonas especiais)
 */
public record RegiaoMacro(String id, String nome, Ponto centro, int raio,
        int dificuldade, Terreno terreno, Umidade umidade) {

    /** O carater do relevo da regiao. Entra na escolha do bioma. */
    public enum Terreno {
        /** Costa e praia. */
        COSTEIRO,
        /** Planicie baixa. */
        PLANICIE,
        /** Colina ondulada. */
        COLINA,
        /** Mesa alta. */
        PLANALTO,
        /** Serra. */
        MONTANHA,
        /** Brejo e canal. */
        ALAGADO
    }

    /** Quanta agua o ar carrega. Entra na escolha do bioma e da vegetacao. */
    public enum Umidade {
        /** Seco. */
        SECA,
        /** Media. */
        MEDIA,
        /** Umida. */
        UMIDA,
        /** Encharcada. */
        ENCHARCADA
    }

    public RegiaoMacro {
        if (raio <= 0) {
            throw new IllegalArgumentException("regiao sem raio: " + id);
        }
        if (dificuldade < 1 || dificuldade > 8) {
            throw new IllegalArgumentException(
                    "faixa de dificuldade fora de 1..8 em " + id + ": " + dificuldade);
        }
    }

    /** Distancia do ponto ao centro desta regiao. */
    public double distanciaDe(double x, double z) {
        return Math.hypot(x - this.centro.x(), z - this.centro.z());
    }
}
