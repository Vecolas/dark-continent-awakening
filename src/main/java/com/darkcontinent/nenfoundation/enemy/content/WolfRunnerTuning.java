package com.darkcontinent.nenfoundation.enemy.content;

import com.darkcontinent.nenfoundation.enemy.ai.RegrasDeFlanco;
import com.darkcontinent.nenfoundation.enemy.ai.RegrasDeRastro;
import com.darkcontinent.nenfoundation.enemy.ai.squad.SquadRules;
import com.darkcontinent.nenfoundation.enemy.combat.AttackDefinition;
import com.darkcontinent.nenfoundation.enemy.combat.AttackHitbox;
import com.darkcontinent.nenfoundation.enemy.combat.RegrasDeSalto;

/**
 * Os numeros do Wolf Runner que nascem com o COMPORTAMENTO dele.
 *
 * <p><b>Por que este arquivo existe em vez de mais metodos em
 * {@link ChimeraProfiles}.</b> Aquele arquivo e hostil a merge: ele guarda os
 * perfis de toda a colonia quimera, e neste momento varias frentes escrevem o
 * comportamento de formigas diferentes ao mesmo tempo. Todas precisariam
 * acrescentar metodos no MESMO arquivo, e o resultado de um merge assim nao e um
 * conflito barulhento -- e uma resolucao apressada em que o metodo de alguem
 * some. Metodo que some nao da erro de compilacao quando o chamador some junto:
 * da um mob que perdeu o flanco e continua nascendo, correndo e mordendo, e
 * passando em todo portao como um perseguidor comum.</p>
 *
 * <p><b>O que fica la e o que fica aqui.</b> Em {@code ChimeraProfiles} ficam a
 * ficha publicada (atributos, spawn, molde genetico), a recarga e o stagger --
 * tudo que ja estava escrito e que outros sistemas leem. Aqui ficam os numeros
 * NOVOS, os que este comportamento inaugurou: o prazo do rastro, a geometria do
 * flanco, a faixa do bote e a forma dos dois golpes.</p>
 *
 * <p><b>O dano nao e um numero proprio.</b> Ele e lido de
 * {@code ChimeraProfiles.wolfRunner().attributes().attackDamage()} nos DOIS
 * golpes, porque os dois sao a mesma boca. Repetir o 7 aqui criaria duas fontes
 * para a mesma verdade, e girar o atributo numa sessao de balanceamento mudaria
 * a barra de vida do jogador sem mudar este arquivo.</p>
 *
 * <p><b>O mesmo vale para o esquadrao.</b> Teto, espacamento, moral minima, raio
 * de reforco e orcamento de coordenacao saem inteiros de
 * {@link SquadRules#esquadrao()}. Este arquivo nao redeclara nenhum dos cinco:
 * redeclarado, o numero daqui venceria em metade dos caminhos e o de la na
 * outra metade, e a sessao de balanceamento giraria um botao morto.</p>
 */
public final class WolfRunnerTuning {

    private WolfRunnerTuning() { }

    // ------------------------------------------------------------- percepcao

    /** Ticks entre suspeitar e engajar. */
    public static final int TICKS_DE_AVISO = 20;

    /**
     * Ticks que a memoria da AMEACA dura. Ver {@code ThreatMemory}.
     *
     * <p>Ela e o teto do rastro, e nao a mesma coisa que ele: a memoria diz de
     * QUEM o bicho se lembra; o rastro diz ate quando vale ir atras. Por isso
     * {@link #rastro()} recebe este numero em vez de supor um -- a cobranca esta
     * no construtor de {@link RegrasDeRastro}.</p>
     */
    public static final int MEMORIA_DE_ALVO_TICKS = 140;

    /**
     * Meia-abertura do campo de visao, em graus.
     *
     * <p>Larga -- o boneco de treino usa 75, e este usa 80. Um flanqueador
     * precisa VER quem esta contornando junto com ele para nao investir sozinho;
     * cegueira lateral aqui transformaria o esquadrao em quatro bichos soltos
     * fazendo a mesma coisa em momentos diferentes.</p>
     */
    public static final double MEIA_ABERTURA_DA_VISAO_EM_GRAUS = 80.0D;

    /** Alcance da audicao, em blocos. */
    public static final double ALCANCE_DE_AUDICAO = 16.0D;

    /** Fracao de vida abaixo da qual o bicho entra como FERIDO na leitura do esquadrao. */
    public static final float FRACAO_DE_VIDA_CRITICA = 0.25F;

