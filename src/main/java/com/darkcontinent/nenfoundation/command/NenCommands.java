package com.darkcontinent.nenfoundation.command;

import com.darkcontinent.nenfoundation.NenFoundation;
import com.darkcontinent.nenfoundation.nen.profile.PersistentNenData;
import com.darkcontinent.nenfoundation.server.NenProfileService;
import com.darkcontinent.nenfoundation.server.NenAuraService;
import com.darkcontinent.nenfoundation.config.NenConfig;
import com.darkcontinent.nenfoundation.nen.aura.MotorDeAura;
import com.darkcontinent.nenfoundation.api.event.OrigemDoDespertar;
import com.darkcontinent.nenfoundation.nen.category.NenCategory;
import com.darkcontinent.nenfoundation.nen.category.SorteioDeCategoria;
import com.darkcontinent.nenfoundation.server.NenAwakeningService;
import com.darkcontinent.nenfoundation.server.NenCategoryService;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import java.util.stream.Collectors;
import net.minecraft.commands.SharedSuggestionProvider;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import java.util.Locale;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.tree.LiteralCommandNode;
import java.util.LinkedHashSet;
import java.util.Set;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * A arvore inteira de comandos do mod: {@code /nen}.
 *
 * <p>DECISOES QUE ESTE ARQUIVO CARREGA:
 *
 * <p>1. UMA ARVORE, UM ARQUIVO. Brigadier funde nos de mesmo nome quando dois
 * lugares registram {@code /nen}, e a fusao funciona — e exatamente por isso e
 * perigosa: a permissao do outro registro pode ser diferente da desta, e nada
 * acusa. Com um registro so, a permissao da raiz cobre tudo por construcao, e o
 * portao consegue afirmar isso.
 *
 * <p>2. A PERMISSAO MORA NA RAIZ. Comando de debug disponivel ao jogador comum
 * e um exploit com sintaxe amigavel: {@code /nen technique unlock} daria a
 * qualquer um o que a progressao inteira existe para controlar.
 *
 * <p>3. {@code reset} EXIGE A PALAVRA {@code confirmar}, e imprime o dump do
 * que vai destruir ANTES de destruir. Apagar o progresso de alguem nao tem
 * desfazer; a unica recuperacao possivel e o operador ter o texto na tela. Um
 * literal obrigatorio tambem impede que um erro de digitacao complete para algo
 * destrutivo.
 *
 * <p>4. Leitura nao avisa ninguem; MUTACAO avisa os outros operadores
 * ({@code sendSuccess} com {@code true}). Um operador alterando o perfil de
 * outro jogador em silencio e a origem de "meu progresso sumiu" sem rastro.
 *
 * <p>5. Nada aqui escreve o attachment direto. Tudo passa por
 * {@link NenProfileService}, que migra na leitura e valida na escrita.
 *
 * <p>PONTO CEGO DECLARADO: {@code technique unlock} nao consegue conferir se a
 * tecnica EXISTE — nao ha registro de tecnicas ate o M4. Ele confere o
 * namespace, que pega erro de digitacao comum, e avisa que o resto nao foi
 * conferido. Aviso nao e portao; o portao chega junto do registro.
 */
@EventBusSubscriber(modid = NenFoundation.MOD_ID)
public final class NenCommands {

    /** Nivel de operador exigido pela raiz. */
    public static final int NIVEL_DE_OPERADOR = 2;

    private static final DynamicCommandExceptionType NAMESPACE_ERRADO =
            new DynamicCommandExceptionType(id -> Component.literal(
                    "Id fora do namespace do mod: " + id
                            + ". Use nenfoundation:<nome>."));

    private static final DynamicCommandExceptionType CATEGORIA_DESCONHECIDA =
            new DynamicCommandExceptionType(nome -> Component.literal(
                    "Categoria desconhecida: " + nome + ". As seis sao "
                            + NenCategory.REAIS.stream()
                                    .map(NenCategory::getSerializedName)
                                    .collect(Collectors.joining(", ")) + "."));

