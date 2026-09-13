package com.darkcontinent.nenfoundation.client.vfx.debug;

import com.darkcontinent.nenfoundation.client.vfx.AuraRenderLod;
import com.darkcontinent.nenfoundation.client.vfx.AuraVisualMode;
import com.darkcontinent.nenfoundation.client.vfx.MedidorDeVfx;
import com.darkcontinent.nenfoundation.client.vfx.SobreposicaoDeVfx;
import com.darkcontinent.nenfoundation.client.vfx.SobreposicaoDeVfx.AjusteDePerfil;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;

/**
 * {@code /nenvfx} -- ajustar a aura e tirar capturas, sem recompilar.
 *
 * <p><b>COMANDO DE CLIENTE, e nao de servidor.</b> Ele e registrado em
 * {@link RegisterClientCommandsEvent}, e essa escolha e o contrato inteiro
 * desta classe: nada aqui muda tecnica, aura, custo, cooldown ou visibilidade.
 * O que ele mexe e no que ESTE cliente desenha. Registrado no evento de
 * servidor, o mesmo codigo passaria a decidir aparencia para os outros -- e
 * seria o cliente virando autoridade sobre alguma coisa, que e o que o ADR-001
 * existe para impedir.
 *
 * <p>PERMISSIONADO MESMO SENDO DE CLIENTE. Nao porque ele poderia trapacear --
 * ele nao pode --, mas porque um jogador que descobre {@code /nenvfx off} num
 * servidor de jogo passa a ver um mundo diferente do que os outros veem, e o
 * relato de bug que sai dali custa uma tarde a quem for investigar.
 *
 * <p>RECUSA SEMPRE TEM MOTIVO, e por chave de traducao. Um comando que falha em
 * silencio produz o pior relato de bug que existe -- esta escrito assim no
 * {@code CLAUDE.md}, e vale aqui como vale no resto.
 *
 * <p>NENHUM SUBCOMANDO SEM CONSUMIDOR. {@code bloom} nao existe neste arquivo
 * porque o passe de pos-processamento nasce no AV5: um botao que nao gira nada
 * e o erro numero 7 do {@code CLAUDE.md} em forma de comando.
 *
 * <p>CLIENT-ONLY.
 */
public final class AuraDebugCommands {

    /** Valor que devolve o controle ao servidor, em todos os subcomandos. */
    private static final String AUTO = "auto";

    private AuraDebugCommands() {
    }