    // ---------------------------------------------------------------- rastro
    //
    // O PRAZO E O QUE SEPARA "TENSO" DE "INJUSTO", e e o unico numero deste
    // arquivo que muda o carater do bicho sem mudar um unico ponto de dano.

    /**
     * Ticks que o rastro de quem fugiu continua valendo.
     *
     * <p>Cinco segundos. Com a velocidade 0.38 deste bicho contra os 0.1 de um
     * jogador andando, cinco segundos sao tempo de sobra para alcancar quem
     * correu em linha reta -- e nao sao suficientes para quem quebrou a linha de
     * visao e trocou de direcao. E essa a diferenca que o numero compra: fugir
     * nao e correr mais rapido, e sair do rastro.</p>
     *
     * <p>Ele e MENOR que {@link #MEMORIA_DE_ALVO_TICKS} de proposito, e a
     * relacao entre os dois e cobrada. Maior, o bicho seguiria o rastro de
     * alguem de quem ja esqueceu: correria ate um ponto vazio, sem alvo, e
     * pararia ali sem motivo visivel.</p>
     */
    public static final int TICKS_DE_RASTRO = 100;

    /**
     * A que distancia do ponto do rastro ele considera que chegou, em blocos.
     *
     * <p>1.5 e pouco mais que a propria hitbox (1.0). Menor, a navegacao para
     * antes de "chegar" e o bicho fica parado sobre o ponto ate o prazo vencer,
     * o que o jogador le como um mob travado.</p>
     */
    public static final double RAIO_DE_CHEGADA_DO_RASTRO = 1.5D;

    /** As regras do rastro, com a continencia contra a memoria cobrada no construtor. */
    public static RegrasDeRastro rastro() {
        return new RegrasDeRastro(TICKS_DE_RASTRO, RAIO_DE_CHEGADA_DO_RASTRO,
                MEMORIA_DE_ALVO_TICKS);
    }

    // ---------------------------------------------------------------- flanco

    /**
     * Quantos membros vivos o esquadrao precisa ter para ele AVANCAR.
     *
     * <p>Dois, e este e o numero que define o bicho. Com um, ele seria um
     * perseguidor comum de HP 28 que morre para qualquer jogador com espada de
     * ferro -- e o flanco viraria enfeite, porque nao ha flanco quando nao ha
     * quem segure a frente. Nao e botao de balanceamento: girar isto nao ajusta
     * dificuldade, troca o que o mob ensina.</p>
     */
    public static final int MEMBROS_PARA_FLANQUEAR = 2;

    /**
     * Posto do flanco, em graus a partir do OLHAR do alvo.
     *
     * <p>Oitenta graus e quase o lado exato. Menos que isso e uma diagonal, e
     * diagonal ainda e visivel; mais que 120 poria o bicho nas costas do alvo,
     * onde ele ja nao aparece em tela nenhuma e o jogador apanha sem nunca ter
     * tido a chance de ler o aviso. Oitenta e o limite do campo de visao humano
     * em tela: da para VER o bicho e mesmo assim ter de escolher entre ele e
     * quem esta na frente.</p>
     */
    public static final double ANGULO_DE_FLANCO_EM_GRAUS = 80.0D;

    /**
     * Meia-abertura em que o ALVO enxerga o flanqueador, em graus.
     *
     * <p>Dentro dela o bote e recusado, porque investir de frente e a mesma
     * coisa que nao flanquear. Ela e mais estreita que
     * {@link #ANGULO_DE_FLANCO_EM_GRAUS}, e essa relacao e cobrada em
     * {@link RegrasDeFlanco}: com o posto dentro do arco, o bicho andaria ate um
     * lugar de onde nunca receberia permissao para fechar, e giraria em volta do
     * jogador para sempre sem atacar.</p>
     */
    public static final double ARCO_FRONTAL_DO_ALVO_EM_GRAUS = 55.0D;

    /**
     * Raio do arco em que ele circula antes de fechar, em blocos.
     *
     * <p>Tem de ser MAIOR que {@link #ALCANCE_DA_MORDIDA} -- senao "circular" e
     * "morder" acontecem no mesmo lugar -- e grande o bastante para que os dois
     * flancos deixem o espacamento do esquadrao entre si.
     * {@link RegrasDeFlanco} cobra as duas coisas no construtor, com a conta
     * feita, em vez de deixa-las num comentario.</p>
     */
    public static final double RAIO_DO_CONTORNO = 3.2D;

