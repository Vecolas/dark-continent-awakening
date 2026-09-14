package com.darkcontinent.nenfoundation.enemy.content;

import com.darkcontinent.nenfoundation.enemy.ai.RegrasDeArranque;
import com.darkcontinent.nenfoundation.enemy.ai.squad.SquadRules;
import com.darkcontinent.nenfoundation.enemy.combat.AttackDefinition;
import com.darkcontinent.nenfoundation.enemy.combat.AttackHitbox;

/**
 * Os numeros do Cheetah Leader que nascem com o COMPORTAMENTO dele.
 *
 * <p><b>Por que este arquivo existe em vez de mais metodos em
 * {@link ChimeraProfiles}.</b> Aquele arquivo guarda os perfis das formigas
 * todas, e neste momento varias frentes escrevem o comportamento de formigas
 * diferentes ao mesmo tempo. Todas precisariam acrescentar metodos no MESMO
 * arquivo, e o resultado de um merge assim nao e um conflito barulhento -- e uma
 * resolucao apressada em que o metodo de alguem some. Metodo que some nao da
 * erro de compilacao quando o chamador some junto: da um mob que perdeu o
 * arranque e continua nascendo, perseguindo e passando em todo portao, so que
 * como um felino que anda sempre na mesma velocidade.</p>
 *
 * <p><b>O que fica la e o que fica aqui.</b> Em {@code ChimeraProfiles} ficam a
 * ficha publicada (atributos, spawn, metadata), o molde genetico, a recarga e o
 * stagger -- tudo que ja estava escrito e que outros sistemas leem. Aqui ficam os
 * numeros NOVOS, os que este comportamento inaugurou: a forma do arranque e a
 * forma do bote.</p>
 *
 * <p><b>O dano nao e um numero proprio.</b> Ele e lido de
 * {@code ChimeraProfiles.cheetahLeader().attributes().attackDamage()}, porque o
 * bote e o unico ataque do bicho. Repetir o 14 aqui criaria duas fontes para a
 * mesma verdade, e girar o atributo numa sessao de balanceamento mudaria a barra
 * de vida do jogador sem mudar este arquivo.</p>
 *
 * <p><b>O mesmo vale para o esquadrao.</b> Teto, espacamento, moral minima, raio
 * de reforco e orcamento de coordenacao saem inteiros de
 * {@link SquadRules#esquadrao()}. Este arquivo nao redeclara nenhum dos cinco:
 * redeclarado, o numero daqui venceria em metade dos caminhos e o de la na outra
 * metade, e a sessao de balanceamento giraria um botao morto.</p>
 *
 * <p><b>E a velocidade base tambem nao mora aqui.</b> Os 0.46 sao atributo, e o
 * arranque e um MULTIPLICADOR sobre o que a navegacao ja usa. Copiar o 0.46 para
 * este arquivo transformaria a velocidade do bicho em duas verdades, e a divisao
 * de balanceamento -- girar o atributo -- passaria a mudar so metade do
 * comportamento.</p>
 */
public final class CheetahLeaderTuning {

    private CheetahLeaderTuning() { }

    // --------------------------------------------------------------- arranque
    //
    // A FICHA INTEIRA DO BICHO ESTA NESTES SEIS NUMEROS. Ele e o mais rapido da
    // colonia (0.46) e um dos mais faceis de ferir (armadura 3). Somar couro a
    // velocidade daria um lider que ninguem alcanca E ninguem fere -- e um bicho
    // assim nao e dificil, e so interminavel.
    //
    // O arranque nao CRIA velocidade: ele a redistribui. Trinta ticks acima da
    // media sao pagos com quarenta e cinco abaixo dela, e `RegrasDeArranque`
    // cobra essa conta no construtor em vez de deixa-la num comentario.

    /**
     * Ticks de corrida acelerada.
     *
     * <p>Segundo e meio -- o suficiente para cruzar a dezena de blocos que separa
     * "ele me viu" de "ele esta em cima de mim", e curto o bastante para o
     * jogador ver a corrida COMECAR e TERMINAR na mesma tela. Um arranque longo
     * viraria velocidade de cruzeiro e o momento de virada se perderia.</p>
     */
    public static final int TICKS_DE_ARRANQUE = 30;

