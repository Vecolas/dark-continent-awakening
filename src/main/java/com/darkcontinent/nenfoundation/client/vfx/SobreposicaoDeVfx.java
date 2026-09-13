package com.darkcontinent.nenfoundation.client.vfx;

import com.darkcontinent.nenfoundation.client.vfx.model.AuraPerfilVisual;

/**
 * Os ajustes de DIRECAO DE ARTE em runtime -- o que os comandos e os sliders
 * escrevem, e o que o desenho le.
 *
 * <p>POR QUE ELE EXISTE. Ajustar a aura recompilando custa um {@code gradlew}
 * por numero. O roteiro do gate diz a frase inteira em
 * {@code docs/testing/av-aura-visual.md} secao 2: direcao de arte sem
 * recompilar e a diferenca entre <b>uma tarde e uma semana</b>.
 *
 * <p>ELE MORA EM {@code client.vfx}, E NAO EM {@code client.vfx.debug}, de
 * proposito. Quem LE isto sao o renderer, o carregador de perfil e o emissor de
 * particulas; quem ESCREVE sao os comandos e a tela de tuning. Se o estado
 * morasse no pacote de debug, o renderer passaria a importar a interface de
 * depuracao -- e a seta ficaria apontando para o lado errado.
 *
 * <p><b>NADA AQUI PERSISTE.</b> {@link #limpar()} roda no logout, no mesmo
 * ponto de saida que ja limpa o cache e a sessao de vfx. O motivo esta escrito
 * na issue #168: sem isso, alguem aprova uma captura com um valor que nao esta
 * em perfil nenhum, e a captura passa a mentir sobre o que o jogo mostra.
 *
 * <p><b>SO O JOGADOR LOCAL.</b> O modo e o output forcados valem para quem esta
 * na maquina, e nao para os outros jogadores na tela. Forcar o estado visual de
 * OUTRA pessoa desenharia uma coisa que o servidor nunca disse -- e uma captura
 * tirada assim provaria exatamente nada. Os ajustes de PERFIL (alpha, Fresnel,
 * fluxo, ruido) valem para todo mundo, porque perfil ja e decisao de cliente.
 *
 * <p>NENHUM DESTES CAMPOS TOCA ESTADO AUTORITATIVO. Tecnica, aura, custo,
 * cooldown e visibilidade continuam sendo do servidor; daqui sai aparencia.
 *
 * <p>CLIENT-ONLY.
 */
public final class SobreposicaoDeVfx {

    /** Ausencia, para os campos numericos. Negativo nunca e valor valido em nenhum deles. */
    private static final float NENHUM = -1.0F;

    private static volatile boolean desligado;
    private static volatile boolean congelado;
    private static volatile AuraVisualMode modoForcado;
    private static volatile AuraRenderLod lodForcado;
    private static volatile float outputForcado = NENHUM;
    private static volatile float densidadeForcada = NENHUM;
    private static volatile int ribbonsForcadas = -1;

    private static volatile float alphaInterno = NENHUM;
    private static volatile float alphaBorda = NENHUM;
    private static volatile float alphaExterno = NENHUM;
    private static volatile float fatorDeFresnel = NENHUM;
    private static volatile float fluxo = NENHUM;
    private static volatile float escalaDeRuido = NENHUM;

    private SobreposicaoDeVfx() {
    }

    // ------------------------------------------------------------- escrita

    /** Desliga TODO o desenho de aura deste cliente, sem tocar na tecnica. */
    public static void desligar(boolean valor) {
        desligado = valor;
    }

    /**
     * Congela a interpolacao no quadro atual.
     *
     * <p>E o que torna possivel a verificacao do AV2 "a pool e estavel (sem
     * jitter por frame)": dois quadros seguidos da mesma coisa so se comparam
     * se a coisa parou de mudar entre eles.
     */
    public static void congelar(boolean valor) {
        congelado = valor;
    }

    /** Forca o modo visual do jogador local. {@code null} devolve o controle ao servidor. */
    public static void forcarModo(AuraVisualMode modo) {
        modoForcado = modo;
    }

