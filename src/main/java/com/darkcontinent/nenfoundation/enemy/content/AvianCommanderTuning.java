package com.darkcontinent.nenfoundation.enemy.content;

import com.darkcontinent.nenfoundation.enemy.ai.RegrasDeComandoAereo;
import com.darkcontinent.nenfoundation.enemy.ai.RegrasDeOrdemDeEsquadrao;
import com.darkcontinent.nenfoundation.enemy.ai.squad.SquadRules;
import com.darkcontinent.nenfoundation.enemy.chimera.nen.TacticalNenIntent;
import com.darkcontinent.nenfoundation.enemy.combat.AttackDefinition;
import com.darkcontinent.nenfoundation.enemy.combat.AttackHitbox;
import java.util.Objects;

/**
 * Os numeros do Avian Commander que nascem com o COMPORTAMENTO dele.
 *
 * <p><b>Por que este arquivo existe em vez de mais metodos em
 * {@link ChimeraProfiles}.</b> Aquele arquivo guarda a ficha publicada das nove
 * formigas, e neste momento varias frentes escrevem o comportamento de formigas
 * diferentes ao mesmo tempo. Todas precisariam acrescentar metodos no MESMO
 * arquivo, e o resultado de um merge assim nao e um conflito barulhento -- e uma
 * resolucao apressada em que o metodo de alguem some. Metodo que some nao da erro
 * de compilacao quando o chamador some junto: da um mob que perdeu o comando
 * aereo e continua nascendo, voando e passando em todo portao, so que como um
 * oficial forte com asas.</p>
 *
 * <p><b>O que fica la e o que fica aqui.</b> Em {@code ChimeraProfiles} ficam a
 * ficha publicada (atributos, spawn, faccao), a recarga entre golpes e o stagger
 * -- tudo que outros sistemas ja leem. Aqui ficam os numeros NOVOS, os que este
 * comportamento inaugurou: a altitude de comando, a geometria do mergulho e a
 * janela de subida.</p>
 *
 * <p><b>O dano nao e um numero proprio.</b> Ele e lido de
 * {@code ChimeraProfiles.avianCommander().attributes().attackDamage()}, porque o
 * mergulho e o unico golpe dela. Repetir o 15 aqui criaria duas fontes para a
 * mesma verdade, e girar o atributo numa sessao de balanceamento mudaria a barra
 * de vida do jogador sem mudar este arquivo.</p>
 *
 * <p><b>O mesmo vale para o bando.</b> Teto, espacamento, moral minima, raio de
 * reforco e orcamento de coordenacao saem inteiros de {@link SquadRules#esquadrao()}.
 * Este arquivo nao redeclara nenhum dos cinco: redeclarado, o numero daqui
 * venceria em metade dos caminhos e o de la na outra metade, e a sessao de
 * balanceamento giraria um botao morto.</p>
 *
 * <p><b>Nada aqui e botao de tuning, e e por isso que esta em codigo.</b> Estes
 * sao os limites de DESIGN do bicho -- girar a altitude de comando nao ajusta a
 * dificuldade, troca o que o mob ensina. O CLAUDE.md pede que o motivo fique
 * escrito ao lado, e ele esta.</p>
 */
public final class AvianCommanderTuning {

    private AvianCommanderTuning() { }

    // ------------------------------------------------------------------- voo

    /**
     * Altura sobre o chao, em blocos, a partir da qual ela enxerga o campo e
     * comanda.
     *
     * <p>Sete, e o numero tem duas pontas. Por cima: alta demais e ela sai do
     * alcance de tudo que o jogador tem no comeco do jogo, e o encontro vira uma
     * espera. Por baixo: sete blocos e pouco mais que o dobro do alcance de um
     * golpe corpo a corpo, ou seja, ela fica FORA de alcance enquanto comanda e
     * DENTRO assim que desce -- que e exatamente a leitura que o mob precisa
     * entregar.</p>
     *
     * <p>Nao e botao de balanceamento: e a ficha. Baixando isto, ela comanda de
     * perto e a tatica de derruba-la desaparece sem que nada reprove.</p>
     */
    public static final double ALTITUDE_DE_COMANDO = 7.0D;

    /**
     * Altura minima para COMECAR um mergulho.
     *
     * <p>Igual a altitude de comando, e a igualdade e deliberada: o mergulho sai
     * de onde ela comanda, e nao de meia altura. Com um valor menor, ela poderia
     * atacar durante a subida -- e a subida e a janela do jogador. Duas coisas
     * disputando a mesma janela terminam com a janela sumindo, sem erro nenhum.</p>
     */
    public static final double ALTITUDE_PARA_MERGULHAR = ALTITUDE_DE_COMANDO;

