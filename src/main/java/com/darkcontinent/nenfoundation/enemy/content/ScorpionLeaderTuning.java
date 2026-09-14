package com.darkcontinent.nenfoundation.enemy.content;

import com.darkcontinent.nenfoundation.enemy.ai.RegrasDeFerrao;
import com.darkcontinent.nenfoundation.enemy.ai.squad.SquadRules;
import com.darkcontinent.nenfoundation.enemy.chimera.nen.TacticalNenIntent;
import com.darkcontinent.nenfoundation.enemy.combat.AttackDefinition;
import com.darkcontinent.nenfoundation.enemy.combat.AttackHitbox;
import com.darkcontinent.nenfoundation.enemy.combat.RegrasDeVeneno;
import com.darkcontinent.nenfoundation.enemy.perception.VisionCone;

/**
 * Os numeros do Scorpion Leader que nascem com o COMPORTAMENTO dela.
 *
 * <p><b>Por que este arquivo existe em vez de mais metodos em
 * {@link ChimeraProfiles}.</b> Aquele arquivo guarda as fichas das nove formigas
 * publicadas, e neste momento varias frentes escrevem o comportamento de formigas
 * diferentes ao mesmo tempo. Todas precisariam acrescentar metodos no MESMO
 * arquivo, e o resultado de um merge assim nao e um conflito barulhento -- e uma
 * resolucao apressada em que o metodo de alguem some. Metodo que some nao da erro
 * de compilacao quando o chamador some junto: da uma formiga que perdeu o veneno
 * e continua nascendo, atacando e passando em todo portao.</p>
 *
 * <p><b>O que fica la e o que fica aqui.</b> Em {@code ChimeraProfiles} ficam a
 * ficha publicada (HP 120, dano 12, velocidade 0.27, armadura 8, alcance 28), a
 * recarga e o stagger -- tudo que outros sistemas ja leem. Aqui ficam os numeros
 * NOVOS, os que este comportamento inaugurou: o veneno, a forma dos dois
 * telegrafos e a geometria dos dois alcances.</p>
 *
 * <p><b>O dano nao e um numero proprio.</b> A pinca usa
 * {@code ChimeraProfiles.scorpionLeader().attributes().attackDamage()} inteiro, e
 * o ferrao usa uma FRACAO dele. Repetir o 12 aqui criaria duas fontes para a mesma
 * verdade, e girar o atributo numa sessao de balanceamento mudaria a barra de vida
 * do jogador sem mudar este arquivo.</p>
 *
 * <p><b>O esquadrao tambem nao.</b> Teto, espacamento, moral minima, raio de
 * reforco e orcamento de coordenacao saem inteiros de {@link SquadRules#esquadrao()}.
 * Redeclarados aqui, o numero daqui venceria em metade dos caminhos e o de la na
 * outra metade, e a sessao de balanceamento giraria um botao morto.</p>
 */
public final class ScorpionLeaderTuning {

    private ScorpionLeaderTuning() { }

    // ------------------------------------------------------------------ veneno
    //
    // O VENENO E A FICHA INTEIRA. O dano direto dela e MENOR que o do guepardo
    // (14) de proposito: o preco real vem depois do golpe. As tres constantes
    // abaixo se leem JUNTAS, e e por isso que elas viram um RegrasDeVeneno -- e ele
    // que impede um teto menor que uma dose, que apagaria o acumulo em silencio.

    /**
     * Quanto cada ferroada SOMA a duracao do veneno, em ticks.
     *
     * <p>Doze segundos, e o numero nao e estetico: ele tem de ser MAIOR que o
     * intervalo entre duas ferroadas, senao a primeira dose expira antes da
     * segunda chegar e o acumulo -- que a ficha do bicho promete -- nunca
     * acontece em jogo. A conta do intervalo esta em
     * {@link #TICKS_DE_GUARDA_DO_FERRAO}, e {@code ScorpionLeaderTuningTest} cobra
     * a desigualdade em vez de deixa-la num comentario.</p>
     *
     * <p>Um veneno que some rapido nao e veneno: e um segundo tipo de dano direto,
     * pago por um telegrafo de 28 ticks que nao entregou nada. A DURACAO e o que
     * o faz existir.</p>
     */
    public static final int DURACAO_POR_FERROADA_EM_TICKS = 240;

