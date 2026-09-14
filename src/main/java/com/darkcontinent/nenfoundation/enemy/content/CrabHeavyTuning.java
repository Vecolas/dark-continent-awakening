package com.darkcontinent.nenfoundation.enemy.content;

import com.darkcontinent.nenfoundation.enemy.combat.AttackDefinition;
import com.darkcontinent.nenfoundation.enemy.combat.AttackHitbox;
import com.darkcontinent.nenfoundation.enemy.combat.GrabRules;
import com.darkcontinent.nenfoundation.enemy.combat.RegrasDeCarapacaOrientada;
import com.darkcontinent.nenfoundation.enemy.combat.RegrasDePinca;

/**
 * Os numeros de COMPORTAMENTO do Crab Heavy, num arquivo so dele.
 *
 * <p><b>Por que eles nao moram em {@link ChimeraProfiles}.</b> Aquele arquivo e
 * a tabela das nove formigas, e por isso e o ponto mais hostil a merge do
 * pacote: enquanto varias frentes escrevem uma formiga cada, todas escreveriam
 * no mesmo arquivo, nas mesmas regioes, ao mesmo tempo. Conflito de merge da
 * erro e e barato; o que nao da erro -- e e o motivo real desta separacao -- e a
 * resolucao de conflito que "aceita os dois lados" e deixa cair em silencio o
 * metodo de quem mergeou primeiro. O perfil continua sendo a fonte da FICHA (HP
 * 60, dano 9, velocidade 0.21, armadura 8, recarga, stagger); daqui saem os
 * numeros que so este bicho tem.</p>
 *
 * <p><b>O que NAO esta aqui, de proposito: o dano da pincada e o valor da
 * armadura.</b> O dano e o {@code ATTACK_DAMAGE} do perfil, lido do atributo no
 * instante em que o golpe comeca; a armadura e o {@code ARMOR}, lido no instante
 * do impacto. Copiar o 9 e o 8 para ca criaria a config paralela que este
 * projeto ja sabe que comete: o numero do perfil viraria o botao morto, e a
 * sessao de balanceamento nao mudaria nada. O que esta aqui e a FRACAO da
 * armadura que cada angulo encontra -- que e regra, e nao valor.</p>
 */
public final class CrabHeavyTuning {

    private CrabHeavyTuning() { }

    // ------------------------------------------------------------- carapaca

    /**
     * A placa cheia de frente, metade no flanco, quase nada pelas costas.
     *
     * <p>Os cinco numeros se leem juntos, e a conta que eles tem de fechar e "o
     * caminho mais longo tem de pagar mais que o caminho curto". Com armadura 8 e
     * um golpe de ferro (7 de dano cru), a formula do jogo devolve algo perto de
     * 5 de dano de frente e quase 7 pelas costas: e uma briga de 12 golpes contra
     * uma de 9. O degrau e sentido sem ser trivial -- e isso e deliberado. Um
     * ventre que zerasse a armadura transformaria o encontro em "acerte uma vez
     * pelas costas e acabou", e o caranguejo deixaria de segurar linha nenhuma.</p>
     *
     * <p><b>0.45 de cosseno</b> e um arco frontal de cerca de 63 graus para cada
     * lado: e o setor que o bicho consegue manter virado para o alvo enquanto
     * anda. Aberto demais (0.2, ~78 graus) nao sobraria flanco; fechado demais
     * (0.8, ~37 graus) faria qualquer passo lateral do jogador ja valer como
     * flanco, e o quebra-cabeca se resolveria sozinho.</p>
     *
     * <p><b>-0.35 de cosseno</b> poe a fronteira do ventre em cerca de 110 graus:
     * e preciso passar bem do meio do bicho para chegar la. O flanco fica sendo,
     * entao, uma faixa larga de 47 graus -- larga de proposito, porque e ela que
     * ENSINA: o jogador sente o dano subir enquanto contorna e continua
     * contornando.</p>
     *
     * <p><b>Por que o flanco nao e ventre.</b> A carapaca deste bicho e uma placa
     * so, mais larga que o corpo, e ela sobra pelos lados -- esta desenhada assim
     * em {@code crab_heavy_geo.py}. De lado o golpe ainda encontra a aba dela; o
     * que aparece de lado e a COSTURA com o abdomen, e nao o abdomen. Dar ventre
     * cheio ao flanco faria a regra contradizer o desenho, e a contradicao nao
     * levanta erro: ela ensina o jogador a mirar onde o desenho diz que e duro.</p>
     */
    public static RegrasDeCarapacaOrientada carapaca() {
        return new RegrasDeCarapacaOrientada(0.45D, -0.35D, 1.0F, 0.55F, 0.15F);
    }

    // -------------------------------------------------------------- pincada

    /**
     * Aviso da pincada, em ticks. Copiado por {@code crab_heavy_animacoes.py}.
     *
     * <p>Dezesseis ticks e mais longo que o telegrafo de um golpe comum pelo mesmo
     * motivo que o do Melanin Lizard: o que este ataque tira do jogador nao e
     * vida, e CONTROLE. Perda de controle precisa de mais tempo de leitura do que
     * perda de HP, porque ela nao tem como ser desfeita depois.</p>
     */
    public static final int PINCADA_WINDUP_TICKS = 16;

