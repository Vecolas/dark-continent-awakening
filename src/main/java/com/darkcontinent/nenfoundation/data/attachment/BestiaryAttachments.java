package com.darkcontinent.nenfoundation.data.attachment;

import com.darkcontinent.nenfoundation.NenFoundation;
import com.darkcontinent.nenfoundation.bestiary.BestiaryPlayerData;
import java.util.function.Supplier;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/** Registro do conhecimento persistente, separado do perfil de Nen. */
public final class BestiaryAttachments {
    public static final DeferredRegister<AttachmentType<?>> TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, NenFoundation.MOD_ID);
    public static final Supplier<AttachmentType<BestiaryPlayerData>> BESTIARY = TYPES.register(
            "bestiary_player_data", () -> AttachmentType.builder(() -> BestiaryPlayerData.EMPTY)
                    .serialize(BestiaryPlayerData.CODEC).copyOnDeath().build());

    private BestiaryAttachments() { }
}
