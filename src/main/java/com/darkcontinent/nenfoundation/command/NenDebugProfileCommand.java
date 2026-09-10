package com.darkcontinent.nenfoundation.command;

import com.darkcontinent.nenfoundation.NenFoundation;
import com.darkcontinent.nenfoundation.nen.profile.PersistentNenData;
import com.darkcontinent.nenfoundation.server.NenProfileService;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/** Comando minimo de leitura do perfil; a suite de debug completa pertence a #7. */
@EventBusSubscriber(modid = NenFoundation.MOD_ID)
public final class NenDebugProfileCommand {

    static final int NIVEL_DE_OPERADOR = 2;

    private NenDebugProfileCommand() {
    }

    @SubscribeEvent
    public static void aoRegistrarComandos(RegisterCommandsEvent evento) {
        registrar(evento.getDispatcher());
    }

    static void registrar(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("nen")
                .requires(fonte -> fonte.hasPermission(NIVEL_DE_OPERADOR))
                .then(Commands.literal("debug")
                        .then(Commands.literal("profile")
                                .executes(contexto -> mostrarPerfil(contexto.getSource())))));
    }

    private static int mostrarPerfil(CommandSourceStack fonte) throws CommandSyntaxException {
        PersistentNenData perfil = NenProfileService.ler(fonte.getPlayerOrException());
        fonte.sendSuccess(() -> Component.literal(formatar(perfil)), false);
        return 1;
    }

    static String formatar(PersistentNenData perfil) {
        return String.format(Locale.ROOT,
                "NenProfile{schema=%d, awakened=%s, category=%s, revealed=%s, "
                        + "auraPotential=%.3f, control=%.3f, output=%.3f, "
                        + "proficiencies={%s}, techniques=[%s], abilities=[%s], flags=[%s]}",
                perfil.schemaVersion(),
                perfil.awakened(),
                perfil.category().getSerializedName(),
                perfil.categoryRevealed(),
                perfil.auraPotential(),
                perfil.control(),
                perfil.output(),
                proficienciasOrdenadas(perfil.techniqueProficiency()),
                idsOrdenados(perfil.unlockedTechniques()),
                idsOrdenados(perfil.unlockedAbilities()),
                idsOrdenados(perfil.progressionFlags()));
    }

    private static String proficienciasOrdenadas(Map<ResourceLocation, Double> proficiencias) {
        return proficiencias.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entrada -> String.format(Locale.ROOT, "%s=%.3f",
                        entrada.getKey(), entrada.getValue()))
                .collect(Collectors.joining(", "));
    }

    private static String idsOrdenados(Set<ResourceLocation> ids) {
        return ids.stream().map(Object::toString).sorted().collect(Collectors.joining(", "));
    }
}