    private static final DynamicCommandExceptionType CATEGORIA_NEUTRA =
            new DynamicCommandExceptionType(nome -> Component.literal(
                    "'" + nome + "' e a AUSENCIA de categoria, e nao uma categoria"
                            + " atribuivel. Para limpar um perfil use"
                            + " /nen reset confirmar."));

    /** As seis reais, derivadas do enum. Uma lista literal aqui divergiria dele. */
    private static final SuggestionProvider<CommandSourceStack> CATEGORIAS_REAIS =
            (ctx, builder) -> SharedSuggestionProvider.suggest(
                    NenCategory.REAIS.stream().map(NenCategory::getSerializedName), builder);

    private NenCommands() {
    }

    @SubscribeEvent
    public static void aoRegistrarComandos(RegisterCommandsEvent evento) {
        registrar(evento.getDispatcher());
    }

    /** Registra a arvore. Devolve o no da raiz para que o portao possa inspeciona-la. */
    public static LiteralCommandNode<CommandSourceStack> registrar(
            CommandDispatcher<CommandSourceStack> dispatcher) {

        LiteralArgumentBuilder<CommandSourceStack> raiz = Commands.literal("nen")
                .requires(fonte -> fonte.hasPermission(NIVEL_DE_OPERADOR));

        raiz.then(Commands.literal("debug")
                .then(Commands.literal("aura")
                        .requires(fonte -> NenConfig.devModeAtivo())
                        .then(Commands.argument("alvo", EntityArgument.player())
                                .executes(ctx -> mostrarAura(ctx, alvoDoArgumento(ctx))))
                        .then(Commands.literal("gastar")
                                .then(Commands.argument("quantidade", DoubleArgumentType.doubleArg())
                                        .then(Commands.argument("alvo", EntityArgument.player())
                                                .executes(ctx -> gastarAura(ctx, alvoDoArgumento(ctx))))))
                        .then(Commands.literal("definir")
                                .then(Commands.argument("quantidade", DoubleArgumentType.doubleArg(0))
                                        .then(Commands.argument("alvo", EntityArgument.player())
                                                .executes(ctx -> definirAura(ctx, alvoDoArgumento(ctx)))))))
                .then(Commands.literal("profile")
                        .executes(ctx -> mostrarResumo(ctx, alvoOuProprio(ctx)))
                        .then(Commands.argument("alvo", EntityArgument.player())
                                .executes(ctx -> mostrarResumo(ctx, alvoDoArgumento(ctx)))))
                .then(Commands.literal("dump")
                        .executes(ctx -> mostrarDump(ctx, alvoOuProprio(ctx)))
                        .then(Commands.argument("alvo", EntityArgument.player())
                                .executes(ctx -> mostrarDump(ctx, alvoDoArgumento(ctx))))));

        raiz.then(Commands.literal("technique")
                .then(Commands.literal("unlock")
                        .then(Commands.argument("tecnica", ResourceLocationArgument.id())
                                .executes(ctx -> mudarTecnica(ctx, true, false))
                                .then(Commands.argument("alvo", EntityArgument.player())
                                        .executes(ctx -> mudarTecnica(ctx, true, true)))))
                .then(Commands.literal("lock")
                        .then(Commands.argument("tecnica", ResourceLocationArgument.id())
                                .executes(ctx -> mudarTecnica(ctx, false, false))
                                .then(Commands.argument("alvo", EntityArgument.player())
                                        .executes(ctx -> mudarTecnica(ctx, false, true))))));

        raiz.then(Commands.literal("awaken")
                .executes(ctx -> despertar(ctx, alvoOuProprio(ctx)))
                .then(Commands.argument("alvo", EntityArgument.player())
                        .executes(ctx -> despertar(ctx, alvoDoArgumento(ctx)))));

        // OS TRES SUBCOMANDOS DE CATEGORIA SAO TRES, e nao um com uma flag.
        //
        // `set` e `reveal` sao as duas operacoes que o dominio separa, e o
        // ponto do M3 inteiro e que elas sejam separadas. Um unico
        // `/nen category <cat> --revelar` juntaria de volta, na interface, o
        // que o modelo separou -- e o comando viraria o jeito mais facil de
        // revelar sem querer durante um teste.
        raiz.then(Commands.literal("category")
                .then(Commands.literal("set")
                        .then(Commands.argument("categoria", StringArgumentType.word())
                                .suggests(CATEGORIAS_REAIS)
                                .executes(ctx -> definirCategoria(ctx, false))
                                .then(Commands.argument("alvo", EntityArgument.player())
                                        .executes(ctx -> definirCategoria(ctx, true)))))
                .then(Commands.literal("reveal")
                        .executes(ctx -> revelarCategoria(ctx, alvoOuProprio(ctx)))
                        .then(Commands.argument("alvo", EntityArgument.player())
                                .executes(ctx -> revelarCategoria(ctx, alvoDoArgumento(ctx)))))
                .then(Commands.literal("roll")
                        .then(Commands.argument("semente", LongArgumentType.longArg())
                                .executes(ctx -> sortearCategoria(ctx, false))
                                .then(Commands.argument("alvo", EntityArgument.player())
                                        .executes(ctx -> sortearCategoria(ctx, true))))));

        raiz.then(Commands.literal("reset")
                .then(Commands.literal("confirmar")
                        .executes(ctx -> resetar(ctx, alvoOuProprio(ctx)))
                        .then(Commands.argument("alvo", EntityArgument.player())
                                .executes(ctx -> resetar(ctx, alvoDoArgumento(ctx))))));

        return dispatcher.register(raiz);
    }