    /**
     * Teto de duracao acumulada, para o ENCONTRO inteiro.
     *
     * <p>Duas doses e meia. O teto nao existe para limitar esta formiga sozinha --
     * sozinha ela estabiliza perto de uma dose e meia. Ele existe porque um
     * esquadrao pode ter mais de uma formiga venenosa, e o acumulo e lido do
     * proprio alvo: sem teto, tres ferroadas de tres formigas somariam quase um
     * minuto de veneno de nivel alto, e o jogador morreria de dano continuo que
     * nenhuma das tres, sozinha, era capaz de causar. O log fica limpo e a ficha
     * de cada bicho continua certa.</p>
     */
    public static final int TETO_DE_DURACAO_EM_TICKS = 600;

    /**
     * Nivel maximo do efeito: 1, ou seja, veneno II.
     *
     * <p>A regua que importa e o intervalo de dano do veneno vanilla: 25 ticks no
     * nivel I e 12 no nivel II. Nivel II drena cerca de 1.7 por segundo, e a
     * ferroada direta custa {@link #FRACAO_DE_DANO_DO_FERRAO} do dano da ficha --
     * seis. Ou seja: sete segundos de veneno de nivel II ja custam mais do que a
     * ferroada que o aplicou, e e essa comparacao que faz "sair de perto" ser uma
     * jogada.</p>
     *
     * <p>Nivel III cortaria o intervalo para seis ticks e transformaria o acumulo
     * em execucao por uma fonte que o jogador nao consegue apontar --
     * {@link RegrasDeVeneno#AMPLIFICADOR_LIMITE} recusa passar disso.</p>
     */
    public static final int AMPLIFICADOR_MAXIMO_DO_VENENO = 1;

    // ------------------------------------------------------------------ pinca
    //
    // O GOLPE QUE NAO ENVENENA, e a existencia dele e o que torna o ferrao
    // legivel. Sem um golpe comum frequente, o telegrafo de 28 ticks vira "o
    // ataque dela" em vez de "o caro", e o jogador nao aprende o que evitar.

    /** Ticks de aviso: seis decimos de segundo de garras abrindo. */
    public static final int WINDUP_DA_PINCA = 12;
    /** Ticks em que a garra existe. */
    public static final int JANELA_DA_PINCA = 5;
    /** Ticks de recuperacao: a janela em que o jogador pune sem risco. */
    public static final int RECUPERACAO_DA_PINCA = 14;
    /** Empurrao da pinca. Pequeno: quem joga o alvo longe perde o proprio alcance. */
    public static final float EMPURRAO_DA_PINCA = 0.35F;

    /**
     * Distancia (centro a centro) em que ela decide golpear com a pinca.
     *
     * <p>Tem de ser MENOR que {@code caixaDaPinca().maxZ() + MEIA_LARGURA_DE_UM_ALVO},
     * e o teste cobra isso. Maior, ela comeca um aviso contra alguem que ja esta
     * fora do alcance da garra -- e como a navegacao trava durante o golpe, a
     * pinca no limite da distancia NUNCA acertaria. Isso nao da erro nenhum: da
     * uma formiga que erra sozinha e parece quebrada.</p>
     */
    public static final double ALCANCE_DA_PINCA = 1.1D;

    /**
     * Alcance da pinca DESENHADA, em blocos, do centro do bicho ate a ponta.
     *
     * <p>Copiado de {@code scorpion_leader_geo.py}: a caixa {@code claw_left}
     * comeca em z = -14 px, e 14/16 = 0.875. Duplicacao DECLARADA -- a outra ponta
     * e {@code valida_alcance_da_pinca}, e as duas juntas formam um portao que
     * morde dos dois lados: quem encolher a garra la reprova la, quem esticar a
     * caixa aqui reprova em {@code ScorpionLeaderTuningTest}.</p>
     */
    public static final double ALCANCE_DESENHADO_DA_PINCA = 0.875D;

    // ----------------------------------------------------------------- ferrao
    //
    // A FORMA E O OPOSTO DA DA PINCA, e isso e a ficha: aviso mais que dobrado,
    // janela mais curta, alcance maior e dano direto pela METADE. O que ele cobra
    // nao esta no golpe.

