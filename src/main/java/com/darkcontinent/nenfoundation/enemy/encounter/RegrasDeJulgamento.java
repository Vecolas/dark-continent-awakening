package com.darkcontinent.nenfoundation.enemy.encounter;

/**
 * Regras do JULGAMENTO -- a mecanica principal do kiriko, e o oposto de todos
 * os outros mobs deste repositorio.
 *
 * <p>DECISAO CENTRAL QUE ESTE RECORD CARREGA: <b>o kiriko nao ataca ninguem; ele
 * AVALIA.</b> Ele aparece em forma humana, observa como o jogador se comporta e
 * decide. Quem ataca, ou quem fere um bicho pacifico na frente dele, e
 * REPROVADO. Quem espera, nao saca arma e o deixa em paz e APROVADO -- ganha a
 * recompensa e ve o bicho ir embora sem uma briga. <b>O jogador vence este
 * encontro NAO LUTANDO.</b> Se alguem "consertar" isso transformando o kiriko em
 * mais um bicho que corre atras de voce, a ficha inteira foi ignorada e nada vai
 * acusar: o build segue verde, o mob spawna, anda e bate.</p>
 *
 * <p>ESTA E A UNICA FONTE DO VEREDITO. Nenhuma outra classe decide aprovado ou
 * reprovado -- a entidade mede o mundo (quem esta perto, quem bateu em quem,
 * ha quantos ticks ninguem saca arma) e pergunta AQUI. Espalhado pelo tick da
 * entidade e pelas Goals, o veredito viraria tres {@code if} diferentes, e a
 * divergencia entre eles nao daria erro nenhum: apareceria como um kiriko que
 * as vezes reprova quem ficou parado -- o pior relato de bug que existe.</p>
 *
 * <p>REPROVACAO NAO SE DESFAZ. Este record e sem memoria de proposito: ele
 * responde sobre uma pontuacao que lhe entregam. Quem chama e que guarda o
 * veredito, e o guarda PARA SEMPRE -- um kiriko reprovado nao volta a avaliar,
 * nao volta ao disfarce e nao aprova depois. "Ele te deu uma segunda chance"
 * apagaria o peso da unica decisao que o encontro pede ao jogador.</p>
 *
 * <p>O TETO DA PACIENCIA E REGRA, NAO DETALHE. {@link #pontuar} nunca devolve
 * mais do que {@link #limiteDeAprovacao()}: paciencia acumulada e uma
 * aprovacao pendente, e nao um CREDITO. Sem o teto, dez segundos parado
 * comprariam varias agressoes de graca -- o jogador esperaria, acumularia
 * duzentos pontos, bateria cinco vezes e ainda seria aprovado. Isso nao daria
 * erro: daria um exame que qualquer um passa batendo. E o mesmo cuidado que
 * {@code RegrasDeFisgada} toma com o piso zero da tensao, na outra ponta.</p>
 *
 * <p>COMO OS NUMEROS DO PERFIL SE LEEM JUNTOS (200, 40, 25, 1, 60): a paciencia
 * enche o teto em 60 ticks e a aprovacao so acontece aos 200, entao o jogador
 * passa tres segundos a mais de bom comportamento do que precisaria. Uma
 * pancada custa 40 -- de teto cheio sobra 20, e ele nao reprova de imediato:
 * precisa de mais 40 ticks de paz para voltar ao teto. A SEGUNDA pancada em
 * menos de 40 ticks leva a pontuacao a negativa e reprova. Na pratica isso quer
 * dizer o que a ficha diz -- quem ataca e reprovado --, porque ninguem bate num
 * mob uma vez so: o proprio tempo de recarga da espada e menor do que a janela
 * de perdao. O que o teto compra e o caso honesto: quem acertou sem querer e
 * parou na hora.</p>
 *
 * @param ticksDeObservacao tempo minimo de olho no jogador antes de qualquer
 *     aprovacao; aprovar mais rapido faria o teste nao existir
 * @param custoDeAgressao quanto custa ferir o proprio kiriko (positivo; entra
 *     negativo)
 * @param custoDeCrueldade quanto custa ferir um inocente na frente dele
 *     (positivo; entra negativo)
 * @param ganhoPorPaciencia quanto rende cada tick de jogador perto, visivel e
 *     em paz
 * @param limiteDeAprovacao pontuacao necessaria para aprovar, e tambem o TETO
 *     da pontuacao
 */
