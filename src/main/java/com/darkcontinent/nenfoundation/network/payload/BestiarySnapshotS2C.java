package com.darkcontinent.nenfoundation.network.payload;

import com.darkcontinent.nenfoundation.NenFoundation;
import com.darkcontinent.nenfoundation.bestiary.BestiaryProgress;
import com.darkcontinent.nenfoundation.bestiary.BestiaryEntryDefinition;
import com.mojang.serialization.JsonOps;
import com.google.gson.JsonParser;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Conhecimento do próprio jogador; nunca inclui entradas de outro jogador. */
public record BestiarySnapshotS2C(Map<ResourceLocation, BestiaryProgress> entries,
        Map<ResourceLocation, BestiaryEntryDefinition> definitions)
        implements CustomPacketPayload {
    public static final Type<BestiarySnapshotS2C> TYPE =
            new Type<>(NenFoundation.id("bestiary_snapshot"));
    private static final StreamCodec<io.netty.buffer.ByteBuf, Map<ResourceLocation, String>> PROGRESS_RAW =
            ByteBufCodecs.map(LinkedHashMap::new, ResourceLocation.STREAM_CODEC,
                    ByteBufCodecs.STRING_UTF8);
    private static final StreamCodec<io.netty.buffer.ByteBuf, Map<ResourceLocation, String>> DEFINITIONS_RAW =
            ByteBufCodecs.map(LinkedHashMap::new, ResourceLocation.STREAM_CODEC,
                    ByteBufCodecs.STRING_UTF8);
    private static final StreamCodec<io.netty.buffer.ByteBuf, Wire> RAW = StreamCodec.composite(
            PROGRESS_RAW, Wire::progress,
            DEFINITIONS_RAW, Wire::definitions,
            Wire::new);
    public static final StreamCodec<io.netty.buffer.ByteBuf, BestiarySnapshotS2C> STREAM_CODEC =
            RAW.map(BestiarySnapshotS2C::decode, BestiarySnapshotS2C::encode);

    public BestiarySnapshotS2C(Map<ResourceLocation, BestiaryProgress> entries) {
        this(entries, Map.of());
    }

    public BestiarySnapshotS2C {
        entries = Map.copyOf(entries);
        definitions = Map.copyOf(definitions);
    }

    private static BestiarySnapshotS2C decode(Wire raw) {
        var decoded = new LinkedHashMap<ResourceLocation, BestiaryProgress>();
        raw.progress().forEach((id, json) -> BestiaryProgress.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(json))
                .resultOrPartial(error -> { throw new IllegalArgumentException("progresso de bestiario invalido: " + error); })
                .ifPresent(progress -> decoded.put(id, progress)));
        var definitions = new LinkedHashMap<ResourceLocation, BestiaryEntryDefinition>();
        raw.definitions().forEach((id, json) -> BestiaryEntryDefinition.CODEC
                .parse(JsonOps.INSTANCE, JsonParser.parseString(json))
                .resultOrPartial(error -> { throw new IllegalArgumentException("definicao de bestiario invalida: " + error); })
                .ifPresent(definition -> definitions.put(id, new BestiaryEntryDefinition(id,
                        definition.entityType(), definition.category(), definition.threat(), definition.studiedAt(),
                        definition.masteredAt(), definition.habitatKey(), definition.summaryKey(),
                        definition.behaviorKey(), definition.combatKey()))));
        return new BestiarySnapshotS2C(decoded, definitions);
    }

    private static Wire encode(BestiarySnapshotS2C payload) {
        var encodedProgress = new LinkedHashMap<ResourceLocation, String>();
        payload.entries.forEach((id, progress) -> encodedProgress.put(id,
                BestiaryProgress.CODEC.encodeStart(JsonOps.INSTANCE, progress).getOrThrow().toString()));
        var definitions = new LinkedHashMap<ResourceLocation, String>();
        payload.definitions.forEach((id, definition) -> definitions.put(id,
                BestiaryEntryDefinition.CODEC.encodeStart(JsonOps.INSTANCE, definition).getOrThrow().toString()));
        return new Wire(encodedProgress, definitions);
    }

    private record Wire(Map<ResourceLocation, String> progress, Map<ResourceLocation, String> definitions) { }

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
