package com.darkcontinent.nenfoundation.server;

import com.darkcontinent.nenfoundation.nen.aura.MotorDeAura;
import com.darkcontinent.nenfoundation.nen.profile.PersistentNenData;
import com.darkcontinent.nenfoundation.nen.technique.NenContext;
import com.darkcontinent.nenfoundation.nen.technique.StopReason;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/** Implementacao server-side do contrato M0: toda pergunta consulta a sessao atual. */
public record ContextoDeNen(ServerPlayer jogador) implements NenContext {
    @Override public PersistentNenData perfil() { return NenProfileService.ler(this.jogador); }
    @Override public double auraDisponivel() { return NenAuraService.consultar(this.jogador).auraAtual(); }
    @Override public boolean gastarAura(double quantidade) {
        return NenAuraService.gastar(this.jogador, quantidade) == MotorDeAura.Gasto.PERMITIDO;
    }
    @Override public long tickDoServidor() { return this.jogador.serverLevel().getGameTime(); }
    @Override public boolean tecnicaAtiva(ResourceLocation tecnica) {
        return NenRuntimeService.estadoDe(this.jogador).tecnicasAtivas().contains(tecnica);
    }

    @Override public void desligar(ResourceLocation tecnica, StopReason motivo) {
        NenTechniqueService.desligar(this.jogador, tecnica, motivo);
    }
}