public record RegrasDeJulgamento(int ticksDeObservacao, int custoDeAgressao, int custoDeCrueldade,
        int ganhoPorPaciencia, int limiteDeAprovacao) {

    public RegrasDeJulgamento {
        if (ticksDeObservacao < 1
                // Custo zero ou negativo faria agressao PREMIAR quem agrediu, e o
                // encontro inteiro passaria a ensinar o contrario do que quer ensinar.
                || custoDeAgressao <= 0 || custoDeCrueldade <= 0
                // Paciencia que nao rende nada nunca chega ao limite: o kiriko observaria
                // para sempre e nunca aprovaria ninguem -- sem erro nenhum no log.
                || ganhoPorPaciencia < 1
                // Limite zero aprovaria de cara qualquer um que sobrevivesse aos ticks de
                // observacao, inclusive quem bateu e ficou com pontuacao zero.
                || limiteDeAprovacao < 1) {
            throw new IllegalArgumentException("regras de julgamento invalidas");
        }
    }

    /**
     * A pontuacao do proximo tick, a partir da atual e das tres coisas que o
     * kiriko pode ter visto neste tick.
     *
     * <p>Agressao e crueldade SUBTRAEM; paciencia SOMA. As tres entradas sao
     * independentes e podem valer no mesmo tick -- ferir o kiriko com um golpe
     * de area que tambem pegou um bicho pacifico custa as duas coisas, e assim
     * tem de ser: sao duas falhas, nao uma.</p>
     *
     * <p>O resultado nunca passa de {@link #limiteDeAprovacao()} -- ver o teto
     * explicado na documentacao do record. Ele NAO tem piso: a pontuacao precisa
     * poder ficar negativa, porque negativa e exatamente o que
     * {@link #reprova(int)} le.</p>
     */
    public int pontuar(int atual, boolean atacouOKiriko, boolean feriuInocente, boolean esperouEmPaz) {
        int pontuacao = atual;
        if (atacouOKiriko) {
            pontuacao -= custoDeAgressao;
        }
        if (feriuInocente) {
            pontuacao -= custoDeCrueldade;
        }
        if (esperouEmPaz) {
            pontuacao += ganhoPorPaciencia;
        }
        return Math.min(limiteDeAprovacao, pontuacao);
    }

    /**
     * REPROVADO: a pontuacao ficou NEGATIVA.
     *
     * <p>Zero nao reprova, e isso nao e um detalhe de sinal: zero e quem acabou
     * de chegar e ainda nao fez nada. Reprovar no zero condenaria todo mundo no
     * primeiro tick do encontro, antes de o exame existir.</p>
     *
     * <p>Quem chama guarda o veredito e NAO volta atras -- ver a documentacao do
     * record.</p>
     */
    public boolean reprova(int pontuacao) {
        return pontuacao < 0;
    }

    /**
     * APROVADO: pontuacao no limite <b>E</b> tempo de observacao cumprido.
     *
     * <p>AS DUAS, e essa e a regra que mais importa deste arquivo. So a
     * pontuacao aprovaria quem ficou quieto por tres segundos, e um exame de
     * tres segundos nao e um exame: o jogador nem perceberia que foi avaliado, e
     * a recompensa chegaria antes de ele entender por que. So o tempo aprovaria
     * quem passou dez segundos batendo e parou no fim. O encontro que a ficha
     * descreve exige as duas coisas ao mesmo tempo.</p>
     */
    public boolean aprova(int pontuacao, int ticksObservados) {
        return pontuacao >= limiteDeAprovacao && ticksObservados >= ticksDeObservacao;
    }
}
