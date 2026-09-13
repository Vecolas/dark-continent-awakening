package com.darkcontinent.nenfoundation.network;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.darkcontinent.nenfoundation.bestiary.BestiaryKnowledgeLevel;
import com.darkcontinent.nenfoundation.bestiary.BestiaryNenStatus;
import com.darkcontinent.nenfoundation.bestiary.BestiaryProgress;
import com.darkcontinent.nenfoundation.network.payload.BestiarySnapshotS2C;
import io.netty.buffer.Unpooled;
import java.util.Map;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

class BestiarySnapshotPayloadTest {
    @Test
    void payloadPreservaProgressoCompleto() {
        var id = ResourceLocation.fromNamespaceAndPath("nenfoundation", "foxbear");
        var progress = new BestiaryProgress(BestiaryKnowledgeLevel.STUDIED, 4, 2, 1, 6,
                10L, 20L, Set.of("opening_after_charge"), Set.of("territorial"),
                Set.of("captured"), Set.of("field_note"), BestiaryNenStatus.AWAKENED);
        var original = new BestiarySnapshotS2C(Map.of(id, progress));
        var buffer = Unpooled.buffer();
        BestiarySnapshotS2C.STREAM_CODEC.encode(buffer, original);
        var decoded = BestiarySnapshotS2C.STREAM_CODEC.decode(buffer);

        assertEquals(progress, decoded.entries().get(id));
    }
}
