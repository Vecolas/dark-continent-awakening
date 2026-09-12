package com.darkcontinent.nenfoundation.nen.aura;

import java.util.EnumMap;
import java.util.Map;

/**
 * Quanto da aura esta em cada regiao do corpo.
 *
 * <p>A SOMA E SEMPRE 1.0, e e essa invariante que da risco as tecnicas.
 * Concentrar num lugar TIRA de outro: e isso que faz Gyo ser uma escolha e Ko
 * ser perigoso. Uma alocacao que nao fechasse deixaria de ser troca e viraria
 * bonus -- e um bonus nao tem como ser errado, entao nao tem como ser tatico.
 *
 * <p>IMUTAVEL. Redistribuir devolve uma alocacao nova. Com Ryu a redistribuicao
 * acontece constantemente e em cima de estado compartilhado; um objeto mutavel
 * ali produziria o erro numero 2 da lista do CLAUDE.md -- dois jogadores
 * escrevendo no mesmo lugar, sem erro nenhum, so com aura trocada.
 *
 * <p>VIVE NO SERVIDOR. Existe um {@code AuraDistribution} em {@code client.vfx}
 * que e cosmetico e nasceu antes desta classe; ele passa a ser projecao do que
 * o servidor mandar. Ver o <b>ADR-014</b>.
 *
 * <p>ELA NAO E SEIS RESERVAS. A aura continua sendo um pool so; a alocacao diz
 * como esse pool e APLICADO, e nao onde ele e guardado.
 */
public final class AlocacaoDeAura {

    /**
     * Folga da soma, em ponto flutuante.
     *
     * <p>Somar seis floats de 1/6 nao da exatamente 1.0, e recusar por causa
     * disso rejeitaria a alocacao mais comum que existe -- a de repouso.
     */
    private static final float FOLGA = 1.0e-4F;

    private static final AlocacaoDeAura UNIFORME = construirUniforme();

    private final Map<RegiaoDoCorpo, Float> fracoes;

    private AlocacaoDeAura(Map<RegiaoDoCorpo, Float> fracoes) {
        this.fracoes = fracoes;
    }

    /** Aura espalhada por igual: o estado de quem nao esta concentrando nada. */
    public static AlocacaoDeAura uniforme() {
        return UNIFORME;
    }

    /**
     * Concentra uma fracao numa regiao, e divide o resto entre as outras.
     *
     * <p>E A OPERACAO DE GYO E DE KO, com politicas diferentes so no numero:
     * Gyo concentra uma parte, Ko concentra quase tudo. Nao ha dois metodos
     * porque nao ha duas contas.
     *
     * @param regiao      onde concentrar
     * @param concentrada de 0 a 1; quanto da aura total vai para ela
     * @throws IllegalArgumentException se a fracao nao for finita ou sair de 0..1
     */
    public static AlocacaoDeAura concentrando(RegiaoDoCorpo regiao, float concentrada) {
        if (regiao == null) {
            throw new IllegalArgumentException("regiao obrigatoria");
        }
        if (!Float.isFinite(concentrada) || concentrada < 0.0F || concentrada > 1.0F) {
            // NaN CHEGA AQUI SE NINGUEM BARRAR. O clamp com Math.min/max nao
            // segura NaN -- ele o propaga -- e uma alocacao com NaN quebraria a
            // soma sem lancar nada.
            throw new IllegalArgumentException(
                    "fracao concentrada invalida: " + concentrada);
        }

        int outras = RegiaoDoCorpo.values().length - 1;
        float restoPorRegiao = (1.0F - concentrada) / outras;

        EnumMap<RegiaoDoCorpo, Float> mapa = new EnumMap<>(RegiaoDoCorpo.class);
        for (RegiaoDoCorpo r : RegiaoDoCorpo.values()) {
            mapa.put(r, r == regiao ? concentrada : restoPorRegiao);
        }
        return new AlocacaoDeAura(Map.copyOf(mapa));
    }

    /**
     * Reconstroi uma alocacao a partir das fracoes, NA ORDEM DO ENUM.
     *
     * <p>DEVOLVE VAZIO em vez de lancar quando os numeros nao fecham. Ela existe
     * para a leitura de rede, e pacote malformado nao pode derrubar a conexao
     * do cliente -- quem chama decide o que fazer, e o padrao seguro e a
     * alocacao de repouso.
     *
     * <p>RECUSA, E NAO NORMALIZA. Corrigir fracoes tortas aqui esconderia um
     * emissor quebrado atras de numeros plausiveis, e o defeito apareceria
     * meses depois como aura no lugar errado.
     */
    public static java.util.Optional<AlocacaoDeAura> deFracoes(float[] fracoes) {
        RegiaoDoCorpo[] regioes = RegiaoDoCorpo.values();
        if (fracoes == null || fracoes.length != regioes.length) {
            return java.util.Optional.empty();
        }
        EnumMap<RegiaoDoCorpo, Float> mapa = new EnumMap<>(RegiaoDoCorpo.class);
        for (int i = 0; i < regioes.length; i++) {
            if (!Float.isFinite(fracoes[i]) || fracoes[i] < 0.0F) {
                return java.util.Optional.empty();
            }
            mapa.put(regioes[i], fracoes[i]);
        }
        AlocacaoDeAura candidata = new AlocacaoDeAura(Map.copyOf(mapa));
        return candidata.soma() ? java.util.Optional.of(candidata) : java.util.Optional.empty();
    }

    /** Quanto da aura esta nesta regiao, de 0 a 1. */
    public float em(RegiaoDoCorpo regiao) {
        return this.fracoes.getOrDefault(regiao, 0.0F);
    }

    /** A regiao com mais aura. Empate devolve a primeira na ordem do enum. */
    public RegiaoDoCorpo maisConcentrada() {
        RegiaoDoCorpo maior = RegiaoDoCorpo.CABECA;
        for (RegiaoDoCorpo r : RegiaoDoCorpo.values()) {
            if (em(r) > em(maior)) {
                maior = r;
            }
        }
        return maior;
    }

    /**
     * Se a soma fecha em 1.0, dentro da folga de ponto flutuante.
     *
     * <p>PUBLICO para o portao poder conferir qualquer alocacao construida por
     * qualquer caminho -- inclusive um que ainda nao existe.
     */
    public boolean soma() {
        float total = 0.0F;
        for (RegiaoDoCorpo r : RegiaoDoCorpo.values()) {
            total += em(r);
        }
        return Math.abs(total - 1.0F) <= FOLGA;
    }

    /** As fracoes, em ordem de {@link RegiaoDoCorpo}. Para a rede e para o desenho. */
    public Map<RegiaoDoCorpo, Float> comoMapa() {
        return this.fracoes;
    }

    @Override
    public boolean equals(Object outro) {
        return outro instanceof AlocacaoDeAura a && this.fracoes.equals(a.fracoes);
    }

    @Override
    public int hashCode() {
        return this.fracoes.hashCode();
    }

    @Override
    public String toString() {
        return "AlocacaoDeAura" + this.fracoes;
    }

    private static AlocacaoDeAura construirUniforme() {
        float fatia = RegiaoDoCorpo.fracaoUniforme();
        EnumMap<RegiaoDoCorpo, Float> mapa = new EnumMap<>(RegiaoDoCorpo.class);
        for (RegiaoDoCorpo r : RegiaoDoCorpo.values()) {
            mapa.put(r, fatia);
        }
        return new AlocacaoDeAura(Map.copyOf(mapa));
    }
}
