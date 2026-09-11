package com.darkcontinent.nenfoundation.api.event;

/**
 * COMO um jogador despertou para o Nen.
 *
 * <p>POR QUE ISTO EXISTE, e nao um simples {@code despertar(jogador)}:
 *
 * <p>O cânone e explicito em separar os dois caminhos. Uma pessoa desperta
 * gradualmente por treino, ou tem os nos de aura abertos a forca por outro
 * usuario -- e o despertar forcado e PERIGOSO. A skill de lore diz, com todas
 * as letras, que ele "nao deve ser tratado como um atalho inofensivo".
 *
 * <p>Isto nao obriga a implementar consequencia agora. Obriga a que a API nao
 * FECHE A PORTA para ela. Um metodo sem nocao de origem transforma as duas
 * coisas na mesma, e no dia em que o despertar forcado precisar doer, a
 * informacao para distinguir ja se perdeu -- em todo save que existir ate la.
 *
 * <p>O que este enum NAO faz hoje: nenhuma origem aplica efeito diferente.
 * Ele so viaja nos eventos, para que um listener possa reagir. Isso esta
 * declarado de proposito, e nao e um esquecimento.
 */
public enum OrigemDoDespertar {

    /**
     * Treino gradual. O caminho seguro, e o padrao do onboarding.
     */
    TREINO,

    /**
     * Outro usuario de Nen abriu os nos de aura deste jogador.
     *
     * <p>No cânone isto e perigoso. Se um dia houver consequencia -- dano,
     * exaustao, risco de morte --, e aqui que ela se prende.
     */
    FORCADO,

    /**
     * Comando de operador ou seed de teste.
     *
     * <p>Existe para que QA e reproducao de bug nao precisem fingir que
     * treinaram. Nunca deve ganhar consequencia de gameplay: ela mediria o
     * ambiente de teste, e nao o jogo.
     */
    COMANDO
}
