package com.darkcontinent.nenfoundation.enemy.content;

import com.darkcontinent.nenfoundation.enemy.combat.AttackDefinition;
import com.darkcontinent.nenfoundation.enemy.combat.AttackHitbox;
import com.darkcontinent.nenfoundation.enemy.combat.RegrasDeViragem;
import com.darkcontinent.nenfoundation.enemy.combat.WeakPoint;
import com.darkcontinent.nenfoundation.enemy.combat.WeakPointRegistry;
import com.darkcontinent.nenfoundation.enemy.combat.WeakPointResolver;
import com.darkcontinent.nenfoundation.enemy.perception.VisionCone;
import java.util.Map;

/**
 * Os numeros do King White Stag Beetle que nascem com o COMPORTAMENTO dele.
 *
 * <p><b>Por que este arquivo existe em vez de mais metodos em
 * {@link GreedIslandProfiles}.</b> Aquele arquivo e a fila unica das sete
 * criaturas da ilha, e neste momento varias frentes escrevem o comportamento de
 * criaturas diferentes ao mesmo tempo. Todas precisariam acrescentar metodos no
 * MESMO bloco de linhas, e o resultado de um merge assim nao e um conflito
 * barulhento -- e uma resolucao apressada em que o metodo de alguem some. Metodo
 * que some nao da erro de compilacao quando o chamador some junto: da um mob que
 * perdeu o ponto fraco e continua nascendo, atacando e passando em todo portao.
 * Aqui o arquivo tem um dono so, e um conflito aqui e sempre uma disputa real.</p>
 *
 * <p><b>O que fica la e o que fica aqui.</b> Em {@code GreedIslandProfiles} ficam
 * a ficha publicada (atributos, spawn, card, condicao de captura), a recarga e o
 * stagger -- tudo que ja estava escrito e que outros sistemas leem. Aqui ficam os
 * numeros NOVOS, os que este comportamento inaugurou: a forma do telegrafo, a
 * geometria das duas janelas de ventre, o alcance dos chifres e a duracao do
 * tombo.</p>
 *
 * <p><b>O dano nao e um numero proprio.</b> Ele e lido de
 * {@code GreedIslandProfiles.kingWhiteStagBeetle().attributes().attackDamage()},
 * porque a investida e o unico ataque do bicho. Repetir o 12 aqui criaria duas
 * fontes para a mesma verdade, e girar o atributo numa sessao de balanceamento
 * mudaria a barra de vida do jogador sem mudar este arquivo.</p>
 */
public final class KingWhiteStagBeetleTuning {

    private KingWhiteStagBeetleTuning() { }

    // ------------------------------------------------------------- regioes

    /** Id da regiao vulneravel; o mesmo nome aparece no registro e nos dois resolvers. */
    public static final String REGIAO_DO_VENTRE = "ventre";
    /**
     * Id da regiao comum -- e ela se chama "carapaca" de proposito.
     *
     * <p>O nome padrao nos outros mobs e "body". Aqui o nome carrega a ficha: o
     * corpo comum DESTE bicho e uma casca de armadura 9, e quem for depurar um
     * relato de "nao tiro vida dele" le a resposta no proprio id da regiao.</p>
     */
    public static final String REGIAO_DA_CARAPACA = "carapaca";

    /**
     * Multiplicador do ventre.
     *
     * <p>A conta que importa NAO e a da barra de vida, e sim a do stagger. Com
     * {@code kingWhiteStagBeetleStagger()} -- limiar 30, resistencia 6, decaimento
     * 0.5 por tick -- um golpe de espada de diamante (7) na carapaca entra com 1,
     * e o decaimento come 6 no intervalo entre duas espadadas: a carapaca NUNCA
     * acumula stagger, por aritmetica e nao por um sinalizador. O mesmo golpe no
     * ventre entra com 22 e dois acertos derrubam o bicho.</p>
     *
     * <p>E essa diferenca -- e nao o dano -- que ensina o jogador a esperar o
     * telegrafo. Baixar isto para 3.0 nao quebra nada, nao reprova portao nenhum,
     * e faz os dois acertos virarem tres que nao cabem na janela de aviso: o
     * encontro passa a nao ter resposta, e ninguem consegue dizer por que.</p>
     */
    public static final float MULTIPLICADOR_DO_VENTRE = 4.0F;

