package com.darkcontinent.nenfoundation.command;

import com.darkcontinent.nenfoundation.NenFoundation;
import com.darkcontinent.nenfoundation.nen.profile.PersistentNenData;
import com.darkcontinent.nenfoundation.server.NenProfileService;
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

        raiz.then(Commands.literal("reset")
                .then(Commands.literal("confirmar")
                        .executes(ctx -> resetar(ctx, alvoOuProprio(ctx)))
                        .then(Commands.argument("alvo", EntityArgument.player())
                                .executes(ctx -> resetar(ctx, alvoDoArgumento(ctx))))));

        return dispatcher.register(raiz);
    }

    // ------------------------------------------------------------- acoes

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

    private static ServerPlayer alvoOuProprio(CommandContext<CommandSourceStack> ctx)
            throws CommandSyntaxException {
        return ctx.getSource().getPlayerOrException();
    }

    private static ServerPlayer alvoDoArgumento(CommandContext<CommandSourceStack> ctx)
            throws CommandSyntaxException {
        return EntityArgument.getPlayer(ctx, "alvo");
    }
}