    /** Ticks de aviso: um segundo e quatro decimos de cauda armando sobre o dorso. */
    public static final int WINDUP_DO_FERRAO = 28;
    /** Ticks em que a ponta existe. Curta: o desvio precisa ser possivel. */
    public static final int JANELA_DO_FERRAO = 4;
    /** Ticks de recuperacao: a cauda fica baixa, e e a melhor hora de bater nela. */
    public static final int RECUPERACAO_DO_FERRAO = 20;
    /** Empurrao da ferroada. Menor que o da pinca: uma ponta perfura, nao arremessa. */
    public static final float EMPURRAO_DO_FERRAO = 0.2F;

    /**
     * Fracao do dano da ficha que a ferroada cobra NA HORA.
     *
     * <p>Metade, e essa e a troca que define o bicho: a ferroada custa seis de
     * imediato contra os doze da pinca, e a diferenca e paga depois, em veneno. Um
     * ferrao que cobrasse o dano cheio E o veneno faria o golpe comum nao ter
     * razao de existir -- e sem golpe comum, o telegrafo longo deixa de ter com o
     * que ser comparado.</p>
     */
    public static final float FRACAO_DE_DANO_DO_FERRAO = 0.5F;

    /**
     * Distancia (centro a centro) em que ela decide ferroar.
     *
     * <p>Maior que {@link #ALCANCE_DA_PINCA}, e {@link RegrasDeFerrao} recusa o
     * contrario no construtor: invertidos, ela sempre entraria na distancia da
     * pinca primeiro, a pinca dispararia a cada recarga e a ferroada nunca sairia
     * -- com o veneno implementado, testado, documentado e ausente do jogo.</p>
     */
    public static final double ALCANCE_DO_FERRAO = 1.3D;

    /**
     * Alcance do ferrao DESENHADO em repouso, em blocos, a frente do centro.
     *
     * <p>Copiado de {@code scorpion_leader_geo.py}: o raio do chicote (do pivot de
     * {@code tail_base} ate o canto mais distante de {@code stinger}) e 15.84 px, e
     * o pivot fica 7 px atras do centro -- sobram 8.84 px, ou 0.55 bloco. E pouco,
     * e tem de ser: uma cauda mais longa nao caberia na altura da hitbox.</p>
     */
    public static final double ALCANCE_DESENHADO_DO_FERRAO = 0.55D;

    /**
     * Quanto o corpo viaja para a frente no primeiro tick da janela do ferrao.
     *
     * <p><b>Este numero e a ponte entre a arte e a regra.</b> O ferrao desenhado
     * alcanca {@link #ALCANCE_DESENHADO_DO_FERRAO}; a caixa reivindica
     * {@code caixaDoFerrao().maxZ()}. A diferenca e paga aqui, pelo arranco --
     * aplicado UMA vez por instancia de ataque, nunca a cada tick da janela.</p>
     *
     * <p>Tirar o arranco sem encolher a caixa da um jogador envenenado por uma
     * ponta que, na tela, parou antes dele. Por isso a soma e cobrada nos DOIS
     * lados: em {@code scorpion_leader_geo.py} ({@code valida_alcance_do_ferrao})
     * e em {@code ScorpionLeaderTuningTest}.</p>
     */
    public static final double AVANCO_DO_FERRAO = 0.55D;

    /**
     * Intervalo minimo entre duas ferroadas, em ticks.
     *
     * <p>Ele fica espremido entre dois limites, e os dois sao cobrados por teste:</p>
     *
     * <ul>
     *   <li><b>maior que o ciclo do proprio ferrao</b> (28+4+20 = 52). Menor que
     *       isso e guarda nenhuma: a proxima ferroada estaria liberada antes de a
     *       anterior terminar, e o golpe comum -- que da ao jogador com o que
     *       comparar o telegrafo longo -- nunca apareceria;</li>
     *   <li><b>menor que {@link #DURACAO_POR_FERROADA_EM_TICKS}</b>. Maior, a dose
     *       anterior expira antes da proxima chegar, o acumulo nunca sai do nivel I
     *       e o teto vira decoracao -- com o veneno funcionando perfeitamente e a
     *       mecanica que ele existe para servir desaparecida.</li>
     * </ul>
     */
    public static final int TICKS_DE_GUARDA_DO_FERRAO = 140;

    // ---------------------------------------------------------------- combate

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
     * Recarga imposta a quem interrompeu um golpe dela.
     *
     * <p>Mais longa que a recarga normal (60): interromper uma formiga de armadura
     * 8 custa varios golpes bem colocados, e tem de VALER. Sem isso, {@code reset()}
     * devolveria a fase para IDLE e ela poderia recomecar no tick seguinte -- o
     * jogador aprenderia a nao interromper, que e o oposto do que o telegrafo longo
     * existe para ensinar.</p>
     */
    public static final int RECARGA_APOS_INTERRUPCAO = 80;

