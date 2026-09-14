package com.darkcontinent.nenfoundation.enemy.encounter;

/**
 * Em que ponto da vida um encontro esta.
 *
 * <p><b>ARMED existe separado de ACTIVE, e a separacao e a entrega.</b> Sem ela
 * nao ha como distinguir "o encontro existe neste lugar e espera alguem" de "o
 * encontro esta acontecendo AGORA com entidades vivas no mundo". As duas leituras
 * precisam ser diferentes exatamente no momento em que importa: ao ligar o
 * servidor. Um encontro que salva como ACTIVE e volta sem entidades tem de saber
 * que precisa spawnar; um que salva como ARMED nao pode spawnar nada ate alguem
 * chegar. Colapsar os dois da uma de duas falhas, e as duas sao caladas -- ou
 * entidades duplicadas a cada restart, ou um encontro que nunca mais acontece.</p>
 *
 * <p><b>COOLDOWN nao e o mesmo que COMPLETED.</b> Concluido e permanente para
 * quem concluiu; em recarga e temporario para o LUGAR. Um encontro repetivel
 * precisa dos dois estados, e um nao repetivel simplesmente nunca sai de
 * COMPLETED.</p>
 */
public enum EncounterState {
    /** Existe no registro e nao faz nada. Nenhuma entidade, nenhum relogio. */
    DORMANT,
    /** Ancorado no mundo e esperando alguem entrar. Ainda sem entidades. */
    ARMED,
    /** Acontecendo: ha entidades vivas e participantes registrados. */
    ACTIVE,
    /** Terminou com sucesso. A recompensa ja foi travada no ledger. */
    COMPLETED,
    /** Terminou sem sucesso -- todos os participantes sairam, morreram ou desistiram. */
    FAILED,
    /** Terminou e vai voltar. O relogio do lugar, nao o do jogador. */
    COOLDOWN;

    /** Estados em que ha (ou deveria haver) entidade viva no mundo. */
    public boolean temEntidadesVivas() {
        return this == ACTIVE;
    }

    /** Estados terminais do episodio; o encontro nao aceita mais participante. */
    public boolean terminou() {
        return this == COMPLETED || this == FAILED || this == COOLDOWN;
    }
}
