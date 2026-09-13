package com.darkcontinent.nenfoundation.client.vfx.model;

import com.darkcontinent.nenfoundation.client.vfx.ribbon.AuraRibbonProfile;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Os numeros de arte da shell, carregados de DADO e nao escritos no codigo.
 *
 * <p>ELE SUBSTITUIU {@code AuraShellOpacity} e {@code AuraShellMaterial}, que
 * eram dois records com metodos {@code ten()} e {@code ren()} devolvendo
 * constantes. Enquanto a trilha AV corria, cada entrega declarava "os numeros
 * seguem no codigo, e saem quando o carregador existir". Ele existe agora.
 *
 * <p><b>RESOURCE PACK, E NAO DATAPACK</b> -- e isto e uma correcao ao que a
 * documentacao dizia. Perfil visual e decisao de CLIENTE: ele nao muda custo,
 * alcance, dano nem visibilidade autoritativa. Num datapack, o SERVIDOR passaria
 * a ditar como a aura aparece na tela de cada um, o que contradiz tanto o
 * ADR-001 quanto o proprio {@code perfis-visuais.md}, que classifica estes
 * numeros como "sessao de arte" e nao como regra. Em {@code assets/}, eles
 * recarregam com F3+T e cada pessoa pode sobrepor com um resource pack.
 *
 * <p>UM ARQUIVO POR MODO, em {@code assets/nenfoundation/nen_vfx/}. Ten e Ren
 * sao o MESMO efeito com numeros diferentes -- e ter os dois no mesmo formato e
 * o que torna essa frase verdadeira no codigo, e nao so no texto.
 *
 * <p>AS INVARIANTES VIAJAM COM O DADO. O codec valida na leitura: alpha entre 0
 * e 1, expoente de Fresnel DIMINUINDO da camada interna para a externa. Um
 * arquivo torto e recusado com motivo, e o perfil de emergencia assume -- um
 * resource pack quebrado nao pode derrubar o render.
 *
 * @param alphaInterno     filme interno; presenca no corpo
 * @param alphaBorda       a borda, que carrega a leitura
 * @param alphaExterno     halo externo; se aparecer como pele solida, esta forte demais
 * @param fresnelInterno   expoente da camada interna; MAIOR estreita a regiao brilhante
 * @param fresnelBorda     expoente da borda
 * @param fresnelExterno   expoente do halo; o menor dos tres
 * @param velocidadeDeFluxo com que rapidez a energia sobe pelo corpo
 * @param escalaDeRuido    quantas repeticoes do ruido cabem na superficie
 * @param reforcoDaBorda   quanto o Fresnel soma a intensidade
 * @param densidadeDeParticula quantas faiscas de ACABAMENTO acompanham a shell
 * @param tamanhoDeParticula   o quanto cada faisca cresce com a intensidade
 * @param filamentos           quantos, de que tamanho e de quanto em quanto tempo
 */
