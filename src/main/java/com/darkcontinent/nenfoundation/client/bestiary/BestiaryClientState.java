package com.darkcontinent.nenfoundation.client.bestiary;

import com.darkcontinent.nenfoundation.bestiary.BestiaryPlayerData;
import com.darkcontinent.nenfoundation.bestiary.BestiaryProgress;
import net.minecraft.resources.ResourceLocation;
import java.util.Map;
import com.darkcontinent.nenfoundation.bestiary.BestiaryKnowledgeLevel;

/** Cache somente-leitura do conhecimento do jogador local; sync entra na próxima fatia. */
public final class BestiaryClientState {
    private static BestiaryPlayerData data = BestiaryPlayerData.EMPTY;

    private BestiaryClientState() { }
    public static BestiaryProgress progress(ResourceLocation id) { return data.progress(id); }
    public static void replace(BestiaryPlayerData novo) { data = novo; }
    public static void replaceLevels(Map<ResourceLocation, BestiaryKnowledgeLevel> levels) {
        var novo = new java.util.HashMap<ResourceLocation, BestiaryProgress>();
        levels.forEach((id, level) -> novo.put(id, new BestiaryProgress(level, 0, 0, 0, 0,
                0L, 0L, java.util.Set.of(), java.util.Set.of(), java.util.Set.of(), java.util.Set.of())));
        data = new BestiaryPlayerData(novo);
    }
    public static void clear() { data = BestiaryPlayerData.EMPTY; }
}
