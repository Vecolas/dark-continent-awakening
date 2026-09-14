package com.darkcontinent.nenfoundation.enemy.content;

import com.darkcontinent.nenfoundation.enemy.ai.RegrasDeExaustaoDeBolha;
import com.darkcontinent.nenfoundation.enemy.ai.RegrasDeSaltoDeBolha;
import com.darkcontinent.nenfoundation.enemy.combat.AttackDefinition;
import com.darkcontinent.nenfoundation.enemy.combat.AttackHitbox;
import com.darkcontinent.nenfoundation.enemy.greedisland.CaptureCondition;

/**
 * Os numeros de COMPORTAMENTO do Bubble Horse, num arquivo so dele.
 *
 * <p><b>Por que eles nao moram em {@link GreedIslandProfiles}.</b> Aquele arquivo
 * e a fila unica das sete criaturas da ilha, e por isso e o ponto mais hostil a
 * merge do pacote: enquanto varias frentes escrevem um bicho cada, todas
 * escreveriam no mesmo arquivo, nas mesmas regioes, ao mesmo tempo. Conflito de
 * merge da erro e e barato; o que nao da erro -- e e o motivo real desta
 * separacao -- e a resolucao de conflito que "aceita os dois lados" e deixa cair
 * silenciosamente o metodo de quem mergeou primeiro. O perfil continua sendo a
 * fonte da FICHA (HP, dano, velocidade, armadura, recarga, stagger, card e
 * condicao de captura); daqui saem os numeros que so este bicho tem.</p>
 *
 * <p><b>O que NAO esta aqui, de proposito: o dano da patada.</b> Ele e o
 * {@code ATTACK_DAMAGE} do perfil, lido do atributo no instante em que o golpe
 * comeca. Copiar o 3 para ca criaria a config paralela que este projeto ja sabe
 * que comete: o numero do perfil viraria o botao morto, e a sessao de
 * balanceamento nao mudaria nada.</p>
 *
 * <p><b>E o que NAO esta aqui por razao mais forte: o limiar da janela de
 * captura.</b> Ele e lido de {@code GreedIslandProfiles.capturas()}, porque a
 * captura JA o declara. Escrito de novo aqui, ele seria a segunda fonte para a
 * mesma verdade -- e a divergencia e a pior possivel neste bicho: o cavalo
 * pararia num ponto de vida que nao paga card, o jogador pararia de bater e nao
 * receberia nada, sem um erro para procurar.</p>
 */
public final class BubbleHorseTuning {

    private BubbleHorseTuning() { }

    /** O id na tabela de perfis e na de capturas. Uma grafia, um lugar. */
    public static final String ID = "bubble_horse";

    /** Regiao unica de dano: este bicho NAO tem ponto fraco -- ver abaixo. */
    public static final String REGIAO_COMUM = "corpo";

    // ------------------------------------------------------------ locomocao
    //
    // A FUGA E A FICHA. Os quatro numeros abaixo se leem juntos: a cada ciclo de
    // vinte ticks o cavalo passa doze parado e oito no ar. Um ciclo mais curto
    // aproxima o salto de uma corrida e apaga a pausa; um ciclo mais longo deixa
    // o bicho alcancavel a pe. Nenhum dos dois reprova portao nenhum.

    /**
     * Ticks parado entre um impulso e o proximo.
     *
     * <p>Doze ticks -- seis decimos de segundo -- e o tempo de leitura: e nele
     * que o jogador ve as bolhas achatarem e decide para onde correr. Zerar a
     * pausa nao daria erro: daria um cavalo que desliza, e um cavalo que desliza
     * nao pode ser antecipado, so perseguido -- e perseguir e a resposta errada.</p>
     *
     * <p>Este numero e COPIADO em {@code bubble_horse_animacoes.py}, que o soma
     * com {@link #TICKS_DE_ARCO_DO_SALTO} para fixar a duracao do clipe de salto.
     * A duplicacao e declarada: a outra ponta e a regua
     * {@code valida_ciclo_do_salto}, que reprova quando as duas discordam.</p>
     */
    public static final int TICKS_DE_PAUSA_ENTRE_SALTOS = 12;