    /** Registra {@code /nenvfx}. Chamado do barramento principal, so no cliente. */
    public static void registrar(RegisterClientCommandsEvent evento) {
        LiteralArgumentBuilder<CommandSourceStack> raiz = Commands.literal("nenvfx")
                .requires(fonte -> permitido());

        raiz.then(Commands.literal("on").executes(ctx -> {
            SobreposicaoDeVfx.desligar(false);
            return relatar(ctx, "nenfoundation.vfx.desenho_ligado");
        }));
        raiz.then(Commands.literal("off").executes(ctx -> {
            SobreposicaoDeVfx.desligar(true);
            return relatar(ctx, "nenfoundation.vfx.desenho_desligado");
        }));

        LiteralArgumentBuilder<CommandSourceStack> estado = Commands.literal("state");
        for (AuraVisualMode modo : AuraVisualMode.values()) {
            if (modo == AuraVisualMode.CUSTOM) {
                // CUSTOM NAO E UM ESTADO QUE SE FORCA: ele existe para uma
                // habilidade trazer o proprio perfil, e forcar o nome sem o
                // perfil junto desenharia o perfil de emergencia com um rotulo
                // que mente sobre o que esta na tela.
                continue;
            }
            String nome = modo.name().toLowerCase(Locale.ROOT);
            estado.then(Commands.literal(nome).executes(ctx -> {
                SobreposicaoDeVfx.forcarModo(modo);
                return relatar(ctx, "nenfoundation.vfx.estado_forcado", nome);
            }));
        }
        estado.then(Commands.literal(AUTO).executes(ctx -> {
            SobreposicaoDeVfx.forcarModo(null);
            return relatar(ctx, "nenfoundation.vfx.estado_automatico");
        }));
        raiz.then(estado);

        raiz.then(Commands.literal("output")
                .then(Commands.literal(AUTO).executes(ctx -> {
                    SobreposicaoDeVfx.forcarOutput(-1.0F);
                    return relatar(ctx, "nenfoundation.vfx.output_automatico");
                }))
                .then(Commands.argument("valor", FloatArgumentType.floatArg(0.0F, 1.0F))
                        .executes(ctx -> {
                            float valor = FloatArgumentType.getFloat(ctx, "valor");
                            SobreposicaoDeVfx.forcarOutput(valor);
                            return relatar(ctx, "nenfoundation.vfx.output_forcado", valor);
                        })));

        raiz.then(Commands.literal("particulas")
                .then(Commands.literal(AUTO).executes(ctx -> {
                    SobreposicaoDeVfx.forcarDensidade(-1.0F);
                    return relatar(ctx, "nenfoundation.vfx.particulas_automatico");
                }))
                .then(Commands.argument("densidade", FloatArgumentType.floatArg(0.0F, 2.0F))
                        .executes(ctx -> {
                            float valor = FloatArgumentType.getFloat(ctx, "densidade");
                            SobreposicaoDeVfx.forcarDensidade(valor);
                            return relatar(ctx, "nenfoundation.vfx.particulas_forcada", valor);
                        })));

        raiz.then(Commands.literal("ribbons")
                .then(Commands.literal(AUTO).executes(ctx -> {
                    SobreposicaoDeVfx.forcarRibbons(-1);
                    return relatar(ctx, "nenfoundation.vfx.ribbons_automatico");
                }))
                .then(Commands.argument("quantidade", IntegerArgumentType.integer(0, 64))
                        .executes(ctx -> {
                            int valor = IntegerArgumentType.getInteger(ctx, "quantidade");
                            SobreposicaoDeVfx.forcarRibbons(valor);
                            return relatar(ctx, "nenfoundation.vfx.ribbons_forcadas", valor);
                        })));

        LiteralArgumentBuilder<CommandSourceStack> lod = Commands.literal("lod");
        for (AuraRenderLod nivel : AuraRenderLod.values()) {
            String nome = nivel.name().toLowerCase(Locale.ROOT);
            lod.then(Commands.literal(nome).executes(ctx -> {
                SobreposicaoDeVfx.forcarLod(nivel);
                return relatar(ctx, "nenfoundation.vfx.lod_forcado", nome);
            }));
        }
        lod.then(Commands.literal(AUTO).executes(ctx -> {
            SobreposicaoDeVfx.forcarLod(null);
            return relatar(ctx, "nenfoundation.vfx.lod_automatico");
        }));
        raiz.then(lod);

        raiz.then(Commands.literal("freeze").executes(ctx -> {
            boolean novo = !SobreposicaoDeVfx.congelado();
            SobreposicaoDeVfx.congelar(novo);
            return relatar(ctx, novo ? "nenfoundation.vfx.congelado"
                    : "nenfoundation.vfx.descongelado");
        }));

        LiteralArgumentBuilder<CommandSourceStack> ajuste = Commands.literal("ajuste");
        for (AjusteDePerfil alvo : AjusteDePerfil.values()) {
            ajuste.then(Commands.literal(alvo.nome())
                    .then(Commands.literal(AUTO).executes(ctx -> {
                        SobreposicaoDeVfx.forcarNoPerfil(alvo, -1.0F);
                        return relatar(ctx, "nenfoundation.vfx.ajuste_automatico", alvo.nome());
                    }))
                    .then(Commands.argument("valor",
                            FloatArgumentType.floatArg(alvo.minimo(), alvo.maximo()))
                            .executes(ctx -> {
                                float valor = FloatArgumentType.getFloat(ctx, "valor");
                                SobreposicaoDeVfx.forcarNoPerfil(alvo, valor);
                                return relatar(ctx, "nenfoundation.vfx.ajuste_forcado",
                                        alvo.nome(), valor);
                            })));
        }
        raiz.then(ajuste);

        raiz.then(Commands.literal("capture")
                .then(Commands.literal("modo")
                        .then(Commands.literal("on").executes(AuraDebugCommands::ligarCaptura))
                        .then(Commands.literal("off").executes(AuraDebugCommands::desligarCaptura)))
                .then(Commands.literal("agora")
                        .then(Commands.argument("nome", StringArgumentType.word())
                                .executes(AuraDebugCommands::capturarAgora)))
                .then(Commands.literal("lote")
                        .then(Commands.argument("etiqueta", StringArgumentType.word())
                                .executes(AuraDebugCommands::capturarLote))));

        raiz.then(Commands.literal("tuning").executes(ctx -> {
            // A TELA ABRE NO PROXIMO TICK, e nao aqui. Abrir uma Screen de
            // dentro da execucao de um comando fecha o chat por cima dela, e a
            // tela nasce sem foco.
            TelaDeTuningDeAura.pedirAbertura();
            return relatar(ctx, "nenfoundation.vfx.tuning_aberto");
        }));

        raiz.then(Commands.literal("reset").executes(ctx -> {
            SobreposicaoDeVfx.limpar();
            return relatar(ctx, "nenfoundation.vfx.reset");
        }));

        raiz.then(Commands.literal("status").executes(AuraDebugCommands::status));

        evento.getDispatcher().register(raiz);
    }

