package com.darkcontinent.nenfoundation.server;

import com.darkcontinent.nenfoundation.bestiary.BestiaryKnowledgeLevel;
import com.darkcontinent.nenfoundation.bestiary.BestiaryPlayerData;
import com.darkcontinent.nenfoundation.bestiary.BestiaryProgress;
import com.darkcontinent.nenfoundation.data.attachment.BestiaryAttachments;
import com.darkcontinent.nenfoundation.network.payload.BestiarySnapshotS2C;
import java.util.LinkedHashMap;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

/** Única porta server-side para ler, mutar e sincronizar conhecimento. */
public final class BestiaryPlayerService {
    private BestiaryPlayerService() { }

    public static BestiaryPlayerData ler(ServerPlayer jogador) {
        return jogador.getData(BestiaryAttachments.BESTIARY);
    }

    public static BestiaryProgress progresso(ServerPlayer jogador, ResourceLocation id) {
        return ler(jogador).progress(id);
    }

    public static void observar(ServerPlayer jogador, ResourceLocation id, long gameTime) {
        atualizar(jogador, id, progresso(jogador, id).observe(gameTime));
    }

    public static void lutar(ServerPlayer jogador, ResourceLocation id) {
        atualizar(jogador, id, progresso(jogador, id).fought());
    }

    private static void atualizar(ServerPlayer jogador, ResourceLocation id, BestiaryProgress progresso) {
        var antigo = ler(jogador);
        var novo = antigo.withProgress(id, progresso);
        if (!novo.equals(antigo)) {
            jogador.setData(BestiaryAttachments.BESTIARY, novo);
            sincronizar(jogador);
        }
    }

    public static void sincronizar(ServerPlayer jogador) {
        if (jogador.connection == null) return;
        var entries = new LinkedHashMap<ResourceLocation, BestiaryKnowledgeLevel>();
        ler(jogador).entries().forEach((id, progress) -> entries.put(id, progress.knowledgeLevel()));
        var payload = new BestiarySnapshotS2C(entries);
        if (jogador.connection.hasChannel(payload.type())) {
            PacketDistributor.sendToPlayer(jogador, payload);
        }
    }
}
