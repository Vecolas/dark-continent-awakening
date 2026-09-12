package com.darkcontinent.nenfoundation.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Ajustes que sao SO do cliente que os le.
 *
 * <p>ARQUIVO SEPARADO DE {@link NenConfig}, e por dois motivos.
 *
 * <p>O primeiro e de significado: nada aqui muda regra de Nen. Densidade de
 * particula e duracao de transicao sao conforto visual -- dois jogadores no
 * mesmo servidor podem escolher valores diferentes sem que o jogo fique
 * diferente para eles. Um numero desses num spec COMMON seria decidido pelo
 * servidor, que e o oposto do que se quer.
 *
 * <p>O segundo e de convivencia: {@code NenConfig} e arquivo hostil a merge, e
 * duas pessoas mexendo nele ao mesmo tempo conflitam por um motivo que nao tem
 * nada a ver com o trabalho de nenhuma das duas.
 *
 * <p>NAO COLOQUE REGRA AQUI. Custo, cooldown, alcance e dano moram no lado do
 * servidor; se um numero desses aparecer neste arquivo, o cliente virou
 * autoridade sobre alguma coisa (ADR-001).
 */
public final class NenClientConfig {

    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    private static final ModConfigSpec.DoubleValue DENSIDADE_DE_PARTICULAS = BUILDER
            .comment("Quanta particula de aura este cliente desenha, de 0 a 2.",
                    "Zero desliga o efeito sem desligar o Nen -- a tecnica continua",
                    "valendo no servidor; o que some e o desenho.")
            .defineInRange("vfx.densidadeDeParticulas", 1.0D, 0.0D, 2.0D);

    private static final ModConfigSpec.DoubleValue ESCALA_DE_TRANSICAO = BUILDER
            .comment("Multiplicador da duracao das transicoes de aura.",
                    "1.0 e o tempo de projeto: ligar Ten leva ~350 ms, subir para",
                    "Ren ~900 ms, e suprimir com Zetsu ~300 ms. Acima de 1 tudo fica",
                    "mais lento; abaixo, mais seco.",
                    "",
                    "ELE SUBSTITUIU `vfx.ticksDeTransicao`, que dava um numero unico",
                    "para todas as trocas -- e com ele ligar Ten e explodir em Ren",
                    "levavam exatamente o mesmo tempo. A duracao de cada troca agora",
                    "mora em AuraTransicao; aqui fica so o gosto de quem joga.")
            .defineInRange("vfx.escalaDeTransicao", 1.0D, 0.1D, 5.0D);

    private static final ModConfigSpec.EnumValue<
            com.darkcontinent.nenfoundation.client.vfx.AuraVisualQuality> QUALIDADE = BUILDER
            .comment("Quanta aura este cliente desenha.",
                    "OFF desliga o desenho sem desligar o Nen -- a tecnica continua",
                    "valendo no servidor. LOW mantem so a borda, que e o que diz",
                    "QUAL e o estado. ULTRA desenha tudo o que a distancia permitir.")
            .defineEnum("vfx.qualidade",
                    com.darkcontinent.nenfoundation.client.vfx.AuraVisualQuality.HIGH);

    /** O spec deste arquivo, registrado como CLIENT. */
    public static final ModConfigSpec SPEC = BUILDER.build();

    private NenClientConfig() {
    }

    /**
     * A qualidade escolhida por este cliente.
     *
     * <p>Ela NASCE COM CONSUMIDOR, como a regra exige: quem a le e o corte de
     * nivel de detalhe, no mesmo PR em que a chave aparece. Chave declarada
     * antes do consumidor e config orfa, e alguem passa uma tarde girando um
     * botao morto.
     */
    public static com.darkcontinent.nenfoundation.client.vfx.AuraVisualQuality qualidade() {
        return QUALIDADE.get();
    }

    /** Densidade de particulas escolhida por este cliente. */
    public static double densidadeDeParticulas() {
        return DENSIDADE_DE_PARTICULAS.get();
    }

    /**
     * O multiplicador de duracao escolhido por este cliente.
     *
     * <p>DEVOLVE A ESCALA, e nao o passo. Quem sabe quanto uma transicao dura e
     * {@code AuraTransicao} -- cada troca tem o proprio tempo. Devolver um passo
     * daqui recriaria a fonte unica que este PR acabou de eliminar.
     */
    public static float escalaDeTransicao() {
        return ESCALA_DE_TRANSICAO.get().floatValue();
    }
}