    /** Forca o output visual do jogador local, de 0 a 1. Negativo devolve o controle. */
    public static void forcarOutput(float valor) {
        outputForcado = Float.isFinite(valor) && valor >= 0.0F ? Math.min(valor, 1.0F) : NENHUM;
    }

    /**
     * Forca a densidade de particulas, sem escrever na config. Negativo devolve
     * o controle.
     *
     * <p>ELE EXISTE POR CAUSA DE UMA CAPTURA ESPECIFICA. O criterio do
     * ADR-015 -- "com {@code vfx.densidadeDeParticulas = 0.0}, ainda se le
     * Ten?" -- e o que decide o gate do AV1, e ate agora ele custava editar o
     * TOML e reentrar no mundo. Duas capturas tiradas assim, com uma reentrada
     * no meio, nao compartilham hora, pose nem enquadramento -- ou seja, nao
     * sao comparaveis, que e justamente o que o protocolo A/B exige.
     *
     * <p>Ela NAO escreve na config de proposito: um valor de captura que
     * sobrevivesse a sessao viraria o padrao de alguem sem que ninguem
     * decidisse isso.
     */
    public static void forcarDensidade(float valor) {
        densidadeForcada = Float.isFinite(valor) && valor >= 0.0F ? Math.min(valor, 2.0F) : NENHUM;
    }

    /** Forca o nivel de detalhe dos OUTROS jogadores. {@code null} volta a distancia. */
    public static void forcarLod(AuraRenderLod lod) {
        lodForcado = lod;
    }

    /** Forca a contagem de filamentos. Negativo volta ao perfil. */
    public static void forcarRibbons(int quantidade) {
        ribbonsForcadas = quantidade < 0 ? -1 : quantidade;
    }

    /** Um numero de perfil, por nome. Valor negativo devolve o controle ao dado. */
    public static void forcarNoPerfil(AjusteDePerfil ajuste, float valor) {
        if (ajuste == null) {
            return;
        }
        float saneado = Float.isFinite(valor) && valor >= 0.0F ? valor : NENHUM;
        switch (ajuste) {
            case ALPHA_INTERNO -> alphaInterno = saneado;
            case ALPHA_BORDA -> alphaBorda = saneado;
            case ALPHA_EXTERNO -> alphaExterno = saneado;
            case FRESNEL -> fatorDeFresnel = saneado;
            case FLUXO -> fluxo = saneado;
            case RUIDO -> escalaDeRuido = saneado;
        }
    }

    /**
     * Quem liga, desliga.
     *
     * <p>Chamado no logout, junto do cache e da sessao de vfx. Sem isto, um
     * ajuste feito num mundo continuaria valendo no proximo -- e a proxima
     * captura sairia com um numero que ninguem lembra de ter posto.
     */
    public static void limpar() {
        desligado = false;
        congelado = false;
        modoForcado = null;
        lodForcado = null;
        outputForcado = NENHUM;
        densidadeForcada = NENHUM;
        ribbonsForcadas = -1;
        alphaInterno = NENHUM;
        alphaBorda = NENHUM;
        alphaExterno = NENHUM;
        fatorDeFresnel = NENHUM;
        fluxo = NENHUM;
        escalaDeRuido = NENHUM;
    }

    // ------------------------------------------------------------- leitura

    /** Se existe QUALQUER sobreposicao ativa. E o que acende o aviso no overlay. */
    public static boolean ativa() {
        return desligado || congelado || modoForcado != null || lodForcado != null
                || outputForcado >= 0.0F || densidadeForcada >= 0.0F || ribbonsForcadas >= 0
                || alphaInterno >= 0.0F || alphaBorda >= 0.0F || alphaExterno >= 0.0F
                || fatorDeFresnel >= 0.0F || fluxo >= 0.0F || escalaDeRuido >= 0.0F;
    }

    public static boolean desligado() {
        return desligado;
    }

