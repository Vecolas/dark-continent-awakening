package com.darkcontinent.nenfoundation.enemy.combat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Portao da pinca: onde a presa fica, com que forca ela e puxada e quando escapa.
 *
 * <p>Segurar SEM montar e mais barato para o jogador -- ele mantem a camera --
 * e mais caro para o codigo: ninguem garante que a vitima continua ali. Todos os
 * defeitos abaixo sao dessa familia, e nenhum levanta excecao em jogo.</p>
 */
class RegrasDePincaTest {

    /** Os mesmos numeros do Crab Heavy, escritos aqui como CASO e nao como fonte. */
    private static RegrasDePinca regras() {
        return new RegrasDePinca(1.1D, 3.0D, 0.55D);
    }

    // ------------------------------------------------------- o caso normal

    @Test
    @DisplayName("a presa fica a frente do corpo, na altura do chao do predador")
    void oPontoDaPincaFicaAFrenteENoChao() {
        RegrasDePinca pinca = regras();
        Vec3 ponto = pinca.pontoDaPinca(new Vec3(10.0D, 64.0D, 10.0D), new Vec3(0.0D, 0.0D, 1.0D));

        assertEquals(10.0D, ponto.x, 1.0E-6D);
        assertEquals(11.1D, ponto.z, 1.0E-6D);
        assertEquals(64.0D, ponto.y, 1.0E-6D,
                "erguer a presa ate a altura da garra exigiria segura-la contra a gravidade todo"
                        + " tick, e o resultado e um jogador tremendo no ar -- alem de uma soltura"
                        + " no alto, com dano de queda que ninguem pediu");
    }

    @Test
    @DisplayName("a direcao e normalizada: um vetor longo nao joga a presa longe")
    void aDirecaoENormalizada() {
        Vec3 ponto = regras().pontoDaPinca(Vec3.ZERO, new Vec3(0.0D, 0.0D, 40.0D));
        assertEquals(1.1D, ponto.length(), 1.0E-6D,
                "sem normalizar, o comprimento do vetor de olhar entraria na conta e a presa"
                        + " seria mantida a dezenas de blocos do bicho, com o relogio do agarrao"
                        + " correndo normalmente");
    }

    @Test
    @DisplayName("o puxao e LIMITADO, e some quando a presa ja esta no ponto")
    void oPuxaoELimitadoENuncaVibra() {
        RegrasDePinca pinca = regras();
        Vec3 ponto = new Vec3(0.0D, 0.0D, 0.0D);

        // Perto: o puxao fecha a distancia inteira num tick, sem exagerar.
        Vec3 perto = pinca.puxao(new Vec3(0.0D, 0.0D, 0.2D), ponto);
        assertEquals(0.2D, perto.length(), 1.0E-6D);

        // Longe: o puxao e cortado no teto. Sem o corte a presa seria ARREMESSADA
        // tres blocos num tick, atravessando o que houvesse no caminho -- e o
        // sintoma seria um jogador do outro lado da parede, ou morto de queda.
        Vec3 longe = pinca.puxao(new Vec3(0.0D, 0.0D, 3.0D), ponto);
        assertEquals(0.55D, longe.length(), 1.0E-6D);
        assertTrue(longe.z < 0.0D, "o puxao tem de apontar PARA o ponto de pinca");

        // Ja no ponto: zero. Normalizar um deslocamento quase nulo faria a presa
        // vibrar em torno da pinca todo tick, e vibracao e o tipo de defeito que
        // ninguem consegue descrever num relato de bug.
        assertEquals(Vec3.ZERO, pinca.puxao(ponto, ponto));
    }

    @Test
    @DisplayName("fora do raio a presa deixa de estar presa -- e essa e a saida do teleporte")
    void oRaioDeRupturaSoltaQuemSumiu() {
        RegrasDePinca pinca = regras();
        assertTrue(pinca.aindaPresa(0.0D));
        assertTrue(pinca.aindaPresa(3.0D), "o limite e inclusivo: estar exatamente no raio ainda"
                + " e estar preso, senao um empurrao de 3.0 exato soltaria de graca");
        assertFalse(pinca.aindaPresa(3.01D),
                "sem esta resposta o predador continuaria segurando quem foi teleportado para"
                        + " longe: relogio correndo, pulsos de dano saindo e nenhuma excecao em"
                        + " lugar nenhum");
    }

    // ----------------------------------------------------- o caso recusado

    @Test
    @DisplayName("direcao sem comprimento e RECUSADA, e nao normalizada para NaN")
    void direcaoNulaRecusaComMotivo() {
        RegrasDePinca pinca = regras();
        assertThrows(IllegalArgumentException.class,
                () -> pinca.pontoDaPinca(Vec3.ZERO, Vec3.ZERO));
        // Olhar reto para baixo tem componente horizontal nula: o caso nao e
        // teorico, e a presa seria presa em lugar nenhum -- com o relogio correndo.
        assertThrows(IllegalArgumentException.class,
                () -> pinca.pontoDaPinca(Vec3.ZERO, new Vec3(0.0D, -1.0D, 0.0D)));
        assertThrows(IllegalArgumentException.class, () -> pinca.aindaPresa(Double.NaN));
        assertThrows(IllegalArgumentException.class, () -> pinca.aindaPresa(-1.0D));
    }

    // ------------------------------------- os casos que DEVEM ser reprovados

    @Test
    @DisplayName("REPROVA: raio de ruptura menor que o arco da propria pinca")
    void raioMenorQueOArcoDeveReprovar() {
        // O ponto de pinca gira em volta do predador. Com raio menor que o
        // diametro do arco (2 x distancia), um caranguejo que simplesmente se
        // virasse perderia a presa sozinho -- sem ninguem ter feito nada, e so
        // quando o bicho gira, que e quando ele procura o proximo alvo.
        assertThrows(IllegalArgumentException.class, () -> new RegrasDePinca(1.1D, 2.0D, 0.55D),
                "raio 2.0 nao cobre o arco de uma pinca a 1.1 bloco, e o agarrao se desfaz"
                        + " sozinho na primeira viragem");
    }

    @Test
    @DisplayName("REPROVA: raio de ruptura menor que o puxao de um tick")
    void raioMenorQueOPuxaoDeveReprovar() {
        assertThrows(IllegalArgumentException.class, () -> new RegrasDePinca(0.4D, 0.9D, 1.2D),
                "com o puxao maior que o raio, a vitima que atrasar um unico tick ja estara fora"
                        + " e sera solta de graca: o agarrao 'as vezes nao pega', que e o pior"
                        + " relato de bug que existe");
    }

    @Test
    @DisplayName("REPROVA: geometria de pinca zerada ou nao finita")
    void geometriaInvalidaDeveReprovar() {
        assertThrows(IllegalArgumentException.class, () -> new RegrasDePinca(0.0D, 3.0D, 0.55D));
        assertThrows(IllegalArgumentException.class, () -> new RegrasDePinca(1.1D, 3.0D, 0.0D));
        assertThrows(IllegalArgumentException.class,
                () -> new RegrasDePinca(Double.NaN, 3.0D, 0.55D));
    }
}
