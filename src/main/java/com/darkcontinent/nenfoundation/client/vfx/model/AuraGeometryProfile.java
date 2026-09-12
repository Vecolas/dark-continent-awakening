package com.darkcontinent.nenfoundation.client.vfx.model;

import net.minecraft.client.model.geom.builders.CubeDeformation;

/**
 * As espessuras da shell, e a UNICA conversao de bloco para unidade de modelo.
 *
 * <p>OS NUMEROS ESTAO EM BLOCOS, e {@link CubeDeformation} recebe UNIDADES DE
 * MODELO. Um bloco tem dezesseis unidades, entao a conversao e {@code x16} --
 * e ela mora aqui, num lugar so.
 *
 * <p>ESPALHAR A CONVERSAO E O DEFEITO QUE ESTA CLASSE EXISTE PARA IMPEDIR.
 * {@code new CubeDeformation(0.032F)} e perfeitamente valido: ele infla dois
 * MILESIMOS de bloco. Nao lanca, nao avisa, e a shell simplesmente nao aparece
 * -- o pior relato de bug que existe, porque nao ha erro para procurar.
 *
 * <p>Para calibrar o olho: a segunda camada da skin vanilla usa 0,25 unidade
 * (jaqueta e mangas) e 0,5 unidade (chapeu). A camada interna de Ten, 0,032
 * bloco, da 0,51 unidade -- ela passa POR FORA do overlay da skin, que e
 * exatamente o requisito.
 *
 * <p>AS TRES ESPESSURAS SAO ESTRITAMENTE CRESCENTES, e o construtor recusa o
 * contrario. Duas superficies na mesma posicao e z-fighting garantido, e
 * z-fighting nao aparece como erro: aparece como cintilacao que alguem vai
 * tentar consertar no shader meses depois.
 *
 * <p>OS VALORES SAO PONTO DE PARTIDA DE DIRECAO DE ARTE, nunca medidos em jogo.
 * Eles vivem aqui porque o carregador de perfil em datapack ainda nao existe --
 * ele e a issue #98. Quando chegar, estes valores SAEM daqui: numero que foi
 * para o dado tem de sair do codigo, ou a proxima pessoa gira um botao morto.
 *
 * @param espessuraInterna filme interno, em blocos
 * @param espessuraBorda   a borda, em blocos
 * @param espessuraExterna halo externo, em blocos
 */
public record AuraGeometryProfile(float espessuraInterna, float espessuraBorda,
        float espessuraExterna) {

    /** Unidades de modelo por bloco. Nao e ajuste: e a escala do formato. */
    public static final float UNIDADES_POR_BLOCO = 16.0F;

    /**
     * Teto de espessura, em blocos.
     *
     * <p>LIMITE DE DESIGN, e nao botao de tuning. Poder extremo aumenta
     * densidade, brilho, velocidade e pressao -- nao tamanho. Sem este teto, a
     * primeira sessao de balanceamento transforma o jogador numa esfera.
     */
    public static final float ESPESSURA_MAXIMA = 0.25F;

    public AuraGeometryProfile {
        validar(espessuraInterna, "espessuraInterna");
        validar(espessuraBorda, "espessuraBorda");
        validar(espessuraExterna, "espessuraExterna");
        if (!(espessuraInterna < espessuraBorda && espessuraBorda < espessuraExterna)) {
            throw new IllegalArgumentException(
                    "as espessuras devem ser estritamente crescentes (interna < borda < externa);"
                            + " superficies coincidentes produzem z-fighting: "
                            + espessuraInterna + ", " + espessuraBorda + ", " + espessuraExterna);
        }
    }

    /** O perfil de Ten. Ponto de partida de arte; ver o javadoc da classe. */
    public static AuraGeometryProfile ten() {
        return new AuraGeometryProfile(0.032F, 0.052F, 0.078F);
    }

    /** O perfil de Ren: a MESMA shell, mais densa. Nao e outro efeito. */
    public static AuraGeometryProfile ren() {
        return new AuraGeometryProfile(0.045F, 0.072F, 0.110F);
    }

    /** A espessura deste passe, em blocos. */
    public float espessuraDe(AuraShellPass passe) {
        return switch (passe) {
            case INTERNA -> this.espessuraInterna;
            case BORDA -> this.espessuraBorda;
            case EXTERNA -> this.espessuraExterna;
        };
    }

    /**
     * A espessura deste passe em UNIDADES DE MODELO -- a conversao, isolada.
     *
     * <p>Ela existe separada de {@link #deformacaoDe} para poder ser provada
     * sem nenhum tipo do Minecraft: os campos de {@link CubeDeformation} nao sao
     * publicos, entao um teste que so tivesse a deformacao nao teria como
     * conferir o numero -- e a conversao esquecida e justamente o defeito que
     * nao lanca nada.
     */
    public float unidadesDe(AuraShellPass passe) {
        return espessuraDe(passe) * UNIDADES_POR_BLOCO;
    }

    /** A deformacao deste passe, ja em unidades de modelo. */
    public CubeDeformation deformacaoDe(AuraShellPass passe) {
        return new CubeDeformation(unidadesDe(passe));
    }

    private static void validar(float valor, String nome) {
        if (!Float.isFinite(valor) || valor <= 0.0F) {
            throw new IllegalArgumentException(nome + " deve ser finito e maior que zero: " + valor);
        }
        if (valor > ESPESSURA_MAXIMA) {
            throw new IllegalArgumentException(nome + " passa do teto de design de "
                    + ESPESSURA_MAXIMA + " bloco: " + valor);
        }
    }
}
