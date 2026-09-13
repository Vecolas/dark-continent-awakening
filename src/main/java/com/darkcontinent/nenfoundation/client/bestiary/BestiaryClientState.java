package com.darkcontinent.nenfoundation.client.bestiary;

import com.darkcontinent.nenfoundation.bestiary.BestiaryPlayerData;
import com.darkcontinent.nenfoundation.bestiary.BestiaryProgress;
import net.minecraft.resources.ResourceLocation;
import java.util.Map;

/** Cache somente-leitura do conhecimento do jogador local; sync entra na próxima fatia. */
public final class BestiaryClientState {
    private static BestiaryPlayerData data = BestiaryPlayerData.EMPTY;

    private BestiaryClientState() { }
    public static BestiaryProgress progress(ResourceLocation id) { return data.progress(id); }
    public static void replace(BestiaryPlayerData novo) { data = novo; }
    public static void replaceLevels(Map<ResourceLocation, BestiaryProgress> progressos) {
        data = new BestiaryPlayerData(progressos);
    }
    public static void clear() { data = BestiaryPlayerData.EMPTY; }
}
