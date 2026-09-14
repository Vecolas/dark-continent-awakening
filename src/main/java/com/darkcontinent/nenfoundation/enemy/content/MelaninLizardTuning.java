package com.darkcontinent.nenfoundation.enemy.content;

import com.darkcontinent.nenfoundation.enemy.ai.RegrasDeCamuflagemDeRocha;
import com.darkcontinent.nenfoundation.enemy.combat.AttackDefinition;
import com.darkcontinent.nenfoundation.enemy.combat.AttackHitbox;
import com.darkcontinent.nenfoundation.enemy.combat.GrabRules;
import com.darkcontinent.nenfoundation.enemy.combat.WeakPoint;
import com.darkcontinent.nenfoundation.enemy.combat.WeakPointRegistry;
import com.darkcontinent.nenfoundation.enemy.combat.WeakPointResolver;
import java.util.Map;

/**
 * Os numeros de COMPORTAMENTO do Melanin Lizard, num arquivo so dele.
 *
 * <p><b>Por que eles nao moram em {@link GreedIslandProfiles}.</b> Aquele arquivo
 * e a tabela das sete criaturas da ilha, e por isso e o ponto mais hostil a merge
 * do pacote: enquanto varias frentes escrevem um mob cada, todas escreveriam no
 * mesmo arquivo, nas mesmas regioes, ao mesmo tempo. Conflito de merge da erro e
 * e barato; o que nao da erro -- e e o motivo real desta separacao -- e a
 * resolucao de conflito que "aceita os dois lados" e deixa cair silenciosamente o
 * metodo de quem mergeou primeiro. O perfil continua sendo a fonte da FICHA (HP,
 * dano, velocidade, armadura, recarga, stagger); daqui saem os numeros que so
 * este bicho tem.</p>
 *
 * <p><b>O que NAO esta aqui, de proposito: o dano do bote.</b> Ele e o
 * {@code ATTACK_DAMAGE} do perfil, lido do atributo na hora em que o golpe
 * comeca. Copiar o 7 para ca criaria a config paralela que este projeto ja sabe
 * que comete: o numero do perfil viraria o botao morto, e a sessao de
 * balanceamento nao mudaria nada.</p>
 */
public final class MelaninLizardTuning {

    private MelaninLizardTuning() { }

    // ----------------------------------------------------------------- bote

    /**
     * Aviso do bote, em ticks. Copiado por {@code melanin_lizard_animacoes.py}.
     *
     * <p>Doze ticks e mais longo que o telegrafo de um golpe comum, e isso e
     * deliberado: o que este ataque tira do jogador nao e vida, e CONTROLE. Ele
     * prende. Uma perda de controle precisa de mais tempo de leitura do que uma
     * perda de HP, porque ela nao tem como ser desfeita depois.</p>
     */
    public static final int BOTE_WINDUP_TICKS = 12;
    /** Janela que machuca e que agarra. Curta: o bote e um salto, nao um abraco. */
    public static final int BOTE_ACTIVE_TICKS = 5;
    /** Recuperacao -- e tambem a janela em que o lagarto pode ser punido. */
    public static final int BOTE_RECOVERY_TICKS = 18;

    /**
     * Empurrao do bote: quase nada, e e assim de proposito.
     *
     * <p>Empurrao forte AFASTA a vitima, e afastar e exatamente o contrario do
     * que este golpe quer. Um knockback generoso aqui produziria um mob que morde
     * e joga para longe quem ele acabou de tentar agarrar -- funcionando,
     * animando e sem nunca prender ninguem.</p>
     */
    public static final float BOTE_KNOCKBACK = 0.12F;

    /**
     * Distancia em que o lagarto decide dar o bote, em blocos.
     *
     * <p>Ela e igual ao limite distante de {@link #CAIXA_DO_BOTE} de proposito:
     * decidir de mais longe do que a caixa alcanca produz um bicho que ataca o
     * vazio, e decidir de mais perto desperdica a metade da caixa. As duas
     * versoes rodam sem um erro no log.</p>
     */
    public static final double ALCANCE_DO_BOTE = 2.0D;