    /** Ticks de voo do salto; a soma com a pausa e o ciclo que a animacao copia. */
    public static final int TICKS_DE_ARCO_DO_SALTO = 8;

    /**
     * Blocos por tick somados ao afastamento no instante do impulso.
     *
     * <p>Com o arrasto do ar, 0.34 por tick move o cavalo cerca de dois blocos e
     * meio por salto. E mais do que um jogador andando e menos do que um jogador
     * correndo: fugir funciona, mas nao o bastante para o bicho sumir -- e um
     * bicho que some nao chega a ensinar nada.</p>
     */
    public static final double IMPULSO_HORIZONTAL = 0.34D;

    /**
     * Blocos por tick somados para cima no impulso.
     *
     * <p>0.36 tira o corpo do chao por cerca de nove ticks, que e o arco que a
     * animacao desenha. Ele existe para o salto ter ALTURA visivel: um impulso so
     * horizontal seria mecanicamente identico e leria como escorregao.</p>
     */
    public static final double IMPULSO_VERTICAL = 0.36D;

    /**
     * Distancia em que a ameaca deixa de valer uma fuga, em blocos.
     *
     * <p>Dez blocos, contra os 24 de {@code followRange}: ele PERCEBE muito mais
     * longe do que foge. Essa folga e o que permite ao jogador se aproximar
     * devagar e ver o cavalo antes de espanta-lo. Igualar os dois faria o bicho
     * saltar assim que entrasse no alcance de percepcao, e ninguem chegaria perto
     * o bastante para descobrir que ele vale card.</p>
     */
    public static final double DISTANCIA_DE_CONFORTO = 10.0D;

    /**
     * Quantos ciclos de salto sem sair do lugar contam como encurralado.
     *
     * <p>Dois. Um so seria ruido -- um salto pode falhar por um bloco no caminho
     * -- e quatro deixariam o cavalo apanhar encostado na parede sem reagir. Ele
     * e limite de DESIGN, e nao botao de balanceamento: o que se ajusta e o
     * alcance e o dano do coice, nao a definicao de estar preso.</p>
     */
    public static final int CICLOS_PERDIDOS_PARA_ENCURRALAR = 2;

    // ---------------------------------------------------------------- coice
    //
    // O DANO 3 E O QUE SOBRA, e nao o que ele procura. Ver o javadoc de
    // RegrasDeSaltoDeBolha.coiceia: o construtor cobra que o alcance do coice seja
    // MENOR que a distancia de conforto, senao o bicho passa a golpear antes de
    // tentar fugir.

    /**
     * Distancia (centro a centro) em que ele decide coicear.
     *
     * <p>Ela tem de ser MENOR que {@code maxZ + }{@link #MEIA_LARGURA_DE_UM_ALVO},
     * e o teste cobra isso. Maior, o cavalo comeca uma empinada de catorze ticks
     * contra alguem que ja esta fora do alcance da pata -- e como ele trava o
     * corpo durante o golpe, o ataque no limite NUNCA acertaria. Isso nao da erro
     * nenhum: da um bicho que erra sozinho e parece quebrado.</p>
     */
    public static final double DISTANCIA_DO_COICE = 1.2D;

    /**
     * Meia-largura do alvo tipico -- um jogador tem 0.6 de lado.
     *
     * <p>Nao e botao de balanceamento: e a diferenca entre as duas reguas que este
     * arquivo usa. A distancia de DECISAO e medida de centro a centro
     * ({@code distanceTo}); a caixa de golpe e testada contra a BORDA do alvo
     * ({@code AABB.intersects}). Sem esta conversao as duas parecem comparaveis e
     * nao sao.</p>
     */
    public static final double MEIA_LARGURA_DE_UM_ALVO = 0.3D;

