package com.darkcontinent.nenfoundation.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Configuracao comum do Nen Foundation.
 *
 * <p>DECISAO QUE ESTE ARQUIVO CARREGA — e a mais facil de violar sem perceber:
 *
 * <p>Uma chave so entra aqui QUANDO O CONSUMIDOR DELA EXISTE. Nada de declarar
 * "custo de Ren" antes de Ren existir. Numero configuravel que ninguem le e um
 * botao morto: a proxima pessoa passa uma tarde girando-o e concluindo que o
 * sistema esta quebrado. Igualmente proibido o inverso — chave declarada aqui e
 * constante paralela no consumidor: o consumidor ganha, a configuracao vira
 * arquivo orfao, e a sessao de balanceamento inteira nao muda nada no jogo.
 *
 * <p>Por isso, em M0, so existem as chaves de desenvolvimento. Formulas de
 * aura chegam no M2, junto do Aura Engine; custos de tecnica no M4; custo e
 * cooldown de habilidade no M5. Cada numero chega junto do sistema que ele
 * mede.
 *
 * <p>ARQUIVO HOSTIL A MERGE: uma pessoa por vez.
 */
public final class NenConfig {

    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    private static final ModConfigSpec.BooleanValue DEV_MODE = BUILDER
            .comment("Liga diagnostico de desenvolvimento: transicoes de estado,",
                     "contadores de sync e o overlay de depuracao.",
                     "Deve ficar DESLIGADO em servidor de jogo: o log fica ruidoso",
                     "o bastante para esconder erro de verdade.")
            .define("dev.enabled", false);

    private static final ModConfigSpec.BooleanValue LOG_TRANSICOES = BUILDER
            .comment("Registra cada transicao de estado de tecnica em log.",
                     "So tem efeito com dev.enabled = true.")
            .define("dev.logStateTransitions", false);

    private static final ModConfigSpec.IntValue PEDIDOS_POR_SEGUNDO = BUILDER
            .comment("Cota C2S por jogador, somando ativacao, desativacao e habilidade.",
                     "Excesso e cortado antes da fila; contadores aparecem no logout em modo dev.")
            .defineInRange("network.requestsPerSecond", 20, 1, 200);

    public static final ModConfigSpec SPEC = BUILDER.build();

    private NenConfig() {
    }

    public static int pedidosPorSegundo() {
        return PEDIDOS_POR_SEGUNDO.get();
    }

    /**
     * Valor derivado, lido na hora.
     *
     * <p>Nao guarde o resultado num campo: a configuracao e recarregavel, e uma
     * copia guardada no boot ignora a recarga sem dar erro.
     */
    public static boolean devModeAtivo() {
        return DEV_MODE.get();
    }

    /**
     * {@code true} so quando o modo de desenvolvimento tambem esta ligado.
     *
     * <p>A conjuncao mora AQUI, e nao em cada chamador. Espalhada, alguem
     * esquece um {@code &&} e o servidor de producao registra transicao de
     * estado a cada tick.
     */
    public static boolean logarTransicoes() {
        return DEV_MODE.get() && LOG_TRANSICOES.get();
    }
}
