package com.darkcontinent.nenfoundation.client.vfx.model;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * O que so existe quando a aura e LIBERADA: colunas, anel de chao e detritos.
 *
 * <p>UM BLOCO SO, E NAO OITO CAMPOS SOLTOS, por duas razoes. A primeira e de
 * leitura: os tres componentes nascem juntos na mesma janela da transicao
 * TEN para REN e somem juntos na volta; separa-los no arquivo convidaria alguem
 * a ligar coluna sem pressao, que e uma leitura que a direcao de arte nao pede.
 * A segunda e aritmetica: {@code RecordCodecBuilder} para em dezesseis campos, e
 * {@link AuraPerfilVisual} ja usava doze.
 *
 * <p><b>TEN TEM ESTE BLOCO, E ELE E TODO ZERO.</b> Isso e escolha, e nao
 * descuido: a ausencia e a informacao, e ela precisa estar escrita no dado. Um
 * bloco opcional com padrao zero daria o mesmo resultado hoje e permitiria
 * amanha um perfil que esqueceu a pressao -- e "esqueceu" e indistinguivel de
 * "zero de proposito" quando ninguem escreveu qual era.
 *
 * <p>OS TETOS SAO CONSTANTES DE CODIGO, e nao chaves. Oito colunas, quarenta e
 * oito segmentos e doze detritos sao limites de DESENHO: passar de oito colunas
 * transforma pressao em fogueira e inverte a hierarquia de leitura (personagem,
 * borda, filamentos, chao, faiscas -- nessa ordem). Quem quer menos mexe na
 * densidade, que e config; o teto existe para que um numero absurdo num resource
 * pack nao trave o cliente.
 *
 * @param colunas        quantas correntes verticais, no detalhe cheio
 * @param alturaMinima   altura da coluna mais curta, em BLOCOS
 * @param alturaMaxima   altura da coluna mais alta, em BLOCOS
 * @param anel           quanto o anel de pressao aparece, de 0 a 1
 * @param anelRaioMinimo raio do anel com intensidade zero, em BLOCOS
 * @param anelRaioMaximo raio do anel com intensidade cheia, em BLOCOS
 * @param anelSegmentos  quantos segmentos a malha horizontal tem
 * @param detritos       quantos cubos cosmeticos sobem do chao
 */