    /** Ticks de empinada: o aviso, e ele e longo para um bicho pequeno. */
    public static final int WINDUP_DA_PATADA = 14;
    /** Ticks em que a pata machuca. Curto: este golpe e um recurso, nao uma arma. */
    public static final int JANELA_DA_PATADA = 4;
    /** Ticks de recuperacao -- e a janela em que o jogador pode punir, e errar. */
    public static final int RECUPERACAO_DA_PATADA = 16;

    /**
     * Empurrao da patada.
     *
     * <p>Meio bloco, e ele NAO e cosmetico: afastar quem encurralou o cavalo e a
     * unica forma de o golpe cumprir o proposito dele, que e abrir espaco para o
     * proximo salto. Empurrao zero daria um coice que machuca e deixa o bicho
     * exatamente onde estava -- preso, batendo, ate morrer.</p>
     */
    public static final float EMPURRAO_DA_PATADA = 0.5F;

    /**
     * Recarga imposta a quem interrompeu o golpe.
     *
     * <p>Mais longa que a recarga normal ({@code bubbleHorseRecarga()} = 70):
     * interromper tem de VALER. Sem isso, {@code reset()} devolveria a fase para
     * IDLE e ele poderia recomecar no tick seguinte -- e o jogador aprenderia a
     * nao interromper, que e o oposto do que este encontro pede.</p>
     */
    public static final int RECARGA_APOS_INTERRUPCAO = 90;

    /**
     * Caixa do coice, em coordenadas LOCAIS. A FRENTE E +Z.
     *
     * <p><b>+Z, e isto nao e distracao.</b> Na GEOMETRIA do modelo (formato
     * Bedrock) a frente e -Z; na transformacao de {@link AttackHitbox#noMundo},
     * que e matematica de mundo, com yaw 0 o olhar vanilla aponta para +Z.
     * Misturar as duas ja custou um bug neste repositorio: a caixa ficou ATRAS do
     * mob, que atacava, animava e machucava quem estivesse pelas costas. Fase
     * certa, cooldown certo, log limpo.</p>
     *
     * <p><b>E o golpe sai a FRENTE mesmo se chamando coice.</b> O bicho EMPINA e
     * desce as patas dianteiras; um coice traseiro de verdade exigiria uma caixa
     * com z negativo, que e a mesma inversao acima escrita de proposito -- e a
     * proxima pessoa a ler este arquivo nao teria como saber qual das duas era a
     * intencao.</p>
     *
     * <p>O {@code maxZ} de 1.0 nao e um numero solto: a pata DESENHADA alcanca
     * 1.03 bloco a partir do centro, e {@code bubble_horse_geo.py} reprova se a
     * caixa passar do desenho. Caixa maior que a pata da um jogador que apanha de
     * um casco que, na tela, parou antes dele.</p>
     */
    public static final AttackHitbox CAIXA_DA_PATADA =
            new AttackHitbox(-0.7D, 0.0D, 0.3D, 0.7D, 1.5D, 1.0D);

    // ------------------------------------------------------------- exaustao

    /**
     * Ticks de janela de captura: sete segundos.
     *
     * <p>Longo o bastante para o jogador PERCEBER que alguma coisa mudou, parar,
     * e agir. Curto o bastante para ser uma janela: com trinta segundos a decisao
     * de parar de bater deixaria de ter custo, e com um segundo o cavalo pareceria
     * apenas ter travado por um instante.</p>
     */
    public static final int TICKS_DA_JANELA = 140;

