package com.darkcontinent.nenfoundation.enemy.content;

import com.darkcontinent.nenfoundation.enemy.ai.RegrasDeMatilha;
import com.darkcontinent.nenfoundation.enemy.ai.squad.SquadRules;
import com.darkcontinent.nenfoundation.enemy.combat.AttackDefinition;
import com.darkcontinent.nenfoundation.enemy.combat.AttackHitbox;

/**
 * Os numeros do Wolf Pack Hunter que nascem com o COMPORTAMENTO dele.
 *
 * <p><b>Por que este arquivo existe em vez de mais metodos em
 * {@link GreedIslandProfiles}.</b> Aquele arquivo e hostil a merge: ele guarda os
 * perfis das sete criaturas da ilha, e neste momento varias frentes escrevem o
 * comportamento de criaturas diferentes ao mesmo tempo. Todas elas precisariam
 * acrescentar metodos no MESMO arquivo, e o resultado de um merge assim nao e um
 * conflito barulhento -- e uma resolucao apressada em que o metodo de alguem
 * some. Metodo que some nao da erro de compilacao quando o chamador some junto:
 * da um mob que perdeu a coordenacao de bando e continua nascendo, atacando e
 * passando em todo portao, so que como quatro bichos soltos.</p>
 *
 * <p><b>O que fica la e o que fica aqui.</b> Em {@code GreedIslandProfiles} ficam
 * a ficha publicada (atributos, spawn, card, condicao de captura), a recarga e o
 * stagger -- tudo que ja estava escrito e que outros sistemas leem. Aqui ficam os
 * numeros NOVOS, os que este comportamento inaugurou: a forma da mordida, a
 * geometria do cerco e o salto.</p>
 *
 * <p><b>O dano nao e um numero proprio.</b> Ele e lido de
 * {@code GreedIslandProfiles.wolfPackHunter().attributes().attackDamage()}, porque
 * a mordida e o unico ataque do bicho. Repetir o 6 aqui criaria duas fontes para
 * a mesma verdade, e girar o atributo numa sessao de balanceamento mudaria a barra
 * de vida do jogador sem mudar este arquivo.</p>
 *
 * <p><b>O mesmo vale para o bando.</b> Teto, espacamento, moral minima, raio de
 * reforco e orcamento de coordenacao saem inteiros de {@link SquadRules#matilha()}.
 * Este arquivo nao redeclara nenhum dos cinco: redeclarado, o numero daqui
 * venceria em metade dos caminhos e o de la na outra metade, e a sessao de
 * balanceamento giraria um botao morto.</p>
 */
public final class WolfPackHunterTuning {

    private WolfPackHunterTuning() { }

    // ------------------------------------------------------------------ bando

    /**
     * Quantos membros vivos o bando precisa ter para o lobo AVANCAR.
     *
     * <p>Dois, e este e o numero que define o bicho. Com um, ele seria um
     * perseguidor comum de HP 26 que morre para qualquer jogador com espada de
     * ferro -- e a existencia de {@code Squad} viraria decoracao. Com tres, dois
     * lobos sobreviventes recuariam de um jogador ferido, e o jogador aprenderia
     * que matar um resolve o encontro.</p>
     *
     * <p>Nao e botao de balanceamento: e a ficha. Girar isto nao ajusta a
     * dificuldade, troca o que o mob ensina.</p>
     */
    public static final int MEMBROS_PARA_AVANCAR = 2;

    /**
     * Raio do anel em que os membros que nao investem se postam, em blocos.
     *
     * <p>Tem de ser MAIOR que {@link #ALCANCE_DA_MORDIDA} -- senao "segurar o
     * anel" e "morder" acontecem no mesmo lugar -- e grande o bastante para que
     * quatro postos a 90 graus deixem {@code SquadRules.matilha().espacamento()}
     * de corda entre vizinhos. {@link RegrasDeMatilha} cobra as duas coisas no
     * construtor, com a conta feita, em vez de deixa-las num comentario.</p>
     *
     * <p>Com 3.4 a corda e 4.81 blocos contra os 2.5 exigidos. A folga e
     * deliberada: o anel e onde o jogador ainda consegue sair andando, e um anel
     * colado nao daria a ele a leitura de estar CERCADO antes de estar preso.</p>
     */
    public static final double RAIO_DO_CERCO = 3.4D;

