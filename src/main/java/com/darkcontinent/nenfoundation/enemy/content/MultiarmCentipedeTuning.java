package com.darkcontinent.nenfoundation.enemy.content;

import com.darkcontinent.nenfoundation.enemy.chimera.nen.TacticalNenIntent;
import com.darkcontinent.nenfoundation.enemy.combat.AttackDefinition;
import com.darkcontinent.nenfoundation.enemy.combat.AttackHitbox;
import com.darkcontinent.nenfoundation.enemy.combat.GolpeEncadeado;
import com.darkcontinent.nenfoundation.enemy.combat.SequenciaDeGolpes;
import java.util.List;

/**
 * Os numeros do Multiarm Centipede que nascem com o COMPORTAMENTO dele.
 *
 * <p><b>Por que este arquivo existe em vez de mais metodos em
 * {@link ChimeraProfiles}.</b> Aquele arquivo e a fila unica de todas as formigas
 * quimera, e neste momento varias frentes escrevem o comportamento de formigas
 * diferentes ao mesmo tempo. Todas precisariam acrescentar metodos no MESMO bloco
 * de linhas, e o resultado de um merge assim nao e um conflito barulhento -- e
 * uma resolucao apressada em que o metodo de alguem some. Metodo que some nao da
 * erro de compilacao quando o chamador some junto: da um mob que perdeu a
 * sequencia e continua nascendo, atacando uma vez so e passando em todo
 * portao.</p>
 *
 * <p><b>O que fica la e o que fica aqui.</b> Em {@code ChimeraProfiles} ficam a
 * ficha publicada (atributos, spawn), a recarga entre sequencias e o stagger --
 * tudo que ja estava escrito e que outros sistemas leem. Aqui ficam os numeros
 * NOVOS, os que este comportamento inaugurou: a forma dos tres telegrafos, a
 * emenda entre os golpes, as tres caixas e a fracao de dano do golpe encadeado.</p>
 *
 * <p><b>O dano cheio nao e um numero proprio.</b> Ele e lido de
 * {@code ChimeraProfiles.multiarmCentipede().attributes().attackDamage()}, e o
 * golpe encadeado e uma FRACAO dele. Repetir o 13 aqui criaria duas fontes para a
 * mesma verdade, e girar o atributo numa sessao de balanceamento mudaria a barra
 * de vida do jogador sem mudar este arquivo.</p>
 */
public final class MultiarmCentipedeTuning {

    private MultiarmCentipedeTuning() { }

    // ------------------------------------------------------------- percepcao

    /** Ticks de memoria de alvo; e tambem o prazo do {@code ThreatMemory}. */
    public static final int MEMORIA_DE_ALVO_TICKS = 140;
    /** Ticks de aviso antes de o cerebro passar de WARN para ENGAGE. */
    public static final int TICKS_DE_AVISO = 20;
    /** Meia-abertura do campo de visao, em graus. Larga: ela tem olhos laterais. */
    public static final double ABERTURA_DA_VISAO_EM_GRAUS = 80.0D;
    /** Alcance de audicao em blocos, para som de intensidade 1. */
    public static final double ALCANCE_DE_AUDICAO = 16.0D;
    /** Fracao da vida abaixo da qual o cerebro pode decidir fugir. */
    public static final float FRACAO_DE_VIDA_CRITICA = 0.25F;

    // -------------------------------------------------------- os tres golpes
    //
    // A FORMA E A FICHA: dois golpes rapidos e um terceiro longo. Os tres
    // telegrafos sao PROPRIOS -- cada janela tem o seu -- e o do ultimo e mais que
    // o DOBRO dos outros. E esse degrau, e nao o dano, que ensina o jogador a
    // esperar a ultima. SequenciaDeGolpes reprova se ele for invertido.

    /** Aviso do primeiro golpe: o par de bracos traseiro, o mais curto dos tres. */
    public static final int WINDUP_DO_BRACO_TRASEIRO = 12;
    /** Ticks em que o primeiro golpe existe. */
    public static final int JANELA_DO_BRACO_TRASEIRO = 4;
    /**
     * Recuperacao do primeiro golpe.
     *
     * <p>Ela quase nunca e tocada inteira: o golpe seguinte a corta depois de
     * {@link #TICKS_DE_EMENDA}. Ela existe com oito ticks porque e o que da folga
     * para a emenda caber dentro dela -- uma recuperacao de tres ticks faria o
     * encadeado chegar depois de a linha do tempo fechar, e o mob dispararia UM
     * golpe e ficaria parado a recarga inteira.</p>
     */
    public static final int RECUPERACAO_DO_BRACO_TRASEIRO = 8;

