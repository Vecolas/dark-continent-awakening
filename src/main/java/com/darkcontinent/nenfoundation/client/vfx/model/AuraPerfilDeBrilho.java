package com.darkcontinent.nenfoundation.client.vfx.model;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Quanto este modo BRILHA, e em que raio.
 *
 * <p>DOIS NUMEROS, E NAO UM. A forca decide o peso do composite aditivo; o raio
 * decide o TAMANHO do halo. Sao coisas diferentes e falham de formas diferentes:
 * forca alta demais estoura a imagem, raio alto demais vira NEVOA. Trinta pixels
 * de halo nao e "bloom forte" -- e outro efeito, e um que apaga a silhueta do
 * personagem, que e o item 1 da hierarquia de leitura.
 *
 * <p><b>O RAIO MORA NO PERFIL, E O TETO MORA NO CODIGO.</b> A sessao de arte
 * precisa girar o raio por tecnica -- Ten pede 2 a 3 px e Ren pede 4 a 7 --, e
 * por isso ele e dado. O maximo absoluto e limite de DESENHO: acima dele o
 * efeito deixa de ser halo, e nenhum ajuste de arte deveria poder atravessar
 * essa fronteira.
 *
 * <p>O RAIO E EM PIXELS DE TELA, e nao em texels do alvo. Quem desenha converte,
 * porque o alvo e de meia resolucao e a conta de conversao e do pipeline, nao da
 * arte. Escrever texels aqui faria o mesmo perfil produzir halos de tamanhos
 * diferentes conforme a resolucao da janela.
 *
 * <p><b>ELE SUBSTITUIU UM {@code float bloom} SOLTO, e a troca conserta uma
 * divida de um marco.</b> O AV4 declarou a chave sem consumidor -- o erro numero
 * 7 do {@code CLAUDE.md} em miniatura, e declarado como tal no proprio commit.
 * O consumidor existe a partir do AV5, e a chave vira objeto agora porque e
 * agora que ela ganha o segundo numero.
 *
 * @param forca peso do composite aditivo, de 0 a 1
 * @param raio  raio do desfoque, em PIXELS DE TELA
 */
public record AuraPerfilDeBrilho(float forca, float raio) {

    /**
     * Raio maximo, em pixels de tela.
     *
     * <p>TRAVADO EM CODIGO COMO LIMITE DE DESENHO, e nao como botao. A
     * intensidade e que e botao. Oito pixels ja e generoso para Ren; trinta e
     * nevoa, e nevoa nao le como energia -- le como lente suja.
     */
    public static final float RAIO_MAXIMO = 8.0F;

    /** Sem brilho nenhum. E o de Zetsu, e o de quem nunca despertou. */
    public static final AuraPerfilDeBrilho NENHUM = new AuraPerfilDeBrilho(0.0F, 0.0F);

    public static final Codec<AuraPerfilDeBrilho> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.floatRange(0.0F, 1.0F).fieldOf("forca").forGetter(AuraPerfilDeBrilho::forca),
            Codec.floatRange(0.0F, RAIO_MAXIMO).fieldOf("raio")
                    .forGetter(AuraPerfilDeBrilho::raio))
            .apply(i, AuraPerfilDeBrilho::new));

    public AuraPerfilDeBrilho {
        if (!Float.isFinite(forca) || forca < 0.0F || forca > 1.0F) {
            throw new IllegalArgumentException("forca do brilho fora de 0..1: " + forca);
        }
        if (!Float.isFinite(raio) || raio < 0.0F || raio > RAIO_MAXIMO) {
            throw new IllegalArgumentException("raio do brilho fora de 0.." + RAIO_MAXIMO
                    + " px: " + raio + ". Acima disso o halo vira nevoa, e nevoa apaga a"
                    + " silhueta do personagem");
        }
    }

    /** Se ha brilho a somar. Zero em qualquer um dos dois ja e "nao ha". */
    public boolean existe() {
        return this.forca > 0.0F && this.raio > 0.0F;
    }

    /** O mesmo bloco, apagado. */
    public AuraPerfilDeBrilho apagado() {
        return NENHUM;
    }

    /** O bloco a meio caminho entre dois. */
    public static AuraPerfilDeBrilho interpolar(AuraPerfilDeBrilho a, AuraPerfilDeBrilho b,
            float t) {
        float u = Math.clamp(t, 0.0F, 1.0F);
        return new AuraPerfilDeBrilho(a.forca + (b.forca - a.forca) * u,
                a.raio + (b.raio - a.raio) * u);
    }
}
