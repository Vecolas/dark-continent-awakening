package com.darkcontinent.nenfoundation.config;

import com.darkcontinent.nenfoundation.nen.aura.ParametrosDeAura;
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

    private static final ModConfigSpec.DoubleValue AURA_MAXIMA_BASE = BUILDER
            .comment("Reserva inicial de aura de um jogador despertado.",
                     "O potencial persistente e somado pela formula do Aura Engine.")
            .defineInRange("aura.maximaBase", 100.0D, 0.0D, 1_000_000.0D);

    private static final ModConfigSpec.DoubleValue AURA_REGENERACAO_POR_SEGUNDO = BUILDER
            .comment("Aura recuperada por segundo enquanto o pool puder recuperar.",
                     "A formula converte este valor para a cadencia real do servidor.")
            .defineInRange("aura.regeneracaoPorSegundo", 1.0D, 0.0D, 1_000_000.0D);

    private static final ModConfigSpec.DoubleValue AURA_OUTPUT_BASE = BUILDER
            .comment("Teto base de aura manifestada nas operacoes instantaneas de um tick.",
                     "O output persistente e somado; isto nao aumenta a reserva.")
            .defineInRange("aura.outputBase", 10.0D, 0.0D, 1_000_000.0D);

    private static final ModConfigSpec.IntValue INTERVALO_DE_SYNC = BUILDER
            .comment("Intervalo minimo, em ticks, entre deltas alterados. Tick limpo nao envia.")
            .defineInRange("network.runtimeSyncTicks", 5, 1, 100);

    private static final ModConfigSpec.IntValue INTERPOLACAO_DE_AURA = BUILDER
            .comment("Duracao visual em ticks de cliente. Zero aplica o delta imediatamente.")
            .defineInRange("client.auraInterpolationTicks", 5, 0, 20);

    private static final ModConfigSpec.IntValue INTERPOLACAO_DE_OUTPUT = BUILDER
            .comment("Duracao visual do Output em ticks de cliente. Zero aplica imediatamente.")
            .defineInRange("client.outputInterpolationTicks", 3, 0, 20);

    private static final ModConfigSpec.DoubleValue MULTIPLICADOR_MAXIMO_DE_REGENERACAO = BUILDER
            .comment("Teto do multiplicador de regeneracao, depois de TODAS as tecnicas ativas.",
                    "Sem teto, o que impediria numero absurdo seria a exclusao entre tecnicas --",
                    "uma regra de outro lugar, que pode mudar sem ninguem lembrar desta. Ver ADR-010.")
            .defineInRange("aura.multiplicadorMaximoDeRegeneracao", 3.0D, 0.0D, 100.0D);

    private static final ModConfigSpec.DoubleValue TEN_CUSTO_POR_SEGUNDO = BUILDER
            .comment("Aura que Ten consome por segundo enquanto estiver ativo.",
                    "Pelo item 6 do ADR-010 o saldo de Ten precisa ser NEGATIVO EM ABSOLUTO:",
                    "a reserva tem de CAIR com Ten ligado. Por isso este custo supera a",
                    "regeneracao TOTAL (base x multiplicador), e nao apenas o ganho sobre a base.")
            .defineInRange("tecnica.ten.custoPorSegundo", 3.0D, 0.0D, 1_000.0D);

    private static final ModConfigSpec.DoubleValue TEN_MULTIPLICADOR_DE_REGENERACAO = BUILDER
            .comment("Quanto Ten multiplica a regeneracao de Aura enquanto ativo.",
                    "E a retencao do canone: com Ten, a aura se conserva melhor.",
                    "Ele REDUZ o custo de manter Ten; ele nao o paga.")
            .defineInRange("tecnica.ten.multiplicadorDeRegeneracao", 2.0D, 0.0D, 10.0D);

    private static final ModConfigSpec.DoubleValue TETO_DE_OUTPUT_EM_REPOUSO = BUILDER
            .comment("Fracao do Output que o jogador consegue liberar SEM tecnica nenhuma.",
                    "Abaixo de 1.0 de proposito: e o que da a Ren o que levantar.",
                    "Em 1.0, Ren deixa de ter efeito observavel.")
            .defineInRange("aura.tetoDeOutputEmRepouso", 0.5D, 0.0D, 1.0D);

    private static final ModConfigSpec.DoubleValue REN_CUSTO_POR_SEGUNDO = BUILDER
            .comment("Aura que Ren consome por segundo enquanto estiver ativo.",
                    "MUITO maior que o de Ten, e a proporcao entre os dois e que faz Ren",
                    "ser estado de combate e Ten estado de repouso.")
            .defineInRange("tecnica.ren.custoPorSegundo", 10.0D, 0.0D, 1_000.0D);

    private static final ModConfigSpec.DoubleValue REN_TETO_DE_OUTPUT = BUILDER
            .comment("Teto de Output enquanto Ren estiver ativo.",
                    "Precisa ser MAIOR que aura.tetoDeOutputEmRepouso, senao Ren nao levanta nada.")
            .defineInRange("tecnica.ren.tetoDeOutput", 1.0D, 0.0D, 1.0D);

    public static final ModConfigSpec SPEC = BUILDER.build();

    /** O objeto e estavel; cada metodo le a config carregada no momento do uso. */
    public static final ParametrosDeAura AURA = new ParametrosDeAura() {
        @Override public double maximaBase() { return AURA_MAXIMA_BASE.get(); }
        @Override public double regeneracaoPorSegundo() { return AURA_REGENERACAO_POR_SEGUNDO.get(); }
        @Override public double outputBase() { return AURA_OUTPUT_BASE.get(); }
        @Override public double multiplicadorMaximoDeRegeneracao() {
            return MULTIPLICADOR_MAXIMO_DE_REGENERACAO.get();
        }
    };

    public static int intervaloDeSync() { return INTERVALO_DE_SYNC.get(); }
    public static int interpolacaoDeAura() { return INTERPOLACAO_DE_AURA.get(); }
    public static int interpolacaoDeOutput() { return INTERPOLACAO_DE_OUTPUT.get(); }

    private NenConfig() {
    }

    /** Custo de Ten por segundo. Lido no instante do uso, para respeitar recarga. */
    public static double tenCustoPorSegundo() { return TEN_CUSTO_POR_SEGUNDO.get(); }

    /** Multiplicador de regeneracao de Ten. Lido no instante do uso. */
    public static double tenMultiplicadorDeRegeneracao() {
        return TEN_MULTIPLICADOR_DE_REGENERACAO.get();
    }

    /** Teto de Output sem tecnica nenhuma. Lido no instante do uso. */
    public static float tetoDeOutputEmRepouso() {
        return TETO_DE_OUTPUT_EM_REPOUSO.get().floatValue();
    }

    /** Custo de Ren por segundo. */
    public static double renCustoPorSegundo() { return REN_CUSTO_POR_SEGUNDO.get(); }

    /** Teto de Output com Ren ativo. */
    public static double renTetoDeOutput() { return REN_TETO_DE_OUTPUT.get(); }

    public static int pedidosPorSegundo() {
        return PEDIDOS_POR_SEGUNDO.get();
    }

    /** Le a capacidade base no instante do uso para respeitar recarga de config. */
    public static double auraMaximaBase() {
        return AURA_MAXIMA_BASE.get();
    }

    /** Le a regeneracao por segundo no instante do uso. */
    public static double auraRegeneracaoPorSegundo() {
        return AURA_REGENERACAO_POR_SEGUNDO.get();
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