    /**
     * Quanto um golpe nesta formiga tira da moral do esquadrao inteiro.
     *
     * <p>Pouco, e menos do que a matilha de lobos cobra: este esquadrao e ancorado
     * numa lider de HP 120 e armadura 8, e a licao dele nao e "quebre a moral" --
     * e "o veneno vem de uma peca so". Um abalo alto faria o esquadrao recuar antes
     * de o jogador precisar decidir alguma coisa sobre o ferrao.</p>
     */
    public static final int ABALO_POR_GOLPE_NA_LIDER = 4;

    // ------------------------------------------------------------- percepcao

    /** Ticks de memoria de alvo; e tambem o prazo do {@code ThreatMemory}. */
    public static final int MEMORIA_DE_ALVO_TICKS = 140;
    /** Ticks de aviso antes de o cerebro passar de WARN para ENGAGE. */
    public static final int TICKS_DE_AVISO = 20;
    /** Abertura total do cone de visao, em graus. */
    public static final double ABERTURA_DA_VISAO = 80.0D;
    /** Alcance de audicao em blocos, para som de intensidade 1. */
    public static final double ALCANCE_DE_AUDICAO = 16.0D;
    /** Fracao da vida abaixo da qual o cerebro pode decidir fugir. */
    public static final float FRACAO_DE_VIDA_CRITICA = 0.25F;

    // ------------------------------------------------------------- montagem

    /** A regra de veneno do bicho, com a continencia cobrada no construtor. */
    public static RegrasDeVeneno veneno() {
        return new RegrasDeVeneno(DURACAO_POR_FERROADA_EM_TICKS, TETO_DE_DURACAO_EM_TICKS,
                AMPLIFICADOR_MAXIMO_DO_VENENO);
    }

    /** A escolha entre os dois golpes, com a ordem dos alcances cobrada no construtor. */
    public static RegrasDeFerrao ferrao() {
        return new RegrasDeFerrao(ALCANCE_DA_PINCA, ALCANCE_DO_FERRAO,
                TICKS_DE_GUARDA_DO_FERRAO);
    }

    /** As regras do esquadrao que ela lidera. Saem inteiras do framework. */
    public static SquadRules esquadrao() { return SquadRules.esquadrao(); }

    /** O cone de visao dela, a partir do alcance da ficha. */
    public static VisionCone coneDeVisao(double alcance) {
        return VisionCone.deGraus(alcance, ABERTURA_DA_VISAO);
    }

    /**
     * A INTENCAO de Nen traduzida em POSTURA -- e so isso.
     *
     * <p><b>Esta e a fronteira, e ela e a razao deste metodo existir separado.</b>
     * O {@code TacticalNenController} devolve uma intencao; o que acontece com ela
     * aqui e uma decisao de COMBATE do inimigo -- fechar a guarda e parar de
     * comecar golpes. Nenhuma aura e calculada, nenhum custo e pago, nenhuma
     * tecnica e ativada: o Nen Foundation e a unica autoridade sobre Nen
     * (CLAUDE.md), e uma segunda autoridade nao daria erro -- daria
     * desbalanceamento que ninguem consegue explicar.</p>
     *
     * <p>So {@code MANTER_KEN} fecha a guarda. {@code MANTER_TEN} e o piso de quem
     * despertou e nao muda postura nenhuma; {@code ELEVAR_REN} e ofensivo e fechar
     * a guarda nele seria o oposto do que a intencao diz.
     * {@code ENTRAR_EM_ZETSU} nao e lido aqui de proposito: ela e a ANCORA do
     * esquadrao, com HP 120 e armadura 8, e nao recua -- um recuo dela deixaria os
     * membros sem a peca em torno da qual eles se postam.</p>
     */
    public static boolean guardaFechada(TacticalNenIntent intencao) {
        return intencao == TacticalNenIntent.MANTER_KEN;
    }

