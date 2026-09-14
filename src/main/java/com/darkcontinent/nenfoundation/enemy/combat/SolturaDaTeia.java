package com.darkcontinent.nenfoundation.enemy.combat;

/**
 * Como a presa saiu da teia -- e ela SEMPRE sai por um destes.
 *
 * <p><b>O enum inteiro e a promessa de que existe saida.</b> Prender sem saida e
 * morte sem resposta: o jogador fica olhando a propria tela ate o esquadrao
 * chegar, sem nada que ele possa fazer, e isso nao aparece como bug -- aparece
 * como "esse mob e injusto". Cada valor aqui e uma porta aberta, e
 * {@link EstadoDeTeia} nao tem caminho de saida que nao passe por um deles.</p>
 *
 * <p><b>As duas primeiras sao as que o jogador controla ou aguenta;</b> as duas
 * ultimas sao os pontos de saida do mundo, e existem separados de proposito. Uma
 * limpeza que devolvesse sempre o mesmo valor esconderia a diferenca entre "a
 * teia acabou" e "a aranha morreu segurando alguem" -- e a segunda e exatamente
 * a que deixa um jogador preso para sempre se for esquecida.</p>
 */
public enum SolturaDaTeia {
    /** Ninguem saiu neste tick: a teia continua segurando. */
    NENHUMA,

    /** O prazo acabou. E a saida que existe para quem nao conseguiu fazer nada. */
    TEMPO_ESGOTADO,

    /**
     * Alguem acertou a fiandeira e o fio rasgou.
     *
     * <p>E a saida ATIVA, e a razao de ela existir e que a passiva sozinha nao
     * ensina nada: com so o relogio, a resposta certa do jogador seria esperar --
     * e esperar e precisamente o que o esquadrao quer.</p>
     */
    FIO_ROMPIDO,

    /** A presa deixou de existir: morreu, deslogou, trocou de dimensao. */
    PRESA_SUMIU,

    /**
     * A aranha caiu, foi removida ou o chunk descarregou.
     *
     * <p>Esta e a que nao pode faltar. Sem ela, matar a Spider Webber no tick
     * exato em que ela segura alguem deixaria a vitima imobilizada ate o
     * restart -- sem excecao, sem log, e com o jogador reiniciando o servidor
     * para se libertar.</p>
     */
    PREDADOR_CAIU
}
