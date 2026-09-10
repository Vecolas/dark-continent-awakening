package com.darkcontinent.nenfoundation.data.attachment;

import com.darkcontinent.nenfoundation.NenFoundation;
import com.darkcontinent.nenfoundation.nen.profile.PersistentNenData;
import java.util.function.Supplier;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * Os Data Attachments do mod.
 *
 * <p>DECISOES QUE ESTE ARQUIVO CARREGA:
 *
 * <p>1. So o dado PERSISTENTE e attachment serializado. Estado de runtime nao
 * entra aqui — ver ADR-002. Um attachment serializado e gravado com o jogador;
 * usar um para aura atual significa disco a cada tick.
 *
 * <p>2. {@code copyOnDeath()} e ligado de proposito: progresso de Nen sobrevive
 * a morte. A politica de RUNTIME na morte (aura, tecnicas ativas, cooldown) e
 * outra coisa e mora no servico de runtime, nao aqui. [NF-3]
 *
 * <p>3. O NeoForge NAO sincroniza attachment com o cliente sozinho. Toda
 * sincronizacao e explicita, via os payloads de
 * {@code com.darkcontinent.nenfoundation.network}. Assumir sync automatico
 * produz um HUD que mostra o valor de outra pessoa, ou nada. [NF-3]
 */
public final class NenAttachments {

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, NenFoundation.MOD_ID);

    /** O progresso de Nen do jogador. Ver {@link PersistentNenData}. */
    public static final Supplier<AttachmentType<PersistentNenData>> NEN_PERSISTENTE =
            ATTACHMENT_TYPES.register("nen_persistente", () -> AttachmentType
                    .builder(() -> PersistentNenData.NAO_DESPERTADO)
                    .serialize(PersistentNenData.CODEC)
                    .copyOnDeath()
                    .build());

    private NenAttachments() {
    }
}
