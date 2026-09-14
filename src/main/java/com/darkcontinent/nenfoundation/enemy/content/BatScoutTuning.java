package com.darkcontinent.nenfoundation.enemy.content;

import com.darkcontinent.nenfoundation.enemy.ai.RegrasDeRelatorioDeBatedor;
import com.darkcontinent.nenfoundation.enemy.ai.RegrasDeVisaoNoturna;
import com.darkcontinent.nenfoundation.enemy.ai.RegrasDeVooDeBatedor;
import com.darkcontinent.nenfoundation.enemy.combat.AttackDefinition;
import com.darkcontinent.nenfoundation.enemy.combat.AttackHitbox;

/**
 * Os numeros de comportamento do Bat Scout.
 *
 * <p><b>Por que este arquivo existe separado de {@link ChimeraProfiles}.</b>
 * Aquele arquivo e a fila unica de perfis: toda formiga passa por ele, e neste
 * momento tres frentes estao escrevendo criaturas ao mesmo tempo. Se cada uma
 * acrescentasse os proprios numeros de janela, raio e limiar la dentro, as tres
 * editariam as mesmas regioes do mesmo arquivo e o merge seria resolvido a mao,
 * tres vezes. Conflito de merge resolvido as pressas nao da erro de compilacao:
 * ele apaga a linha de outra pessoa, e o sintoma e um bicho que perdeu um numero
 * sem que nada acuse. Um arquivo por criatura troca esse risco por um import.</p>
 *
 * <p><b>Camada, e nao config.</b> Isto e o mesmo degrau de {@code ChimeraProfiles}:
 * perfil de balanceamento em Java, lido pelo servidor. O que o projeto exige e que
 * o numero tenha UMA casa -- e a casa destes e aqui. Nenhum deles esta duplicado
 * como constante dentro da entidade; config e datapack sao o degrau seguinte, e
 * valem para todo o bestiario de uma vez, nao para este bicho sozinho.</p>
 *
 * <p><b>O que NAO mora aqui:</b> HP, dano, velocidade, armadura, alcance de
 * percepcao, recarga entre golpes e regras de stagger. Tudo isso ja tem casa em
 * {@code ChimeraProfiles.batScout()}, {@code batScoutRecarga()} e
 * {@code batScoutStagger()}. Copiar qualquer um deles para ca criaria duas fontes
 * para a mesma verdade, e a copia esquecida ganharia no dia da sessao de
 * balanceamento.</p>
 */
public final class BatScoutTuning {

    private BatScoutTuning() {
    }

    // ------------------------------------------------------------- relatorio

    /**
     * Ticks com alvo antes do PRIMEIRO relatorio.
     *
     * <p>E a janela que separa "ele me viu" de "ele contou", e e o numero que
     * decide se correr atras dele resolve. Tres quartos de segundo: menos do que o
     * Radio Rat gasta, porque este bicho VOA e a perseguicao ja e pior para o
     * jogador -- dar a mesma janela do rato daria uma janela inutil.</p>
     */
    public static final int TICKS_DE_OBSERVACAO = 15;

    /**
     * Ticks minimos entre dois relatorios. O relatorio TEM custo.
     *
     * <p>Este e o raio da unica varredura de mundo que o batedor faz fora da
     * percepcao. Sem intervalo ela rodaria a cada tick enquanto houvesse alvo, e o
     * sintoma nao seria erro: seria TPS caindo devagar numa colonia com muitos
     * batedores, que e o relato de bug mais caro que existe.</p>
     *
     * <p>Seis segundos tambem e gameplay: e o tempo em que o jogador ainda pode
     * alcancar o batedor antes do proximo aviso, e e por isso que ele nao e maior.</p>
     */
    public static final int INTERVALO_ENTRE_RELATORIOS = 120;

    /**
     * Ate onde DO BATEDOR um vizinho e alcancado pelo aviso, em blocos.
     *
     * <p>Maior que o do Radio Rat de proposito -- este bicho existe para cobrir
     * terreno. Aumentar sem mexer em {@link #INTERVALO_ENTRE_RELATORIOS} e o jeito
     * de transformar gameplay em varredura cara: o custo da varredura cresce com o
     * cubo do raio, e nada acusa isso.</p>
     */
    public static final double RAIO_DO_AVISO = 24.0D;

    /**
     * Intensidade do som do aviso, 0..1.
     *
     * <p>Ela nao decide o raio do aviso -- decide, do lado de QUEM OUVE, quao longe
     * do alvo o aviso ainda serve: {@code HearingEvent.audivel} compara a distancia
     * do vizinho ao alvo contra o alcance de audicao dele VEZES este numero. E o
     * botao mais afiado do bicho: em 1.0 o aviso vale o mesmo que uma explosao e um
     * mob na borda do proprio alcance ganha um alvo que nunca acharia sozinho.</p>
     */
    public static final double INTENSIDADE_DO_AVISO = 0.6D;

