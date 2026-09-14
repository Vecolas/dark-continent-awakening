package com.darkcontinent.nenfoundation.enemy.combat;

/**
 * O que um atacante de flanco faz neste instante -- e POR QUE, quando nao ataca.
 *
 * <p>"Recusa sempre tem motivo." Um {@code boolean} aqui responderia "pode
 * picar?" e esconderia quatro historias diferentes: nao ha alvo, o alvo esta
 * encarando, o alvo esta longe, ou ela mesma decidiu sumir. As quatro produzem o
 * mesmo silencio em tela, e sem o motivo o relato que chega e <em>"as vezes ele
 * fica dando voltas e nao ataca"</em>.</p>
 */
public enum DecisaoDaPicada {

    /** Pelas costas, ao alcance e com caminho: ela PICA agora. */
    PICA,

    /** Nao ha alvo. E o estado normal da maior parte da vida dela. */
    SEM_ALVO,

    /**
     * Ela recolheu a aura e nao esta lutando.
     *
     * <p>Quem entra em Zetsu deixa de ser sentido e fica indefeso; atacar nesse
     * estado seria desfazer a unica coisa que ele comprou. O efeito aqui e de
     * POSTURA do inimigo -- ela nao golpeia -- e nao de aura: quem decide se ha
     * Zetsu e o Nen Foundation, nunca este pacote.</p>
     */
    ESCONDIDA,

    /**
     * O alvo esta OLHANDO para ela.
     *
     * <p>E a regra inteira do bicho: ela chega pelo lado. Sem esta recusa, o
     * oficial mergulha de frente contra quem esta encarando, e a unica defesa que
     * o encontro ensina -- girar a camera e manter o zumbido no campo de visao --
     * deixa de funcionar. Nada disso da erro: da um mob rapido que ataca de
     * qualquer angulo, e o jogador nunca descobre que havia uma regra.</p>
     */
    ENCARADA,

    /** Longe demais para a agulha; ela fecha a distancia. */
    LONGE,

    /**
     * Nao ha linha de visao.
     *
     * <p>Sem esta porta, ela picaria atraves da parede em que o jogador se
     * abrigou -- dano correto, cooldown correto, log limpo, e um bicho que
     * atravessa blocos.</p>
     */
    SEM_LINHA_DE_VISAO
}
