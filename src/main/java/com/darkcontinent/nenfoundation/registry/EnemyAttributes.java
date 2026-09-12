package com.darkcontinent.nenfoundation.registry;

import com.darkcontinent.nenfoundation.enemy.FoxbearEntity;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;

/** Atributos server-side das entidades do pacote. */
public final class EnemyAttributes {
    private EnemyAttributes() { }
    public static void registrar(EntityAttributeCreationEvent evento) {
        evento.put(EnemyEntityTypes.FOXBEAR.get(), FoxbearEntity.createAttributes().build());
    }
}