    /**
     * Por quantos ticks a colonia guarda a ameaca relatada.
     *
     * <p>Um minuto. Sem prazo, a colonia lembraria para sempre de quem passou por
     * ali uma vez e o alerta nunca baixaria -- uma colonia permanentemente furiosa,
     * sem nada explicando o motivo. Curto demais e o contrario: o batedor avisa, o
     * aviso vence antes de alguem chegar, e a colonia nunca reage.</p>
     */
    public static final int DURACAO_NA_COLONIA = 1200;

    // --------------------------------------------------------- visao noturna

    /**
     * Fracao do alcance que sobraria no escuro para quem NAO tem {@code NIGHT_VISION}.
     *
     * <p>Ele nunca se aplica a esta familia -- {@code ChimeraPeonDefinitions.batScout()}
     * garante o trait. Ele esta aqui porque a regra so significa alguma coisa se o
     * OUTRO lado existir e for caro: sem um numero de perda escrito, "o alcance nao
     * cai no escuro" e uma frase sobre nada.</p>
     */
    public static final double FATOR_NO_ESCURO_SEM_VISAO_NOTURNA = 0.45D;

    /**
     * Nivel de luz a partir do qual ninguem perde alcance.
     *
     * <p>7 e o mesmo degrau em que o jogo vanilla comeca a permitir spawn hostil.
     * Nao e coincidencia nem copia por descuido: e o limiar que o jogador ja
     * aprendeu a reconhecer como "aqui esta escuro", e usar outro faria a regra
     * virar em um lugar que nao parece escuro nenhum.</p>
     */
    public static final int LUZ_DE_PENUMBRA = 7;

    // -------------------------------------------------------------------- voo

    /**
     * Quantos blocos ACIMA do alvo o batedor quer estar.
     *
     * <p>3.5 fica acima do alcance de qualquer golpe corpo-a-corpo e ainda dentro do
     * alcance confortavel de um arco: e o numero que faz a resposta ao bicho ser
     * "pegue alguma coisa que alcance" em vez de "ignore, ele e inalcancavel".</p>
     */
    public static final double ALTURA_DE_VOO_PREFERIDA = 3.5D;

    /**
     * Abaixo desta distancia horizontal ele abre espaco.
     *
     * <p>Sete blocos. Ele nao mantem distancia de tiro -- ele mantem distancia de
     * NAO SER ALCANCADO, e essa e a leitura: quem chega perto faz o bicho recuar, e
     * quem faz o bicho recuar entende sozinho que ele nao veio brigar.</p>
     */
    public static final double DISTANCIA_DE_FUGA = 7.0D;

    /**
     * Ate onde a mordida de encurralado alcanca, em blocos.
     *
     * <p>Este numero e cobrado pela ARTE: o gerador de animacao soma o focinho
     * desenhado com o avanco do clipe de {@code strike} e reprova se a soma nao
     * chegar aqui (ver {@code bat_scout_animacoes.py}). Aumentar este valor sem
     * reabrir o gerador da um morcego que morde de onde, na tela, ele nao esta --
     * e nao ha erro nenhum para procurar.</p>
     */
    public static final double ALCANCE_DA_MORDIDA = 0.55D;

    /**
     * Quanto espaco livre ACIMA dele conta como "da para fugir", em blocos.
     *
     * <p>E o unico fato de mundo que a regra de voo consome, e por isso ele mora
     * aqui e nao dentro da entidade: e ele que decide quando os 3 de dano existem.
     * Baixo demais e um batedor que morde debaixo de qualquer folhagem; alto demais
     * e um batedor que nunca ataca nem preso numa caverna, e o dano dele passa a
     * ser um numero que nenhum jogador chega a ver.</p>
     */
    public static final double ALTURA_LIVRE_PARA_FUGIR = 1.5D;

    /**
     * Impulso vertical ao levar dano, em blocos por tick.
     *
     * <p>E o reflexo que faz o bicho ler como voador em vez de como mob fraco. Alto
     * demais e um morcego que some no ceu ao primeiro arranhao e o combate acaba
     * sem desfecho; zero e um morcego que apanha parado.</p>
     */
    public static final double RECUO_VERTICAL = 0.42D;

    // ---------------------------------------------------------------- mordida

    /** Telegrafo da mordida. Curto -- este golpe e reflexo de bicho sem saida. */
    public static final int MORDIDA_WINDUP_TICKS = 12;

    /** A janela em que a mordida acerta quem estiver colado. */
    public static final int MORDIDA_ACTIVE_TICKS = 3;

    /** A janela de punicao: ele esta no ar, sem nada armado. */
    public static final int MORDIDA_RECOVERY_TICKS = 14;

