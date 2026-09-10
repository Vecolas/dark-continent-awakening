package com.darkcontinent.nenfoundation.server;

import com.darkcontinent.nenfoundation.nen.profile.RuntimeNenState;
import net.minecraft.server.level.ServerPlayer;

/** Participante do unico ciclo central de tick de Nen. */
@FunctionalInterface
public interface NenTickSubsystem {

    void serverTick(ServerPlayer jogador, RuntimeNenState estado);
}