    public static boolean congelado() {
        return congelado;
    }

    /** O modo forcado, ou {@code null}. */
    public static AuraVisualMode modoForcado() {
        return modoForcado;
    }

    /** O nivel de detalhe forcado, ou {@code null}. */
    public static AuraRenderLod lodForcado() {
        return lodForcado;
    }

    /** O output forcado, ou {@code Float#NaN} se nao houver. */
    public static float outputForcado() {
        return outputForcado < 0.0F ? Float.NaN : outputForcado;
    }

    /** O valor de um ajuste de perfil, ou {@code Float#NaN} se nao houver. */
    public static float valorDe(AjusteDePerfil ajuste) {
        float valor = switch (ajuste) {
            case ALPHA_INTERNO -> alphaInterno;
            case ALPHA_BORDA -> alphaBorda;
            case ALPHA_EXTERNO -> alphaExterno;
            case FRESNEL -> fatorDeFresnel;
            case FLUXO -> fluxo;
            case RUIDO -> escalaDeRuido;
        };
        return valor < 0.0F ? Float.NaN : valor;
    }

    /** A contagem de filamentos forcada, ou -1. */
    public static int ribbonsForcadas() {
        return ribbonsForcadas;
    }

    /** A densidade de particulas forcada, ou {@code Float#NaN} se nao houver. */
    public static float densidadeForcada() {
        return densidadeForcada < 0.0F ? Float.NaN : densidadeForcada;
    }

    /** A densidade a usar: a forcada, se houver; senao a da config. */
    public static double aplicarNaDensidade(double daConfig) {
        float forcada = densidadeForcada;
        return forcada < 0.0F ? daConfig : forcada;
    }

    // ---------------------------------------------------------- aplicacao

    /**
     * O estado visual do jogador LOCAL, ja com o que o dev forcou.
     *
     * <p>Este e o unico ponto de aplicacao do modo e do output: ele fica no
     * funil por onde o renderer, o emissor de particulas e a primeira pessoa
     * passam. Espalhar a mesma regra por tres consumidores faria os tres
     * divergirem -- e o sintoma seria a particula mostrando um estado e a shell
     * mostrando outro.
     */
    public static AuraVisualState aplicarNoLocal(AuraVisualState base) {
        if (desligado || base == null) {
            return AuraVisualState.desligado();
        }
        AuraVisualMode modo = modoForcado;
        float saida = outputForcado;
        if (modo == null && saida < 0.0F) {
            return base;
        }
        AuraVisualMode modoFinal = modo != null ? modo : base.mode();
        float intensidade = saida >= 0.0F ? saida : base.intensity();
        if (modoFinal == AuraVisualMode.OFF) {
            return AuraVisualState.desligado();
        }
        // A TRANSICAO VAI PARA O FIM, e nao herda o progresso do estado real.
        // Estado forcado nao esta em transicao para lugar nenhum: deixar o
        // progresso pela metade faria a primeira captura depois do comando sair
        // com a aura subindo, e a comparacao A/B pegaria dois quadros de
        // animacao diferentes sem ninguem perceber.
        return new AuraVisualState(modoFinal, base.preset(), intensidade, 1.0F,
                base.distribution(), base.primaryColor(), base.secondaryColor());
    }

    /** O estado de um jogador QUALQUER: aqui so o desligamento geral morde. */
    public static AuraVisualState aplicarEmTerceiro(AuraVisualState base) {
        if (desligado || base == null) {
            return AuraVisualState.desligado();
        }
        return base;
    }

    /** O nivel de detalhe, ja com o que o dev forcou. */
    public static AuraRenderLod aplicarNoLod(AuraRenderLod base) {
        AuraRenderLod forcado = lodForcado;
        return forcado != null ? forcado : base;
    }

    /** A contagem de filamentos, ja com o que o dev forcou. */
    public static int aplicarNasRibbons(int base) {
        int forcado = ribbonsForcadas;
        return forcado >= 0 ? forcado : base;
    }

