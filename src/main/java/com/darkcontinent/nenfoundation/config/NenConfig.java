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

    private static final ModConfigSpec.BooleanValue TELEMETRIA_DE_INIMIGOS = BUILDER
            .comment("Registra amostras LOCAIS de combate contra inimigos do mod:",
                     "quanto tempo cada encontro durou, quanto dano foi dado e recebido.",
                     "",
                     "DESLIGADO POR PADRAO, e assim tem de ficar. Telemetria e opt-in por",
                     "decisao (issue #150), e ela e LOCAL: o arquivo fica no diretorio do",
                     "mundo e NADA sai da maquina. Um padrao ligado transformaria",
                     "'medir o proprio jogo' em 'coletar sem perguntar', e a diferenca",
                     "nao aparece em lugar nenhum do jogo -- so na conta de quem confia.")
            .define("telemetry.enemyCombat", false);

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
                    "MENOR que a regeneracao total (base x multiplicador), de proposito:",
                    "com Ten a reserva sobe DEVAGAR. No canone Ten e a manutencao que",
                    "conserva aura e mantem a protecao -- quem descansa de verdade e",
                    "Zetsu, e quem gasta e Ren.",
                    "Isto nao contraria o item 6 do ADR-010: a regra vale para estados",
                    "que LIBERAM aura. Ten retem, e o preco dele e nao poder liberar",
                    "acima do teto de repouso.")
            .defineInRange("tecnica.ten.custoPorSegundo", 1.5D, 0.0D, 1_000.0D);

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
                    "COMPRA ~29 SEGUNDOS com a reserva base de 100. A escada do ADR-018",
                    "e desenhada em SEGUNDOS, e o custo e consequencia:",
                    "  custo = aura.maximaBase / duracao + aura.regeneracaoPorSegundo",
                    "MUITO maior que o de Ten, e a proporcao entre os dois e que faz Ren",
                    "ser estado de combate e Ten estado de repouso.",
                    "Era 10.0 ate 2026-09-21, quando durava 11 s -- um golpe, e nao uma",
                    "luta. Ver a issue #160 e o ADR-018.")
            .defineInRange("tecnica.ren.custoPorSegundo", 4.4D, 0.0D, 1_000.0D);

    private static final ModConfigSpec.DoubleValue REN_TETO_DE_OUTPUT = BUILDER
            .comment("Teto de Output enquanto Ren estiver ativo.",
                    "Precisa ser MAIOR que aura.tetoDeOutputEmRepouso, senao Ren nao levanta nada.")
            .defineInRange("tecnica.ren.tetoDeOutput", 1.0D, 0.0D, 1.0D);

    private static final ModConfigSpec.DoubleValue ZETSU_CUSTO_POR_SEGUNDO = BUILDER
            .comment("Aura que Zetsu consome por segundo. O MENOR das tres tecnicas.",
                    "Pelo item 6 do ADR-010 nenhum estado sustentado se paga: Zetsu drena",
                    "devagar, e nao de graca. E o repouso obvio sem ser gratuito.")
            .defineInRange("tecnica.zetsu.custoPorSegundo", 1.2D, 0.0D, 1_000.0D);

    private static final ModConfigSpec.DoubleValue ZETSU_MULTIPLICADOR_DE_REGENERACAO = BUILDER
            .comment("Quanto Zetsu multiplica a regeneracao. O canone e explicito:",
                    "Zetsu recupera melhor, e o estado de descanso.")
            .defineInRange("tecnica.zetsu.multiplicadorDeRegeneracao", 3.0D, 0.0D, 10.0D);

    private static final ModConfigSpec.DoubleValue ZETSU_TETO_DE_OUTPUT = BUILDER
            .comment("Teto de Output com Zetsu ativo. Zero fecha os nos de aura: o jogador",
                    "nao libera NADA, e e por isso que Zetsu exclui Ten e Ren.")
            .defineInRange("tecnica.zetsu.tetoDeOutput", 0.0D, 0.0D, 1.0D);

    private static final ModConfigSpec.DoubleValue GYO_CUSTO_POR_SEGUNDO = BUILDER
            .comment("Aura que Gyo consome por segundo.",
                    "COMPRA ~59 SEGUNDOS com a reserva base de 100.",
                    "Gyo LIBERA aura -- ele e aplicacao de Ren -- entao pelo ADR-013 o",
                    "saldo dele tem de ser negativo. Menor que o de Ren: concentrar",
                    "custa menos que abrir a torneira toda.",
                    "Era 4.0 ate 2026-09-21; ver o ADR-018.")
            .defineInRange("tecnica.gyo.custoPorSegundo", 2.7D, 0.0D, 1_000.0D);

    private static final ModConfigSpec.DoubleValue GYO_FRACAO_CONCENTRADA = BUILDER
            .comment("Quanto da aura total Gyo concentra na regiao escolhida, de 0 a 1.",
                    "O resto e dividido entre as outras cinco regioes -- e essa e a",
                    "troca: concentrar num lugar TIRA de outro.",
                    "0.45 e perto do exemplo do documento-fonte (47% num braco).",
                    "Ko, quando existir, usa a mesma conta com um numero perto de 1.")
            .defineInRange("tecnica.gyo.fracaoConcentrada", 0.45D, 0.0D, 1.0D);

    private static final ModConfigSpec.DoubleValue SHU_CUSTO_POR_SEGUNDO = BUILDER
            .comment("Aura que Shu consome por segundo.",
                    "COMPRA ~71 SEGUNDOS com a reserva base de 100 -- a mais longa das",
                    "que liberam, porque e a que menos redistribui.",
                    "Shu LIBERA aura para envolver o objeto, entao pelo ADR-013 o saldo",
                    "dela e negativo. Mais barata que Gyo: cobrir o que esta na mao",
                    "custa menos que concentrar metade da aura numa regiao.",
                    "Era 3.0 ate 2026-09-21; ver o ADR-018.")
            .defineInRange("tecnica.shu.custoPorSegundo", 2.4D, 0.0D, 1_000.0D);

    private static final ModConfigSpec.DoubleValue SHU_FRACAO_CONCENTRADA = BUILDER
            .comment("Quanto da aura total Shu leva para o braco da mao dominante.",
                    "Menor que a de Gyo: Shu ESTENDE a camada ate o objeto, e nao",
                    "concentra o corpo inteiro num ponto.")
            .defineInRange("tecnica.shu.fracaoConcentrada", 0.30D, 0.0D, 1.0D);

    private static final ModConfigSpec.DoubleValue KEN_CUSTO_POR_SEGUNDO = BUILDER
            .comment("Aura que Ken consome por segundo.",
                    "COMPRA ~45 SEGUNDOS com a reserva base de 100 -- metade a mais que",
                    "Ren, e e nisso que a troca 'pico por duracao' aparece em numero.",
                    "MENOR que o de Ren, e maior que todo o resto. Ren e o pico -- a",
                    "torneira aberta, cara e insustentavel. Ken e a versao que se",
                    "aguenta, e por isso e ele que se treina para durar.",
                    "Se este numero alcancar o de Ren, Ken vira Ren com outro nome.",
                    "Era 6.0 ate 2026-09-21; ver o ADR-018.")
            .defineInRange("tecnica.ken.custoPorSegundo", 3.2D, 0.0D, 1_000.0D);

    private static final ModConfigSpec.DoubleValue KEN_TETO_DE_OUTPUT = BUILDER
            .comment("Teto de Output com Ken ativo.",
                    "ABAIXO do de Ren pelo mesmo motivo do custo: Ken troca pico por",
                    "duracao. Acima do de repouso, senao a tecnica nao libera nada.")
            .defineInRange("tecnica.ken.tetoDeOutput", 0.8D, 0.0D, 1.0D);

    private static final ModConfigSpec.DoubleValue TETO_DE_REDUCAO_DE_DANO = BUILDER
            .comment("Quanto dano a aura pode segurar, no maximo, de 0 a 1.",
                    "EXISTE PARA IMPEDIR IMORTALIDADE. Sem teto, uma combinacao de",
                    "numeros mal escolhidos chega a 100% -- e isso nao da erro nenhum,",
                    "so um jogador que nao morre mais.",
                    "Ha um teto ABSOLUTO no codigo acima deste: nem pedindo 1.0 aqui a",
                    "reducao passa dele.")
            .defineInRange("combate.tetoDeReducaoDeDano", 0.45D, 0.0D, 1.0D);

    private static final ModConfigSpec.DoubleValue TEN_PROTECAO_BASE = BUILDER
            .comment("Quanto Ten protege com a aura espalhada por igual.",
                    "Modesto de proposito: o canone chama a defesa de Ten de suficiente",
                    "contra pressao de Nen e INSUFICIENTE contra ataque de Nen forte.")
            .defineInRange("tecnica.ten.protecaoBase", 0.20D, 0.0D, 1.0D);

    private static final ModConfigSpec.DoubleValue KEN_PROTECAO_BASE = BUILDER
            .comment("Quanto Ken protege com a aura espalhada por igual.",
                    "O MAIOR dos tres: Ken e a principal defesa geral contra Nen.",
                    "Se ele nao for claramente maior que o de Ten, Ken vira um Ten caro.")
            .defineInRange("tecnica.ken.protecaoBase", 0.75D, 0.0D, 1.0D);

    private static final ModConfigSpec.DoubleValue TETO_DE_REFORCO_DE_DANO = BUILDER
            .comment("Quanto a aura pode somar ao golpe, no maximo.",
                    "E O ESPELHO DO TETO DE REDUCAO, e existe pelo mesmo motivo.",
                    "La o numero proibido e 1.0, que e imortalidade; aqui nao ha um",
                    "valor magico -- por isso o limite e escolha de design, e nao um",
                    "botao que alguem gira sem saber o que esta girando.",
                    "1.5 quer dizer: no maximo dois golpes e meio num.",
                    "Ha um teto ABSOLUTO no codigo acima deste.")
            .defineInRange("combate.tetoDeReforcoDeDano", 1.5D, 0.0D, 3.0D);

    private static final ModConfigSpec.DoubleValue TEN_REFORCO_BASE = BUILDER
            .comment("Quanto Ten soma ao golpe com a aura espalhada por igual.",
                    "Quase nada, de proposito: Ten e o estado OCIOSO. Se ele reforcar",
                    "bem, ninguem nunca precisa de Ren -- e Ren e a tecnica que o",
                    "canone descreve como o aumento de poder de ataque.")
            .defineInRange("tecnica.ten.reforcoBase", 0.05D, 0.0D, 3.0D);

    private static final ModConfigSpec.DoubleValue REN_REFORCO_BASE = BUILDER
            .comment("Quanto Ren soma ao golpe com a aura espalhada por igual.",
                    "O DE REFERENCIA. No canone, Ren e liberar aura em volume e e ele",
                    "que aumenta o poder de ataque. Ate hoje Ren so levantava o teto de",
                    "Output e cobrava aura: a metade ofensiva dele nao existia.")
            .defineInRange("tecnica.ren.reforcoBase", 0.25D, 0.0D, 3.0D);

    private static final ModConfigSpec.DoubleValue KEN_REFORCO_BASE = BUILDER
            .comment("Quanto Ken soma ao golpe com a aura espalhada por igual.",
                    "MENOR que o de Ren, e isso e a troca: Ken sustenta Ten e Ren ao",
                    "mesmo tempo pelo corpo inteiro. Quem cobre tudo nao concentra em",
                    "nada -- se Ken reforcar tanto quanto Ren, Ren vira inutil.")
            .defineInRange("tecnica.ken.reforcoBase", 0.15D, 0.0D, 3.0D);

    private static final ModConfigSpec.DoubleValue SHU_REFORCO_BASE = BUILDER
            .comment("Quanto Shu soma ao golpe com a aura espalhada por igual.",
                    "Shu estende a aura ao ITEM na mao, entao o reforco dela e do golpe",
                    "com arma. O numero e modesto porque Shu ja concentra no braco, e a",
                    "concentracao multiplica: o valor final vem da conta, nao daqui.")
            .defineInRange("tecnica.shu.reforcoBase", 0.10D, 0.0D, 3.0D);

    private static final ModConfigSpec.DoubleValue KO_REFORCO_BASE = BUILDER
            .comment("Quanto Ko soma ao golpe com a aura espalhada por igual.",
                    "PARECE BAIXO E NAO E: Ko poe quase toda a aura numa regiao so, e a",
                    "concentracao multiplica este numero por varias vezes. Girar este",
                    "botao para cima e a maneira mais rapida de tornar o combate",
                    "trivial -- meca antes.")
            .defineInRange("tecnica.ko.reforcoBase", 0.25D, 0.0D, 3.0D);

    private static final ModConfigSpec.DoubleValue KO_CUSTO_POR_SEGUNDO = BUILDER
            .comment("Aura que Ko consome por segundo. O MAIOR de todas.",
                    "Ko e um golpe, nao um estado: ele custa muito por pouco tempo.",
                    "NAO MUDOU no rebalanceamento do ADR-018, e isso e escolha. Com",
                    "duracaoEmTicks = 20 ele cobra 20 de aura por golpe -- um quinto da",
                    "reserva base. Como a escada baixou em volta dele, um Ko passou a",
                    "valer ~4,6 s de Ren em vez de ~1,8 s: ficou RELATIVAMENTE mais caro,",
                    "de proposito. Ko e compromisso, e errar o golpe tem de doer.")
            .defineInRange("tecnica.ko.custoPorSegundo", 20.0D, 0.0D, 1_000.0D);

    private static final ModConfigSpec.DoubleValue KO_FRACAO_CONCENTRADA = BUILDER
            .comment("Quanto da aura Ko leva para a regiao escolhida.",
                    "Perto de 1: o canone fala em praticamente TODA a aura num ponto.",
                    "O resto do corpo fica com o que sobra dividido por cinco -- e e",
                    "esse quase-nada que torna errar o golpe catastrofico.")
            .defineInRange("tecnica.ko.fracaoConcentrada", 0.95D, 0.0D, 1.0D);

    private static final ModConfigSpec.IntValue KO_DURACAO_EM_TICKS = BUILDER
            .comment("Quantos ticks Ko dura antes de expirar sozinho.",
                    "E ISTO QUE SEPARA KO DE GYO. Gyo se sustenta; Ko e um golpe.",
                    "Vinte ticks e um segundo -- tempo de acertar, e nao de se defender.")
            .defineInRange("tecnica.ko.duracaoEmTicks", 20, 1, 600);

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

    /** Custo de Gyo por segundo. */
    public static double gyoCustoPorSegundo() { return GYO_CUSTO_POR_SEGUNDO.get(); }

    /** Quanto da aura Gyo concentra na regiao escolhida. */
    public static double gyoFracaoConcentrada() { return GYO_FRACAO_CONCENTRADA.get(); }

    /** Quanto dano a aura pode segurar, no maximo. */
    public static double tetoDeReducaoDeDano() { return TETO_DE_REDUCAO_DE_DANO.get(); }

    /** Quanto Ten protege com a aura espalhada por igual. */
    public static double tenProtecaoBase() { return TEN_PROTECAO_BASE.get(); }

    /** Quanto Ken protege com a aura espalhada por igual. */
    public static double kenProtecaoBase() { return KEN_PROTECAO_BASE.get(); }

    /** Quanto a aura pode somar ao golpe, no maximo. */
    public static double tetoDeReforcoDeDano() { return TETO_DE_REFORCO_DE_DANO.get(); }

    /** Quanto Ten soma ao golpe com a aura espalhada por igual. */
    public static double tenReforcoBase() { return TEN_REFORCO_BASE.get(); }

    /** Quanto Ren soma ao golpe com a aura espalhada por igual. */
    public static double renReforcoBase() { return REN_REFORCO_BASE.get(); }

    /** Quanto Ken soma ao golpe com a aura espalhada por igual. */
    public static double kenReforcoBase() { return KEN_REFORCO_BASE.get(); }

    /** Quanto Shu soma ao golpe com a aura espalhada por igual. */
    public static double shuReforcoBase() { return SHU_REFORCO_BASE.get(); }

    /** Quanto Ko soma ao golpe com a aura espalhada por igual. */
    public static double koReforcoBase() { return KO_REFORCO_BASE.get(); }

    /** Custo de Ko por segundo. */
    public static double koCustoPorSegundo() { return KO_CUSTO_POR_SEGUNDO.get(); }

    /** Quanto da aura Ko concentra. */
    public static double koFracaoConcentrada() { return KO_FRACAO_CONCENTRADA.get(); }

    /** Quantos ticks Ko dura. */
    public static int koDuracaoEmTicks() { return KO_DURACAO_EM_TICKS.get(); }

    /** Custo de Ken por segundo. */
    public static double kenCustoPorSegundo() { return KEN_CUSTO_POR_SEGUNDO.get(); }

    /** Teto de Output com Ken ativo. */
    public static double kenTetoDeOutput() { return KEN_TETO_DE_OUTPUT.get(); }

    /** Custo de Shu por segundo. */
    public static double shuCustoPorSegundo() { return SHU_CUSTO_POR_SEGUNDO.get(); }

    /** Quanto da aura Shu leva para o braco da mao dominante. */
    public static double shuFracaoConcentrada() { return SHU_FRACAO_CONCENTRADA.get(); }

    /** Custo de Zetsu por segundo. */
    public static double zetsuCustoPorSegundo() { return ZETSU_CUSTO_POR_SEGUNDO.get(); }

    /** Multiplicador de regeneracao de Zetsu. */
    public static double zetsuMultiplicadorDeRegeneracao() {
        return ZETSU_MULTIPLICADOR_DE_REGENERACAO.get();
    }

    /** Teto de Output com Zetsu ativo. */
    public static double zetsuTetoDeOutput() { return ZETSU_TETO_DE_OUTPUT.get(); }

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
     * {@code true} quando o jogador LIGOU a telemetria local de combate.
     *
     * <p>Ela NAO depende de {@code dev.enabled} de proposito. Balanceamento e
     * trabalho de quem joga a serio, e nao de quem esta depurando; amarrar as
     * duas obrigaria a ligar o log ruidoso de desenvolvimento para medir um
     * combate, e o ruido esconderia justamente o que a medida procura.</p>
     */
    public static boolean telemetriaDeInimigosAtiva() {
        return TELEMETRIA_DE_INIMIGOS.get();
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