    /** Aviso do segundo golpe: o par medio. Um tick mais rapido que o primeiro. */
    public static final int WINDUP_DO_BRACO_MEDIO = 11;
    /** Ticks em que o segundo golpe existe. */
    public static final int JANELA_DO_BRACO_MEDIO = 4;
    /** Recuperacao do segundo golpe; tambem cortada pela emenda do terceiro. */
    public static final int RECUPERACAO_DO_BRACO_MEDIO = 8;

    /**
     * Aviso do golpe FINAL: o par dianteiro, o longo.
     *
     * <p>Vinte e quatro ticks, mais que o dobro dos outros dois. E ele que diz ao
     * jogador que a sequencia vai acabar -- e, portanto, que a janela de punicao
     * esta chegando. Encurtar isto para doze nao quebraria nada, nao reprovaria
     * portao nenhum (o construtor so exige que seja o maior), e apagaria a unica
     * coisa que este mob ensina: o jogador perderia a marca de onde o combo
     * termina e passaria a punir no meio dele.</p>
     */
    public static final int WINDUP_DO_BRACO_DIANTEIRO = 24;
    /** Ticks em que o golpe final existe. Um a mais que os encadeados: ele varre. */
    public static final int JANELA_DO_BRACO_DIANTEIRO = 5;
    /**
     * Recuperacao do golpe final -- A JANELA DE PUNICAO.
     *
     * <p>Vinte e seis ticks, mais de tres vezes a dos encadeados. Esta e a unica
     * janela em que o jogador pode bater sem apanhar de volta, e ela e a razao de
     * toda a sequencia existir. Encurtar isto e a forma mais silenciosa de apagar
     * o encontro: o combo continua saindo, o telegrafo continua longo, e a
     * recompensa por ter esperado simplesmente nao chega.</p>
     */
    public static final int RECUPERACAO_DO_BRACO_DIANTEIRO = 26;

    /**
     * Ticks de recuperacao que passam antes de o proximo golpe emendar.
     *
     * <p>Quatro. Ele tem de ser maior que zero -- sem nenhum quadro entre dois
     * golpes, os dois leem como um movimento so e o jogador nao consegue CONTAR
     * quantos foram -- e menor que a recuperacao dos golpes encadeados, senao o
     * encadeado chega depois de a linha do tempo fechar em COMPLETE e o combo
     * nunca acontece. As duas pontas sao cobradas por {@code SequenciaDeGolpes}.</p>
     */
    public static final int TICKS_DE_EMENDA = 4;

    /**
     * Quanto do dano da ficha cada golpe ENCADEADO entrega.
     *
     * <p>Os dois primeiros golpes valem 45% do golpe cheio; so o ultimo vale 100%.
     * A razao e a mesma que faz o ultimo ter o telegrafo longo: o combo precisa
     * doer PROGRESSIVAMENTE, senao levar o primeiro golpe ja seria tao caro quanto
     * levar os tres e nao haveria motivo para o jogador tentar sair do meio
     * dele.</p>
     *
     * <p>Subir isto para 1.0 nao quebra nada e nao reprova nenhum construtor: faz
     * a sequencia inteira tirar 39 de vida em pouco mais de dois segundos, e o
     * relato que chega e "esse mob mata do nada". Por isso
     * {@link #TETO_DA_SEQUENCIA_EM_GOLPES} existe e e medido em teste.</p>
     */
    public static final float FRACAO_DO_GOLPE_ENCADEADO = 0.45F;

    /**
     * Teto do combo inteiro, em multiplos do golpe cheio da ficha.
     *
     * <p>REGUA, e nao botao: ela nasce junto com a sequencia porque uma sequencia
     * sem teto e um numero que ninguem mede. Duas vezes o golpe unico e o limite
     * do que um oficial ELITE pode tirar de quem ficou parado durante tres
     * telegrafos; acima disso o encontro deixa de ter margem para erro e o
     * jogador nao consegue dizer qual dos tres golpes o matou.</p>
     */
    public static final float TETO_DA_SEQUENCIA_EM_GOLPES = 2.0F;

    /** Empurrao dos golpes encadeados. Pequeno: afastar o alvo cortaria o combo. */
    public static final float EMPURRAO_DO_ENCADEADO = 0.25F;
    /** Empurrao do golpe final. Grande: ele e o ponto final da frase. */
    public static final float EMPURRAO_DO_FINAL = 0.9F;

    // --------------------------------------------------------------- alcance

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

    /**
     * Distancia (centro a centro) em que ela decide comecar a sequencia.
     *
     * <p>Ela tem de ser MENOR que {@code maxZ do PRIMEIRO golpe +
     * MEIA_LARGURA_DE_UM_ALVO}, e o teste cobra isso. Maior, ela comeca um combo
     * de quatro segundos contra alguem que o primeiro golpe nao alcanca -- e como
     * a navegacao trava durante a sequencia, os dois primeiros golpes batem no ar
     * sempre. Isso nao da erro nenhum: da um oficial que "as vezes nao faz nada" e
     * parece quebrado.</p>
     */
    public static final double ALCANCE_DA_SEQUENCIA = 1.35D;

