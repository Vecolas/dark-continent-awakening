package com.darkcontinent.nenfoundation.network.payload;

import com.darkcontinent.nenfoundation.NenFoundation;
import com.darkcontinent.nenfoundation.bestiary.BestiaryKnowledgeLevel;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Conhecimento do próprio jogador; nunca inclui entradas de outro jogador. */
public record BestiarySnapshotS2C(Map<ResourceLocation, BestiaryKnowledgeLevel> entries)
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
        var decoded = new LinkedHashMap<ResourceLocation, BestiaryKnowledgeLevel>();
        raw.forEach((id, level) -> decoded.put(id, BestiaryKnowledgeLevel.valueOf(level)));
        return new BestiarySnapshotS2C(decoded);
    }

    private static Map<ResourceLocation, String> encode(BestiarySnapshotS2C payload) {
        var encoded = new LinkedHashMap<ResourceLocation, String>();
        payload.entries.forEach((id, level) -> encoded.put(id, level.name()));
        return encoded;
    }

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
