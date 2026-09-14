package com.darkcontinent.nenfoundation.enemy.content;

import com.darkcontinent.nenfoundation.enemy.combat.AttackDefinition;
import com.darkcontinent.nenfoundation.enemy.combat.AttackHitbox;
import com.darkcontinent.nenfoundation.enemy.combat.RegrasDeTeia;
import com.darkcontinent.nenfoundation.enemy.perception.VisionCone;

/**
 * Os numeros da Spider Webber que nascem com o COMPORTAMENTO dela.
 *
 * <p><b>Por que este arquivo existe em vez de mais metodos em
 * {@link ChimeraProfiles}.</b> Aquele arquivo guarda a ficha publicada das
 * formigas inteiras -- atributos, spawn, molde, recarga, stagger -- e neste
 * momento varias frentes escrevem o comportamento de oficiais diferentes ao
 * mesmo tempo. Todas precisariam acrescentar metodos no MESMO arquivo, e o
 * resultado de um merge assim nao e um conflito barulhento: e uma resolucao
 * apressada em que o metodo de alguem some. Metodo que some nao da erro de
 * compilacao quando o chamador some junto -- da um oficial que perdeu a teia e
 * continua nascendo, andando e passando em todo portao.</p>
 *
 * <p><b>O que fica la e o que fica aqui.</b> Em {@code ChimeraProfiles} ficam a
 * ficha publicada, a recarga e o stagger, que outros sistemas ja leem. Aqui ficam
 * os numeros NOVOS, os que este comportamento inaugurou: os dois alcances da
 * teia, o prazo e o limiar de rompimento, a forma do telegrafo e a area que o
 * servidor resolve.</p>
 *
 * <p><b>O dano da teia nao e um numero proprio.</b> Ele e uma FRACAO de
 * {@code ChimeraProfiles.spiderWebber().attributes().attackDamage()}. Repetir o
 * 10 aqui criaria duas fontes para a mesma verdade, e girar o atributo numa
 * sessao de balanceamento mudaria a barra de vida do jogador sem mudar este
 * arquivo. A fracao e baixa de proposito: o perigo desta formiga e o jogador
 * PARADO quando o resto do esquadrao chega, e dano alto faria a teia virar
 * enfeite porque ninguem repararia no que ela custou.</p>
 */
public final class SpiderWebberTuning {

    private SpiderWebberTuning() { }

    // ------------------------------------------------------------------ teia
    //
    // OS DOIS ALCANCES SE LEEM JUNTOS, e e por isso que eles viram um
    // RegrasDeTeia: e o record que impede a faixa de tiro de ficar mais estreita
    // do que um alvo andando consegue atravessar, e que garante que o lugar onde
    // ela PARA seja o mesmo lugar de onde ela ATIRA.

    /**
     * Zona morta, em blocos (centro a centro).
     *
     * <p>Abaixo disto nao ha fio a esticar, e esta e a licao do encontro: quem
     * encosta na Spider Webber desliga a teia. Ela e uma atiradora e NAO ganhou um
     * golpe corpo-a-corpo de consolo -- um golpe assim apagaria a unica resposta
     * que o jogador tem contra a imobilizacao, e o mob continuaria funcionando
     * perfeitamente, so sem ensinar nada.</p>
     *
     * <p>Tres blocos e meio e mais do que a largura de um jogador somada ao corpo
     * desenhado dela: a zona onde a recusa vale e ocupavel de verdade. O gerador
     * de geometria cobra exatamente isso.</p>
     */
    public static final double ALCANCE_MINIMO_DA_TEIA = 3.5D;

    /**
     * Alcance util do fio, em blocos (centro a centro).
     *
     * <p>Nove blocos ficam bem dentro do {@code followRange} de 28: ela ENXERGA
     * muito mais longe do que alcanca, e essa diferenca e o que produz a
     * aproximacao. Igualar os dois faria o oficial atirar no instante em que
     * percebe alguem, de qualquer distancia, e o papel de alcance viraria
     * artilharia sem contraparte.</p>
     */
    public static final double ALCANCE_MAXIMO_DA_TEIA = 9.0D;

    /**
     * Quanto tempo a presa fica presa, em ticks.
     *
     * <p>Tres segundos e o suficiente para um peon atravessar a distancia de um
     * esquadrao e nao e suficiente para matar um jogador de vida cheia. E esse o
     * ajuste: a teia tem de DAR a vantagem ao grupo, e nao resolver a briga
     * sozinha. O teto de {@link RegrasDeTeia#TETO_DE_IMOBILIZACAO_EM_TICKS} existe
     * para que ninguem transforme essa vantagem em sentenca num ajuste de tarde.</p>
     */
    public static final int TICKS_DE_IMOBILIZACAO = 60;

