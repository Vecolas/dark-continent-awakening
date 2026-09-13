package com.darkcontinent.nenfoundation.client.bestiary;

import com.darkcontinent.nenfoundation.bestiary.BestiaryPlayerData;
import com.darkcontinent.nenfoundation.bestiary.BestiaryProgress;
import net.minecraft.resources.ResourceLocation;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

/** Cache somente-leitura do conhecimento do jogador local; sync entra na próxima fatia. */
public final class BestiaryClientState {
    private static BestiaryPlayerData data = BestiaryPlayerData.EMPTY;
    private static boolean initialized;

    private BestiaryClientState() { }
    public static BestiaryProgress progress(ResourceLocation id) { return data.progress(id); }
    public static void replace(BestiaryPlayerData novo) { data = novo; }
    public static void replaceLevels(Map<ResourceLocation, BestiaryProgress> progressos) {
        var anterior = data;
        data = new BestiaryPlayerData(progressos);
        if (initialized) {
            data.entries().forEach((id, progress) -> {
                var old = anterior.progress(id).knowledgeLevel();
                var current = progress.knowledgeLevel();
                if (current.ordinal() > old.ordinal()) {
                    String nome = Component.translatable("bestiary.entry." + id.getPath()).getString();
                    var title = Component.translatable("bestiary.toast.title");
                    var description = Component.translatable(current.ordinal() == 1
                            ? "bestiary.toast.observed" : "bestiary.toast.updated", nome);
                    Minecraft.getInstance().getToasts().addToast(new BestiaryToast(title, description));
                }
            });
        }
        initialized = true;
    }
    public static void clear() { data = BestiaryPlayerData.EMPTY; initialized = false; }
}