    // --------------------------------------------- as duas janelas do ventre
    //
    // Ver o javadoc de RegrasDeViragem para o porque de existirem DUAS: o
    // WeakPointResolver so sabe comparar piso de altura e piso de cosseno, e um
    // ventre "por baixo" precisaria de TETO. A saida foi nao precisar de por
    // baixo -- nas duas posturas em que o ventre conta, ele esta virado para cima
    // ou para a frente.

    /**
     * Altura relativa a partir da qual o impacto conta como ventre COM O BICHO
     * VIRADO.
     *
     * <p>0.54 de 1.4 bloco e 0.756 bloco do chao, ou 12.1 px. A placa do ventre
     * DESENHADA ocupa y 3..5 px no modelo de pe; girado 180 graus em torno do
     * pivo do corpo (y=9), ela passa a ocupar y 13..15 px, ou seja 0.58..0.67 da
     * caixa. O limiar fica de proposito um pouco ABAIXO dela, e
     * {@code king_white_stag_beetle_geo.py} cobra exatamente essa folga.</p>
     *
     * <p>A razao da folga e a mesma do olho do Cyclops: o servidor mede onde o
     * traco ENTRA na caixa de colisao, e nao o pixel que o jogador mirou. Exigir o
     * pixel exato faria o critico quase nunca pagar, e um ponto fraco que nao paga
     * e pior do que nenhum, porque o desenho continua prometendo.</p>
     */
    public static final double ALTURA_MINIMA_DO_VENTRE_DE_COSTAS = 0.54D;

    /**
     * Altura relativa a partir da qual o impacto conta como ventre COM O BICHO
     * EMPINADO.
     *
     * <p>Baixa de proposito: empinado, o que fica exposto e a frente inteira, da
     * boca do ventre ao alto dos chifres. Os 0.22 excluem apenas a linha das
     * pernas traseiras PLANTADAS no chao (y 0..5 px de 22.4, ou seja ate 0.22) --
     * um tiro rasante que acerta o pe nao pode pagar o mesmo que um golpe na
     * barriga erguida.</p>
     */
    public static final double ALTURA_MINIMA_DO_VENTRE_EMPINADO = 0.22D;

    /**
     * Meia-abertura, em graus, em que o ventre empinado fica exposto.
     *
     * <p>Cinquenta e cinco graus e o arco em que a barriga erguida de fato aponta
     * para o jogador. Alargar para 90 faria o critico pagar pelo flanco de um
     * bicho que esta olhando para outro lugar, e a licao -- encarar o telegrafo,
     * que e o lugar mais perigoso do encontro -- deixaria de custar alguma coisa.
     * Isso nao da erro: da um chefe que se derruba sozinho e ninguem sabe por
     * que.</p>
     */
    public static final double MEIA_ABERTURA_DO_VENTRE_EMPINADO_EM_GRAUS = 55.0D;

    // ------------------------------------------------------------- o tombo

    /**
     * Quanto tempo ele fica de costas, em ticks.
     *
     * <p>Tres segundos. E a JANELA DO JOGADOR e o unico premio que o encontro
     * paga, entao ela precisa caber duas espadadas confortavelmente -- nao
     * apertadas. Encurtar isto sem mexer em {@link #TICKS_PARA_LEVANTAR}
     * transformaria o quebra-cabeca em sorte: o jogador faria tudo certo e as
     * vezes chegaria tarde, sem nada na tela explicando a diferenca.</p>
     */
    public static final int TICKS_DE_COSTAS = 60;

