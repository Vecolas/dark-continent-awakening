package com.darkcontinent.nenfoundation.client.vfx;

/**
 * Snapshot visual interpolavel. Nao contem aura autoritativa nem regras de combate.
 *
 * <p><b>ELE NAO CARREGA MAIS NUMERO DE ARTE.</b> Ate o AV3 havia aqui um
 * {@code AuraVisualPreset} com sete campos -- e cinco deles (espessura da
 * shell, intensidade de borda, de fluxo, amplitude e frequencia de pulso) nao
 * tinham UM leitor sequer no repositorio. Eram o erro numero 7 do
 * {@code CLAUDE.md} em forma de record: o numero foi para o dado
 * ({@code assets/nenfoundation/nen_vfx/*.json}) e ficou tambem no codigo, onde
 * quem girasse o botao nao mudaria nada.
 *
 * <p>A DIVISAO QUE FICOU: aqui mora o que muda a cada tick e por jogador --
 * modo, intensidade, progresso da transicao, alocacao por regiao e as duas
 * cores. Os numeros de arte, que sao os mesmos para todos os jogadores no mesmo
 * modo, moram no perfil e se buscam com
 * {@code AuraPerfis.de(estado.mode())}.
 *
 * <p><b>DOIS MODOS, A PARTIR DO AV4, E ISSO CORRIGE UM DEFEITO REAL.</b>
 * {@link #mode()} continua sendo o modo ASSENTADO -- ele so vira no ultimo tick
 * da transicao, e tudo o que existia antes (LOD, audio, particula) continua
 * lendo dele. O que faltava era {@link #modoAlvo()}: para onde a transicao esta
 * indo. Sem ele, quem desenha buscava o perfil por {@code mode()} e o MATERIAL
 * saltava de Ten para Ren num quadro, enquanto a intensidade subia suave. Isso
 * nao lanca nada e nao aparece em teste: aparece como um estalo que a pessoa
 * relata como "bug de render". Com os dois, o perfil se interpola entre as duas
 * pontas e a troca deixa de ser seca.
 *
 * <p>{@link #fases()} carrega os PESOS por componente. Ver
 * {@link AuraTransitionSample}: e o que permite que a shell contraia enquanto a
 * borda ainda nem comecou a crescer.
 */
public record AuraVisualState(AuraVisualMode mode, AuraVisualMode modoAlvo, float intensity,
        float transitionProgress, AuraDistribution distribution, int primaryColor,
        int secondaryColor, AuraTransitionSample fases) {
    public AuraVisualState {
        if (mode == null || modoAlvo == null || distribution == null || fases == null) {
            throw new NullPointerException(
                    "modo, modo alvo, distribuicao e fases sao obrigatorios");
        }
        validar(intensity, "intensity");
        validar(transitionProgress, "transitionProgress");
    }

    /**
     * A forma curta: sem transicao em curso.
     *
     * <p>ELA EXISTE PARA QUEM NAO TEM INTERPOLADOR -- os outros jogadores, o
     * estado forcado da sessao de arte, o desligado. Nesses casos o alvo E o
     * modo, e os pesos sao os assentados daquele modo. Escrever isso em cada
     * chamada convidaria alguem a passar um alvo diferente por engano, e o
     * sintoma seria um perfil interpolado para um destino que nao existe.
     */
    public AuraVisualState(AuraVisualMode mode, float intensity, float transitionProgress,
            AuraDistribution distribution, int primaryColor, int secondaryColor) {
        this(mode, mode, intensity, transitionProgress, distribution, primaryColor,
                secondaryColor, AuraTransitionSample.assentado(mode));
    }

    /**
     * Se ha alguma coisa para desenhar.
     *
     * <p>O ALVO TAMBEM CONTA. Sem ele, a transicao {@code REN -> OFF} pararia de
     * desenhar no instante em que {@code mode()} virasse OFF -- que e o ultimo
     * tick, onde a intensidade ja chegou a zero -- e isso funcionaria. Mas a
     * transicao {@code OFF -> TEN} tem {@code mode() == OFF} o caminho INTEIRO,
     * e sem esta linha ela nao desenharia nada ate o ultimo quadro, aparecendo
     * de uma vez. E o "poof" que a fase de LIGAR existe para nao produzir.
     */
    public boolean enabled() {
        return (mode != AuraVisualMode.OFF || modoAlvo != AuraVisualMode.OFF)
                && intensity > 0.0F;
    }

    /** O modo cujo PERFIL carrega a leitura agora: o alvo, quando ha transicao. */
    public AuraVisualMode modoDominante() {
        return this.transitionProgress >= 0.5F ? this.modoAlvo : this.mode;
    }

    /**
     * O mesmo estado, com OUTRA distribuicao -- o caminho do ripple (#103).
     *
     * <p>Ela existe para que o impacto entre no funil sem que ninguem monte um
     * {@code AuraVisualState} a mao. Montar a mao significa repetir os oito
     * campos, e o dia em que um deles nascer e o chamador esquecer produz um
     * estado com modo certo e fases erradas -- que nao lanca, so desenha
     * diferente.
     *
     * <p>Devolve {@code this} quando a distribuicao e a mesma: isto roda por
     * jogador por quadro, e alocar um record identico e lixo por quadro.
     */
    public AuraVisualState comDistribuicao(AuraDistribution outra) {
        if (outra == null || outra.equals(this.distribution)) {
            return this;
        }
        return new AuraVisualState(mode, modoAlvo, intensity, transitionProgress, outra,
                primaryColor, secondaryColor, fases);
    }

    public static AuraVisualState desligado() {
        return new AuraVisualState(AuraVisualMode.OFF, 0.0F, 1.0F,
                AuraDistribution.zetsu(), 0xFFFFFFFF, 0xFFFFFFFF);
    }

    private static void validar(float valor, String nome) {
        if (!Float.isFinite(valor) || valor < 0.0F || valor > 1.0F) {
            throw new IllegalArgumentException(nome + " deve estar entre 0 e 1");
        }
    }
}