    /**
     * O perfil visual, ja com o que os sliders mudaram.
     *
     * <p>OS VALORES SAO PRENSADOS NA FAIXA antes de construir o record. O
     * construtor de {@link AuraPerfilVisual} LANCA para alpha fora de 0..1 --
     * e uma excecao aqui acontece dentro do render, que derruba o desenho do
     * mundo inteiro por causa de um slider.
     *
     * <p>O FRESNEL E UM FATOR, e nao tres numeros. A invariante do perfil e que
     * o expoente DIMINUI da camada interna para a externa; multiplicar os tres
     * pelo mesmo fator positivo preserva a ordem, enquanto tres sliders
     * independentes a quebrariam em dois movimentos de mouse.
     */
    public static AuraPerfilVisual aplicarNoPerfil(AuraPerfilVisual base) {
        if (base == null) {
            return AuraPerfilVisual.SEGURO;
        }
        float fator = fatorDeFresnel;
        boolean mexeNoFresnel = fator > 0.0F;
        if (!mexeNoFresnel && alphaInterno < 0.0F && alphaBorda < 0.0F && alphaExterno < 0.0F
                && fluxo < 0.0F && escalaDeRuido < 0.0F) {
            return base;
        }
        return new AuraPerfilVisual(
                alpha(alphaInterno, base.alphaInterno()),
                alpha(alphaBorda, base.alphaBorda()),
                alpha(alphaExterno, base.alphaExterno()),
                positivo(mexeNoFresnel ? base.fresnelInterno() * fator : NENHUM,
                        base.fresnelInterno()),
                positivo(mexeNoFresnel ? base.fresnelBorda() * fator : NENHUM,
                        base.fresnelBorda()),
                positivo(mexeNoFresnel ? base.fresnelExterno() * fator : NENHUM,
                        base.fresnelExterno()),
                naoNegativo(fluxo, base.velocidadeDeFluxo()),
                positivo(escalaDeRuido, base.escalaDeRuido()),
                base.reforcoDaBorda());
    }

    private static float alpha(float sobreposto, float original) {
        return sobreposto < 0.0F ? original : Math.clamp(sobreposto, 0.0F, 1.0F);
    }

    private static float positivo(float sobreposto, float original) {
        return sobreposto <= 0.0F ? original : Math.min(sobreposto, 64.0F);
    }

    private static float naoNegativo(float sobreposto, float original) {
        return sobreposto < 0.0F ? original : Math.min(sobreposto, 64.0F);
    }

    /**
     * Os numeros de perfil que um slider pode girar.
     *
     * <p>O REFORCO DA BORDA FICOU DE FORA de proposito: ele e o unico do perfil
     * que soma intensidade em vez de descrever forma, e girar os dois juntos
     * (alpha da borda e reforco) faz um esconder o efeito do outro na captura.
     */
    public enum AjusteDePerfil {
        ALPHA_INTERNO("alpha_interno", 0.0F, 1.0F),
        ALPHA_BORDA("alpha_borda", 0.0F, 1.0F),
        ALPHA_EXTERNO("alpha_externo", 0.0F, 1.0F),
        FRESNEL("fresnel", 0.1F, 4.0F),
        FLUXO("fluxo", 0.0F, 1.0F),
        RUIDO("ruido", 0.5F, 16.0F);

        private final String nome;
        private final float minimo;
        private final float maximo;

        AjusteDePerfil(String nome, float minimo, float maximo) {
            this.nome = nome;
            this.minimo = minimo;
            this.maximo = maximo;
        }

        /** O nome usado no comando e no slider. */
        public String nome() {
            return this.nome;
        }

        public float minimo() {
            return this.minimo;
        }

        public float maximo() {
            return this.maximo;
        }

        /** O ajuste com este nome, ou {@code null}. */
        public static AjusteDePerfil porNome(String nome) {
            for (AjusteDePerfil ajuste : values()) {
                if (ajuste.nome.equals(nome)) {
                    return ajuste;
                }
            }
            return null;
        }
    }
}
