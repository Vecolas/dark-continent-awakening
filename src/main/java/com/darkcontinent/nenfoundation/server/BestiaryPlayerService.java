package com.darkcontinent.nenfoundation.server;

import com.darkcontinent.nenfoundation.bestiary.BestiaryPlayerData;
import com.darkcontinent.nenfoundation.bestiary.BestiaryProgress;
import com.darkcontinent.nenfoundation.bestiary.BestiaryEntryDefinition;
import com.darkcontinent.nenfoundation.bestiary.BestiaryKnowledgeLevel;
import com.darkcontinent.nenfoundation.bestiary.BestiaryRegistry;
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
        var entry = BestiaryRegistry.get(id);
        if (entry != null) atualizar(jogador, id, pesquisar(progresso(jogador, id).observe(gameTime), entry, 1,
                BestiaryKnowledgeLevel.OBSERVED));
    }

    public static void lutar(ServerPlayer jogador, ResourceLocation id) {
        var entry = BestiaryRegistry.get(id);
        if (entry != null) atualizar(jogador, id, pesquisar(progresso(jogador, id).fought(), entry, 2,
                BestiaryKnowledgeLevel.FOUGHT));
    }

    /** A futura Research Table chamará esta porta; o cliente nunca escolhe o nível. */
    public static void pesquisar(ServerPlayer jogador, ResourceLocation id, int pontos) {
        if (pontos <= 0) return;
        var entry = BestiaryRegistry.get(id);
        if (entry != null) atualizar(jogador, id, pesquisar(progresso(jogador, id), entry, pontos,
                BestiaryKnowledgeLevel.STUDIED));
    }

    private static BestiaryProgress pesquisar(BestiaryProgress atual, BestiaryEntryDefinition entry,
            int pontos, BestiaryKnowledgeLevel minimo) {
        var resultado = atual.withResearchPoints(pontos,
                minimo == BestiaryKnowledgeLevel.STUDIED ? atual.knowledgeLevel() : minimo);
        if (resultado.researchPoints() >= entry.masteredAt()) {
            return resultado.withResearchPoints(0, BestiaryKnowledgeLevel.MASTERED);
        }
        if (resultado.researchPoints() >= entry.studiedAt()) {
            return resultado.withResearchPoints(0, BestiaryKnowledgeLevel.STUDIED);
        }
        return resultado;
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