    /** As regras do flanco, com a geometria cobrada no construtor. */
    public static RegrasDeFlanco flanco() {
        return new RegrasDeFlanco(SquadRules.esquadrao(), MEMBROS_PARA_FLANQUEAR,
                ANGULO_DE_FLANCO_EM_GRAUS, ARCO_FRONTAL_DO_ALVO_EM_GRAUS,
                ALCANCE_DA_MORDIDA, RAIO_DO_CONTORNO);
    }

    // --------------------------------------------------------------- mordida
    //
    // O GOLPE CURTO, para quando o alvo ja esta colado. A forma e a de um bicho
    // rapido: aviso curto, janela curta, recuperacao que e a punicao. A ameaca
    // deste mob nunca foi o golpe individual -- e o outro chegando pelo lado
    // enquanto voce olha para este.

    /** Ticks de aviso da mordida: meio segundo de focinho aberto. */
    public static final int WINDUP_DA_MORDIDA = 10;
    /** Ticks em que a mordida existe. */
    public static final int JANELA_DA_MORDIDA = 4;
    /** Ticks de recuperacao: a janela em que o jogador pune. */
    public static final int RECUPERACAO_DA_MORDIDA = 10;
    /** Empurrao da mordida. Pequeno: quem empurra o alvo para fora do cerco perde o cerco. */
    public static final float EMPURRAO_DA_MORDIDA = 0.25F;

    /**
     * Meia-largura do alvo tipico -- um jogador tem 0.6 de lado.
     *
     * <p>Nao e botao de balanceamento: e a conversao entre as duas reguas que
     * este arquivo usa. A distancia de DECISAO e medida de centro a centro
     * ({@code distanceTo}); a caixa de golpe e testada contra a BORDA do alvo
     * ({@code AABB.intersects}). Sem esta conversao as duas parecem comparaveis
     * e nao sao.</p>
     */
    public static final double MEIA_LARGURA_DE_UM_ALVO = 0.3D;

    /**
     * Distancia (centro a centro) em que ele fecha a boca.
     *
     * <p>Tem de ser MENOR que {@code maxZ + MEIA_LARGURA_DE_UM_ALVO}, e o teste
     * cobra isso. Maior, o bicho comeca um aviso contra alguem que ja esta fora
     * do alcance da boca -- e como ele trava a navegacao durante o golpe, a
     * mordida no limite da distancia NUNCA acertaria. Isso nao da erro nenhum:
     * da um esquadrao que erra sozinho e parece quebrado.</p>
     */
    public static final double ALCANCE_DA_MORDIDA = 1.2D;

    /**
     * Alcance do focinho DESENHADO, em blocos, do centro do bicho ate a ponta.
     *
     * <p>Copiado de {@code wolf_runner_geo.py}: a caixa {@code jaw} comeca em
     * z = -8 px, e 8/16 = 0.5. Duplicacao DECLARADA -- a outra ponta e a regua
     * {@code valida_focinho_alcanca_a_mordida} daquele arquivo, e as duas juntas
     * formam um portao que morde dos dois lados: quem encolher o focinho la
     * reprova la, quem esticar a caixa aqui reprova aqui.</p>
     */
    public static final double ALCANCE_DESENHADO_DO_FOCINHO = 0.5D;

    /**
     * Quanto o corpo viaja para a frente durante a janela que machuca, em blocos.
     *
     * <p><b>Este numero e a ponte entre a arte e a regra.</b> O focinho desenhado
     * alcanca 0.5 -- e nao pode alcancar mais, porque o modelo inteiro tem de
     * caber nos 16 px de comprimento da hitbox. A caixa de mordida reivindica
     * 1.0. A diferenca e paga aqui.</p>
     *
     * <p>Tirar o avanco sem encolher a caixa da um jogador que apanha de uma boca
     * que, na tela, parou antes dele -- dano certo, cooldown certo, log limpo, e
     * a unica leitura que ele tem quebrada.</p>
     */
    public static final double AVANCO_DA_INVESTIDA = 0.5D;

    /**
     * A mordida curta.
     *
     * <p>O aviso e interrompivel e a JANELA nao: depois que a boca fecha, ela
     * fecha. Interromper durante os 4 ticks ativos faria o bicho cancelar um
     * golpe que o jogador ja viu sair -- e com meio segundo de telegrafo, a
     * leitura e a unica defesa que existe contra ele.</p>
     *
     * <p>A recuperacao TAMBEM nao e interrompivel, e aqui a razao e de grupo: se
     * o cambaleio cortasse a recuperacao, o membro interrompido voltaria a fila
     * de ataque antes dos irmaos que nao apanharam, e punir um deles aceleraria
     * o cerco em vez de o abrir.</p>
     */
    public static AttackDefinition mordida() {
        return new AttackDefinition("bite", WINDUP_DA_MORDIDA, JANELA_DA_MORDIDA,
                RECUPERACAO_DA_MORDIDA, dano(), EMPURRAO_DA_MORDIDA, true, false, false);
    }

