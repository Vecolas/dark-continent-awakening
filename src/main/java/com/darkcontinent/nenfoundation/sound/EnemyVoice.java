package com.darkcontinent.nenfoundation.sound;

import java.util.Objects;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * Os cinco sons de UM inimigo, juntos.
 *
 * <p><b>Eles viajam juntos porque sao um conjunto, e nao cinco campos.</b> Um mob
 * com ambiente e sem alerta nao e "quase completo": e um mob que ataca do nada.
 * Guardados soltos na entidade, o quinto seria esquecido no decimo bicho -- e o
 * esquecido nao da erro, so deixa de tocar.</p>
 *
 * <p>Os holders sao {@code DeferredHolder} e nao {@code SoundEvent}: o registro
 * ainda nao aconteceu quando esta classe e construida, e resolver cedo demais
 * devolveria nulo -- que viraria {@code NullPointerException} no primeiro
 * grunhido, dentro de um tick de servidor.</p>
 */
public record EnemyVoice(
        DeferredHolder<SoundEvent, SoundEvent> ambiente,
        DeferredHolder<SoundEvent, SoundEvent> alerta,
        DeferredHolder<SoundEvent, SoundEvent> ataque,
        DeferredHolder<SoundEvent, SoundEvent> dor,
        DeferredHolder<SoundEvent, SoundEvent> morte) {

    public EnemyVoice {
        Objects.requireNonNull(ambiente, "voz sem ambiente");
        Objects.requireNonNull(alerta, "voz sem alerta: o mob atacaria do nada");
        Objects.requireNonNull(ataque, "voz sem ataque");
        Objects.requireNonNull(dor, "voz sem dor");
        Objects.requireNonNull(morte, "voz sem morte");
    }

    public SoundEvent ambienteResolvido() { return ambiente.get(); }
    public SoundEvent alertaResolvido() { return alerta.get(); }
    public SoundEvent ataqueResolvido() { return ataque.get(); }
    public SoundEvent dorResolvida() { return dor.get(); }
    public SoundEvent morteResolvida() { return morte.get(); }
}