    /**
     * Quantos ticks de CADA aviso ela ainda mira antes de travar a direcao.
     *
     * <p>Seis, e tem de caber no MENOR dos tres avisos (11 ticks). Depois disso
     * aquele golpe esta comprometido. Mirar o aviso inteiro transformaria os tres
     * golpes em mira-laser: ela giraria junto com quem desvia, o desvio deixaria
     * de existir e os telegrafos virariam decoracao. Nao mirar nada faria o
     * contrario, um oficial que erra sozinho. Nenhum dos dois da erro.</p>
     */
    public static final int TICKS_DE_MIRA_NO_WINDUP = 6;

    /**
     * Recarga imposta a quem interrompeu a sequencia.
     *
     * <p>Mais longa que a recarga normal entre sequencias (65): cortar um combo de
     * tres golpes tem de VALER mais do que espera-lo terminar. Sem isso,
     * {@code reset()} devolveria a fase para IDLE e ela poderia recomecar a
     * sequencia inteira no tick seguinte -- e o jogador aprenderia a NAO
     * interromper, que e o oposto do que o stagger existe para ensinar.</p>
     */
    public static final int RECARGA_APOS_INTERRUPCAO = 80;

    /** Id da regiao atingida. Este mob nao tem ponto fraco: a carapaca e uniforme. */
    public static final String REGIAO_COMUM = "body";

    /**
     * A fracao de aura que o nucleo publica para uma formiga hoje: NENHUMA.
     *
     * <p><b>Isto e um ponto cego declarado, e nao um valor de balanceamento.</b> O
     * Nen Foundation e a unica autoridade sobre Nen (CLAUDE.md), e ele hoje
     * mantem pool de aura para JOGADOR, nao para entidade. Inventar aqui uma
     * fracao plausivel -- 0.8, 1.0, o que fosse -- seria a segunda autoridade
     * sobre a mesma mecanica, e duas autoridades divergem sem dar erro.</p>
     *
     * <p>Zero e a unica resposta honesta, e ela tem consequencia visivel:
     * {@code TacticalNenController} recusa qualquer intencao com aura abaixo da
     * reserva, entao a formiga responde {@code NENHUMA} enquanto o pool nao
     * existir. A decisao continua sendo CONSULTADA e CONSUMIDA todo tick, para que
     * o dia em que a #145 ligar o pool de verdade nao precise de codigo novo aqui
     * -- so de um numero que passe a vir de quem tem autoridade para calcula-lo.</p>
     */
    public static final double FRACAO_DE_AURA_NAO_PUBLICADA = 0.0D;

    // -------------------------------------------------------------- montagem

    /**
     * Caixa do golpe do par de bracos TRASEIRO -- o primeiro da sequencia.
     *
     * <p><b>+Z, e isto nao e distracao.</b> Na GEOMETRIA do modelo (formato
     * Bedrock) a frente e -Z; na transformacao de {@link AttackHitbox}, que e
     * matematica de mundo, com yaw 0 o olhar vanilla aponta para +Z. Misturar as
     * duas ja custou um bug neste repositorio: a caixa ficou ATRAS do mob, que
     * atacava, animava e nao encostava em quem estava na frente. Ver
     * {@code DummyEnemyEntity.CAIXA_DO_GOLPE}.</p>
     *
     * <p>O {@code maxZ} de 1.10 nao e um numero solto: o par traseiro DESENHADO
     * alcanca 1.12 blocos a frente do centro, e
     * {@code multiarm_centipede_geo.py} reprova se a caixa passar do desenho.</p>
     *
     * <p>O {@code minZ} de 0.5 exclui o proprio corpo dela -- a hitbox tem 0.8 de
     * meia-largura, entao ninguem consegue estar a menos que isso do centro.</p>
     */
    public static AttackHitbox caixaDoBracoTraseiro() {
        return new AttackHitbox(-0.95D, 0.20D, 0.50D, 0.95D, 1.40D, 1.10D);
    }

    /**
     * Caixa do golpe do par MEDIO -- o segundo da sequencia, e ele alcanca mais.
     *
     * <p>O alcance CRESCE ao longo do combo (1.10, 1.40, 1.70) porque os pares
     * desenhados sao progressivamente mais longos, e porque e isso que impede o
     * jogador de escapar do golpe final recuando um passo depois do primeiro.
     * {@code SequenciaDeGolpes} reprova a ordem invertida, e o gerador de arte
     * reprova a caixa que passe do braco.</p>
     */
    public static AttackHitbox caixaDoBracoMedio() {
        return new AttackHitbox(-1.00D, 0.20D, 0.50D, 1.00D, 1.90D, 1.40D);
    }

