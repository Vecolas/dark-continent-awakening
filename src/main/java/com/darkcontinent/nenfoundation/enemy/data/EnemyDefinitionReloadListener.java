package com.darkcontinent.nenfoundation.enemy.data;

import com.darkcontinent.nenfoundation.NenFoundation;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Carrega definitions do datapack e instala o catálogo somente quando todos os
 * arquivos passam pelo codec e pelas validações de referência.
 *
 * <p>A decisão central é atomicidade: um JSON inválido, duplicado ou com tag
 * malformada não pode substituir um catálogo válido por um snapshot parcial.
 * Os perfis Java continuam sendo a fonte legada enquanto o schema não for
 * migrado por conteúdo; este listener não inventa balanceamento para eles.</p>
 */
@EventBusSubscriber(modid = NenFoundation.MOD_ID)
public final class EnemyDefinitionReloadListener extends SimpleJsonResourceReloadListener {
    public static final String FOLDER = "enemy_definitions";
    private static final Logger LOG = LoggerFactory.getLogger(EnemyDefinitionReloadListener.class);
    private static final Gson GSON = new Gson();
    private static final EnemyDefinitionRegistry REGISTRY = new EnemyDefinitionRegistry();
    private final EnemyDefinitionRegistry destino;

    public EnemyDefinitionReloadListener() {
        this(REGISTRY);
    }

    EnemyDefinitionReloadListener(EnemyDefinitionRegistry destino) {
        super(GSON, FOLDER);
        this.destino = destino;
    }

    public static EnemyDefinitionRegistry registry() {
        return REGISTRY;
    }

    @SubscribeEvent
    public static void aoRegistrarRecarregador(AddReloadListenerEvent evento) {
        evento.addListener(new EnemyDefinitionReloadListener());
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> arquivos,
            ResourceManager gerenciador, ProfilerFiller profiler) {
        ArrayList<EnemyDefinition> candidatas = new ArrayList<>();
        for (Map.Entry<ResourceLocation, JsonElement> arquivo : arquivos.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.comparing(ResourceLocation::toString))).toList()) {
            ResourceLocation recurso = arquivo.getKey();
            EnemyDefinition.CODEC.parse(JsonOps.INSTANCE, arquivo.getValue())
                    .resultOrPartial(erro -> LOG.error(
                            "Definition de inimigo invalida em data/{}/{}: {}",
                            recurso.getNamespace(), recurso.getPath(), erro))
                    .ifPresent(definition -> {
                        var problemas = EnemyDefinitionValidator.problemas(definition);
                        if (problemas.isEmpty()) {
                            candidatas.add(definition);
                        } else {
                            LOG.error("Definition de inimigo invalida em data/{}/{}: {}",
                                    recurso.getNamespace(), recurso.getPath(), String.join("; ", problemas));
                        }
                    });
        }
        if (candidatas.size() != arquivos.size()) {
            LOG.error("Catalogo de inimigos nao aplicado: {}/{} arquivos validos; snapshot anterior preservado.",
                    candidatas.size(), arquivos.size());
            return;
        }
        try {
            destino.replaceAll(candidatas);
        } catch (IllegalArgumentException erro) {
            LOG.error("Catalogo de inimigos nao aplicado: {}; snapshot anterior preservado.",
                    erro.getMessage());
        }
    }
}