    /**
     * Ticks de lentidao que pagam o arranque -- A JANELA DO JOGADOR.
     *
     * <p>Dois segundos e um quarto: tempo de fechar distancia e encaixar dois
     * golpes num bicho de armadura 3. Esta e a resposta do encontro, e nao um
     * botao: encurta-la nao deixa o guepardo mais perigoso, deixa a perseguicao
     * sem fim.</p>
     */
    public static final int TICKS_DE_FADIGA = 45;

    /**
     * Ticks, depois da fadiga, em que o arranque segue negado.
     *
     * <p>Tres segundos. Sem eles, o jogador que gastou a janela de fadiga e
     * recuou levaria um novo arranque de imediato, e recuar deixaria de ser uma
     * escolha. {@code RegrasDeArranque} exige que a recarga seja pelo menos tao
     * longa quanto o arranque -- aqui ela e o dobro, de proposito.</p>
     */
    public static final int TICKS_DE_RECARGA = 60;

    /** Fator sobre a velocidade de navegacao durante o arranque. */
    public static final double MULTIPLICADOR_DE_ARRANQUE = 1.55D;

    /** Fator durante a fadiga. Abaixo de 1: e ele que faz a fadiga custar. */
    public static final double MULTIPLICADOR_DE_FADIGA = 0.55D;

    /**
     * Distancia minima, centro a centro, para o arranque valer.
     *
     * <p>Seis blocos. Abaixo disso o arranque viajaria quase nada e entregaria a
     * FADIGA colada no jogador -- a vantagem do bicho viraria uma desvantagem, e
     * nada acusaria. O bote ja cobre a distancia curta.</p>
     */
    public static final double DISTANCIA_MINIMA_DO_ARRANQUE = 6.0D;

    /**
     * Distancia maxima para o arranque valer.
     *
     * <p>Vinte e seis blocos, contra os 34 de alcance de percepcao da ficha. A
     * folga e deliberada: alem disso ele chegaria FATIGADO, e o jogador
     * aprenderia que o arranque e inofensivo. O teste cobra que este numero fique
     * abaixo do alcance de percepcao -- acima dele, a regra autorizaria um
     * arranque contra alguem que o bicho nem sabe que existe, e o numero seria
     * letra morta.</p>
     */
    public static final double DISTANCIA_MAXIMA_DO_ARRANQUE = 26.0D;

    /**
     * A que distancia do alvo um aliado ja conta como "o combate esta aberto".
     *
     * <p>Quatro blocos. O arranque deste bicho existe para ele ABRIR o combate;
     * gasto para entrar num combate que um companheiro ja abriu, ele nao abre
     * nada e ainda chega fatigado no meio da briga. Tem de ser menor que
     * {@link #DISTANCIA_MINIMA_DO_ARRANQUE}, e {@code RegrasDeArranque} cobra
     * isso: mais largo, qualquer companheiro parado ao lado dele cancelaria todo
     * arranque e a ficha desligaria em silencio.</p>
     */
    public static final double DISTANCIA_DE_ABERTURA = 4.0D;

    // ------------------------------------------------------------------- bote
    //
    // A FORMA E O OPOSTO DA DO CICLOPE, e isso e a ficha. O gigante avisa por 30
    // ticks; este guepardo avisa por 12. O que machuca nele e ter CHEGADO -- o
    // bote e so o que acontece depois. Um windup longo daria ao jogador tempo de
    // tratar a chegada como um duelo comum, e o arranque inteiro se apagaria sem
    // nada acusar.

    /** Ticks de aviso: 0.6 s de agachamento, boca aberta e placa erguida. */
    public static final int WINDUP_DO_BOTE = 12;

    /** Ticks em que o bote existe. */
    public static final int JANELA_DO_BOTE = 5;

