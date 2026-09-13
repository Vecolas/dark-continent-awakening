package com.darkcontinent.nenfoundation.server;

import com.darkcontinent.nenfoundation.bestiary.BestiaryPlayerData;
import com.darkcontinent.nenfoundation.bestiary.BestiaryProgress;
import com.darkcontinent.nenfoundation.bestiary.BestiaryEntryDefinition;
import com.darkcontinent.nenfoundation.bestiary.BestiaryKnowledgeLevel;
import com.darkcontinent.nenfoundation.bestiary.BestiaryNenStatus;
import com.darkcontinent.nenfoundation.bestiary.BestiaryResearchService;
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
        if (entry != null) atualizar(jogador, id, BestiaryResearchService.aplicar(progresso(jogador, id).observe(gameTime), entry, 1,
                BestiaryKnowledgeLevel.OBSERVED));
    }

    public static void lutar(ServerPlayer jogador, ResourceLocation id) {
        var entry = BestiaryRegistry.get(id);
        if (entry != null) atualizar(jogador, id, BestiaryResearchService.aplicar(progresso(jogador, id).fought(), entry, 2,
                BestiaryKnowledgeLevel.FOUGHT));
    }

    public static void derrotar(ServerPlayer jogador, ResourceLocation id) {
        var entry = BestiaryRegistry.get(id);
        if (entry != null) atualizar(jogador, id, BestiaryResearchService.aplicar(progresso(jogador, id).defeated(), entry, 3,
                BestiaryKnowledgeLevel.FOUGHT));
    }

    /** A futura Research Table chamará esta porta; o cliente nunca escolhe o nível. */
    public static void pesquisar(ServerPlayer jogador, ResourceLocation id, int pontos) {
        if (pontos <= 0) return;
        var entry = BestiaryRegistry.get(id);
        if (entry != null) atualizar(jogador, id, BestiaryResearchService.aplicar(progresso(jogador, id), entry, pontos,
                BestiaryKnowledgeLevel.STUDIED));
    }

    public static void descobrir(ServerPlayer jogador, ResourceLocation id, int pontos, String descoberta) {
        if (pontos <= 0) return;
        var entry = BestiaryRegistry.get(id);
        if (entry != null) {
            var resultado = BestiaryResearchService.aplicar(progresso(jogador, id), entry, pontos, BestiaryKnowledgeLevel.STUDIED)
                    .withSpecialDiscovery(descoberta);
            atualizar(jogador, id, resultado);
        }
    }

    public static void descobrirPontoFraco(ServerPlayer jogador, ResourceLocation id, String pontoFraco) {
        var entry = BestiaryRegistry.get(id);
        if (entry != null) {
            atualizar(jogador, id, progresso(jogador, id).withWeakPoint(pontoFraco));
        }
    }

    public static void descobrirNen(ServerPlayer jogador, ResourceLocation id, BestiaryNenStatus status) {
        var entry = BestiaryRegistry.get(id);
        if (entry != null) atualizar(jogador, id, progresso(jogador, id).withNenStatus(status));
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
        var entries = new LinkedHashMap<ResourceLocation, BestiaryProgress>();
        ler(jogador).entries().forEach(entries::put);
        var payload = new BestiarySnapshotS2C(entries);
        if (jogador.connection.hasChannel(payload.type())) {
            PacketDistributor.sendToPlayer(jogador, payload);
        }
    }

    public static void substituir(ServerPlayer jogador, BestiaryPlayerData data) {
        jogador.setData(BestiaryAttachments.BESTIARY, data);
        sincronizar(jogador);
    }
}