    /**
     * O golpe de pinca.
     *
     * <p>O windup e interrompivel e a JANELA nao: depois que a garra fecha, ela
     * fecha. Interromper durante os 5 ticks ativos faria a formiga cancelar um
     * golpe que o jogador ja viu sair, e com um telegrafo de seis decimos de
     * segundo a leitura e a unica defesa que existe contra ele.</p>
     *
     * <p>A recuperacao TAMBEM e interrompivel, ao contrario da do ferrao: e a
     * janela barata do bicho, e punir nela tem de ser a coisa obvia a fazer.</p>
     */
    public static AttackDefinition pinca() {
        return new AttackDefinition("claw", WINDUP_DA_PINCA, JANELA_DA_PINCA,
                RECUPERACAO_DA_PINCA,
                ChimeraProfiles.scorpionLeader().attributes().attackDamage(),
                EMPURRAO_DA_PINCA, true, false, true);
    }

    /**
     * A ferroada -- o unico ataque que envenena.
     *
     * <p>O aviso e interrompivel, e isso e o coracao do encontro: vinte e oito
     * ticks sao tempo de sobra para um jogador que leu a cauda armando trocar o
     * veneno por dois golpes bem colocados. Tornar o aviso nao-interrompivel
     * apagaria a unica resposta que o bicho oferece, sem que nada reprovasse.</p>
     *
     * <p>A recuperacao NAO e interrompivel, ao contrario da da pinca: ela ja e a
     * punicao. Cambalear dentro dela devolveria a fase para IDLE, e a recarga por
     * interrupcao ({@link #RECARGA_APOS_INTERRUPCAO}) acabaria ENCURTANDO a janela
     * em que o jogador bate de graca.</p>
     */
    public static AttackDefinition ferroada() {
        return new AttackDefinition("sting", WINDUP_DO_FERRAO, JANELA_DO_FERRAO,
                RECUPERACAO_DO_FERRAO,
                ChimeraProfiles.scorpionLeader().attributes().attackDamage()
                        * FRACAO_DE_DANO_DO_FERRAO,
                EMPURRAO_DO_FERRAO, true, false, false);
    }

    /**
     * Caixa da pinca, em coordenadas LOCAIS. A FRENTE E +Z.
     *
     * <p><b>+Z, e isto nao e distracao.</b> Na GEOMETRIA do modelo (formato
     * Bedrock) a frente e -Z; na transformacao de {@link AttackHitbox}, que e
     * matematica de mundo, com yaw 0 o olhar vanilla aponta para +Z. Misturar as
     * duas ja custou um bug neste repositorio: a caixa ficou ATRAS do mob, que
     * atacava, animava e nao encostava em quem estava na frente. Ver
     * {@code DummyEnemyEntity.CAIXA_DO_GOLPE}.</p>
     *
     * <p>Ela e larga (1.4 bloco) e BAIXA (1.0): duas garras varrem a frente do
     * corpo, e nao alcancam quem esta em cima de um bloco. O {@code maxZ} de 0.85
     * e coberto pela garra desenhada, que alcanca
     * {@link #ALCANCE_DESENHADO_DA_PINCA} -- e este golpe nao tem arranco nenhum
     * para pagar a diferenca.</p>
     */
    public static AttackHitbox caixaDaPinca() {
        return new AttackHitbox(-0.7D, 0.0D, 0.3D, 0.7D, 1.0D, 0.85D);
    }

    /**
     * Caixa do ferrao, em coordenadas LOCAIS. A FRENTE E +Z.
     *
     * <p>Ela e o oposto da caixa da pinca: ESTREITA (0.9 bloco) e ALTA (1.6, a
     * altura inteira da hitbox). Uma ponta nao varre -- ela perfura, e vem de
     * cima. A altura existe porque o chicote desce por cima do dorso: um
     * {@code maxY} baixo faria a ferroada errar quem esta em pe num bloco, no
     * exato angulo em que a animacao mostra a ponta passando na altura da cabeca
     * dele.</p>
     *
     * <p>O {@code maxZ} de 1.05 e coberto por {@link #ALCANCE_DESENHADO_DO_FERRAO}
     * (0.55) mais {@link #AVANCO_DO_FERRAO} (0.55), e as duas pontas dessa conta
     * sao cobradas -- aqui, por {@code ScorpionLeaderTuningTest}, e la, por
     * {@code scorpion_leader_geo.py}.</p>
     */
    public static AttackHitbox caixaDoFerrao() {
        return new AttackHitbox(-0.45D, 0.0D, 0.3D, 0.45D, 1.6D, 1.05D);
    }
}