    /**
     * Dano na fiandeira que rasga o fio antes do prazo.
     *
     * <p>A SAIDA ATIVA. Oito de dano sao um golpe bom ou dois medianos: o preso
     * com arma de alcance se solta sozinho, e quem esta sem ela depende de um
     * companheiro -- que e exatamente a leitura de um encontro de esquadrao. Sem
     * esta saida o jogo certo seria ESPERAR, e esperar e o que o esquadrao
     * quer.</p>
     */
    public static final float DANO_QUE_LIBERTA = 8.0F;

    // ------------------------------------------------------------- telegrafo
    //
    // A FORMA e a ficha: aviso longo, janela curta, recuperacao longa. Um aviso
    // mais curto que a janela deixa de ser telegrafo e vira uma teia que prende
    // sem avisar -- com o mesmo alcance, o mesmo cooldown e o mesmo log limpo.

    /** Ticks de aviso: um segundo e meio com o abdome erguido e a fiandeira mirando. */
    public static final int WINDUP_DA_TEIA = 30;
    /** Ticks em que o fio existe. Curto: sair do cone precisa ser possivel. */
    public static final int JANELA_DA_TEIA = 6;
    /** Ticks de recuperacao: a janela em que o jogador fecha a distancia e a desliga. */
    public static final int RECUPERACAO_DA_TEIA = 20;

    /**
     * Quantos ticks do aviso ela ainda MIRA antes de travar a direcao.
     *
     * <p>Quatorze dos trinta. Depois disso o fio esta comprometido e a direcao nao
     * muda mais. Mirar o aviso inteiro transformaria a teia em mira-laser: ela
     * giraria junto com quem desvia, o desvio deixaria de existir e o telegrafo de
     * um segundo e meio viraria decoracao. Nao mirar nada faria o contrario, um
     * oficial que erra sozinho. Nenhum dos dois da erro.</p>
     */
    public static final int TICKS_DE_MIRA_NO_WINDUP = 14;

    /**
     * Fracao do ATTACK_DAMAGE que a teia cobra.
     *
     * <p>Um quinto de 10 sao 2 de dano. O numero e baixo de proposito e nao e um
     * dano proprio: e uma fracao do atributo, para que o balanceamento continue
     * tendo UM lugar. Subir esta fracao nao quebra nada -- so troca o mob por
     * outro, em que a teia e o dano competem pela atencao do jogador e a
     * imobilizacao deixa de ser a informacao.</p>
     */
    public static final float FRACAO_DE_DANO_DA_TEIA = 0.2F;

    /**
     * Empurrao da teia: ZERO, e o zero e a decisao.
     *
     * <p>Um golpe que empurra e um golpe que AFASTA o alvo -- e afastar e
     * exatamente o oposto de prender. Com knockback, a vitima sairia da area no
     * mesmo tick em que foi pega, e metade das teias pareceria "nao ter pegado".
     * O campo existe no {@link AttackDefinition} e o valor tinha de ser escolhido;
     * escolher zero em silencio deixaria a proxima pessoa achando que alguem
     * esqueceu.</p>
     */
    public static final float EMPURRAO_DA_TEIA = 0.0F;

    /**
     * Recarga imposta a quem interrompeu o lancamento.
     *
     * <p>Mais longa que a recarga normal (55): interromper o aviso tem de VALER.
     * Sem isso, o {@code reset} devolveria a fase para IDLE e ela recomecaria no
     * tick seguinte -- o jogador aprenderia a nao interromper, que e o oposto do
     * que o telegrafo longo existe para ensinar.</p>
     */
    public static final int RECARGA_APOS_INTERRUPCAO = 80;

    // ----------------------------------------------------------- espacamento

    /**
     * Multiplicador de velocidade ao fechar distancia.
     *
     * <p>Um: ela aproxima no proprio passo. Aproximar correndo faria o oficial de
     * alcance chegar ao corpo-a-corpo, que e o lugar onde ele nao funciona.</p>
     */
    public static final double VELOCIDADE_DE_APROXIMACAO = 1.0D;