    /**
     * Ticks de folego antes de ele poder exaurir de novo: dez segundos.
     *
     * <p>Maior que a janela de proposito. Quem deixou a janela passar tem de
     * PERSEGUIR um cavalo ferido por dez segundos antes de ganhar outra -- e essa
     * perseguicao e a punicao por nao ter lido o sinal. Igualar os dois faria a
     * segunda janela chegar quase de graca, e a primeira deixaria de importar.</p>
     */
    public static final int TICKS_DE_FOLEGO = 200;

    // ------------------------------------------------------------- montagem

    /**
     * A condicao de captura publicada, com a coerencia COBRADA.
     *
     * <p>A checagem existe porque a janela de exaustao so faz sentido sobre uma
     * captura nao-letal. Trocada por {@code porAbate()} la, ela viraria um
     * intervalo em que o bicho fica parado apanhando -- e o unico sintoma seria um
     * mob que parece bugado, com a ficha, o loot e o card todos corretos.</p>
     */
    public static CaptureCondition condicaoDeCaptura() {
        CaptureCondition condicao = GreedIslandProfiles.capturas().get(ID);
        if (condicao == null) {
            throw new IllegalStateException("nao ha condicao de captura publicada para '" + ID
                    + "': a janela de exaustao deste bicho existe para colher exatamente essa"
                    + " condicao, e sem ela o cavalo pararia para nada.");
        }
        if (!condicao.exigeNaoLetal()) {
            throw new IllegalStateException("a captura de '" + ID + "' deixou de ser nao-letal:"
                    + " a janela de exaustao vira um intervalo em que o bicho fica parado"
                    + " apanhando, e o unico sintoma e um mob que parece bugado.");
        }
        return condicao;
    }

    /**
     * A regra da janela, com o limiar LIDO da condicao de captura.
     *
     * <p>Lido, e nao escrito de novo: o ponto em que o cavalo para tem de ser o
     * mesmo ponto em que o card passa a valer. Dois numeros iguais hoje divergem
     * no primeiro ajuste, e a divergencia e muda -- o jogador ve o bicho parar,
     * para de bater, e nao recebe nada.</p>
     */
    public static RegrasDeExaustaoDeBolha exaustao() {
        return new RegrasDeExaustaoDeBolha(condicaoDeCaptura().vidaMaximaFracao(),
                TICKS_DA_JANELA, TICKS_DE_FOLEGO);
    }

    /**
     * A regra do salto, com o coice junto.
     *
     * <p>Os dois viajam no mesmo record porque o construtor precisa cobrar que o
     * alcance do coice seja menor que a distancia de conforto. Separados, os dois
     * numeros divergiriam e o bicho passaria a golpear antes de tentar fugir.</p>
     */
    public static RegrasDeSaltoDeBolha salto() {
        return new RegrasDeSaltoDeBolha(TICKS_DE_PAUSA_ENTRE_SALTOS, TICKS_DE_ARCO_DO_SALTO,
                IMPULSO_HORIZONTAL, IMPULSO_VERTICAL, DISTANCIA_DE_CONFORTO, DISTANCIA_DO_COICE);
    }

    /**
     * A patada, com o dano LIDO do atributo na hora.
     *
     * <p>O windup e interrompivel e a JANELA nao: depois que a pata desce, ela
     * desce. Interromper durante os 4 ticks ativos faria o cavalo cancelar um
     * golpe que o jogador ja viu sair, e a leitura -- que e a unica coisa que um
     * ataque telegrafado entrega -- deixaria de valer.</p>
     *
     * @param danoDoAtributo {@code ATTACK_DAMAGE} medido pelo servidor no instante
     *        em que o golpe comeca; congelar esse valor numa constante daqui faria
     *        todo buff, debuff e ajuste de perfil sumirem sem aviso
     */
    public static AttackDefinition patada(float danoDoAtributo) {
        return new AttackDefinition("bubble_horse_patada", WINDUP_DA_PATADA, JANELA_DA_PATADA,
                RECUPERACAO_DA_PATADA, danoDoAtributo, EMPURRAO_DA_PATADA, true, false, true);
    }
}