    /**
     * Caixa da mordida, em coordenadas LOCAIS. A FRENTE E +Z.
     *
     * <p><b>+Z, e isto nao e distracao.</b> Na GEOMETRIA do modelo (formato
     * Bedrock) a frente e -Z; na transformacao de {@link AttackHitbox}, que e
     * matematica de mundo, com yaw 0 o olhar vanilla aponta para +Z. Misturar as
     * duas ja custou um bug neste repositorio: a caixa ficou ATRAS do mob, que
     * atacava, animava e nao encostava em quem estava na frente. Ver
     * {@code DummyEnemyEntity.CAIXA_DO_GOLPE}.</p>
     *
     * <p>A caixa e ESTREITA (0.8 bloco) de proposito. Uma tora varre; uma boca
     * nao. Caixa larga num esquadrao de quatro significaria quatro varreduras
     * cobrindo o circulo inteiro, e sair de perto deixaria de ser resposta.</p>
     *
     * <p>O {@code minZ} de 0.25 exclui o proprio corpo; o {@code maxZ} de 1.0 e
     * coberto pelo focinho desenhado (0.5) mais o avanco (0.5), e as duas pontas
     * dessa conta sao cobradas -- aqui, por {@code WolfRunnerTuningTest}, e la,
     * por {@code wolf_runner_geo.py}.</p>
     */
    public static AttackHitbox caixaDaMordida() {
        return new AttackHitbox(-0.4D, 0.0D, 0.25D, 0.4D, 0.9D, 1.0D);
    }

    // ------------------------------------------------------------------ bote
    //
    // O SALTO DE APROXIMACAO. Ele nao e um segundo ataque com numeros maiores: e
    // a FORMA de chegar. O aviso e longo justamente porque o deslocamento e
    // grande -- telegrafo e deslocamento andam juntos, e e essa proporcao que
    // impede o salto de virar teleporte.

    /** Ticks de aviso do bote: 0.8 s de agachamento visivel. */
    public static final int WINDUP_DO_BOTE = 16;
    /** Ticks em que o bote machuca -- e tambem os ticks em que o corpo viaja. */
    public static final int JANELA_DO_BOTE = 5;
    /** Ticks de recuperacao: a aterrissagem, e a maior janela de punicao do mob. */
    public static final int RECUPERACAO_DO_BOTE = 14;
    /** Empurrao do bote. Maior que o da mordida: um corpo em voo carrega inercia. */
    public static final float EMPURRAO_DO_BOTE = 0.45F;

    /**
     * Distancia minima que autoriza o bote, em blocos.
     *
     * <p>Maior que {@link #ALCANCE_DA_MORDIDA}, e o teste cobra a relacao.
     * Menor, o bicho escolheria o golpe caro quando o barato ja alcanca: gastaria
     * 16 ticks de agachamento para viajar zero blocos, e na tela seria um mob que
     * se prepara por um segundo e morde exatamente onde ja estava.</p>
     */
    public static final double ALCANCE_MINIMO_DO_BOTE = 2.4D;

    /**
     * Distancia maxima que autoriza o bote, em blocos.
     *
     * <p>Ela e cobrada contra a BALISTICA em {@link RegrasDeSalto}: com o
     * impulso abaixo, o alcance estimado fica em torno de 4.3 blocos, e aceitar
     * alvos muito alem disso faria o bicho pousar no vazio e recomecar. O jogador
     * aprenderia que o salto e inofensivo, sem nada no log.</p>
     */
    public static final double ALCANCE_MAXIMO_DO_BOTE = 4.5D;

    /**
     * Desnivel maximo do alvo, em blocos.
     *
     * <p>Um bloco: o degrau. Mais que isso e mais alto do que o impulso vertical
     * sobe, e o construtor de {@link RegrasDeSalto} reprova -- ele bateria na
     * parede e cairia de volta com a recarga gasta.</p>
     */
    public static final double DESNIVEL_MAXIMO_DO_BOTE = 1.0D;