    /**
     * Altura em que o mergulho acaba e a subida comeca, em blocos.
     *
     * <p>Um e meio: a barriga dela passa dentro do alcance de um golpe do chao, e
     * e esse instante que paga o mergulho. Zero faria ela POUSAR, e uma
     * comandante pousada e um oficial de chao com asas. Tres ou mais e um
     * mergulho que ninguem alcanca -- ela desceria, machucaria e subiria sem
     * nunca estar ao alcance de nada.</p>
     */
    public static final double ALTURA_DE_ABANDONO_DO_MERGULHO = 1.5D;

    /**
     * Ticks em que ela NAO comanda depois de um mergulho.
     *
     * <p>Trinta -- um segundo e meio. E o preco do golpe, e o unico preco que ele
     * tem: nesse intervalo o bando fica com o alvo velho e sem reagrupar, e quem
     * sobreviveu ao mergulho ganha uma janela real para matar um membro ou sair do
     * cerco. Encurtar isto nao deixa ela mais perigosa -- apaga a resposta do
     * jogador, e o encontro vira uma sequencia de mergulhos sem intervalo.</p>
     *
     * <p>A outra ponta deste numero e o clipe {@code recovery} em
     * {@code avian_commander_animacoes.py}, que e cobrado contra ele: um clipe
     * mais curto poria ela de volta no alto na tela enquanto o servidor ainda a
     * proibe de comandar, e o jogador leria "acabou" antes de acabar.</p>
     */
    public static final int TICKS_DE_SUBIDA_APOS_MERGULHO = 30;

    /**
     * Distancia horizontal em que ela se compromete com o mergulho, em blocos.
     *
     * <p>Vinte e quatro, contra os 36 de {@code followRange}. A diferenca e o
     * espaco em que ela VE e nao ataca -- e e nele que o comando acontece. Igualar
     * os dois transformaria "ela viu voce" em "ela esta vindo", e o alcance 36
     * deixaria de significar visao para significar agressao.</p>
     */
    public static final double ALCANCE_DE_MERGULHO = 24.0D;

    /** Quantos membros vivos fazem "reagrupar" significar alguma coisa. */
    public static final int MEMBROS_PARA_REAGRUPAR = 3;

    /** Multiplicador de velocidade ao ganhar altitude. */
    public static final double VELOCIDADE_DE_SUBIDA = 1.2D;
    /** Multiplicador de velocidade ao manter o posto de comando. */
    public static final double VELOCIDADE_DE_COMANDO = 0.9D;
    /** Multiplicador de velocidade no mergulho -- o unico momento em que ela e rapida. */
    public static final double VELOCIDADE_DE_MERGULHO = 1.6D;

    // -------------------------------------------------------------- mergulho
    //
    // A FORMA DO GOLPE E A DE QUEM AVISA. Dezoito ticks de windup contra os dez
    // do Wolf Pack Hunter: ela nao ganha por surpresa, ganha por posicao. Um
    // windup curto num golpe que vem de sete blocos acima seria um golpe que
    // ninguem consegue ler a tempo, e a resposta do jogador (sair de baixo)
    // deixaria de existir.

    /** Ticks de aviso: asas recolhidas e o corpo apontado para baixo. */
    public static final int WINDUP_DO_MERGULHO = 18;
    /** Ticks em que as garras machucam. */
    public static final int JANELA_DO_MERGULHO = 6;
    /**
     * Ticks de recuperacao do ATAQUE.
     *
     * <p>Menor que {@link #TICKS_DE_SUBIDA_APOS_MERGULHO} de proposito, e os dois
     * numeros medem coisas diferentes: este acaba quando ela pode atacar de novo,
     * aquele acaba quando ela pode COMANDAR de novo. Fundidos num so, ou o ataque
     * ficaria preso pelo silencio do comando, ou o comando voltaria junto com o
     * ataque -- e no segundo caso o mergulho deixaria de custar nada.</p>
     */
    public static final int RECUPERACAO_DO_MERGULHO = 24;

    /** Empurrao do mergulho. Grande: ele tira o alvo do lugar, e e disso que o bando vive. */
    public static final float EMPURRAO_DO_MERGULHO = 0.55F;

