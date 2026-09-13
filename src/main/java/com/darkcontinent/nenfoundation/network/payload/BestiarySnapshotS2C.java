package com.darkcontinent.nenfoundation.network.payload;

import com.darkcontinent.nenfoundation.NenFoundation;
import com.darkcontinent.nenfoundation.bestiary.BestiaryProgress;
import com.mojang.serialization.JsonOps;
import com.google.gson.JsonParser;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Conhecimento do próprio jogador; nunca inclui entradas de outro jogador. */
public record BestiarySnapshotS2C(Map<ResourceLocation, BestiaryProgress> entries)
        implements CustomPacketPayload {
    public static final Type<BestiarySnapshotS2C> TYPE =
            new Type<>(NenFoundation.id("bestiary_snapshot"));
    private static final StreamCodec<io.netty.buffer.ByteBuf, Map<ResourceLocation, String>> RAW =
            ByteBufCodecs.map(LinkedHashMap::new, ResourceLocation.STREAM_CODEC,
                    ByteBufCodecs.STRING_UTF8);
    public static final StreamCodec<io.netty.buffer.ByteBuf, BestiarySnapshotS2C> STREAM_CODEC =
            RAW.map(BestiarySnapshotS2C::decode, BestiarySnapshotS2C::encode);

    public BestiarySnapshotS2C {
        entries = Map.copyOf(entries);
    }

    private static BestiarySnapshotS2C decode(Map<ResourceLocation, String> raw) {
        var decoded = new LinkedHashMap<ResourceLocation, BestiaryProgress>();
        raw.forEach((id, json) -> BestiaryProgress.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(json))
                .resultOrPartial(error -> { throw new IllegalArgumentException("progresso de bestiario invalido: " + error); })
                .ifPresent(progress -> decoded.put(id, progress)));
        return new BestiarySnapshotS2C(decoded);
    }

    private static Map<ResourceLocation, String> encode(BestiarySnapshotS2C payload) {
        var encoded = new LinkedHashMap<ResourceLocation, String>();
        payload.entries.forEach((id, progress) -> encoded.put(id,
                BestiaryProgress.CODEC.encodeStart(JsonOps.INSTANCE, progress).getOrThrow().toString()));
        return encoded;
    }

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