    /** Velocidade inicial horizontal do bote, em blocos por tick. */
    public static final double IMPULSO_HORIZONTAL_DO_BOTE = 0.62D;
    /** Velocidade inicial vertical do bote, em blocos por tick. */
    public static final double IMPULSO_VERTICAL_DO_BOTE = 0.42D;

    /** As regras do salto, com a balistica cobrada no construtor. */
    public static RegrasDeSalto salto() {
        return new RegrasDeSalto(WINDUP_DO_BOTE, ALCANCE_MINIMO_DO_BOTE, ALCANCE_MAXIMO_DO_BOTE,
                DESNIVEL_MAXIMO_DO_BOTE, IMPULSO_HORIZONTAL_DO_BOTE, IMPULSO_VERTICAL_DO_BOTE);
    }

    /**
     * O bote: mesma boca, mesmo dano, forma diferente.
     *
     * <p>O dano NAO e maior que o da mordida, e isso e decisao de ficha. O que o
     * bote compra nao e dano, e DISTANCIA -- e um salto que tambem machucasse
     * mais tornaria a mordida curta irrelevante, deixando o mob com um golpe so
     * e um telegrafo so.</p>
     */
    public static AttackDefinition bote() {
        return new AttackDefinition("pounce", WINDUP_DO_BOTE, JANELA_DO_BOTE,
                RECUPERACAO_DO_BOTE, dano(), EMPURRAO_DO_BOTE, true, false, false);
    }

    // ------------------------------------------------------------ locomocao

    /**
     * Multiplicador de velocidade ao contornar.
     *
     * <p>Um: contornar e um trajeto mais LONGO, e nao mais rapido. Acelerar o
     * contorno para compensar a volta apagaria o custo do flanco -- o bicho
     * chegaria pelo lado no mesmo tempo em que chegaria pela frente, e o jogador
     * perderia a janela em que ele decide para quem virar.</p>
     */
    public static final double MULTIPLICADOR_DE_CONTORNO = 1.0D;

    /**
     * Multiplicador de velocidade ao recuar.
     *
     * <p>Maior que um: quem recua tem de conseguir sair. Igual ao de avanco, o
     * jogador alcanca o bicho em fuga e o recuo vira uma morte mais lenta -- que
     * e o mesmo que nao ter recuo, com mais passos.</p>
     */
    public static final double MULTIPLICADOR_DE_RECUO = 1.2D;

    /** Multiplicador de velocidade ao fechar para a mordida. */
    public static final double MULTIPLICADOR_DE_INVESTIDA = 1.1D;

    /**
     * Quanto um golpe recebido abala a moral do esquadrao inteiro.
     *
     * <p>Pouco por golpe, de proposito: a moral existe para o grupo quebrar
     * JUNTO depois de apanhar bastante, e nao para ele desistir no primeiro
     * arranhao. Alto demais, o esquadrao recua antes de o jogador entender que
     * havia um esquadrao.</p>
     */
    public static final int ABALO_POR_GOLPE_NO_MEMBRO = 10;

    /**
     * Id da regiao de impacto comum -- este mob nao tem ponto fraco declarado.
     *
     * <p>Ela existe porque {@code AttackController.tryHit} exige um nome de
     * regiao, e passar o nome de uma regiao vulneravel aqui aplicaria o
     * multiplicador no lado ERRADO do combate: o bicho bateria mais forte por
     * causa de um ponto fraco que e dele. O dano final ficaria plausivel demais
     * para alguem notar sem medir.</p>
     */
    public static final String REGIAO_COMUM = "body";

    /**
     * Recarga imposta a quem interrompeu um golpe.
     *
     * <p>Mais longa que a recarga normal ({@code ChimeraProfiles.wolfRunnerRecarga()}
     * = 35): interromper tem de VALER. Sem isso, {@code reset()} devolveria a
     * fase para IDLE e o bicho poderia recomecar no tick seguinte -- e num
     * esquadrao, interromper deixaria de ser tatica e o jogador aprenderia a
     * ignorar o cambaleio.</p>
     */
    public static final int RECARGA_APOS_INTERRUPCAO = 55;

    /**
     * O dano dos dois golpes, lido do atributo publicado.
     *
     * <p>Uma fonte so. Repetido aqui, o numero deste arquivo venceria no golpe e
     * o do atributo venceria em tudo o mais, e girar o balanceamento mudaria
     * metade do jogo.</p>
     */
    private static float dano() {
        return ChimeraProfiles.wolfRunner().attributes().attackDamage();
    }
}