    /**
     * Os ticks FINAIS da janela em que ele ja esta se endireitando.
     *
     * <p>Quase um segundo de aviso visivel. Ele existe porque a janela e curta e
     * silenciosa: sem uma pose distinta no fim, a leitura do jogador seria "de
     * costas" ate o instante em que a investida seguinte acerta. O cliente escolhe
     * o clipe por este numero -- ver {@code EstadoDeViragem.levantando()}.</p>
     */
    public static final int TICKS_PARA_LEVANTAR = 18;

    // ---------------------------------------------------------- a investida
    //
    // A FORMA e a ficha: aviso longo, janela curta, recuperacao longa. O aviso nao
    // e so telegrafo -- ele e a POSE em que o bicho pode ser derrubado a pancada,
    // e por isso ele e o trecho mais longo dos tres. Um windup mais curto que a
    // janela deixaria de ser aviso e viraria um mob que bate sem telegrafo, com o
    // mesmo dano, o mesmo cooldown e o mesmo log limpo.

    /** Ticks de aviso: 1.3 s empinado, com os chifres erguidos. */
    public static final int WINDUP_DA_INVESTIDA = 26;
    /** Ticks em que a investida machuca. Curto: o desvio precisa ser possivel. */
    public static final int JANELA_DA_INVESTIDA = 8;
    /** Ticks de recuperacao: a janela em que o jogador pune sem risco. */
    public static final int RECUPERACAO_DA_INVESTIDA = 22;

    /**
     * Quantos ticks do aviso ele ainda MIRA antes de travar a direcao.
     *
     * <p>Quatorze dos vinte e seis. Depois disso a investida esta comprometida e a
     * direcao nao muda mais -- e e justamente por isso que ela pode ERRAR, que e a
     * outra metade do encontro. Mirar o aviso inteiro transformaria a investida em
     * mira-laser: o besouro giraria junto com quem desvia, a investida nunca
     * erraria, e {@code DecisaoDeViragem.VIRA_PELA_INVESTIDA} deixaria de
     * acontecer sem que nada acusasse -- metade das formas de virar o bicho
     * sumiria em silencio.</p>
     */
    public static final int TICKS_DE_MIRA_NO_WINDUP = 14;

    /** Empurrao de quem leva a chifrada; ele joga, nao empurra. */
    public static final float EMPURRAO_DA_INVESTIDA = 0.9F;

    /**
     * Impulso horizontal aplicado UMA vez, no primeiro tick da janela ACTIVE.
     *
     * <p>Uma vez, e nao a cada tick: a investida e um arranco, e um empurrao por
     * tick daria uma aceleracao constante que atravessa o alvo e continua. O atrito
     * do jogo cuida do resto, e e disso que sai {@link #AVANCO_ESTIMADO_DA_INVESTIDA}.</p>
     */
    public static final double VELOCIDADE_DA_INVESTIDA = 0.9D;

    /**
     * Quanto o arranco avanca, em blocos -- ESTIMATIVA, e declarada como tal.
     *
     * <p>Sai do atrito horizontal de mob em chao comum (0.6 de aderencia * 0.91),
     * somando os oito ticks da janela: 0.9 * (1 - 0.546^8) / (1 - 0.546), que da
     * perto de 1.98. O 1.8 aqui e a versao conservadora desse numero.</p>
     *
     * <p><b>Ele NAO foi medido em jogo</b>, e esta escrito assim para que ninguem
     * o leia como medida. Ele existe para uma unica coisa: o portao de
     * {@code KingWhiteStagBeetleTuningTest} conferir que
     * {@link #ALCANCE_DA_INVESTIDA} nao e maior do que o arranco consegue cobrir.
     * Se for, o besouro comeca a investida contra alguem que ele nunca vai
     * alcancar, erra TODA vez e se derruba sozinho para sempre -- o que nao da
     * erro nenhum e parece exatamente com um chefe quebrado.</p>
     */
    public static final double AVANCO_ESTIMADO_DA_INVESTIDA = 1.8D;