    /**
     * Meia-largura do alvo tipico -- um jogador tem 0.6 de lado.
     *
     * <p>Nao e botao de balanceamento: e a conversao entre as duas reguas que este
     * arquivo usa. A distancia de DECISAO e medida de centro a centro
     * ({@code distanceTo}); a caixa de golpe e testada contra a BORDA do alvo
     * ({@code AABB.intersects}). Sem esta conversao as duas parecem comparaveis e
     * nao sao.</p>
     */
    public static final double MEIA_LARGURA_DE_UM_ALVO = 0.3D;

    /**
     * Distancia (centro a centro) em que ela decide fechar as garras.
     *
     * <p>Tem de ser MENOR que {@code maxZ + MEIA_LARGURA_DE_UM_ALVO}, e o teste
     * cobra isso. Maior, ela comeca um windup de 18 ticks contra alguem que ja
     * esta fora do alcance das garras, e o mergulho inteiro sai para o vazio. Isso
     * nao da erro: da uma comandante que erra sozinha e parece quebrada.</p>
     */
    public static final double ALCANCE_DAS_GARRAS = 1.05D;

    /**
     * Quanto o corpo dela viaja para a frente durante a janela que machuca.
     *
     * <p><b>Este numero e a ponte entre a arte e a regra.</b> A garra DESENHADA
     * alcanca {@link #ALCANCE_DESENHADO_DA_GARRA} blocos a frente do centro -- e
     * nao pode alcancar mais, porque o modelo inteiro tem de caber nos 1.4 bloco
     * da hitbox. A caixa do mergulho reivindica 0.95. A diferenca e paga aqui,
     * pelo impulso.</p>
     *
     * <p>Tirar o impulso sem encolher a caixa da um jogador que apanha de uma
     * garra que, na tela, parou antes dele -- dano certo, cooldown certo, log
     * limpo, e a unica leitura que ele tem quebrada. Por isso a soma e cobrada nos
     * DOIS lados: em {@code avian_commander_geo.py}
     * ({@code valida_garra_alcanca_o_mergulho}) e em {@code AvianCommanderTuningTest}.</p>
     */
    public static final double IMPULSO_DO_MERGULHO = 0.65D;

    /**
     * Alcance da garra DESENHADA, em blocos, do centro do bicho ate a ponta.
     *
     * <p>Copiado de {@code avian_commander_geo.py}: a caixa {@code talon_left}
     * comeca em z = -6 px, e 6/16 = 0.375. Duplicacao DECLARADA -- a outra ponta e
     * a regua daquele arquivo, e as duas juntas formam um portao que morde dos
     * dois lados: quem encolher a garra la reprova la, quem esticar a caixa aqui
     * reprova aqui.</p>
     */
    public static final double ALCANCE_DESENHADO_DA_GARRA = 0.375D;

    /**
     * Recarga imposta a quem interrompeu o mergulho.
     *
     * <p>Mais longa que a recarga normal ({@code ChimeraProfiles.avianCommanderRecarga()},
     * 50 ticks): interromper uma comandante tem de VALER duas vezes -- ela perde o
     * golpe E perde o comando. Sem isto, {@code reset()} devolveria a fase para
     * IDLE e ela poderia recomecar no tick seguinte, e o jogador aprenderia a
     * ignorar o cambaleio do unico mob em que o cambaleio muda o bando inteiro.</p>
     */
    public static final int RECARGA_APOS_INTERRUPCAO = 70;

    // ------------------------------------------------------------------ Nen

    /**
     * A intencao de Nen permite mergulhar?
     *
     * <p><b>Esta e a UNICA ligacao entre Nen e o comportamento dela, e ela e de mao
     * unica.</b> O {@code TacticalNenController} devolve uma INTENCAO; aqui essa
     * intencao vira uma decisao do INIMIGO -- mergulhar ou ficar no posto. Nada
     * neste metodo toca aura, custo ou tecnica: o Nen Foundation e a unica
     * autoridade sobre Nen (CLAUDE.md), e um inimigo que gerisse a propria aura
     * seria a segunda -- duas autoridades sobre a mesma mecanica divergem sem dar
     * erro, e o sintoma e desbalanceamento que ninguem consegue explicar.</p>
     *
     * <p>Duas intencoes VETAM o mergulho, e por razoes opostas:</p>
     *
     * <ul>
     *   <li>{@code ENTRAR_EM_ZETSU} e a intencao de quem desistiu da briga --
     *       mergulhar recolhida seria entregar de graca o unico bicho do encontro
     *       que sabe sumir;</li>
     *   <li>{@code MANTER_KEN} e defesa fechada e cara -- larga-la para atacar
     *       gastaria a defesa no exato instante em que ela e necessaria.</li>
     * </ul>
     *
     * <p>Tudo o mais mergulha, {@code NENHUMA} inclusive: uma formiga sem Nen
     * continua sendo uma comandante. O contrario -- exigir Nen para atacar --
     * deixaria toda formiga dormente inofensiva, e a colonia inteira e dormente no
     * comeco.</p>
     */
    public static boolean mergulhoPermitidoPor(TacticalNenIntent intencao) {
        Objects.requireNonNull(intencao, "intencao de Nen ausente: nulo aqui viraria"
                + " NullPointerException no meio do tick do servidor, e a pilha nao diria qual"
                + " comandante deixou de responder");
        return intencao != TacticalNenIntent.ENTRAR_EM_ZETSU
                && intencao != TacticalNenIntent.MANTER_KEN;
    }