    // ------------------------------------------------------------- acoes

    /** Diagnostico de runtime, sem escrita de perfil ou despertar. */
    private static int mostrarAura(CommandContext<CommandSourceStack> ctx, ServerPlayer alvo) {
        var estado = NenAuraService.consultar(alvo);
        String texto = String.format(Locale.ROOT, "aura %s: %.3f/%.3f output=%.3f exausto=%s %s",
                alvo.getGameProfile().getName(), estado.auraAtual(), estado.auraMaxima(),
                NenAuraService.output(alvo), estado.exausto(), NenAuraService.medida(alvo));
        ctx.getSource().sendSuccess(() -> Component.literal(texto), false);
        return 1;
    }

    private static int gastarAura(CommandContext<CommandSourceStack> ctx, ServerPlayer alvo) {
        MotorDeAura.Gasto gasto = NenAuraService.gastar(alvo, DoubleArgumentType.getDouble(ctx, "quantidade"));
        if (gasto != MotorDeAura.Gasto.PERMITIDO) {
            String chave = switch (gasto) {
                case INVALIDO -> "aura_invalida";
                case NAO_DESPERTO -> "nao_desperto";
                case SEM_AURA -> "aura_insuficiente";
                case OUTPUT_EXCEDIDO -> "output_excedido";
                default -> throw new IllegalStateException("recusa desconhecida");
            };
            ctx.getSource().sendFailure(Component.translatable("nenfoundation.error." + chave));
            return 0;
        }
        ctx.getSource().sendSuccess(() -> Component.literal("Gasto aplicado a " + alvo.getName().getString()), true);
        return mostrarAura(ctx, alvo);
    }

    private static int definirAura(CommandContext<CommandSourceStack> ctx, ServerPlayer alvo) {
        double quantidade = DoubleArgumentType.getDouble(ctx, "quantidade");
        var estado = NenAuraService.consultar(alvo);
        if (!Double.isFinite(quantidade) || quantidade > estado.auraMaxima()) {
            ctx.getSource().sendFailure(Component.translatable("nenfoundation.error.aura_invalida"));
            return 0;
        }
        estado.definirAuraAtual(quantidade);
        ctx.getSource().sendSuccess(() -> Component.literal("Aura de debug alterada para " + alvo.getName().getString()), true);
        return mostrarAura(ctx, alvo);
    }

    private static int mostrarResumo(CommandContext<CommandSourceStack> ctx, ServerPlayer alvo) {
        PersistentNenData perfil = NenProfileService.ler(alvo);
        String texto = RelatorioDePerfil.resumo(perfil);
        ctx.getSource().sendSuccess(() -> Component.literal(texto), false);
        return 1;
    }

