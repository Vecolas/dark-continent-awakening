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

    // ------------------------------------------------------------------ aura

    private static final ModConfigSpec.DoubleValue AURA_BASE_MAXIMA = BUILDER
            .comment("Reserva maxima de aura de um jogador recem-despertado.",
                     "Multiplicada por auraPotential do perfil nas formulas do M2.",
                     "Nasce aqui porque o AuraPool (consumidor) existe a partir do M2.")
            .defineInRange("aura.baseMaximum", 100.0D, 1.0D, 1_000_000.0D);

    private static final ModConfigSpec.DoubleValue AURA_REGEN_POR_TICK = BUILDER
            .comment("Aura regenerada por tick (20 ticks/s).",
                     "Padrao 0.05 = ~1 de aura por segundo. Ajustar com regua de spark.")
            .defineInRange("aura.regenPerTick", 0.05D, 0.0D, 1000.0D);

    private static final ModConfigSpec.DoubleValue AURA_LIMIAR_EXAUSTAO = BUILDER
            .comment("Fracao do maximo abaixo da qual o jogador entra em estado de exaustao.",
                     "0.10 = abaixo de 10% da aura maxima. Usada pelo HUD e por eventos futuros.")
            .defineInRange("aura.exhaustionThreshold", 0.10D, 0.0D, 1.0D);

    // Vigor removido no pivot para AOP.

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

    // -------------------------------------------------------------- aura api

    /** Reserva maxima de aura base. Lida na hora; configuracao e recarregavel. */
    public static double auraBaseMaxima() {
        return AURA_BASE_MAXIMA.get();
    }

    /** Aura regenerada por tick. */
    public static double auraRegenPorTick() {
        return AURA_REGEN_POR_TICK.get();
    }

    /**
     * Fracao do maximo abaixo da qual o jogador esta em exaustao de aura.
     *
     * <p>Valor derivado: use {@code auraAtual <= auraMaxima * auraLimiarExaustao()} para
     * decidir se o estado de exaustao esta ativo.
     */
    public static double auraLimiarExaustao() {
        return AURA_LIMIAR_EXAUSTAO.get();
    }

}