public record AuraPerfilDePressao(int colunas, float alturaMinima, float alturaMaxima,
        float anel, float anelRaioMinimo, float anelRaioMaximo, int anelSegmentos,
        int detritos) {

    /**
     * Teto duro de colunas.
     *
     * <p>Passar daqui transforma pressao em fogueira. Limite de desenho, e nao
     * botao de ajuste.
     */
    public static final int TETO_DE_COLUNAS = 8;

    /** Teto de segmentos do anel. Acima disso o custo sobe e a forma nao muda. */
    public static final int TETO_DE_SEGMENTOS = 48;

    /** Teto de detritos vivos por jogador. Trava de seguranca contra config absurda. */
    public static final int TETO_DE_DETRITOS = 12;

    /** Raio maximo do anel, em blocos. Acima disso o chao compete com o personagem. */
    public static final float RAIO_MAXIMO = 2.0F;

    /** Altura maxima de uma coluna, em blocos. */
    public static final float ALTURA_MAXIMA = 2.5F;

    /** Nenhuma pressao. E o bloco de Ten, e o de Zetsu. */
    public static final AuraPerfilDePressao NENHUMA =
            new AuraPerfilDePressao(0, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0, 0);

    public static final Codec<AuraPerfilDePressao> CODEC = RecordCodecBuilder
            .<AuraPerfilDePressao>create(i -> i.group(
                    Codec.intRange(0, TETO_DE_COLUNAS).fieldOf("colunas")
                            .forGetter(AuraPerfilDePressao::colunas),
                    Codec.floatRange(0.0F, ALTURA_MAXIMA).fieldOf("altura_minima")
                            .forGetter(AuraPerfilDePressao::alturaMinima),
                    Codec.floatRange(0.0F, ALTURA_MAXIMA).fieldOf("altura_maxima")
                            .forGetter(AuraPerfilDePressao::alturaMaxima),
                    Codec.floatRange(0.0F, 1.0F).fieldOf("anel")
                            .forGetter(AuraPerfilDePressao::anel),
                    Codec.floatRange(0.0F, RAIO_MAXIMO).fieldOf("anel_raio_minimo")
                            .forGetter(AuraPerfilDePressao::anelRaioMinimo),
                    Codec.floatRange(0.0F, RAIO_MAXIMO).fieldOf("anel_raio_maximo")
                            .forGetter(AuraPerfilDePressao::anelRaioMaximo),
                    Codec.intRange(0, TETO_DE_SEGMENTOS).fieldOf("anel_segmentos")
                            .forGetter(AuraPerfilDePressao::anelSegmentos),
                    Codec.intRange(0, TETO_DE_DETRITOS).fieldOf("detritos")
                            .forGetter(AuraPerfilDePressao::detritos))
                    .apply(i, AuraPerfilDePressao::new))
            .validate(AuraPerfilDePressao::ordemDasFaixas);

    /**
     * Minimo antes do maximo, nas duas faixas.
     *
     * <p>EM {@code validate}, E NAO NO CONSTRUTOR, pelo mesmo motivo que a ordem
     * do Fresnel: sao regras entre DOIS campos, e o construtor LANCA -- uma
     * excecao dentro do reload de recursos derruba o carregamento inteiro por
     * causa de um resource pack torto, em vez de virar um erro com motivo.
     */
    private static DataResult<AuraPerfilDePressao> ordemDasFaixas(AuraPerfilDePressao perfil) {
        if (perfil.alturaMinima > perfil.alturaMaxima) {
            return DataResult.error(() -> "altura_minima (" + perfil.alturaMinima
                    + ") e maior que altura_maxima (" + perfil.alturaMaxima + ")");
        }
        if (perfil.anelRaioMinimo > perfil.anelRaioMaximo) {
            return DataResult.error(() -> "anel_raio_minimo (" + perfil.anelRaioMinimo
                    + ") e maior que anel_raio_maximo (" + perfil.anelRaioMaximo + ")");
        }
        return DataResult.success(perfil);
    }

    public AuraPerfilDePressao {
        teto(colunas, TETO_DE_COLUNAS, "colunas");
        teto(anelSegmentos, TETO_DE_SEGMENTOS, "anel_segmentos");
        teto(detritos, TETO_DE_DETRITOS, "detritos");
        faixa(alturaMinima, ALTURA_MAXIMA, "altura_minima");
        faixa(alturaMaxima, ALTURA_MAXIMA, "altura_maxima");
        faixa(anel, 1.0F, "anel");
        faixa(anelRaioMinimo, RAIO_MAXIMO, "anel_raio_minimo");
        faixa(anelRaioMaximo, RAIO_MAXIMO, "anel_raio_maximo");
    }

    /** Se ha alguma coisa para desenhar. Ten responde {@code false}. */
    public boolean existe() {
        return this.colunas > 0 || this.anel > 0.0F || this.detritos > 0;
    }

    /**
     * O raio do anel para uma intensidade visual.
     *
     * <p>A INTERPOLACAO E DAQUI, e nao de quem desenha: o anel e os detritos
     * precisam do MESMO raio -- os detritos nascem dentro dele --, e duas contas
     * para o mesmo raio sao duas verdades que um dia discordam.
     */
    public float raioPara(float intensidade) {
        float t = Math.clamp(intensidade, 0.0F, 1.0F);
        return this.anelRaioMinimo + (this.anelRaioMaximo - this.anelRaioMinimo) * t;
    }

    /** A altura da coluna sorteada por esta semente, dentro da faixa. */
    public float alturaDe(long semente) {
        float t = ((semente >>> 24) & 0xFFFF) / 65535.0F;
        return this.alturaMinima + (this.alturaMaxima - this.alturaMinima) * t;
    }

    /** O mesmo bloco, em zero. Estrutura, e nao arte: Zetsu e ausencia total. */
    public AuraPerfilDePressao apagado() {
        return NENHUMA;
    }

    /**
     * O bloco a meio caminho entre dois.
     *
     * <p>AS CONTAGENS ARREDONDAM, e nao truncam. Com truncamento, o ultimo passo
     * antes de chegar ao alvo desenharia cinco colunas de seis pedidas -- e a
     * sexta apareceria de uma vez no quadro final, que e exatamente a troca seca
     * que a transicao em fases existe para nao produzir.
     */
    public static AuraPerfilDePressao interpolar(AuraPerfilDePressao a, AuraPerfilDePressao b,
            float t) {
        float u = Math.clamp(t, 0.0F, 1.0F);
        return new AuraPerfilDePressao(
                Math.round(a.colunas + (b.colunas - a.colunas) * u),
                ler(a.alturaMinima, b.alturaMinima, u),
                ler(a.alturaMaxima, b.alturaMaxima, u),
                ler(a.anel, b.anel, u),
                ler(a.anelRaioMinimo, b.anelRaioMinimo, u),
                ler(a.anelRaioMaximo, b.anelRaioMaximo, u),
                Math.round(a.anelSegmentos + (b.anelSegmentos - a.anelSegmentos) * u),
                Math.round(a.detritos + (b.detritos - a.detritos) * u));
    }

    private static float ler(float a, float b, float t) {
        return a + (b - a) * t;
    }

    private static void teto(int valor, int limite, String nome) {
        if (valor < 0 || valor > limite) {
            throw new IllegalArgumentException(
                    nome + " fora do teto de design de " + limite + ": " + valor);
        }
    }

    private static void faixa(float valor, float limite, String nome) {
        if (!Float.isFinite(valor) || valor < 0.0F || valor > limite) {
            throw new IllegalArgumentException(
                    nome + " deve estar entre 0 e " + limite + ": " + valor);
        }
    }
}
