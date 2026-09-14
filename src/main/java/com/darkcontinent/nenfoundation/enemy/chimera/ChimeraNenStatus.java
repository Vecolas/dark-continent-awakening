package com.darkcontinent.nenfoundation.enemy.chimera;

/**
 * Quanto de Nen uma formiga JA tem -- e o padrao e NENHUM.
 *
 * <p><b>Por que DORMANT e o valor de nascimento de toda peon.</b> A issue #121 e
 * explicita: nao dar Nen ativo a todo peon. Se o padrao fosse acordado, cada
 * formiga nova ganharia Nen por omissao -- e isso nao daria erro nenhum: daria
 * uma colonia inteira usando aura antes de a historia justificar, com o jogador
 * perdendo a leitura de que Nen e raro e caro.</p>
 *
 * <p><b>Ele e um ESTADO, e nao um booleano, porque a progressao importa.</b>
 * "Acordando" e diferente de "acordado": e a janela em que a formiga ja reage a
 * aura e ainda nao a usa, e e nela que o jogador percebe que a colonia esta
 * mudando. Um booleano apagaria justamente o aviso.</p>
 *
 * <p>ESTE ENUM NAO IMPLEMENTA NEN. Ele so registra o estagio. As regras de aura
 * sao autoridade do nucleo de Nen (CLAUDE.md), e a ligacao entre os dois e a
 * issue #145 -- nao esta aqui, e nao pode nascer aqui por conveniencia.</p>
 */
public enum ChimeraNenStatus {
    /** Nasce assim. Nao percebe aura e nao usa. */
    DORMANT,
    /** Percebe, ainda nao usa. E o aviso que o jogador tem. */
    AWAKENING,
    /** Usa o basico. */
    AWAKENED,
    /** Treinada: usa com intencao. Reservado aos oficiais. */
    TRAINED;

    /** Algum grau de Nen ja existe? */
    public boolean desperto() { return this != DORMANT; }

    /**
     * Progressao MONOTONA: nunca regride.
     *
     * <p>Nen aprendido nao se desaprende, e permitir a volta abriria a porta para
     * um estado oscilando entre dois valores a cada tick -- o que o jogador leria
     * como um bicho piscando, sem causa visivel.</p>
     */
    public ChimeraNenStatus avancarPara(ChimeraNenStatus proximo) {
        if (proximo == null) throw new NullPointerException("estagio ausente");
        return proximo.ordinal() > ordinal() ? proximo : this;
    }
}