    /**
     * Caixa do bote, em coordenadas LOCAIS. A FRENTE E +Z.
     *
     * <p>+Z, e nao -Z. A geometria Bedrock do modelo tem a frente em -Z, mas
     * {@link AttackHitbox#noMundo} e matematica de MUNDO, onde yaw 0 olha para
     * +Z. Misturar as duas convencoes poe a caixa ATRAS do bicho: ele ataca,
     * anima, publica a fase certa e machuca quem estiver pelas costas. Nao ha
     * erro nenhum nisso -- o boneco de treino (#138) so descobriu com gametest.</p>
     *
     * <p>Ela e baixa (ate 0.9 de altura) porque o bicho e baixo: uma caixa alta
     * faria o lagarto morder quem esta em cima de um bloco sem nunca encostar
     * nele na tela.</p>
     */
    public static final AttackHitbox CAIXA_DO_BOTE =
            new AttackHitbox(-0.6D, 0.0D, 0.4D, 0.6D, 0.9D, ALCANCE_DO_BOTE);

    /** Recarga imposta a quem foi interrompido -- interromper nao pode premiar. */
    public static final int RECARGA_APOS_INTERRUPCAO = 60;

    /** Alcance do traco que procura o ponto de impacto na caixa do lagarto. */
    public static final double ALCANCE_DO_TRACO = 6.0D;

    /**
     * O bote, com o dano LIDO do atributo na hora.
     *
     * @param danoDoAtributo {@code ATTACK_DAMAGE} medido pelo servidor no instante
     *        em que o golpe comeca; congelar esse valor numa constante daqui
     *        faria todo buff, debuff e ajuste de perfil sumirem sem aviso
     */
    public static AttackDefinition bote(float danoDoAtributo) {
        return new AttackDefinition("melanin_lizard_bote", BOTE_WINDUP_TICKS, BOTE_ACTIVE_TICKS,
                BOTE_RECOVERY_TICKS, danoDoAtributo, BOTE_KNOCKBACK, true, false, true);
    }

    // -------------------------------------------------------------- agarrao

    /**
     * Quatro segundos preso, um pulso de 2 a cada segundo, e 9 de dano NO LAGARTO
     * compram a soltura.
     *
     * <p>Os numeros se leem juntos, e a conta tem de dar "bater e melhor do que
     * esperar": aguentar os 80 ticks calado custa 8 de vida (4 pulsos de 2, e o
     * tick 0 nao pulsa), enquanto reagir custa acertar 9 num lagarto de 40. Se o
     * custo de esperar ficar menor que o de reagir, a janela de escape vira
     * decoracao -- ela continua existindo, ninguem nunca a usa, e o mob passa a
     * ser um atraso de quatro segundos em vez de uma briga.</p>
     *
     * <p>Ele prende MENOS tempo que o Frog-In-Waiting (100 ticks) e machuca menos
     * por pulso: o sapo engole, este aqui so imobiliza. O perigo dele e o que
     * chega enquanto voce esta preso.</p>
     */
    public static GrabRules agarrao() {
        return new GrabRules(80, 20, 2.0F, 9.0F);
    }

    /**
     * O maior alvo que cabe na mordida: 2.0 de altura por 1.0 de largura.
     *
     * <p>NAO e botao de balanceamento -- e o tamanho da boca, e ela e pequena. O
     * lagarto tem 1.6 x 0.8 bloco e PRENDE a presa no chao; ele nao engole nada.
     * Um jogador (0.6 x 1.8) cabe; um golem de ferro (1.4 x 2.7) e recusado, e a
     * recusa tem motivo declarado em {@code GrabRefusal.ALVO_GRANDE_DEMAIS}.</p>
     *
     * <p>Sem o limite, a recusa nao existiria e o sintoma seria silencioso: um
     * lagarto de meio bloco de altura segurando um cyclops no chao.</p>
     */
    public static final double ALTURA_MAXIMA_DA_PRESA = 2.0D;
    public static final double LARGURA_MAXIMA_DA_PRESA = 1.0D;