    /** Repuxao da mordida. Quase nada: ela nao existe para afastar, e para doer um pouco. */
    public static final float REPUXAO_DA_MORDIDA = 0.08F;

    /**
     * Recarga imposta a quem foi interrompido no meio da mordida.
     *
     * <p>Maior que a recarga normal de proposito: interromper tem de PAGAR. Sem
     * isso o batedor cambaleia, cai, e volta a morder assim que a janela fecha --
     * e o jogador aprende que acertar este bicho nao adianta, que e o contrario do
     * que ele ensina.</p>
     */
    public static final int RECARGA_APOS_INTERRUPCAO = 70;

    // ------------------------------------------------------- limites de design

    /**
     * Quantos vizinhos um unico relatorio pode alcancar.
     *
     * <p><b>LIMITE DE DESIGN, e nao botao de balanceamento</b> -- e por isso ele
     * fica no codigo, com o motivo escrito. Ele existe para que o custo do
     * relatorio tenha TETO independente de quantos mobs a colonia tenha empilhado
     * num chunk: sem teto, um ninho com sessenta formigas transforma cada aviso em
     * sessenta entregas, e o custo cresce com a populacao justamente no momento em
     * que o servidor menos pode pagar. Girar isto numa sessao de balanceamento nao
     * mudaria a dificuldade -- mudaria o custo, que e outra conversa.</p>
     */
    public static final int MAXIMO_DE_VIZINHOS_POR_AVISO = 12;

    // ---------------------------------------------------------- as regras puras

    /** As regras puras do relatorio, ja alimentadas com os numeros acima. */
    public static RegrasDeRelatorioDeBatedor relatorio() {
        return new RegrasDeRelatorioDeBatedor(TICKS_DE_OBSERVACAO, INTERVALO_ENTRE_RELATORIOS,
                RAIO_DO_AVISO, INTENSIDADE_DO_AVISO, DURACAO_NA_COLONIA);
    }

    /** A regra pura da visao noturna, que e o que torna o trait observavel. */
    public static RegrasDeVisaoNoturna visaoNoturna() {
        return new RegrasDeVisaoNoturna(FATOR_NO_ESCURO_SEM_VISAO_NOTURNA, LUZ_DE_PENUMBRA);
    }

    /** As regras puras do voo: subir, afastar, e so morder encurralado. */
    public static RegrasDeVooDeBatedor voo() {
        return new RegrasDeVooDeBatedor(ALTURA_DE_VOO_PREFERIDA, DISTANCIA_DE_FUGA,
                ALCANCE_DA_MORDIDA, RECUO_VERTICAL);
    }

    /**
     * A caixa da mordida, em coordenadas LOCAIS. A FRENTE E +Z.
     *
     * <p>+Z, e nao -Z: -Z e a frente da GEOMETRIA do modelo (formato Bedrock), e
     * esta caixa passa por {@code AttackHitbox.noMundo}, que e matematica de mundo
     * -- com yaw 0 o olhar vanilla aponta para +Z. Trocar os dois poe a caixa ATRAS
     * do morcego: ele morde, anima, e acerta quem estiver pelas costas. Nao ha erro
     * nenhum nisso; foi um gametest do boneco de treino que achou a primeira vez.</p>
     *
     * <p>Ela vai ate {@link #ALCANCE_DA_MORDIDA} e nao mais: e esse numero que a
     * arte cobra contra o focinho desenhado mais a arremetida do clipe.</p>
     */
    public static AttackHitbox caixaDaMordida() {
        return new AttackHitbox(-0.3D, 0.0D, 0.0D, 0.3D, 0.7D, ALCANCE_DA_MORDIDA);
    }

    /**
     * A mordida: o unico golpe do batedor, e ele so sai encurralado.
     *
     * <p><b>O dano vem de fora.</b> Ele e lido do atributo da entidade na hora em
     * que o golpe comeca, e nao escrito aqui: {@code ChimeraProfiles.batScout()} ja
     * declara {@code attackDamage}, e uma constante paralela neste arquivo venceria
     * a config no dia em que alguem girasse a de la.</p>
     *
     * <p><b>As tres janelas sao interrompiveis, e isso e a ficha do bicho.</b> Ele
     * tem 16 de vida e armadura 0; a promessa do encontro e que quem alcanca o
     * batedor resolve o batedor. Trocar qualquer uma por {@code false} nao quebra
     * nada visivel -- ele continua mordendo, o dano continua saindo, o log fica
     * limpo -- e o que some e a recompensa de ter chegado perto.</p>
     */
    public static AttackDefinition mordida(float dano) {
        return new AttackDefinition("bat_scout_bite", MORDIDA_WINDUP_TICKS, MORDIDA_ACTIVE_TICKS,
                MORDIDA_RECOVERY_TICKS, dano, REPUXAO_DA_MORDIDA, true, true, true);
    }
}