    /**
     * Ticks de recuperacao: a janela em que o jogador pune.
     *
     * <p>Mais longa que o aviso. Num bicho que ja chegou primeiro, essa janela e
     * o pagamento do bote: sem ela, chegar primeiro seria vantagem sem preco.</p>
     */
    public static final int RECUPERACAO_DO_BOTE = 14;

    /** Empurrao do bote. Pequeno: quem joga o alvo para longe perde a distancia que gastou o arranque para fechar. */
    public static final float EMPURRAO_DO_BOTE = 0.4F;

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
     * Distancia (centro a centro) em que ele decide dar o bote.
     *
     * <p>Tem de ser MENOR que {@code maxZ + MEIA_LARGURA_DE_UM_ALVO}, e o teste
     * cobra isso. Maior, ele comeca um windup contra alguem que ja esta fora do
     * alcance -- e como ele trava a navegacao durante o golpe, o bote no limite
     * da distancia NUNCA acertaria. Isso nao da erro nenhum: da um lider que erra
     * sozinho e parece quebrado.</p>
     */
    public static final double ALCANCE_DO_BOTE = 1.3D;

    /**
     * Quanto o corpo viaja para a frente durante a janela que machuca.
     *
     * <p><b>Este numero e a ponte entre a arte e a regra.</b> O focinho DESENHADO
     * alcanca {@link #ALCANCE_DESENHADO_DA_FRENTE} blocos -- e nao pode alcancar
     * mais, porque o modelo inteiro tem de caber nos 1.2 bloco de comprimento da
     * hitbox. A caixa do bote reivindica 1.05. A diferenca e paga aqui.</p>
     *
     * <p>Tirar o avanco sem encolher a caixa da um jogador que apanha de um
     * guepardo que, na tela, parou antes dele -- dano certo, cooldown certo, log
     * limpo, e a unica leitura que ele tem quebrada. Por isso a soma e cobrada
     * nos DOIS lados: em {@code cheetah_leader_geo.py}
     * ({@code valida_bote_alcanca_a_caixa}) e em {@code CheetahLeaderTuningTest}.</p>
     */
    public static final double AVANCO_DO_BOTE = 0.55D;

    /**
     * Alcance do focinho DESENHADO, em blocos, do centro do bicho ate a ponta.
     *
     * <p>Copiado de {@code cheetah_leader_geo.py}: a caixa {@code snout} comeca em
     * z = -9 px, e 9/16 = 0.5625. Duplicacao DECLARADA -- a outra ponta e a regua
     * daquele arquivo, e as duas juntas formam um portao que morde dos dois
     * lados: quem encurtar o focinho la reprova la, quem esticar a caixa aqui
     * reprova aqui.</p>
     */
    public static final double ALCANCE_DESENHADO_DA_FRENTE = 0.5625D;

    /**
     * Impulso horizontal aplicado no primeiro tick da janela ACTIVE.
     *
     * <p>Aplicado UMA vez por instancia de ataque, e nao a cada tick da janela:
     * somado cinco vezes, o guepardo atravessaria o alvo e sairia do outro lado,
     * e o proprio {@code tryHit} -- que so acerta cada alvo uma vez por
     * instancia -- esconderia o defeito atras de um dano correto.</p>
     */
    public static final double IMPULSO_DO_BOTE = 0.48D;

    /**
     * Recarga imposta a quem interrompeu o bote.
     *
     * <p>Mais longa que a recarga normal (35): interromper tem de VALER.
     * Interromper o lider e a unica coisa que atrasa a chegada dele, e sem esta
     * penalidade {@code reset()} devolveria a fase para IDLE e ele poderia
     * recomecar no tick seguinte.</p>
     */
    public static final int RECARGA_APOS_INTERRUPCAO = 55;

    // ---------------------------------------------------------- percepcao
    //
    // Estes tres moram aqui e nao em ChimeraProfiles pela mesma razao dos
    // demais: eles nasceram com ESTE comportamento. O alcance de VISAO nao e
    // redeclarado -- ele sai de attributes().followRange(), que ja e a ficha.