    // ------------------------------------------------------------- montagem

    /** As regras de altitude, comando e mergulho, com a continencia cobrada no construtor. */
    public static RegrasDeComandoAereo comandoAereo() {
        return new RegrasDeComandoAereo(ALTITUDE_DE_COMANDO, ALTITUDE_PARA_MERGULHAR,
                ALTURA_DE_ABANDONO_DO_MERGULHO, TICKS_DE_SUBIDA_APOS_MERGULHO,
                ALCANCE_DE_MERGULHO);
    }

    /** As regras de ordem, sobre o orcamento de esquadrao -- que nao e redeclarado aqui. */
    public static RegrasDeOrdemDeEsquadrao ordens() {
        return new RegrasDeOrdemDeEsquadrao(SquadRules.esquadrao(), MEMBROS_PARA_REAGRUPAR);
    }

    /** O bando dela: ate oito, espacamento frouxo, coordenacao a cada dez ticks. */
    public static SquadRules bando() {
        return SquadRules.esquadrao();
    }

    /**
     * O mergulho.
     *
     * <p>O windup e interrompivel e a JANELA nao: depois que as garras abrem, elas
     * abrem. Interromper durante os 6 ticks ativos faria ela cancelar um golpe que
     * o jogador ja viu sair de sete blocos acima -- e com um telegrafo desse
     * tamanho, a leitura e a defesa que o encontro inteiro entrega.</p>
     *
     * <p>A recuperacao TAMBEM nao e interrompivel, e aqui a razao e de comando: se
     * o cambaleio cortasse a recuperacao, a comandante interrompida voltaria ao
     * alto -- e portanto ao comando -- antes de quem nao apanhou, e punir o golpe
     * dela aceleraria a proxima ordem em vez de atrasa-la.</p>
     */
    public static AttackDefinition mergulho() {
        return new AttackDefinition("dive", WINDUP_DO_MERGULHO, JANELA_DO_MERGULHO,
                RECUPERACAO_DO_MERGULHO,
                ChimeraProfiles.avianCommander().attributes().attackDamage(),
                EMPURRAO_DO_MERGULHO, true, false, false);
    }

    /**
     * Caixa do mergulho, em coordenadas LOCAIS. A FRENTE E +Z.
     *
     * <p><b>+Z, e isto nao e distracao.</b> Na GEOMETRIA do modelo (formato
     * Bedrock) a frente e -Z; na transformacao de {@link AttackHitbox}, que e
     * matematica de mundo, com yaw 0 o olhar vanilla aponta para +Z. Misturar as
     * duas ja custou um bug neste repositorio: a caixa ficou ATRAS do mob, que
     * atacava, animava e nao encostava em quem estava na frente -- quem estivesse
     * pelas costas e que apanhava. Fase certa, cooldown certo, log limpo.</p>
     *
     * <p>A caixa e ALTA (1.1 bloco) e estreita (1.1 de largura) de proposito: o
     * mergulho vem de cima, e um alvo que se abaixa nao devia escapar de garras
     * que descem. Larga-la para os lados, ao contrario, apagaria a resposta certa
     * -- sair de BAIXO dela e a leitura que o mob ensina.</p>
     *
     * <p>O {@code minZ} de 0.25 exclui o proprio corpo; o {@code maxZ} de 0.95 e
     * coberto pela garra desenhada (0.375) mais o impulso (0.65), e as duas pontas
     * dessa conta sao cobradas -- aqui, por {@code AvianCommanderTuningTest}, e la,
     * por {@code avian_commander_geo.py}.</p>
     */
    public static AttackHitbox caixaDoMergulho() {
        return new AttackHitbox(-0.55D, 0.0D, 0.25D, 0.55D, 1.1D, 0.95D);
    }
}
