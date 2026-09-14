package com.darkcontinent.nenfoundation.enemy.content;

import com.darkcontinent.nenfoundation.enemy.combat.AttackDefinition;
import com.darkcontinent.nenfoundation.enemy.perception.RadioRatReportRules;

/**
 * Os numeros de comportamento do Radio Rat.
 *
 * <p><b>Por que este arquivo existe separado de {@link GreedIslandProfiles}.</b>
 * Aquele arquivo e a fila unica de perfis: toda criatura da ilha passa por ele, e
 * neste momento quatro frentes estao escrevendo criaturas ao mesmo tempo. Se cada
 * uma acrescentasse os proprios numeros de janela, raio e limiar la dentro, as
 * quatro editariam as mesmas regioes do mesmo arquivo e o merge seria resolvido a
 * mao, quatro vezes. Conflito de merge resolvido as pressas nao da erro de
 * compilacao: ele apaga a linha de outra pessoa, e o sintoma e um bicho que
 * perdeu um numero sem que nada acuse. Um arquivo por criatura troca esse risco
 * por um import.</p>
 *
 * <p><b>Camada, e nao config.</b> Isto e o mesmo degrau de
 * {@code GreedIslandProfiles}: perfil de balanceamento em Java, lido pelo
 * servidor. O que o projeto exige e que o numero tenha UMA casa -- e a casa
 * destes e aqui. Nenhum deles esta duplicado como constante dentro da entidade;
 * config e datapack sao o degrau seguinte, e valem para todo o bestiario de uma
 * vez, nao para este bicho sozinho.</p>
 *
 * <p><b>O que NAO mora aqui:</b> HP, dano, velocidade, armadura, alcance,
 * recarga entre golpes e regras de stagger. Tudo isso ja tem casa em
 * {@code GreedIslandProfiles.radioRat()}, {@code radioRatRecarga()} e
 * {@code radioRatStagger()}. Copiar qualquer um deles para ca criaria duas
 * fontes para a mesma verdade, e a copia esquecida ganharia no dia da sessao de
 * balanceamento.</p>
 */
public final class RadioRatTuning {

    private RadioRatTuning() {
    }

    /**
     * Ticks com alvo antes de o PRIMEIRO grito poder comecar.
     *
     * <p>E a janela que separa "ele me viu" de "ele contou", e e o numero que
     * decide se dar meia-volta e correr resolve. Um segundo: tempo de um arco
     * carregado pela metade ou de duas pancadas.</p>
     */
    public static final int TICKS_DE_OBSERVACAO = 20;

    /**
     * Telegrafo do grito. E a segunda janela, e a que o clipe de windup gasta
     * levantando a antena.
     *
     * <p>Encurtar isto e apagar a resposta do encounter, nao "deixar o rato mais
     * agil": o relatorio sai antes de qualquer um conseguir reagir, e como o
     * dano do bicho e 2, ninguem associa a morte que vem depois ao rato que
     * gritou.</p>
     */
    public static final int GRITO_WINDUP_TICKS = 20;

    /** A janela em que o relatorio sai e a mordida acerta quem estiver colado. */
    public static final int GRITO_ACTIVE_TICKS = 4;

    /** A janela de punicao: o rato esta no chao, sem nada armado. */
    public static final int GRITO_RECOVERY_TICKS = 16;

    /**
     * Ate onde do RATO um vizinho e alcancado pelo grito, em blocos.
     *
     * <p>Este e o raio da unica varredura de mundo que este bicho faz fora da
     * percepcao, e ela so roda na janela ACTIVE de um grito -- ou seja, no melhor
     * caso uma vez a cada {@code GRITO_WINDUP_TICKS + GRITO_ACTIVE_TICKS +
     * GRITO_RECOVERY_TICKS + recarga do perfil} ticks. Aumentar o raio sem mexer
     * nessa conta e o jeito de transformar gameplay em varredura por tick: nao da
     * erro, so faz o TPS cair devagar num mundo com muitos ratos, que e o relato
     * de bug mais caro que existe.</p>
     */
    public static final double RAIO_DO_RELATORIO = 16.0D;

    /**
     * Intensidade do som do relatorio, 0..1.
     *
     * <p>Ela nao decide o raio do grito -- decide, do lado de QUEM OUVE, quao
     * longe do alvo o aviso ainda serve: {@code HearingEvent.audivel} compara a
     * distancia do vizinho ao alvo contra o alcance de audicao dele VEZES este
     * numero. Em 1.0 o grito vale o mesmo que uma explosao e um mob na borda do
     * proprio alcance recebe alvo que ele nunca teria achado sozinho; abaixo
     * disso, o aviso so aproveita a quem ja estava por perto. E o botao mais
     * afiado deste bicho.</p>
     */
    public static final double INTENSIDADE_DO_RELATORIO = 0.85D;

    /**
     * Recarga imposta a quem foi interrompido no meio do grito.
     *
     * <p>Maior que a recarga normal de proposito: interromper tem de PAGAR. Sem
     * isso o rato cambaleia e volta a gritar assim que a janela fecha, e o jogador
     * aprende que bater nele nao adianta -- que e o contrario do que o bicho
     * ensina.</p>
     */
    public static final int RECARGA_APOS_INTERRUPCAO = 80;

    /** Repuxao da mordida. Baixo porque ela nao existe para afastar, e para doer um pouco. */
    public static final float REPUXAO_DA_MORDIDA = 0.15F;

    /** As regras puras do relatorio, ja alimentadas com os numeros acima. */
    public static RadioRatReportRules relatorio() {
        return new RadioRatReportRules(TICKS_DE_OBSERVACAO, RAIO_DO_RELATORIO,
                INTENSIDADE_DO_RELATORIO);
    }

    /**
     * O grito: o unico ato do Radio Rat, telegrafado e interrompivel.
     *
     * <p><b>O dano vem de fora.</b> Ele e lido do atributo da entidade na hora em
     * que o golpe comeca, e nao escrito aqui: {@code GreedIslandProfiles.radioRat()}
     * ja declara {@code attackDamage}, e uma constante paralela neste arquivo
     * venceria a config no dia em que alguem girasse a de la -- o erro de "config
     * declarada e constante paralela no consumidor", que este projeto ja sabe que
     * comete.</p>
     *
     * <p><b>O windup e interrompivel, e isso e a ficha do bicho.</b> Trocar por
     * {@code false} nao quebra nada visivel: o rato continua gritando, o dano
     * continua saindo, o log fica limpo. O que some e a unica resposta que o
     * encounter tem, e nenhum portao de compilacao ve isso -- por isso ha um teste
     * que cobra exatamente esta flag.</p>
     */
    public static AttackDefinition grito(float dano) {
        return new AttackDefinition("radio_rat_report", GRITO_WINDUP_TICKS, GRITO_ACTIVE_TICKS,
                GRITO_RECOVERY_TICKS, dano, REPUXAO_DA_MORDIDA, true, false, true);
    }
}
