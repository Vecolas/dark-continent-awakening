package com.darkcontinent.nenfoundation.worldtree;

import net.minecraft.nbt.CompoundTag;

/** Migra o formato legado do estado global para a versão atual antes do decode. */
public final class WorldTreeSaveMigration {
    private static final String VERSION = "generation_version";

    private WorldTreeSaveMigration() {
    }

    public static CompoundTag migrate(CompoundTag input) {
        CompoundTag migrated = input.copy();
        int version = migrated.getInt(VERSION);
        if (version > WorldTreeSavedData.CURRENT_VERSION) {
            throw new IllegalArgumentException("versao de World Tree nao suportada: " + version);
        }
        if (version == 0) {
            migrated.putInt(VERSION, WorldTreeSavedData.CURRENT_VERSION);
        }
        return migrated;
    }
}
