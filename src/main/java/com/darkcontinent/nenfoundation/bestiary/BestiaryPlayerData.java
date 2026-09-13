package com.darkcontinent.nenfoundation.bestiary;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;

/** Attachment persistente por jogador, compartilhado por qualquer cópia do item. */
public record BestiaryPlayerData(Map<ResourceLocation, BestiaryProgress> entries) {
    public static final Codec<BestiaryPlayerData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.unboundedMap(ResourceLocation.CODEC, BestiaryProgress.CODEC)
                    .optionalFieldOf("entries", Map.of()).forGetter(BestiaryPlayerData::entries)
    ).apply(instance, BestiaryPlayerData::new));

    public static final BestiaryPlayerData EMPTY = new BestiaryPlayerData(Map.of());

    public BestiaryPlayerData {
        entries = Map.copyOf(entries);
    }

    public BestiaryProgress progress(ResourceLocation id) {
        return entries.getOrDefault(id, BestiaryProgress.UNKNOWN);
    }

    public BestiaryPlayerData withProgress(ResourceLocation id, BestiaryProgress progress) {
        var copy = new java.util.HashMap<>(entries);
        copy.put(id, progress);
        return new BestiaryPlayerData(copy);
    }
}