    private static int mostrarDump(CommandContext<CommandSourceStack> ctx, ServerPlayer alvo) {
        PersistentNenData perfil = NenProfileService.ler(alvo);
        for (String linha : RelatorioDePerfil.dump(alvo.getGameProfile().getName(), perfil)) {
            ctx.getSource().sendSuccess(() -> Component.literal(linha), false);
        }
        return 1;
    }

    private static int mudarTecnica(
            CommandContext<CommandSourceStack> ctx, boolean desbloquear, boolean alvoExplicito)
            throws CommandSyntaxException {

        // ORDEM QUE E CONTRATO: o argumento e validado ANTES de o alvo ser
        // resolvido. Na ordem inversa, quem digita o namespace errado no
        // console recebe "e preciso um jogador" -- uma mensagem sobre outro
        // problema. E, pior, o erro de namespace vira inalcancavel de qualquer
        // fonte sem jogador, o que o torna impossivel de exercitar.
        ResourceLocation tecnica = validarTecnica(
                ResourceLocationArgument.getId(ctx, "tecnica"));

        ServerPlayer alvo = alvoExplicito ? alvoDoArgumento(ctx) : alvoOuProprio(ctx);

        PersistentNenData depois = NenProfileService.atualizar(alvo, antes -> {
            Set<ResourceLocation> tecnicas = new LinkedHashSet<>(antes.unlockedTechniques());
            if (desbloquear) {
                tecnicas.add(tecnica);
            } else {
                tecnicas.remove(tecnica);
            }
            return comTecnicas(antes, tecnicas);
        });

        String verbo = desbloquear ? "desbloqueada" : "bloqueada";
        ctx.getSource().sendSuccess(() -> Component.literal(
                "Tecnica " + tecnica + " " + verbo + " para "
                        + alvo.getGameProfile().getName()
                        + ". Agora: [" + depois.unlockedTechniques().size() + " tecnica(s)]"),
                true);

        // Ponto cego declarado: nao ha registro de tecnicas ate o M4, entao
        // ninguem consegue dizer se este id corresponde a alguma coisa. Dizer
        // isso em voz alta e melhor que deixar o operador supor que foi
        // conferido.
        if (desbloquear) {
            ctx.getSource().sendSuccess(() -> Component.literal(
                    "  (o id nao foi conferido contra nenhum registro: ele nao existe ate o M4)"),
                    false);
        }
        return 1;
    }

    // ------------------------------------------------ despertar e categoria

    /**
     * Desperta pela API, exatamente como uma quest faria.
     *
     * <p>A origem e {@link OrigemDoDespertar#COMANDO}, e nao TREINO: o cânone
     * separa os caminhos de despertar, e um comando de QA que se disfarca de
     * treino faria um listener futuro medir o ambiente de teste em vez do jogo.
     */
    private static int despertar(CommandContext<CommandSourceStack> ctx, ServerPlayer alvo) {
        NenAwakeningService.Resultado resultado =
                NenAwakeningService.despertar(alvo, OrigemDoDespertar.COMANDO);
        String nome = alvo.getGameProfile().getName();

        switch (resultado) {
            case DESPERTOU -> ctx.getSource().sendSuccess(
                    () -> Component.literal(nome + " despertou para o Nen."), true);
            case JA_ESTAVA -> ctx.getSource().sendSuccess(
                    () -> Component.literal(nome + " ja estava desperto; nada mudou."), false);
            case CANCELADO -> {
                ctx.getSource().sendFailure(Component.literal(
                        "O despertar de " + nome + " foi cancelado por um listener."
                                + " Veja o log do servidor em modo dev."));
                return 0;
            }
            default -> throw new IllegalStateException("resultado desconhecido: " + resultado);
        }
        return 1;
    }