    /**
     * Distancia (centro a centro) em que ele decide investir.
     *
     * <p>Ela e conferida contra {@code caixaDaInvestida().maxZ()} mais a
     * meia-largura do alvo mais o arranco -- ver
     * {@link #AVANCO_ESTIMADO_DA_INVESTIDA}. A folga que sobra e pequena de
     * proposito: e ela que faz o desvio depender de sair da linha, e nao de um
     * passo lateral de meio bloco.</p>
     */
    public static final double ALCANCE_DA_INVESTIDA = 3.0D;

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
     * Recarga imposta a quem interrompeu a investida.
     *
     * <p>Mais longa que a recarga normal (55): interromper um bicho deste tamanho
     * no meio do aviso tem de VALER. Sem isso, {@code reset()} devolveria a fase
     * para IDLE e ele poderia recomecar no tick seguinte -- e o jogador aprenderia
     * a nao interromper, que e o oposto do que a pose empinada existe para
     * ensinar.</p>
     */
    public static final int RECARGA_APOS_INTERRUPCAO = 80;

    /**
     * Alcance do traco que procura o ponto de impacto na caixa do bicho.
     *
     * <p>Seis blocos. A caixa tem 1.4 de altura e o jogador mira de cima para
     * baixo, entao o traco entra perto. Curto demais, o traco nao alcanca, o codigo
     * cai na posicao de reserva e TODO acerto no ventre viraria carapaca -- sem
     * erro, e com o ponto fraco simplesmente nao funcionando.</p>
     */
    public static final double ALCANCE_DO_TRACO = 6.0D;

    // ------------------------------------------------------------ percepcao

    /** Ticks de memoria de alvo; e tambem o prazo do {@code ThreatMemory}. */
    public static final int MEMORIA_DE_ALVO_TICKS = 120;
    /** Ticks de aviso antes de o cerebro passar de WARN para ENGAGE. */
    public static final int TICKS_DE_AVISO = 20;
    /**
     * Meia-abertura do campo de visao, em graus.
     *
     * <p>Larga: olho composto de besouro enxerga quase em volta. Ela e LARGA de
     * proposito para que o encontro nao possa ser resolvido circulando -- essa e a
     * resposta do Cyclops, e a deste bicho e outra. Estreitar isto aqui daria duas
     * saidas para o mesmo quebra-cabeca e apagaria a que tem nome.</p>
     */
    public static final double MEIA_ABERTURA_DA_VISAO_EM_GRAUS = 75.0D;
    /** Alcance de audicao em blocos, para som de intensidade 1. */
    public static final double ALCANCE_DE_AUDICAO = 14.0D;

    /**
     * Fracao da vida abaixo da qual o cerebro poderia decidir fugir: ZERO.
     *
     * <p>Ele nao foge, e o zero e a forma de dizer isso com o mesmo campo que os
     * outros mobs usam. A razao nao e bravura: o card dele tem UMA copia no mundo
     * ({@code GreedIslandProfiles.cards()}, rank B, 1). Um besouro que fugisse com
     * a vida baixa poderia sair do chunk carregado e levar embora a unica copia do
     * card -- e isso nao daria erro nenhum, daria um item que existe na tabela e
     * nunca aparece no jogo.</p>
     */
    public static final float FRACAO_DE_VIDA_CRITICA = 0.0F;

    // ------------------------------------------------------------- montagem

    /** O cone de visao, derivado da meia-abertura declarada acima. */
    public static VisionCone coneDeVisao(double alcance) {
        return VisionCone.deGraus(alcance, MEIA_ABERTURA_DA_VISAO_EM_GRAUS);
    }

    /**
     * O ventre com o bicho EMPINADO: altura baixa, angulo frontal exigido.
     *
     * <p>Os dois pisos sao lidos juntos: acima da linha das pernas plantadas E na
     * frente. Um deles sozinho pagaria pelo chao ou pelo flanco.</p>
     */
    public static WeakPointResolver ventreEmpinado() {
        return new WeakPointResolver(REGIAO_DO_VENTRE, REGIAO_DA_CARAPACA,
                ALTURA_MINIMA_DO_VENTRE_EMPINADO,
                Math.cos(Math.toRadians(MEIA_ABERTURA_DO_VENTRE_EMPINADO_EM_GRAUS)));
    }

