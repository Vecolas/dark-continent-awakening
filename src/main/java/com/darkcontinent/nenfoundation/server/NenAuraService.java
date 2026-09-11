package com.darkcontinent.nenfoundation.server;

import com.darkcontinent.nenfoundation.config.NenConfig;
import com.darkcontinent.nenfoundation.nen.aura.MotorDeAura;
import com.darkcontinent.nenfoundation.nen.profile.RuntimeNenState;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;

/** Compoe perfil vivo, motor e sync no unico scheduler; o registro nao retem entidades. */
public final class NenAuraService {
    private static final Map<UUID, MotorDeAura> MOTORES = new HashMap<>();

    private NenAuraService() { }

    public static void tick(ServerPlayer jogador, RuntimeNenState estado) {
        motor(jogador).tick(NenProfileService.ler(jogador));
        NenSyncService.enviarDeltaSeAuraSuja(jogador);
    }

    private static MotorDeAura motor(ServerPlayer jogador) {
        return MOTORES.computeIfAbsent(jogador.getUUID(), id ->
                new MotorDeAura(NenRuntimeService.estadoDe(jogador), NenConfig.AURA, NenConfig::devModeAtivo));
    }

    public static RuntimeNenState consultar(ServerPlayer jogador) {
        motor(jogador).atualizar(NenProfileService.ler(jogador));
        return NenRuntimeService.estadoDe(jogador);
    }

    public static MotorDeAura.Gasto gastar(ServerPlayer jogador, double quantidade) {
        return motor(jogador).gastar(NenProfileService.ler(jogador), quantidade,
                jogador.serverLevel().getGameTime());
    }

    public static double output(ServerPlayer jogador) {
        return motor(jogador).output(NenProfileService.ler(jogador));
    }

    public static MotorDeAura.Medida medida(ServerPlayer jogador) { return motor(jogador).medida(); }
    static void registrarDelta(ServerPlayer jogador) { motor(jogador).registrarDelta(); }
    static void encerrarSessao(UUID id) { MOTORES.remove(id); }
}
