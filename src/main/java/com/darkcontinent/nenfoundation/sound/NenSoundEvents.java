package com.darkcontinent.nenfoundation.sound;

import com.darkcontinent.nenfoundation.NenFoundation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Registro unico dos sons autorais do Nen Foundation. */
public final class NenSoundEvents {

    /** Alcance de projeto: a doze blocos a ativacao ja deve estar inaudivel. */
    static final float ALCANCE_DA_ATIVACAO_DE_TEN = 10.0F;

    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(BuiltInRegistries.SOUND_EVENT, NenFoundation.MOD_ID);

    public static final DeferredHolder<SoundEvent, SoundEvent> TEN_ACTIVATE =
            SOUND_EVENTS.register("vfx.nen.ten_activate", () -> SoundEvent.createFixedRangeEvent(
                    NenFoundation.id("vfx.nen.ten_activate"), ALCANCE_DA_ATIVACAO_DE_TEN));

    private NenSoundEvents() {}

    /** Mantem a chamada de registro num lugar so. */
    public static void register(IEventBus modEventBus) {
        SOUND_EVENTS.register(modEventBus);
    }
}
