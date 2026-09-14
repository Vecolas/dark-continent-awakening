package com.darkcontinent.nenfoundation.enemy.ai;

/**
 * O que a comandante alada faz com este tick -- e so isso.
 *
 * <p><b>Quatro valores, e nenhum deles e "perseguir".</b> A ausencia e o desenho
 * do bicho escrito onde ele nao pode ser esquecido. Uma comandante que persegue
 * e um mob voador que fica em cima do alvo: ela continua nascendo, atacando,
 * derrubando vida e passando em todo portao -- e o encontro perde a unica coisa
 * que ela ensina, que e derruba-la para calar as ordens. Nada disso levanta
 * excecao; o sintoma e "ela e so um oficial forte que voa".</p>
 *
 * <p>Cada valor existe por uma falha que NAO da erro:</p>
 *
 * <ul>
 *   <li>{@link #SUBIR} -- sem ele, uma comandante empurrada para baixo continua
 *       comandando do chao, e a tatica de traze-la para baixo deixa de existir.
 *       O jogador nao ve nada errado: ve um bando que nunca para de se
 *       reorganizar;</li>
 *   <li>{@link #COMANDAR} -- e a UNICA postura em que uma ordem sai. Escrita como
 *       "sempre que for o tick de coordenacao", a altitude viraria decoracao;</li>
 *   <li>{@link #MERGULHAR} -- o mergulho E o ataque. Sem um estado proprio, o
 *       ataque viraria "chegar perto e bater", que e o que qualquer oficial faz;</li>
 *   <li>{@link #RECOLHER} -- a subida depois do mergulho, e ela e a JANELA DO
 *       JOGADOR. Sem ela, a comandante volta a comandar no tick seguinte ao golpe
 *       e o mergulho passa a ser lucro puro: ela desce, machuca e nao paga nada.</li>
 * </ul>
 */
public enum PosturaDeComando {
    /** Ganhar altitude. Enquanto esta aqui ela NAO comanda -- e o que se forca. */
    SUBIR,
    /** No alto, com o campo a vista: e a unica postura que autoriza uma ordem. */
    COMANDAR,
    /** Descer sobre o alvo. O mergulho e o ataque dela. */
    MERGULHAR,
    /** Sair do mergulho subindo: nem comanda, nem ataca. E a janela do jogador. */
    RECOLHER;

    /** Ela esta em condicao de dar ordem neste tick? So no alto, e so parada la. */
    public boolean comanda() { return this == COMANDAR; }
}