    /**
     * Atribui a categoria SEM revelar. Revelar e {@code /nen category reveal}.
     *
     * <p>E o comando que reproduz o estado do meio -- tem categoria, nao sabe
     * qual e --, que e justamente o estado que nenhum ritual deixa voce montar
     * a mao.
     */
    private static int definirCategoria(
            CommandContext<CommandSourceStack> ctx, boolean alvoExplicito)
            throws CommandSyntaxException {

        // ORDEM QUE E CONTRATO, como em `mudarTecnica`: o argumento e validado
        // ANTES de o alvo ser resolvido. Na ordem inversa, quem digita uma
        // categoria inexistente no console recebe "e preciso um jogador".
        NenCategory categoria = validarCategoria(
                StringArgumentType.getString(ctx, "categoria"));
        ServerPlayer alvo = alvoExplicito ? alvoDoArgumento(ctx) : alvoOuProprio(ctx);

        return aplicarAtribuicao(ctx, alvo, categoria,
                NenCategoryService.atribuir(alvo, categoria));
    }

    /** Reproduz o sorteio de uma semente conhecida. O caminho de QA. */
    private static int sortearCategoria(
            CommandContext<CommandSourceStack> ctx, boolean alvoExplicito)
            throws CommandSyntaxException {

        long semente = LongArgumentType.getLong(ctx, "semente");
        ServerPlayer alvo = alvoExplicito ? alvoDoArgumento(ctx) : alvoOuProprio(ctx);

        // O comando PREVE pelo mesmo caminho que a atribuicao usa. Calcular a
        // categoria aqui e atribuir por outro lado seria a mesma verdade em
        // duas fontes, e elas divergiriam no primeiro ajuste do sorteio.
        NenCategory categoria = SorteioDeCategoria.sortear(semente);

        ctx.getSource().sendSuccess(() -> Component.literal(
                "Semente " + semente + " sorteia "
                        + categoria.getSerializedName() + "."), false);

        return aplicarAtribuicao(ctx, alvo, categoria,
                NenCategoryService.atribuir(alvo, categoria));
    }

    /** O relato das tres respostas possiveis da atribuicao. */
    private static int aplicarAtribuicao(CommandContext<CommandSourceStack> ctx,
            ServerPlayer alvo, NenCategory categoria,
            NenCategoryService.Atribuicao resultado) {

        String nome = alvo.getGameProfile().getName();
        switch (resultado) {
            case ATRIBUIU -> {
                ctx.getSource().sendSuccess(() -> Component.literal(
                        nome + " recebeu a categoria " + categoria.getSerializedName()
                                + ", ainda ESCONDIDA dele."), true);
                ctx.getSource().sendSuccess(() -> Component.literal(
                        "  (para contar a ele: /nen category reveal " + nome + ")"), false);
            }
            case JA_TINHA -> {
                // Recusa com motivo E com o caminho de saida. Dizer so "nao
                // deu" mandaria quem esta reproduzindo um bug adivinhar que a
                // troca de categoria nao existe de proposito.
                ctx.getSource().sendFailure(Component.translatable(
                        "nenfoundation.error.ja_tem_categoria"));
                ctx.getSource().sendSuccess(() -> Component.literal(
                        "  (" + nome + " ja tem categoria. Trocar categoria nao faz"
                                + " parte do M3; para comecar do zero use"
                                + " /nen reset confirmar " + nome + ")"), false);
                return 0;
            }
            case NAO_DESPERTO -> {
                ctx.getSource().sendFailure(Component.translatable(
                        "nenfoundation.error.nao_desperto"));
                ctx.getSource().sendSuccess(() -> Component.literal(
                        "  (desperte antes: /nen awaken " + nome + ")"), false);
                return 0;
            }
            default -> throw new IllegalStateException("resultado desconhecido: " + resultado);
        }
        return 1;
    }

    private static int revelarCategoria(
            CommandContext<CommandSourceStack> ctx, ServerPlayer alvo) {

        NenCategoryService.Revelacao resultado = NenCategoryService.revelar(alvo);
        String nome = alvo.getGameProfile().getName();

        switch (resultado) {
            case REVELOU -> ctx.getSource().sendSuccess(() -> Component.literal(
                    nome + " agora sabe a propria categoria: "
                            + NenProfileService.ler(alvo).category().getSerializedName()
                            + "."), true);
            case JA_SABIA -> ctx.getSource().sendSuccess(() -> Component.literal(
                    nome + " ja sabia; nada mudou."), false);
            case SEM_CATEGORIA -> {
                ctx.getSource().sendFailure(Component.translatable(
                        "nenfoundation.error.sem_categoria"));
                ctx.getSource().sendSuccess(() -> Component.literal(
                        "  (atribua antes: /nen category set <categoria> " + nome
                                + ", ou /nen category roll <semente> " + nome + ")"), false);
                return 0;
            }
            default -> throw new IllegalStateException("resultado desconhecido: " + resultado);
        }
        return 1;
    }