public record AuraPerfilVisual(
        float alphaInterno, float alphaBorda, float alphaExterno,
        float fresnelInterno, float fresnelBorda, float fresnelExterno,
        float velocidadeDeFluxo, float escalaDeRuido, float reforcoDaBorda,
        float densidadeDeParticula, float tamanhoDeParticula,
        AuraRibbonProfile filamentos) {

    /**
     * O perfil de emergencia.
     *
     * <p>ELE NAO E COPIA DE NENHUM ARQUIVO, e essa distincao importa. Se ele
     * repetisse os valores de {@code ten.json}, seriam duas fontes para a mesma
     * verdade e um dia divergiriam em silencio. Ele e conservador de proposito:
     * uma aura fraca, visivelmente mais discreta que qualquer perfil real, para
     * que "o perfil nao carregou" seja PERCEPTIVEL em vez de indistinguivel.
     */
    public static final AuraPerfilVisual SEGURO = new AuraPerfilVisual(
            0.03F, 0.10F, 0.02F, 3.0F, 2.5F, 2.0F, 0.10F, 4.0F, 0.6F, 0.02F, 0.15F,
            new AuraRibbonProfile(4, 0.15F, 0.40F, 0.007F, 1.4F));

    /** Alpha de 0 a 1. Fora disso o codec RECUSA, em vez de lancar. */
    private static final Codec<Float> ALPHA = Codec.floatRange(0.0F, 1.0F);

    /** Positivo. O minimo e pequeno, mas nao zero: escala zero colapsa a UV. */
    private static final Codec<Float> POSITIVO = Codec.floatRange(1.0e-4F, Float.MAX_VALUE);

    private static final Codec<Float> NAO_NEGATIVO = Codec.floatRange(0.0F, Float.MAX_VALUE);

    /**
     * O codec.
     *
     * <p>AS FAIXAS SAO VALIDADAS ANTES DA CONSTRUCAO, e isso nao e detalhe. Um
     * {@code Codec.FLOAT} cru deixaria o valor torto chegar ao construtor, que
     * LANCA -- e uma excecao dentro do reload de recursos derruba o
     * carregamento inteiro por causa de um resource pack quebrado. Com
     * {@code floatRange}, o mesmo arquivo vira um erro com motivo, e o perfil
     * de emergencia assume.
     *
     * <p>A ORDEM DO FRESNEL e conferida em {@code validate}, DEPOIS da
     * construcao -- e por isso ela saiu do construtor compacto. Ver o javadoc
     * de {@link #ordemDoFresnel}.
     */
    public static final Codec<AuraPerfilVisual> CODEC = RecordCodecBuilder.<AuraPerfilVisual>create(
            i -> i.group(
            ALPHA.fieldOf("alpha_interno").forGetter(AuraPerfilVisual::alphaInterno),
            ALPHA.fieldOf("alpha_borda").forGetter(AuraPerfilVisual::alphaBorda),
            ALPHA.fieldOf("alpha_externo").forGetter(AuraPerfilVisual::alphaExterno),
            POSITIVO.fieldOf("fresnel_interno").forGetter(AuraPerfilVisual::fresnelInterno),
            POSITIVO.fieldOf("fresnel_borda").forGetter(AuraPerfilVisual::fresnelBorda),
            POSITIVO.fieldOf("fresnel_externo").forGetter(AuraPerfilVisual::fresnelExterno),
            NAO_NEGATIVO.fieldOf("velocidade_de_fluxo")
                    .forGetter(AuraPerfilVisual::velocidadeDeFluxo),
            POSITIVO.fieldOf("escala_de_ruido").forGetter(AuraPerfilVisual::escalaDeRuido),
            NAO_NEGATIVO.fieldOf("reforco_da_borda").forGetter(AuraPerfilVisual::reforcoDaBorda),
            ALPHA.fieldOf("densidade_de_particula")
                    .forGetter(AuraPerfilVisual::densidadeDeParticula),
            ALPHA.fieldOf("tamanho_de_particula")
                    .forGetter(AuraPerfilVisual::tamanhoDeParticula),
            AuraRibbonProfile.CODEC.fieldOf("filamentos")
                    .forGetter(AuraPerfilVisual::filamentos))
            .apply(i, AuraPerfilVisual::new))
            .validate(AuraPerfilVisual::ordemDoFresnel);

    /**
     * O expoente de Fresnel precisa DIMINUIR da camada interna para a externa.
     *
     * <p>ELA VIVE AQUI, E NAO NO CONSTRUTOR, por um motivo especifico: e uma
     * regra entre TRES campos, e so da para confiri-la depois de ter os tres --
     * ou seja, depois de construir. No construtor, ela lancaria durante a
     * leitura do dado, e excecao no reload de recursos derruba o carregamento
     * por causa de um arquivo torto.
     *
     * <p>Com o mesmo expoente nas tres camadas, elas viram uma so mais opaca e
     * a profundidade que justifica os tres passes desaparece.
     */
    private static DataResult<AuraPerfilVisual> ordemDoFresnel(AuraPerfilVisual perfil) {
        if (perfil.fresnelInterno > perfil.fresnelBorda
                && perfil.fresnelBorda > perfil.fresnelExterno) {
            return DataResult.success(perfil);
        }
        return DataResult.error(() ->
                "o expoente de Fresnel precisa DIMINUIR da camada interna para a externa: "
                        + perfil.fresnelInterno + ", " + perfil.fresnelBorda + ", "
                        + perfil.fresnelExterno);
    }

    public AuraPerfilVisual {
        alpha(alphaInterno, "alpha_interno");
        alpha(alphaBorda, "alpha_borda");
        alpha(alphaExterno, "alpha_externo");
        positivo(fresnelInterno, "fresnel_interno");
        positivo(fresnelBorda, "fresnel_borda");
        positivo(fresnelExterno, "fresnel_externo");
        naoNegativo(velocidadeDeFluxo, "velocidade_de_fluxo");
        positivo(escalaDeRuido, "escala_de_ruido");
        naoNegativo(reforcoDaBorda, "reforco_da_borda");
        alpha(densidadeDeParticula, "densidade_de_particula");
        alpha(tamanhoDeParticula, "tamanho_de_particula");
        if (filamentos == null) {
            throw new NullPointerException("filamentos e obrigatorio; um perfil sem o bloco"
                    + " desenharia zero filamento e pareceria um perfil de Zetsu");
        }
    }

    /** O alpha deste passe. */
    public float alphaDe(AuraShellPass passe) {
        return switch (passe) {
            case INTERNA -> this.alphaInterno;
            case BORDA -> this.alphaBorda;
            case EXTERNA -> this.alphaExterno;
        };
    }

    /** O expoente de Fresnel deste passe. */
    public float fresnelDe(AuraShellPass passe) {
        return switch (passe) {
            case INTERNA -> this.fresnelInterno;
            case BORDA -> this.fresnelBorda;
            case EXTERNA -> this.fresnelExterno;
        };
    }

    /** Um perfil todo em zero, para os estados em que a ausencia e a informacao. */
    public AuraPerfilVisual apagado() {
        // A DENSIDADE DE PARTICULA ZERA JUNTO com os alphas, e o tamanho nao
        // precisa: sem densidade nenhuma faisca nasce, e um tamanho preservado
        // nao desenha nada. Zerar so os alphas -- que era o que este metodo
        // fazia antes de a particula virar dado -- deixaria Zetsu apagando a
        // shell e mantendo a nuvem de poeira, que e exatamente a leitura
        // "poeira desligada" que o ADR-015 existe para nao produzir.
        return new AuraPerfilVisual(0.0F, 0.0F, 0.0F, this.fresnelInterno, this.fresnelBorda,
                this.fresnelExterno, this.velocidadeDeFluxo, this.escalaDeRuido,
                this.reforcoDaBorda, 0.0F, this.tamanhoDeParticula,
                this.filamentos.semFilamentos());
    }

    private static void alpha(float valor, String nome) {
        if (!Float.isFinite(valor) || valor < 0.0F || valor > 1.0F) {
            throw new IllegalArgumentException(nome + " deve estar entre 0 e 1: " + valor);
        }
    }

    private static void positivo(float valor, String nome) {
        if (!Float.isFinite(valor) || valor <= 0.0F) {
            throw new IllegalArgumentException(nome + " deve ser finito e maior que zero: " + valor);
        }
    }

    private static void naoNegativo(float valor, String nome) {
        if (!Float.isFinite(valor) || valor < 0.0F) {
            throw new IllegalArgumentException(nome + " deve ser finito e nao negativo: " + valor);
        }
    }
}
