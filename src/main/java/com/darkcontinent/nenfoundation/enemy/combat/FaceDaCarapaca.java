package com.darkcontinent.nenfoundation.enemy.combat;

/**
 * POR ONDE o golpe entrou num bicho de armadura orientada -- e a lista e fechada.
 *
 * <p><b>Ela nao e enfeite de log.</b> Cada face paga um preco diferente, e o
 * encontro inteiro de um mob encouracado e sobre descobrir qual. Um
 * {@code boolean pelasCostas} colapsaria o flanco no meio do caminho e obrigaria
 * quem le a escolher um dos dois extremos para ele -- e as duas escolhas estao
 * erradas em jogo: tratado como frente, contornar pela metade nao paga nada e o
 * jogador desiste de contornar; tratado como costas, encostar de lado ja entrega
 * o bicho e a carapaca deixa de significar coisa alguma.</p>
 *
 * <p>A face e sempre resolvida pelo SERVIDOR, a partir do angulo entre o corpo
 * do bicho e a direcao de quem bateu. O cliente nunca diz por onde acertou: se
 * dissesse, todo golpe seria pelas costas e o sintoma nao seria erro nenhum --
 * seria um mob que morre depressa demais e ninguem consegue explicar.</p>
 */
public enum FaceDaCarapaca {
    /** O arco frontal: a placa inteira entre o golpe e o bicho. */
    FRENTE,
    /** Os lados: a placa ainda cobre, mas a costura com o abdomen ja aparece. */
    FLANCO,
    /** As costas e o ventre exposto atras da placa. E a resposta do encontro. */
    VENTRE
}
