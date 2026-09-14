package com.darkcontinent.nenfoundation.enemy.ai;

/**
 * As quatro fases do arranque, em ordem e sem atalho.
 *
 * <p><b>Elas sao uma FILA, e nao um conjunto de flags.</b> Duas flags
 * ({@code arrancando} e {@code fatigado}) permitiriam o estado impossivel "as
 * duas ligadas" e o estado mudo "as duas desligadas durante a recarga" -- e
 * nenhum dos dois daria erro. O primeiro daria um bicho rapido E lento no mesmo
 * tick, com o ultimo {@code if} decidindo; o segundo apagaria a recarga, e o
 * arranque voltaria a estar disponivel sem que nada tivesse mudado.</p>
 *
 * <p><b>A ordem e o preco.</b> {@link #ARRANCANDO} e o que o bicho ganha,
 * {@link #FATIGADA} e o que ele paga, e {@link #EM_RECARGA} e o que impede o
 * pagamento de ser cobrado uma vez so. Pular qualquer uma delas nao levanta
 * excecao: transforma um arranque com preco numa velocidade permanente
 * fantasiada, e o encontro vira uma perseguicao que nunca termina.</p>
 */
public enum FaseDoArranque {
    /** Parada, com o arranque disponivel. */
    PRONTA,
    /** Correndo acima da propria velocidade. E o unico ganho. */
    ARRANCANDO,
    /** Abaixo da propria velocidade. E a JANELA DE RESPOSTA do jogador. */
    FATIGADA,
    /** De volta a velocidade normal, ainda sem direito a um novo arranque. */
    EM_RECARGA
}