    /**
     * Multiplicador de velocidade ao recuar.
     *
     * <p>Maior que o de aproximacao, e essa assimetria E o comportamento: se ela
     * recuasse no mesmo passo em que o jogador avanca, a zona morta seria
     * permanente assim que alguem encostasse uma vez, e o mob viraria um saco de
     * pancadas com animacao de fuga.</p>
     */
    public static final double VELOCIDADE_DE_RECUO = 1.25D;

    /** Raio, em blocos, em que ela procura um ponto para tras. */
    public static final int RAIO_DO_RECUO = 8;
    /** Variacao vertical aceita nesse ponto -- ela e terrestre, nao escala torres. */
    public static final int ALTURA_DO_RECUO = 4;

    /**
     * Metade do arco em que a teia se abre, em blocos.
     *
     * <p>Larga porque rede varre em vez de acertar um ponto -- e porque ela e a
     * unica coisa que este mob faz. Um arco estreito transformaria o unico ataque
     * dela num tiro de precisao contra um alvo que se mexe, e o oficial passaria o
     * encontro errando.</p>
     */
    public static final double MEIA_LARGURA_DA_AREA = 2.5D;

    /**
     * Altura da area da teia, em blocos.
     *
     * <p>Dois e vinte cobrem um jogador de pe e o comeco de um pulo. Mais baixa, o
     * pulo viraria a resposta -- e pular nao pode ser a resposta, porque ela e
     * gratuita e some com o encontro. Bem mais alta, a rede pegaria quem esta num
     * andar acima, e a leitura de "ela mira para frente" quebraria.</p>
     */
    public static final double ALTURA_DA_AREA = 2.2D;

    /**
     * Meia-largura do alvo tipico -- um jogador tem 0.6 de lado.
     *
     * <p>Nao e botao de balanceamento: e a conversao entre as duas reguas que este
     * arquivo usa. A distancia de DECISAO e medida de centro a centro
     * ({@code distanceTo}); a area da teia e testada contra a BORDA do alvo
     * ({@code AABB.intersects}). Sem esta conversao as duas parecem comparaveis e
     * nao sao -- e o sintoma e uma aranha que aprova o tiro e nao encosta em
     * ninguem no limite da faixa.</p>
     */
    public static final double MEIA_LARGURA_DE_UM_ALVO = 0.3D;

    // ------------------------------------------------------- imobilizacao

    /**
     * Duracao, em ticks, de cada pulso de lentidao aplicado na presa.
     *
     * <p><b>Por que PULSO curto e nao um efeito longo removido na soltura.</b>
     * Remover um efeito na saida apaga tambem a lentidao que a vitima ja tinha por
     * outro motivo -- e isso nao da erro, da um jogador curado de uma pocao pela
     * aranha. Um pulso de meio segundo reaplicado a cada tick some sozinho depois
     * da soltura: quem liga nao precisa desligar, porque o proprio efeito
     * expira.</p>
     *
     * <p>Curto tambem porque ele e a SEGUNDA metade da imobilizacao. A primeira e
     * a velocidade zerada no servidor; o efeito existe para que o CLIENTE tambem
     * pare, sem o qual a predicao do cliente e a correcao do servidor brigariam e
     * o jogador preso apareceria tremendo no lugar.</p>
     */
    public static final int TICKS_DO_PULSO_DE_LENTIDAO = 10;

    /**
     * Amplificador da lentidao aplicada na presa.
     *
     * <p>Seis, e o numero tem conta atras: a lentidao vanilla tira 15% da
     * velocidade por nivel, entao o nivel 7 (amplificador 6) zera o passo. Menos
     * que isso deixaria a presa andando devagar, o que e outro mob -- e o
     * servidor, que zera a velocidade, discordaria do cliente, que ainda anda.</p>
     */
    public static final int FORCA_DA_LENTIDAO = 6;

    // --------------------------------------------------------------- percepcao

    /** Ticks de memoria de alvo; e tambem o prazo do {@code ThreatMemory}. */
    public static final int MEMORIA_DE_ALVO_TICKS = 140;
    /** Ticks de aviso antes de o cerebro passar de WARN para ENGAGE. */
    public static final int TICKS_DE_AVISO = 20;

    /**
     * Meia-abertura do campo de visao, em graus.
     *
     * <p>Larga -- oito olhos, e a ficha diz INSECTOID de oito patas. Ela ve quase
     * tudo a frente e depende disso: um atirador com cone estreito perderia o alvo
     * exatamente enquanto recua, porque recuar e andar de costas.</p>
     */
    public static final double MEIA_ABERTURA_DA_VISAO_EM_GRAUS = 80.0D;

