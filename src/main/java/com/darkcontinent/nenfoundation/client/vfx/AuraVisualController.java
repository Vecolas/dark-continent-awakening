package com.darkcontinent.nenfoundation.client.vfx;

/**
 * Interpolador client-side. Recebe apenas intencao visual ja autorizada pelo
 * servidor; animacao local nao altera aura, output ou estado de Nen.
 */
public final class AuraVisualController {
    private AuraVisualState atual = AuraVisualState.desligado();

    /**
     * De onde a transicao atual PARTIU, congelado.
     *
     * <p>ELE EXISTE POR CAUSA DE UM DEFEITO REAL. Antes, {@code avancar}
     * interpolava de {@code atual} para {@code alvo} e reescrevia {@code atual}
     * -- ou seja, cada passo saia de um ponto que o passo anterior ja tinha
     * movido. O resultado nao era a curva suave: era um arrasto lento seguido
     * de um ESTALO no ultimo tick, quando {@code t} chega a 1 e cobre de uma vez
     * tudo o que faltava.
     *
     * <p>Os testes nao pegavam porque so usavam passos de 0.5 e 1.0, onde a
     * diferenca nao aparece. Com vinte passos de 0.05 -- que e o caso real de
     * uma transicao de um segundo -- ela aparece inteira.
     */
    private AuraVisualState origem = AuraVisualState.desligado();
    private AuraVisualState alvo = AuraVisualState.desligado();
    private float transicao;

    /**
     * A transicao em curso.
     *
     * <p>ESCOLHIDA NO `receber`, e nao no `avancar`: o par (origem, destino)
     * so existe no instante da troca. Perguntar de novo a cada tick daria a
     * resposta errada assim que `atual.mode()` alcancasse o destino.
     */
    private AuraTransicao transicaoEmCurso = AuraTransicao.PADRAO;

    public AuraVisualState atual() { return atual; }

    public void receber(AuraVisualMode modo, float intensidade) {
        receber(modo, intensidade, AuraDistribution.uniforme());
    }

    public void receber(AuraVisualMode modo, float intensidade, AuraDistribution distribuicao) {
        receber(modo, intensidade, distribuicao, 0xFFFFFFFF, 0xFFFFFFFF);
    }

    /**
     * Idem, com a cor vinda de fora.
     *
     * <p>A COR NAO MORA AQUI, e nao deve morar. Ela e a mesma que identifica a
     * tecnica no HUD, e ter duas fontes para "de que cor e Ren" garante que um
     * dia as duas discordem -- o erro numero 7 da lista do CLAUDE.md. Quem
     * chama sabe qual tecnica esta ligada e passa a cor dela.
     *
     * <p>Antes desta sobrecarga o controlador gravava {@code 0xFFFFFFFF} fixo e
     * um {@code AuraVisualProfile} carregava campos de cor que ninguem lia. Esse
     * record foi removido junto do {@code AuraVisualPreset}: os numeros de arte
     * moram no perfil de {@code assets/nenfoundation/nen_vfx/}.
     */
    public void receber(AuraVisualMode modo, float intensidade, AuraDistribution distribuicao,
            int corPrimaria, int corSecundaria) {
        if (modo == null || distribuicao == null || !Float.isFinite(intensidade)
                || intensidade < 0.0F || intensidade > 1.0F) {
            throw new IllegalArgumentException("comando visual invalido");
        }
        this.alvo = new AuraVisualState(modo, modo == AuraVisualMode.ZETSU ? 0.0F : intensidade,
                0.0F, modo == AuraVisualMode.ZETSU ? AuraDistribution.zetsu() : distribuicao,
                corPrimaria, corSecundaria);
        this.transicaoEmCurso = AuraTransicao.de(this.atual.mode(), modo);
        this.origem = this.atual;
        this.transicao = 0.0F;
    }

    /**
     * Avanca a animacao um tick.
     *
     * <p>A DURACAO VEM DA TABELA, e nao de quem chama. Antes era um passo unico
     * para tudo, e com ele ligar Ten e explodir em Ren levavam o mesmo tempo --
     * uma aura em que nenhuma troca tem peso.
     *
     * @param escala multiplicador do jogador; 1.0 e o tempo de projeto
     */
    public AuraVisualState avancar(float escala) {
        float passo = this.transicaoEmCurso.passoPorTick(escala);
        this.transicao = Math.min(1.0F, this.transicao + passo);
        float t = this.transicaoEmCurso.curva().aplicar(this.transicao);
        // DE `origem` PARA `alvo`, e nunca de `atual`: interpolar a partir do
        // valor ja movido transforma a curva num arrasto com estalo no fim.
        //
        // OS PESOS SAIEM DO PROGRESSO CRU, e nao do curvado: a linha do tempo
        // ja esta escrita em milissegundos com as proprias curvas por janela, e
        // curvar duas vezes deslocaria cada fase de um jeito que ninguem
        // consegue prever lendo a tabela.
        this.atual = interpolar(this.origem, this.alvo, t, this.transicao,
                this.transicaoEmCurso.amostrar(this.transicao));
        return this.atual;
    }

    public void limpar() {
        this.transicaoEmCurso = AuraTransicao.PADRAO;
        this.atual = AuraVisualState.desligado();
        this.alvo = this.atual;
        this.origem = this.atual;
        this.transicao = 1.0F;
    }

    /**
     * @param t         o progresso JA CURVADO; com OVERSHOOT ele passa de 1
     * @param progresso o progresso CRU, de 0 a 1, que decide quando o modo troca
     */
    private static AuraVisualState interpolar(AuraVisualState a, AuraVisualState b, float t,
            float progresso, AuraTransitionSample fases) {
        AuraDistribution d = new AuraDistribution(
                ler(a.distribution().head(), b.distribution().head(), t),
                ler(a.distribution().torso(), b.distribution().torso(), t),
                ler(a.distribution().leftArm(), b.distribution().leftArm(), t),
                ler(a.distribution().rightArm(), b.distribution().rightArm(), t),
                ler(a.distribution().leftLeg(), b.distribution().leftLeg(), t),
                ler(a.distribution().rightLeg(), b.distribution().rightLeg(), t));
        // O MODO TROCA PELO PROGRESSO CRU, e nunca pelo curvado.
        //
        // ISTO FOI UM DEFEITO REAL, e silencioso: com OVERSHOOT o valor curvado
        // ULTRAPASSA 1 na metade do caminho -- de proposito, e e o que faz Ren
        // parecer liberacao. Amarrado a ele, o modo virava REN quando a
        // transicao estava pela metade, e a aura "pulava" de estado antes da
        // hora. Nada lancava; so parecia apressado.
        boolean chegou = progresso >= 1.0F;
        // O PROGRESSO GRAVADO E SATURADO. Com OVERSHOOT o `t` passa de 1 no
        // meio do caminho -- de proposito --, mas `AuraVisualState` valida
        // `transitionProgress` entre 0 e 1 e lancaria. Excecao no tick de render
        // derruba o desenho do mundo, e nao so a aura.
        return new AuraVisualState(chegou ? b.mode() : a.mode(),
                // O ALVO E SEMPRE O DESTINO, inclusive antes de chegar. E ele
                // que permite interpolar o PERFIL entre as duas pontas em vez
                // de trocar de preset num quadro so.
                b.mode(),
                Math.clamp(ler(a.intensity(), b.intensity(), t), 0.0F, 1.0F),
                Math.clamp(t, 0.0F, 1.0F), d, b.primaryColor(), b.secondaryColor(), fases);
    }

    private static float ler(float a, float b, float t) { return a + (b - a) * t; }
}