    /**
     * Caixa do golpe FINAL -- o par dianteiro, com o corpo inteiro atras.
     *
     * <p>A mais alta das tres (de 0 a 2.2) porque este e o golpe em que ela
     * DESABA: o corpo erguido vem junto, e quem esta agachado ou pulando esta
     * igualmente dentro. As outras duas comecam em 0.2 de proposito -- os bracos
     * varrem na altura do peito, e um golpe de braco nao pega quem esta rastejando
     * no chao.</p>
     */
    public static AttackHitbox caixaDoBracoDianteiro() {
        return new AttackHitbox(-1.20D, 0.00D, 0.50D, 1.20D, 2.20D, 1.70D);
    }

    /** Dano cheio da ficha; e o do golpe final, e a base da fracao dos encadeados. */
    private static float danoCheio() {
        return ChimeraProfiles.multiarmCentipede().attributes().attackDamage();
    }

    /**
     * A SEQUENCIA -- tres golpes, tres telegrafos, um so que ensina.
     *
     * <p><b>O windup de cada golpe e interrompivel e a JANELA nao.</b> Depois que
     * o braco varre, ele varre: cancelar durante os ticks ativos faria a formiga
     * desarmar um golpe que o jogador ja viu sair. A RECUPERACAO tambem e
     * interrompivel, e e essa combinacao que da ao stagger um alvo em quase toda a
     * duracao do combo -- que e a unica forma de o jogador cortar a sequencia
     * antes do fim.</p>
     *
     * <p>Os tres golpes tem ids proprios, e {@code SequenciaDeGolpes} reprova
     * repetidos: o id e o que uma tela de debug e um relato de bug usam para
     * responder "qual dos tres me acertou".</p>
     */
    public static SequenciaDeGolpes sequencia() {
        float encadeado = danoCheio() * FRACAO_DO_GOLPE_ENCADEADO;
        return new SequenciaDeGolpes(List.of(
                new GolpeEncadeado(
                        new AttackDefinition("braco_traseiro", WINDUP_DO_BRACO_TRASEIRO,
                                JANELA_DO_BRACO_TRASEIRO, RECUPERACAO_DO_BRACO_TRASEIRO,
                                encadeado, EMPURRAO_DO_ENCADEADO, true, false, true),
                        caixaDoBracoTraseiro()),
                new GolpeEncadeado(
                        new AttackDefinition("braco_medio", WINDUP_DO_BRACO_MEDIO,
                                JANELA_DO_BRACO_MEDIO, RECUPERACAO_DO_BRACO_MEDIO,
                                encadeado, EMPURRAO_DO_ENCADEADO, true, false, true),
                        caixaDoBracoMedio()),
                new GolpeEncadeado(
                        new AttackDefinition("braco_dianteiro", WINDUP_DO_BRACO_DIANTEIRO,
                                JANELA_DO_BRACO_DIANTEIRO, RECUPERACAO_DO_BRACO_DIANTEIRO,
                                danoCheio(), EMPURRAO_DO_FINAL, true, false, true),
                        caixaDoBracoDianteiro())),
                TICKS_DE_EMENDA);
    }

    /**
     * A intencao tatica de Nen vira POSTURA -- e nunca aura, custo ou tecnica.
     *
     * <p><b>Esta e a fronteira do CLAUDE.md escrita em uma linha.</b> O
     * {@code TacticalNenController} responde o que a formiga QUERIA fazer com Nen;
     * quem ativa tecnica, gasta aura e cobra custo e o Nen Foundation, e mais
     * ninguem. O que este metodo faz com a resposta e uma decisao do INIMIGO: uma
     * formiga que acabou de decidir sumir nao comeca um combo de quatro segundos
     * que a prende no lugar.</p>
     *
     * <p>Zetsu e o unico caso, e e de proposito. {@code ELEVAR_REN} e
     * {@code MANTER_KEN} sao intencoes de quem continua lutando, e traduzi-las em
     * "ataca mais" ou "bate mais forte" seria exatamente o que este projeto proibe:
     * um efeito de Nen decidido fora do nucleo, plausivel demais para alguem notar
     * sem medir.</p>
     *
     * <p><b>Hoje este ramo e inalcancavel em jogo</b>, e isso esta declarado: sem
     * pool de aura para entidade (ver {@link #FRACAO_DE_AURA_NAO_PUBLICADA}) o
     * controlador responde sempre {@code NENHUMA}. O recuo por vida critica, que
     * vem do {@code EnemyBrain}, continua valendo e usa o mesmo caminho.</p>
     */
    public static boolean recuaEmVezDeAtacar(TacticalNenIntent intencao) {
        return intencao == TacticalNenIntent.ENTRAR_EM_ZETSU;
    }
}
