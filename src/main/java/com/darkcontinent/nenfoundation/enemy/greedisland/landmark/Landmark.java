package com.darkcontinent.nenfoundation.enemy.greedisland.landmark;

import com.darkcontinent.nenfoundation.enemy.greedisland.GreedIslandConstants.Ponto;

/**
 * Um ponto de referencia da ilha. Secao 67, fase G9.
 *
 * <p><b>LANDMARK E ORIENTACAO, e nao decoracao.</b> Numa ilha de 80.000
 * blocos, o jogador precisa de algo que responda "onde eu estou" sem abrir
 * mapa -- e e isso que transforma wilderness em geografia. O documento pede um
 * grande a cada 1.000-2.500 blocos em regiao percorrida, e 3.000-6.000 em
 * regiao remota.
 *
 * <p><b>ESPACAMENTO IRREGULAR, e o documento manda: "nao usar grid
 * uniforme".</b> Uma grade regular e reconhecivel em cinco minutos, e a partir
 * dali cada landmark deixa de ser descoberta e vira item de lista.
 *
 * <p>O {@code canon} distingue o que vem da obra do que e nosso, pela mesma
 * razao que as coordenadas de cidade carregam o rotulo: para ninguem defender
 * como cânone, numa discussao futura, algo que este projeto inventou.
 *
 * @param raioDeDescoberta a que distancia ele passa a contar como "conhecido"
 */
public record Landmark(String id, Tipo tipo, Ponto ancora, String regiao,
        Canon canon, int raioDeDescoberta) {

    /** A familia do landmark. Ela decide gerador, som e o que ele significa. */
    public enum Tipo {
        /** A arvore de entrada. Unico. */
        ARVORE_SHISO,
        /** O farol de Soufrabi. */
        FAROL,
        /** Queda d'agua grande, onde o rio corta a serra. */
        CACHOEIRA,
        /** Ponte de vao longo sobre rio maior. */
        PONTE_GRANDE,
        /** A boca de um passo de montanha. */
        PASSO,
        /** Rocha isolada, visivel de longe. */
        ROCHEDO,
        /** Arvore antiga, muito maior que a mata em volta. */
        ARVORE_ANTIGA,
        /** Arena de desafio. */
        ARENA,
        /** Sitio ligado a uma carta. */
        SITIO_DE_CARTA,
        /** Sitio ligado aos Game Masters. */
        SITIO_DE_GAME_MASTER,
        /** Habitat de uma criatura unica. */
        HABITAT_UNICO,
        /** Assentamento menor: vilarejo, posto, acampamento fixo. */
        ASSENTAMENTO
    }

    /** De onde vem esta ideia. */
    public enum Canon {
        /** Nomeado ou descrito na obra. */
        DA_OBRA,
        /** Invencao deste projeto, coerente com a obra. */
        ORIGINAL_COMPATIVEL
    }

    public Landmark {
        if (raioDeDescoberta <= 0) {
            throw new IllegalArgumentException(
                    "landmark sem raio de descoberta: " + id + ". Ele nunca seria"
                            + " marcado como conhecido, e o mapa do jogador nao"
                            + " avancaria -- sem erro nenhum.");
        }
    }

    /** Se este ponto ja e perto o bastante para o landmark contar como visto. */
    public boolean descobertoDe(double x, double z) {
        return Math.hypot(x - this.ancora.x(), z - this.ancora.z())
                <= this.raioDeDescoberta;
    }
}