    // ---------------------------------------------------------------- mordida
    //
    // A FORMA E O OPOSTO DA DO CICLOPE, e isso e a ficha. O gigante avisa por 30
    // ticks; este lobo avisa por 10. A ameaca deste mob nunca foi o golpe
    // individual -- sao os outros tres chegando enquanto voce olha para este. Um
    // windup longo daria ao jogador tempo de tratar cada lobo como um duelo, e o
    // encontro inteiro se apagaria sem nada acusar.

    /** Ticks de aviso: meio segundo de agachamento com o focinho aberto. */
    public static final int WINDUP_DA_MORDIDA = 10;
    /** Ticks em que a mordida existe. */
    public static final int JANELA_DA_MORDIDA = 4;
    /**
     * Ticks de recuperacao: a janela em que o jogador pune.
     *
     * <p>Quase o dobro do aviso, e num mob de bando isso faz um trabalho a mais
     * do que num mob solitario: e a recuperacao que ESPACA as mordidas no tempo.
     * Encurta-la nao deixa o lobo mais perigoso -- deixa o CERCO uma trituradora,
     * porque dois investidores passam a poder acertar em janelas sobrepostas.</p>
     */
    public static final int RECUPERACAO_DA_MORDIDA = 8;
    /** Empurrao da mordida. Pequeno: quem empurra o alvo para fora do cerco perde o cerco. */
    public static final float EMPURRAO_DA_MORDIDA = 0.35F;

    /**
     * Meia-largura do alvo tipico -- um jogador tem 0.6 de lado.
     *
     * <p>Nao e botao de balanceamento: e a diferenca entre as duas reguas que
     * este arquivo usa. A distancia de DECISAO e medida de centro a centro
     * ({@code distanceTo}); a caixa de golpe e testada contra a BORDA do alvo
     * ({@code AABB.intersects}). Sem esta conversao as duas parecem comparaveis e
     * nao sao.</p>
     */
    public static final double MEIA_LARGURA_DE_UM_ALVO = 0.3D;

    /**
     * Distancia (centro a centro) em que ele decide morder.
     *
     * <p>Tem de ser MENOR que {@code maxZ + MEIA_LARGURA_DE_UM_ALVO}, e o teste
     * cobra isso. Maior, o lobo comeca um windup contra alguem que ja esta fora
     * do alcance da boca -- e como ele trava a navegacao durante o golpe, a
     * mordida no limite da distancia NUNCA acertaria. Isso nao da erro nenhum: da
     * um bando que erra sozinho e parece quebrado.</p>
     */
    public static final double ALCANCE_DA_MORDIDA = 1.15D;

    /**
     * Quanto o corpo do lobo viaja para a frente durante a janela que machuca.
     *
     * <p><b>Este numero e a ponte entre a arte e a regra.</b> O focinho DESENHADO
     * alcanca {@link #ALCANCE_DESENHADO_DO_FOCINHO} blocos -- e nao pode alcancar
     * mais, porque o modelo inteiro tem de caber nos 0.9 bloco da hitbox. A caixa
     * de mordida reivindica 0.95. A diferenca e paga aqui, pelo salto.</p>
     *
     * <p>Tirar o salto sem encolher a caixa da um jogador que apanha de um lobo
     * que, na tela, parou antes dele -- dano certo, cooldown certo, log limpo, e
     * a unica leitura que ele tem quebrada. Por isso a soma e cobrada nos DOIS
     * lados: em {@code wolf_pack_hunter_geo.py}
     * ({@code valida_focinho_alcanca_a_mordida}) e em {@code WolfPackHunterTuningTest}.</p>
     */
    public static final double AVANCO_DA_INVESTIDA = 0.55D;

    /**
     * Alcance do focinho DESENHADO, em blocos, do centro do lobo ate a ponta.
     *
     * <p>Copiado de {@code wolf_pack_hunter_geo.py}: a caixa {@code snout} comeca
     * em z = -7 px, e 7/16 = 0.4375. Duplicacao DECLARADA -- a outra ponta e a
     * regua daquele arquivo, e as duas juntas formam um portao que morde dos dois
     * lados: quem encolher o focinho la reprova la, quem esticar a caixa aqui
     * reprova aqui.</p>
     */
    public static final double ALCANCE_DESENHADO_DO_FOCINHO = 0.4375D;

