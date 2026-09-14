package com.darkcontinent.nenfoundation.client.vfx;

import com.darkcontinent.nenfoundation.api.SinalDeAura;
import com.darkcontinent.nenfoundation.sound.NenSoundEvents;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.IntFunction;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.sounds.EntityBoundSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;

/** Reproduz o audio posicional que pertence a sessao visual da aura. */
public final class AudioDeAura {

    private static final float VOLUME_DA_ATIVACAO = 0.55F;

    private final Map<Integer, SoundInstance> ativacoes = new HashMap<>();
    // Reutilizado todo tick: audio nao justifica uma colecao descartavel por
    // quadro, multiplicada por cada cliente e por toda a sessao.
    private final Set<Integer> presentes = new HashSet<>();
    private final DetectorDeAtivacaoDeTen detectorDeTerceiros =
            new DetectorDeAtivacaoDeTen();
    private ClientLevel nivelAnterior;

    /**
     * Atualiza o audio do jogador local e dos jogadores observados.
     *
     * <p>O som usa {@link SoundSource#PLAYERS}: ele acompanha o controle de
     * volume de jogadores, em vez de obrigar alguem a desligar todos os
     * efeitos. A instancia e presa ao dono — inclusive enquanto ele se move —
     * e tem alcance fixo de dez blocos.
     */
    public void aoTick(Minecraft mc, boolean ativacaoLocal,
            IntFunction<SinalDeAura> sinalDe) {
        if (mc.level != this.nivelAnterior) {
            pararTudo(mc);
            this.detectorDeTerceiros.limpar();
            this.nivelAnterior = mc.level;
        }
        descartarConcluidas(mc);
        if (mc.level == null || mc.player == null || !mc.player.isAlive()) {
            pararTudo(mc);
            return;
        }

        if (ativacaoLocal) {
            tocar(mc, mc.player);
        }

        this.presentes.clear();
        for (Player jogador : mc.level.players()) {
            if (jogador == mc.player) {
                continue;
            }
            this.presentes.add(jogador.getId());
            if (this.detectorDeTerceiros.atualizar(jogador.getId(),
                    sinalDe.apply(jogador.getId()))) {
                tocar(mc, jogador);
            }
        }
        this.detectorDeTerceiros.reterSomente(this.presentes);
    }

    /** Quem liga, desliga: logout, morte e dimensao passam por aqui. */
    public void limpar(Minecraft mc) {
        pararTudo(mc);
        this.detectorDeTerceiros.limpar();
        this.presentes.clear();
        this.nivelAnterior = null;
    }

    private void tocar(Minecraft mc, Player dono) {
        parar(mc, dono.getId());
        SoundInstance instancia = new EntityBoundSoundInstance(
                NenSoundEvents.TEN_ACTIVATE.get(), SoundSource.PLAYERS,
                VOLUME_DA_ATIVACAO, 1.0F, dono,
                SoundInstance.createUnseededRandom().nextLong());
        this.ativacoes.put(dono.getId(), instancia);
        mc.getSoundManager().play(instancia);
    }

    private void descartarConcluidas(Minecraft mc) {
        this.ativacoes.entrySet().removeIf(
                entrada -> !mc.getSoundManager().isActive(entrada.getValue()));
    }

    private void parar(Minecraft mc, int entidadeId) {
        SoundInstance anterior = this.ativacoes.remove(entidadeId);
        if (anterior != null) {
            mc.getSoundManager().stop(anterior);
        }
    }

    private void pararTudo(Minecraft mc) {
        for (SoundInstance instancia : this.ativacoes.values()) {
            mc.getSoundManager().stop(instancia);
        }
        this.ativacoes.clear();
    }
}
