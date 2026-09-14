package com.darkcontinent.nenfoundation.worldtree.checkpoint;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.List;
import java.util.ArrayList;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.saveddata.SavedData;

/** Progresso individual persistente, salvo no Overworld e indexado por UUID. */
public final class WorldTreePlayerProgressSavedData extends SavedData {
    public static final String DATA_ID = "world_tree_player_progress";
    private static final String PLAYERS = "players";
    private static final String UUID = "uuid";
    private static final String UNLOCKED = "unlocked";
    private final Map<java.util.UUID, Integer> unlocked = new HashMap<>();

    public static SavedData.Factory<WorldTreePlayerProgressSavedData> factory() {
        return new SavedData.Factory<>(WorldTreePlayerProgressSavedData::new,
                WorldTreePlayerProgressSavedData::load, null);
    }

    public static WorldTreePlayerProgressSavedData load(CompoundTag tag,
            HolderLookup.Provider registries) {
        WorldTreePlayerProgressSavedData data = new WorldTreePlayerProgressSavedData();
        ListTag players = tag.getList(PLAYERS, Tag.TAG_COMPOUND);
        for (int i = 0; i < players.size(); i++) {
            CompoundTag player = players.getCompound(i);
            data.unlocked.put(player.getUUID(UUID), player.getInt(UNLOCKED));
        }
        return data;
    }

    public boolean isUnlocked(java.util.UUID player, WorldTreeCheckpoint checkpoint) {
        return (unlocked.getOrDefault(player, 0) & (1 << checkpoint.ordinal())) != 0;
    }

    public boolean unlock(java.util.UUID player, WorldTreeCheckpoint checkpoint) {
        int old = unlocked.getOrDefault(player, 0);
        int updated = old | (1 << checkpoint.ordinal());
        if (old == updated) {
            return false;
        }
        unlocked.put(player, updated);
        setDirty();
        return true;
    }

    public List<WorldTreeCheckpoint> unlocked(java.util.UUID player) {
        int mask = unlocked.getOrDefault(player, 0);
        List<WorldTreeCheckpoint> result = new ArrayList<>();
        for (WorldTreeCheckpoint checkpoint : WorldTreeCheckpoint.values()) {
            if ((mask & (1 << checkpoint.ordinal())) != 0) {
                result.add(checkpoint);
            }
        }
        return List.copyOf(result);
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag players = new ListTag();
        unlocked.forEach((playerId, mask) -> {
            CompoundTag player = new CompoundTag();
            player.putUUID(UUID, playerId);
            player.putInt(UNLOCKED, mask);
            players.add(player);
        });
        tag.put(PLAYERS, players);
        return tag;
    }
}
