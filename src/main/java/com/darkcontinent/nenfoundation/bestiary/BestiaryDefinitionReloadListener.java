package com.darkcontinent.nenfoundation.bestiary;

import com.darkcontinent.nenfoundation.NenFoundation;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Troca atômica das fichas editoriais fornecidas por datapack. */
@EventBusSubscriber(modid = NenFoundation.MOD_ID)
public final class BestiaryDefinitionReloadListener extends SimpleJsonResourceReloadListener {
    private static final String FOLDER = "bestiary";
    private static final Logger LOG = LoggerFactory.getLogger(BestiaryDefinitionReloadListener.class);

    public BestiaryDefinitionReloadListener() { super(new Gson(), FOLDER); }

    @SubscribeEvent
    public static void registrar(AddReloadListenerEvent evento) {
        evento.addListener(new BestiaryDefinitionReloadListener());
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager manager,
            ProfilerFiller profiler) {
        var candidates = new ArrayList<BestiaryEntryDefinition>();
        for (var file : files.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.comparing(ResourceLocation::toString))).toList()) {
            BestiaryEntryDefinition.CODEC.parse(JsonOps.INSTANCE, file.getValue())
                    .resultOrPartial(error -> LOG.error("Bestiary definition inválida em {}: {}", file.getKey(), error))
                    .ifPresent(definition -> candidates.add(new BestiaryEntryDefinition(file.getKey(),
                    definition.entityType(), definition.category(), definition.threat(), definition.studiedAt(),
                    definition.masteredAt(), definition.habitatKey(),
                            definition.summaryKey(), definition.behaviorKey(), definition.combatKey())));
        }
        if (candidates.size() != files.size()) {
            LOG.error("Catálogo de bestiário preservado: {}/{} definições válidas", candidates.size(), files.size());
            return;
        }
        try {
            BestiaryRegistry.replaceAll(candidates);
        } catch (IllegalArgumentException error) {
            LOG.error("Catálogo de bestiário preservado: {}", error.getMessage());
        }
    }
}