    /**
     * Impulso horizontal aplicado no primeiro tick da janela ACTIVE, por tick.
     *
     * <p>Aplicado UMA vez por instancia de ataque, e nao a cada tick da janela:
     * somado quatro vezes, o lobo atravessaria o alvo e sairia do outro lado, e o
     * proprio {@code tryHit} -- que so acerta cada alvo uma vez por instancia --
     * esconderia o defeito atras de um dano correto.</p>
     */
    public static final double IMPULSO_DA_INVESTIDA = 0.42D;

    /**
     * Recarga imposta a quem interrompeu a mordida.
     *
     * <p>Mais longa que a recarga normal (35): interromper tem de VALER. Sem
     * isso, {@code reset()} devolveria a fase para IDLE e o lobo poderia
     * recomecar no tick seguinte -- e num bando de quatro, interromper deixaria de
     * ser uma tatica e o jogador aprenderia a ignorar o cambaleio.</p>
     */
    public static final int RECARGA_APOS_INTERRUPCAO = 50;

    // ------------------------------------------------------------- montagem

    /** As regras do bicho sobre o bando, com a continencia cobrada no construtor. */
    public static RegrasDeMatilha matilha() {
        return new RegrasDeMatilha(SquadRules.matilha(), MEMBROS_PARA_AVANCAR,
                ALCANCE_DA_MORDIDA, RAIO_DO_CERCO);
    }

    /**
     * A mordida.
     *
     * <p>O windup e interrompivel e a JANELA nao: depois que a boca fecha, ela
     * fecha. Interromper durante os 4 ticks ativos faria o lobo cancelar um golpe
     * que o jogador ja viu sair -- e com um telegrafo de meio segundo, a leitura
     * e a unica defesa que existe contra ele.</p>
     *
     * <p>A recuperacao TAMBEM nao e interrompivel, e aqui a razao e de bando: se
     * o cambaleio cortasse a recuperacao, o lobo interrompido voltaria a fila de
     * ataque antes dos irmaos que nao apanharam, e punir um membro aceleraria o
     * cerco em vez de o abrir.</p>
     */
    public static AttackDefinition mordida() {
        return new AttackDefinition("bite", WINDUP_DA_MORDIDA, JANELA_DA_MORDIDA,
                RECUPERACAO_DA_MORDIDA,
                GreedIslandProfiles.wolfPackHunter().attributes().attackDamage(),
                EMPURRAO_DA_MORDIDA, true, false, false);
    }

    /**
     * Caixa da mordida, em coordenadas LOCAIS. A FRENTE E +Z.
     *
     * <p><b>+Z, e isto nao e distracao.</b> Na GEOMETRIA do modelo (formato
     * Bedrock) a frente e -Z; na transformacao de {@link AttackHitbox}, que e
     * matematica de mundo, com yaw 0 o olhar vanilla aponta para +Z. Misturar as
     * duas ja custou um bug neste repositorio: a caixa ficou ATRAS do mob, que
     * atacava, animava e nao encostava em quem estava na frente -- quem estivesse
     * pelas costas e que apanhava. Fase certa, cooldown certo, log limpo. Ver
     * {@code DummyEnemyEntity.CAIXA_DO_GOLPE}.</p>
     *
     * <p>A caixa e ESTREITA (0.9 bloco de largura) de proposito. Uma tora varre;
     * uma boca nao. Caixa larga num bando de quatro significaria quatro varreduras
     * cobrindo o circulo inteiro, e sair de perto deixaria de ser resposta.</p>
     *
     * <p>O {@code minZ} de 0.2 exclui o proprio corpo do lobo; o {@code maxZ} de
     * 0.95 e coberto pelo focinho desenhado (0.44) mais o salto (0.55), e as duas
     * pontas dessa conta sao cobradas -- aqui, por
     * {@code WolfPackHunterTuningTest}, e la, por {@code wolf_pack_hunter_geo.py}.</p>
     */
    public static AttackHitbox caixaDaMordida() {
        return new AttackHitbox(-0.45D, 0.0D, 0.2D, 0.45D, 0.9D, 0.95D);
    }
}