    /** Ticks entre notar e agir. Curto: um lider que hesita nao chega primeiro. */
    public static final int TICKS_DE_AVISO = 20;

    /** Ticks de memoria de ameaca. Longo: quem corre atras nao esquece no primeiro canto. */
    public static final int MEMORIA_DE_ALVO_TICKS = 140;

    /** Meia-abertura do cone de visao, em graus. */
    public static final double MEIA_ABERTURA_DA_VISAO_EM_GRAUS = 80.0D;

    /** Alcance de audicao, em blocos. */
    public static final double ALCANCE_DE_AUDICAO = 16.0D;

    /** Fracao de vida abaixo da qual a percepcao o trata como ferido. */
    public static final float FRACAO_DE_VIDA_CRITICA = 0.25F;

    // ------------------------------------------------------------- montagem

    /** As regras do arranque, com a conta do ciclo cobrada no construtor. */
    public static RegrasDeArranque arranque() {
        return new RegrasDeArranque(TICKS_DE_ARRANQUE, TICKS_DE_FADIGA, TICKS_DE_RECARGA,
                MULTIPLICADOR_DE_ARRANQUE, MULTIPLICADOR_DE_FADIGA,
                DISTANCIA_MINIMA_DO_ARRANQUE, DISTANCIA_MAXIMA_DO_ARRANQUE,
                DISTANCIA_DE_ABERTURA);
    }

    /**
     * O bote.
     *
     * <p>O windup e interrompivel e a JANELA nao: depois que o corpo sai do chao,
     * ele sai. Interromper durante os 5 ticks ativos faria o bicho cancelar um
     * golpe que o jogador ja viu sair -- e com um telegrafo de 0.6 s, a leitura e
     * a unica defesa que existe contra ele.</p>
     *
     * <p>A recuperacao TAMBEM nao e interrompivel, e aqui a razao e de ficha: a
     * recuperacao e o preco de ter chegado primeiro. Se o cambaleio a cortasse,
     * punir o lider o devolveria a fila de ataque mais rapido do que se ninguem
     * tivesse encostado nele, e interromper passaria a ACELERAR o encontro.</p>
     */
    public static AttackDefinition bote() {
        return new AttackDefinition("pounce", WINDUP_DO_BOTE, JANELA_DO_BOTE,
                RECUPERACAO_DO_BOTE,
                ChimeraProfiles.cheetahLeader().attributes().attackDamage(),
                EMPURRAO_DO_BOTE, true, false, false);
    }

    /**
     * Caixa do bote, em coordenadas LOCAIS. A FRENTE E +Z.
     *
     * <p><b>+Z, e isto nao e distracao.</b> Na GEOMETRIA do modelo (formato
     * Bedrock) a frente e -Z; na transformacao de {@link AttackHitbox}, que e
     * matematica de mundo, com yaw 0 o olhar vanilla aponta para +Z. Misturar as
     * duas ja custou um bug neste repositorio: a caixa ficou ATRAS do mob, que
     * atacava, animava e nao encostava em quem estava na frente -- quem estivesse
     * pelas costas e que apanhava. Fase certa, cooldown certo, log limpo.</p>
     *
     * <p>A caixa e ESTREITA (1.1 bloco) contra os 1.2 da hitbox do bicho. Um bote
     * de felino chega pela frente, e nao varre o circulo: caixa larga daria ao
     * jogador que ja saiu do caminho o mesmo dano de quem ficou parado, e sair do
     * caminho deixaria de ser resposta.</p>
     *
     * <p>O {@code minZ} de 0.25 exclui o proprio corpo; o {@code maxZ} de 1.05 e
     * coberto pelo focinho desenhado (0.5625) mais o avanco (0.55), e as duas
     * pontas dessa conta sao cobradas -- aqui, por {@code CheetahLeaderTuningTest},
     * e la, por {@code cheetah_leader_geo.py}.</p>
     */
    public static AttackHitbox caixaDoBote() {
        return new AttackHitbox(-0.55D, 0.0D, 0.25D, 0.55D, 1.2D, 1.05D);
    }
}
