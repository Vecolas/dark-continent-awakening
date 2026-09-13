package com.darkcontinent.nenfoundation.structure;

import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.saveddata.SavedData;

/** Persistência dos candidatos de chunk que já receberam o posto. */
public final class PostoAvancadoSpawnData extends SavedData {
    private static final String CHUNKS = "placed_candidate_chunks";
    private final Set<Long> chunks = new HashSet<>();

    public static SavedData.Factory<PostoAvancadoSpawnData> factory() {
        return new SavedData.Factory<>(PostoAvancadoSpawnData::new,
                PostoAvancadoSpawnData::carregar, null);
    }

    public static PostoAvancadoSpawnData carregar(CompoundTag tag, HolderLookup.Provider registries) {
        var data = new PostoAvancadoSpawnData();
        long[] salvos = tag.getLongArray(CHUNKS);
        for (long chunk : salvos) {
            data.chunks.add(chunk);
        }
        return data;
    }

    public boolean contem(long chunk) {
        return chunks.contains(chunk);
    }

    public void registrar(long chunk) {
        if (chunks.add(chunk)) {
            setDirty();
        }
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        long[] salvos = new long[chunks.size()];
        int i = 0;
        for (long chunk : chunks) {
            salvos[i++] = chunk;
        }
        tag.put(CHUNKS, new LongArrayTag(salvos));
        return tag;
    }
}