    private static int resetar(CommandContext<CommandSourceStack> ctx, ServerPlayer alvo) {
        PersistentNenData antes = NenProfileService.ler(alvo);

        // Antes de apagar, mostre o que vai ser apagado. Nao ha desfazer; o
        // texto na tela e a unica recuperacao possivel.
        ctx.getSource().sendSuccess(() -> Component.literal(
                "Perfil de " + alvo.getGameProfile().getName() + " ANTES do reset:"), true);
        for (String linha : RelatorioDePerfil.dump(alvo.getGameProfile().getName(), antes)) {
            ctx.getSource().sendSuccess(() -> Component.literal(linha), false);
        }

        NenProfileService.atualizar(alvo, qualquer -> PersistentNenData.NAO_DESPERTADO);

        ctx.getSource().sendSuccess(() -> Component.literal(
                "Perfil de " + alvo.getGameProfile().getName() + " zerado."), true);
        return 1;
    }

    // -------------------------------------------------------------- apoio

    /**
     * Copia o perfil trocando so o conjunto de tecnicas.
     *
     * <p>Escrito a mao porque o record nao tem {@code with}. Quando um segundo
     * campo precisar do mesmo tratamento, isto vira um builder — e nao antes.
     */
    private static PersistentNenData comTecnicas(
            PersistentNenData base, Set<ResourceLocation> tecnicas) {
        return new PersistentNenData(
                base.schemaVersion(), base.awakened(), base.category(),
                base.categoryRevealed(), base.auraPotential(), base.control(), base.output(),
                base.techniqueProficiency(), tecnicas,
                base.unlockedAbilities(), base.progressionFlags());
    }

    /**
     * Recusa id fora do namespace do mod.
     *
     * <p>Nao ha registro de tecnicas ate o M4, entao ninguem consegue conferir
     * se a tecnica EXISTE. O namespace e o que da para conferir hoje, e ele
     * pega o erro de digitacao mais comum ({@code minecraft:ten}). E ponto cego
     * declarado, nao cobertura: o portao de verdade chega com o registro.
     */
    static ResourceLocation validarTecnica(ResourceLocation tecnica)
            throws CommandSyntaxException {
        if (!NenFoundation.MOD_ID.equals(tecnica.getNamespace())) {
            throw NAMESPACE_ERRADO.create(tecnica);
        }
        return tecnica;
    }

    /**
     * Converte o texto numa das SEIS categorias reais.
     *
     * <p>{@code undetermined} e recusado com nome proprio, e nao tratado como
     * "categoria desconhecida": ele EXISTE no enum, e alguem que o digite esta
     * tentando apagar a categoria de um jogador -- operacao que nao existe. A
     * mensagem generica mandaria essa pessoa procurar erro de digitacao onde
     * nao ha nenhum.
     *
     * <p>Diferente de {@code technique unlock}, aqui da para conferir de
     * verdade: o enum esta congelado desde o M0. Nao ha ponto cego.
     */
    static NenCategory validarCategoria(String nome) throws CommandSyntaxException {
        if (NenCategory.UNDETERMINED.getSerializedName().equals(nome)) {
            throw CATEGORIA_NEUTRA.create(nome);
        }
        for (NenCategory categoria : NenCategory.REAIS) {
            if (categoria.getSerializedName().equals(nome)) {
                return categoria;
            }
        }
        throw CATEGORIA_DESCONHECIDA.create(nome);
    }

    private static ServerPlayer alvoOuProprio(CommandContext<CommandSourceStack> ctx)
            throws CommandSyntaxException {
        return ctx.getSource().getPlayerOrException();
    }

    private static ServerPlayer alvoDoArgumento(CommandContext<CommandSourceStack> ctx)
            throws CommandSyntaxException {
        return EntityArgument.getPlayer(ctx, "alvo");
    }
}