    /** A janela que machuca e que prende. Curta: a pinca fecha, nao abraca. */
    public static final int PINCADA_ACTIVE_TICKS = 6;

    /**
     * Recuperacao -- e e ela que faz este bicho ser contornavel.
     *
     * <p>Vinte ticks e a janela mais longa do mob depois do aviso, e ela existe
     * para ser usada: e nela que o jogador larga a frente e vai para tras.
     * Encurtar a recuperacao nao deixaria o caranguejo mais perigoso; deixaria o
     * encontro sem a unica janela em que a resposta certa cabe, e o jogador
     * voltaria a bater na placa por falta de alternativa.</p>
     */
    public static final int PINCADA_RECOVERY_TICKS = 20;

    /**
     * Empurrao da pincada: quase nada, e e assim de proposito.
     *
     * <p>Empurrao forte AFASTA a vitima, e afastar e o contrario do que este golpe
     * quer. Um knockback generoso aqui produziria um mob que agarra e joga para
     * longe quem acabou de prender -- funcionando, animando e sem nunca segurar
     * ninguem.</p>
     */
    public static final float PINCADA_KNOCKBACK = 0.1F;

    /**
     * Alcance da pincada, em blocos. E o {@code maxZ} de {@link #CAIXA_DA_PINCADA}.
     *
     * <p>Ela e igual ao limite distante da caixa de proposito: decidir de mais
     * longe do que a caixa alcanca produz um bicho que ataca o vazio, e decidir de
     * mais perto desperdica metade da caixa. As duas versoes rodam sem um erro no
     * log.</p>
     *
     * <p>1.5 e CURTO, e isso e a ficha. Este bicho nao alcanca: ele espera que
     * voce chegue. {@code crab_heavy_animacoes.py} cobra este mesmo numero contra
     * a pinca desenhada em movimento, e por isso ele nao pode crescer aqui
     * sozinho.</p>
     */
    public static final double ALCANCE_DA_PINCADA = 1.5D;

    /**
     * Caixa da pincada, em coordenadas LOCAIS. A FRENTE E +Z.
     *
     * <p>+Z, e nao -Z. A geometria Bedrock do modelo tem a frente em -Z, mas
     * {@link AttackHitbox#noMundo} e matematica de MUNDO, onde yaw 0 olha para
     * +Z. Misturar as duas convencoes poe a caixa ATRAS do bicho: ele ataca,
     * anima, publica a fase certa e machuca quem estiver pelas costas -- que
     * neste mob seria especialmente cruel, porque pelas costas e onde o jogador
     * foi ensinado a ficar. Nao ha erro nenhum nisso; o boneco de treino (#138)
     * so descobriu com gametest.</p>
     *
     * <p>Larga (1.7 bloco) porque as duas pincas fecham juntas, e baixa (ate 1.2)
     * porque o bicho e baixo: uma caixa alta faria o caranguejo agarrar quem esta
     * em cima de um bloco sem nunca encostar nele na tela.</p>
     */
    public static final AttackHitbox CAIXA_DA_PINCADA =
            new AttackHitbox(-0.85D, 0.0D, 0.4D, 0.85D, 1.2D, ALCANCE_DA_PINCADA);

    /** Recarga imposta a quem foi interrompido -- interromper nao pode premiar. */
    public static final int RECARGA_APOS_INTERRUPCAO = 70;

    /** Alcance do traco que procura o ponto de impacto na caixa do caranguejo. */
    public static final double ALCANCE_DO_TRACO = 5.0D;

    /**
     * A pincada, com o dano LIDO do atributo na hora.
     *
     * @param danoDoAtributo {@code ATTACK_DAMAGE} medido pelo servidor no instante
     *        em que o golpe comeca; congelar esse valor numa constante daqui faria
     *        todo buff, debuff e ajuste de perfil sumirem sem aviso
     */
    public static AttackDefinition pincada(float danoDoAtributo) {
        return new AttackDefinition("crab_heavy_pincada", PINCADA_WINDUP_TICKS,
                PINCADA_ACTIVE_TICKS, PINCADA_RECOVERY_TICKS, danoDoAtributo,
                PINCADA_KNOCKBACK, true, false, true);
    }

    // -------------------------------------------------------------- agarrao

