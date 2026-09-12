package com.darkcontinent.nenfoundation.client.vfx.model;

/**
 * Os parametros que o shader da shell le, por modo de Nen.
 *
 * <p>PER-QUADRO, como {@link AuraShellOpacity} e ao contrario de
 * {@link AuraGeometryProfile}: mudar um destes numeros nao reconstroi malha
 * nenhuma, entao um slider em modo dev responde na hora.
 *
 * <p>O EXPOENTE DE FRESNEL VARIA POR CAMADA, e essa e a razao de as tres
 * existirem. Expoente ALTO deixa a regiao brilhante ESTREITA; baixo, espessa.
 * A camada interna usa o maior -- so a silhueta acende --, e o halo externo o
 * menor, para alargar o contorno. Com o mesmo expoente nas tres, as camadas
 * viram uma so mais opaca, e a profundidade que justifica os tres passes
 * desaparece.
 *
 * <p>REN BAIXA OS TRES EXPOENTES, e por isso a borda dele e mais grossa. Nao e
 * outro efeito: e o mesmo, com a borda aberta.
 *
 * <p>NENHUM DESTES NUMEROS FOI MEDIDO EM JOGO. Sao ponto de partida de direcao
 * de arte, e vao mudar na primeira sessao de captura contra a referencia. Eles
 * saem do codigo quando o perfil em datapack existir (#98).
 */
public record AuraShellMaterial(float fresnelInterno, float fresnelBorda, float fresnelExterno,
        float velocidadeDeFluxo, float escalaDeRuido, float reforcoDaBorda) {

    public AuraShellMaterial {
        positivo(fresnelInterno, "fresnelInterno");
        positivo(fresnelBorda, "fresnelBorda");
        positivo(fresnelExterno, "fresnelExterno");
        naoNegativo(velocidadeDeFluxo, "velocidadeDeFluxo");
        positivo(escalaDeRuido, "escalaDeRuido");
        naoNegativo(reforcoDaBorda, "reforcoDaBorda");
        if (!(fresnelInterno > fresnelBorda && fresnelBorda > fresnelExterno)) {
            throw new IllegalArgumentException(
                    "o expoente de Fresnel precisa DIMINUIR da camada interna para a externa,"
                            + " senao as tres camadas viram uma so: "
                            + fresnelInterno + ", " + fresnelBorda + ", " + fresnelExterno);
        }
    }

    /** Ten: borda estreita, fluxo lento. */
    public static AuraShellMaterial ten() {
        return new AuraShellMaterial(3.4F, 2.7F, 2.0F, 0.12F, 4.0F, 0.9F);
    }

    /** Ren: a MESMA shell, com a borda aberta e o fluxo rapido. */
    public static AuraShellMaterial ren() {
        return new AuraShellMaterial(2.4F, 1.8F, 1.3F, 0.32F, 4.0F, 1.2F);
    }

    /** O expoente de Fresnel deste passe. */
    public float fresnelDe(AuraShellPass passe) {
        return switch (passe) {
            case INTERNA -> this.fresnelInterno;
            case BORDA -> this.fresnelBorda;
            case EXTERNA -> this.fresnelExterno;
        };
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