    private static int ligarCaptura(CommandContext<CommandSourceStack> ctx) {
        Minecraft mc = Minecraft.getInstance();
        if (!AuraCaptureMode.instancia().ligar(mc)) {
            return recusar(ctx, "nenfoundation.vfx.captura_ja_ligada");
        }
        return relatar(ctx, "nenfoundation.vfx.captura_ligada");
    }

    private static int desligarCaptura(CommandContext<CommandSourceStack> ctx) {
        AuraCaptureMode.instancia().desligar(Minecraft.getInstance());
        return relatar(ctx, "nenfoundation.vfx.captura_desligada");
    }

    private static int capturarAgora(CommandContext<CommandSourceStack> ctx) {
        String nome = StringArgumentType.getString(ctx, "nome");
        AuraCaptureMode.instancia().capturar(Minecraft.getInstance(), nome);
        return 1;
    }

    private static int capturarLote(CommandContext<CommandSourceStack> ctx) {
        String etiqueta = StringArgumentType.getString(ctx, "etiqueta");
        AuraCaptureMode captura = AuraCaptureMode.instancia();
        if (!captura.ligado()) {
            // RECUSA COM MOTIVO, e nao liga o modo por conta propria. Um lote
            // tirado sem os travamentos produz seis imagens que PARECEM
            // comparaveis e nao sao -- exatamente o resultado que o modo de
            // captura existe para impedir.
            return recusar(ctx, "nenfoundation.vfx.lote_sem_modo");
        }
        captura.iniciarLote(etiqueta);
        return relatar(ctx, "nenfoundation.vfx.lote_iniciado", etiqueta);
    }

    private static int status(CommandContext<CommandSourceStack> ctx) {
        ctx.getSource().sendSuccess(() -> Component.literal(
                "nenvfx: desenho=" + (SobreposicaoDeVfx.desligado() ? "off" : "on")
                        + " | sobreposicao=" + (SobreposicaoDeVfx.ativa() ? "ATIVA" : "nenhuma")
                        + " | captura=" + (AuraCaptureMode.instancia().ligado() ? "on" : "off")
                        + " | chamadas=" + MedidorDeVfx.chamadasDeDesenho()
                        + " | filamentos=" + MedidorDeVfx.filamentos()
                        + " | commit=" + InfoDeBuild.commit()), false);
        return 1;
    }

    private static int relatar(CommandContext<CommandSourceStack> ctx, String chave,
            Object... argumentos) {
        ctx.getSource().sendSuccess(() -> Component.translatable(chave, argumentos), false);
        return 1;
    }

    private static int recusar(CommandContext<CommandSourceStack> ctx, String chave) {
        ctx.getSource().sendFailure(Component.translatable(chave));
        return 0;
    }

    /**
     * Quem pode usar.
     *
     * <p>OP de nivel 2 OU mundo local com cheats. A segunda metade existe
     * porque o cliente de desenvolvimento roda em mundo proprio na maior parte
     * do tempo, e exigir OP ali transformaria a ferramenta de tuning em uma
     * ferramenta que precisa de um servidor de pe para ser usada.
     */
    private static boolean permitido() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && mc.player.hasPermissions(2)) {
            return true;
        }
        return mc.getSingleplayerServer() != null
                && mc.getSingleplayerServer().getWorldData().isAllowCommands();
    }
}