    /**
     * Quatro segundos preso, um pulso de 3 a cada segundo, e 10 de dano NO
     * CARANGUEJO compram a soltura.
     *
     * <p>Os numeros se leem juntos, e a conta tem de dar <b>"bater e melhor do
     * que esperar"</b>: aguentar os 80 ticks calado custa 12 de vida (quatro
     * pulsos de 3), enquanto reagir custa 10 de dano pedido. Se o custo de
     * esperar ficasse menor que o de reagir, a janela de escape viraria
     * decoracao -- ela continuaria existindo, ninguem nunca a usaria, e o mob
     * passaria a ser um atraso de quatro segundos em vez de uma briga.
     * {@code CrabHeavyPerfilTest} cobra essa desigualdade, porque ela e a unica
     * coisa aqui que se quebra sozinha quando alguem ajusta um numero so.</p>
     *
     * <p><b>E ha uma segunda leitura, e ela e o encontro inteiro.</b> Quem esta
     * preso esta NA FRENTE do caranguejo, ou seja, batendo na carapaca. O dano
     * que compra a soltura e o PEDIDO, antes da armadura, entao escapar sozinho
     * continua possivel -- mas quem escapa depressa e quem tem alguem atras do
     * bicho. O agarrao nao e o perigo: o perigo e o que chega enquanto voce esta
     * preso.</p>
     */
    public static GrabRules agarrao() {
        return new GrabRules(80, 20, 3.0F, 10.0F);
    }

    /**
     * O maior alvo que cabe entre as pincas: 2.2 de altura por 1.2 de largura.
     *
     * <p>NAO e botao de balanceamento -- e o vao da garra. Um jogador (0.6 x 1.8)
     * cabe; um golem de ferro (1.4 x 2.7) e recusado, e a recusa tem motivo
     * declarado em {@code GrabRefusal.ALVO_GRANDE_DEMAIS}. Sem o limite a recusa
     * nao existiria, e o sintoma seria silencioso: um caranguejo de um bloco e
     * meio segurando um cyclops de quatro pelo chao.</p>
     */
    public static final double ALTURA_MAXIMA_DA_PRESA = 2.2D;
    public static final double LARGURA_MAXIMA_DA_PRESA = 1.2D;

    /**
     * Onde a presa fica, com que forca ela e puxada e quando ela se solta.
     *
     * <p>1.1 bloco a frente e logo alem da propria caixa de colisao do bicho
     * (1.4 de lado): a vitima fica encostada na pinca, e nao dentro dele. Puxar
     * para dentro empilharia duas entidades no mesmo bloco, e o empurrao natural
     * das entidades gastaria o tick inteiro brigando com o puxao.</p>
     *
     * <p>3.0 de raio de ruptura e mais que o dobro da distancia de pinca, que e o
     * que o proprio {@link RegrasDePinca} exige: o ponto de pinca gira em volta do
     * caranguejo, e um raio curto faria o bicho perder a presa so por virar o
     * corpo. Ele tambem e folgado o bastante para aguentar um empurrao ou um
     * degrau sem soltar, e apertado o bastante para que uma perola ou um
     * teleporte soltem na hora.</p>
     *
     * <p>0.55 bloco por tick de puxao e mais rapido do que um jogador andando
     * (0.22) e mais lento do que a maioria dos empurroes: da para lutar contra
     * ele, nao da para simplesmente sair andando.</p>
     */
    public static RegrasDePinca pinca() {
        return new RegrasDePinca(1.1D, 3.0D, 0.55D);
    }

    /** Quantos blocos a frente tentar soltar a vitima, antes de tentar os lados. */
    public static final double DISTANCIA_DE_SOLTURA = 1.3D;

    /**
     * Quanto subir na ULTIMA tentativa de soltura.
     *
     * <p>Um bloco e meio: o bastante para tirar a vitima de dentro do bicho e
     * pouco o bastante para nao virar queda. Ela e a ultima carta de
     * {@code SafeReleaseSpot} justamente porque teleportar alguem para cima e a
     * soltura que mais surpreende.</p>
     */
    public static final double ALTURA_DE_ESCAPE = 1.5D;

    /**
     * Ticks de pinca aberta depois de soltar alguem.
     *
     * <p>Nao e botao de balanceamento: e o minimo para a soltura ser LEGIVEL. Sem
     * ele o mesmo tick que solta ja recomeca a agarrar, e o jogador nao chega a
     * ver que escapou -- ele so ve que continua preso.</p>
     */
    public static final int TICKS_DE_RETIRADA = 30;

    // ------------------------------------------------------------ a linha

    /**
     * Quantos blocos ele avanca a partir de onde travou combate.
     *
     * <p><b>Limite de DESIGN, e nao botao de tuning.</b> Este bicho e
     * {@code FRONTLINER}: a funcao dele e ocupar espaco e nao deixar passar. Um
     * frontliner que persegue atravessa o encontro inteiro atras de um alvo e
     * deixa aberto exatamente o lugar que ele existia para fechar -- e nada
     * reprova isso, porque perseguir e o comportamento padrao de todo mob do
     * jogo. O bicho continua atacando, acertando e morrendo normalmente; ele so
     * deixa de ser esta peca.</p>
     *
     * <p>Doze blocos e o alcance de percepcao dele menos uma folga: ele avanca
     * ate quase o limite do que enxerga e para. Quem correr mais do que isso
     * consegue passar -- e esse e o preco de nao o matar.</p>
     */
    public static final double ALCANCE_DE_AVANCO = 12.0D;
}