    /** Alcance de audicao em blocos, para som de intensidade 1. */
    public static final double ALCANCE_DE_AUDICAO = 16.0D;

    /** Fracao da vida abaixo da qual o cerebro pode decidir fugir. */
    public static final float FRACAO_DE_VIDA_CRITICA = 0.25F;

    // ------------------------------------------------------------- montagem

    /**
     * As regras da teia: os dois alcances, o prazo e o limiar, num objeto so.
     *
     * <p>Montadas aqui e em lugar nenhum mais. Um segundo {@code new RegrasDeTeia}
     * escrito dentro da entidade seria a segunda fonte para os mesmos quatro
     * numeros, e a divergencia apareceria como uma aranha que recua ate uma
     * distancia em que ela se recusa a atirar.</p>
     */
    public static RegrasDeTeia regras() {
        return new RegrasDeTeia(ALCANCE_MINIMO_DA_TEIA, ALCANCE_MAXIMO_DA_TEIA,
                TICKS_DE_IMOBILIZACAO, DANO_QUE_LIBERTA);
    }

    /**
     * O cone de visao, com o alcance vindo do perfil.
     *
     * @param alcance normalmente {@code attributes().followRange()}
     */
    public static VisionCone coneDeVisao(double alcance) {
        return VisionCone.deGraus(alcance, MEIA_ABERTURA_DA_VISAO_EM_GRAUS);
    }

    /**
     * O lancamento da teia.
     *
     * <p>O windup e interrompivel e a JANELA nao: depois que o fio sai, ele sai.
     * Interromper durante os 6 ticks ativos faria a aranha cancelar um lancamento
     * que o jogador ja viu comecar, e a leitura -- que e a unica coisa que um
     * ataque telegrafado entrega -- deixaria de valer. A recuperacao e
     * interrompivel porque e ela a janela de punicao.</p>
     */
    public static AttackDefinition teia() {
        return new AttackDefinition("web", WINDUP_DA_TEIA, JANELA_DA_TEIA, RECUPERACAO_DA_TEIA,
                ChimeraProfiles.spiderWebber().attributes().attackDamage() * FRACAO_DE_DANO_DA_TEIA,
                EMPURRAO_DA_TEIA, true, false, true);
    }

    /**
     * A area da teia, em coordenadas LOCAIS. A FRENTE E +Z.
     *
     * <p><b>+Z, e isto nao e distracao.</b> Na GEOMETRIA do modelo (formato
     * Bedrock) a frente e -Z; na transformacao de {@link AttackHitbox}, que e
     * matematica de mundo, com yaw 0 o olhar vanilla aponta para +Z. Misturar as
     * duas ja custou um bug neste repositorio: a caixa ficou ATRAS do mob, que
     * atacava, animava e nao encostava em quem estava na frente.</p>
     *
     * <p><b>Ela e uma AREA TELEGRAFADA, e nao um projetil -- e a escolha tem
     * custo.</b> Um projetil proprio seria melhor: o fio teria tempo de voo, o
     * jogador poderia sair do caminho DEPOIS do lancamento, e quem estivesse atras
     * de uma quina seria poupado pela colisao do projetil em vez de pela
     * aproximacao conservadora de uma AABB. O que impede e registro: entidade de
     * projetil precisa de {@code EntityType} proprio, e o pacote
     * {@code enemy/registry} nao pertence a esta frente. Resolver no servidor,
     * como a caixa de golpe do Cyclops, entrega o mesmo contrato -- servidor mede,
     * servidor decide -- com uma janela de esquiva que comeca e termina no
     * telegrafo, e nao no voo.</p>
     *
     * <p>Os dois limites em Z sao DERIVADOS dos alcances de decisao, e nao
     * escritos a mao. Escritos a mao, eles divergiriam do {@link #regras()} na
     * primeira correcao: a aranha aprovaria o tiro a nove blocos e a area pararia
     * a oito, e ela erraria sozinha no limite da faixa sem nenhum erro no log.</p>
     */
    public static AttackHitbox areaDaTeia() {
        return new AttackHitbox(-MEIA_LARGURA_DA_AREA, 0.0D,
                ALCANCE_MINIMO_DA_TEIA - MEIA_LARGURA_DE_UM_ALVO,
                MEIA_LARGURA_DA_AREA, ALTURA_DA_AREA,
                ALCANCE_MAXIMO_DA_TEIA + MEIA_LARGURA_DE_UM_ALVO);
    }
}