    /**
     * O ventre com o bicho DE COSTAS: altura alta, e NENHUM angulo.
     *
     * <p>O -1 no cosseno nao e descuido e nao e "qualquer valor serve": ele e a
     * afirmacao de que um besouro de pernas para o ar nao tem frente.
     * {@code RegrasDeViragem} cobra esse -1 no construtor, para que ninguem o
     * "corrija" para um numero que parece mais cuidadoso e feche metade dos
     * angulos da unica janela segura do encontro.</p>
     */
    public static WeakPointResolver ventreDeCostas() {
        return new WeakPointResolver(REGIAO_DO_VENTRE, REGIAO_DA_CARAPACA,
                ALTURA_MINIMA_DO_VENTRE_DE_COSTAS, -1.0D);
    }

    /** As regras completas da postura, montadas a partir das constantes acima. */
    public static RegrasDeViragem regrasDeViragem() {
        return new RegrasDeViragem(ventreEmpinado(), ventreDeCostas(),
                TICKS_DE_COSTAS, TICKS_PARA_LEVANTAR);
    }

    /** O catalogo: so o ventre vale multiplicador; a carapaca e corpo comum. */
    public static WeakPointRegistry pontosFracos() {
        return new WeakPointRegistry(Map.of(REGIAO_DO_VENTRE,
                new WeakPoint(REGIAO_DO_VENTRE, "ventre", MULTIPLICADOR_DO_VENTRE, true)));
    }

    /**
     * A investida de chifres.
     *
     * <p>O windup e interrompivel e a JANELA nao: depois que os chifres saem, eles
     * saem. Interromper durante os 8 ticks ativos faria o besouro cancelar um golpe
     * que o jogador ja viu partir -- e, pior aqui do que em qualquer outro mob,
     * apagaria a investida ERRADA, que e uma das duas formas de virar o bicho.</p>
     */
    public static AttackDefinition investida() {
        return new AttackDefinition("king_white_stag_beetle_investida", WINDUP_DA_INVESTIDA,
                JANELA_DA_INVESTIDA, RECUPERACAO_DA_INVESTIDA,
                GreedIslandProfiles.kingWhiteStagBeetle().attributes().attackDamage(),
                EMPURRAO_DA_INVESTIDA, true, false, true);
    }

    /**
     * Caixa da investida, em coordenadas LOCAIS. A FRENTE E +Z.
     *
     * <p><b>+Z, e isto nao e distracao.</b> Na GEOMETRIA do modelo (formato
     * Bedrock) a frente e -Z; na transformacao de {@link AttackHitbox}, que e
     * matematica de mundo, com yaw 0 o olhar vanilla aponta para +Z. Misturar as
     * duas ja custou um bug neste repositorio: a caixa ficou ATRAS do mob, que
     * atacava, animava e nao encostava em quem estava na frente. Ver
     * {@code DummyEnemyEntity.CAIXA_DO_GOLPE}.</p>
     *
     * <p>O {@code maxZ} de 1.0 nao e um numero solto: os chifres DESENHADOS
     * alcancam 17 px (1.06 bloco) a frente do centro, e
     * {@code king_white_stag_beetle_geo.py} reprova se a caixa passar do desenho.
     * Caixa maior que a pinca da um jogador que apanha de um chifre que, na tela,
     * parou antes dele.</p>
     *
     * <p>O arco e largo em X (2 blocos) porque a pinca varre, e nao aponta. E o
     * {@code maxY} de 1.1 e baixo de proposito: este bicho tem 1.4 de altura e
     * ataca RENTE AO CHAO -- uma caixa alta acertaria alguem que passou por cima
     * dele, que e a leitura contraria a que a silhueta entrega.</p>
     */
    public static AttackHitbox caixaDaInvestida() {
        return new AttackHitbox(-1.0D, 0.1D, 0.4D, 1.0D, 1.1D, 1.0D);
    }
}