    /**
     * Onde a vitima fica presa, em fracao da altura do lagarto.
     *
     * <p>0.3 e a altura da MANDIBULA desenhada, e nao um numero de gosto: o
     * gerador de geometria confere este mesmo valor contra a caixa da boca
     * (`valida_boca_na_altura_de_encaixe_da_vitima`). Girar ele aqui sem mexer no
     * modelo deixa o preso flutuando ao lado de uma boca que nao segura nada, e
     * nenhum portao de Java ve isso.</p>
     */
    public static final double FRACAO_DE_ENCAIXE_DA_VITIMA = 0.3D;

    /** Quantos blocos a frente tentar soltar a vitima, antes de tentar os lados. */
    public static final double DISTANCIA_DE_SOLTURA = 1.2D;

    /**
     * Quanto subir na ULTIMA tentativa de soltura.
     *
     * <p>Um bloco e meio: o bastante para tirar a vitima de dentro do lagarto e
     * pouco o bastante para nao virar queda. Ela e a ultima carta de
     * {@code SafeReleaseSpot} justamente porque teleportar alguem para cima e a
     * soltura que mais surpreende.</p>
     */
    public static final double ALTURA_DE_ESCAPE = 1.5D;

    /**
     * Ticks de boca fechada depois de soltar alguem.
     *
     * <p>Nao e botao de balanceamento: e o minimo para a soltura ser LEGIVEL. Sem
     * ele o mesmo tick que solta ja recomeca a perseguir, e o jogador nao chega a
     * ver que escapou -- ele so ve que continua sendo mordido.</p>
     */
    public static final int TICKS_DE_RETIRADA = 30;

    // ----------------------------------------------------------- camuflagem

    /**
     * Quebra a 4.5 blocos, com o olhar a menos de ~57 graus do bicho, e volta a
     * ser pedra depois de 6 segundos sem alvo.
     *
     * <p>Os tres se leem juntos. 4.5 blocos e perto o bastante para o jogador que
     * passou reto NAO acordar a pedra, e longe o bastante para quem a encarou de
     * proposito ter um instante antes do bote -- o alcance do bote e 2.0, entao
     * sobram 2.5 blocos de aviso. 0.55 de cosseno e "esta virado para ca", nao "de
     * relance". E 120 ticks de recamuflagem impedem a pedra que pisca: quem
     * perdeu o lagarto de vista tem seis segundos para reencontra-lo antes de ele
     * sumir da paisagem de novo.</p>
     */
    public static RegrasDeCamuflagemDeRocha camuflagem() {
        return new RegrasDeCamuflagemDeRocha(4.5D, 0.55D, 120);
    }

    // ---------------------------------------------------------- ponto fraco

    /**
     * O OLHO -- e ele e o unico contraste alto da textura, de proposito.
     *
     * <p>0.54 de altura relativa e o pe do olho DESENHADO (y 7 de uma hitbox de
     * 12.8 px), medido e nao escolhido; o gerador de textura reprova se o desenho
     * sair dessa faixa. 0.35 de cosseno quer dizer "acertou vindo pela frente":
     * quem ataca um lagarto pelas costas acerta a crista, que e placa de rocha.</p>
     *
     * <p>A regiao padrao e {@code "corpo"} e existe para que sobre corpo comum. Um
     * limiar rasteiro faria todo golpe virar critico, e o multiplicador deixaria
     * de ser recompensa por mira para virar um desconto geral de HP -- sem que
     * nada acusasse.</p>
     */
    public static WeakPointResolver pontoFraco() {
        return new WeakPointResolver("olho", "corpo", 0.54D, 0.35D);
    }

    /**
     * Acertar o olho vale 1.8x.
     *
     * <p>E o que torna a captura NAO-LETAL praticavel: a condicao de card deste
     * bicho e {@code porEnfraquecimento} -- um quarto da vida, sem matar --, e
     * sem uma forma de derrubar HP depressa e com controle o jogador so tem o
     * golpe comum, que passa do ponto e mata. O multiplicador nao e "dano extra":
     * e o botao de precisao que a captura exige.</p>
     */
    public static WeakPointRegistry pontosFracos() {
        return new WeakPointRegistry(Map.of("olho", new WeakPoint("olho", "cabeca", 1.8F, true)));
    }
}
