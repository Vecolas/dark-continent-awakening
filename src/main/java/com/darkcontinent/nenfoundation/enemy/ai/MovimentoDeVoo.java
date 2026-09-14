package com.darkcontinent.nenfoundation.enemy.ai;

/**
 * O que um batedor alado faz com a proxima fracao de segundo.
 *
 * <p><b>Cinco valores, e nenhum deles e "atacar o alvo".</b> Esse e o desenho do
 * bicho escrito onde ele nao pode ser esquecido: {@link #MORDER} existe, mas e o
 * que sobra quando fugir deixou de ser opcao. Um enum com {@code PERSEGUIR} aqui
 * convidaria o primeiro ajuste de gameplay a transformar o batedor num mob de
 * combate ruim -- 3 de dano nao ameaca ninguem --, e o encontro deixaria de ser
 * sobre calar o mensageiro para virar sobre matar um inseto chato.</p>
 */
public enum MovimentoDeVoo {
    /** Manter posicao no ar. E a resposta mais comum, e nao e passividade: e vigia. */
    PAIRAR,
    /** Abrir distancia do alvo. O batedor ja relatou; o que ele quer agora e viver. */
    AFASTAR,
    /** Ganhar altura ate o teto preferido. Altura e o que torna a fuga possivel. */
    SUBIR,
    /** Morder. So acontece encurralado, e e a unica coisa que os 3 de dano fazem. */
    MORDER,
    /**
     * Perder o controle do voo.
     *
     * <p>E o que o cambaleio faz num bicho que voa. Num mob de chao stagger e um
     * passo trocado; aqui e a queda -- e e por isso que interromper este bicho vale
     * a pena. Sem este valor o batedor cambalearia pairando, e o jogador que
     * acertou o tiro nao ganharia nada visivel por ter acertado.</p>
     */
    CAIR
}
