package com.darkcontinent.nenfoundation.enemy.entity;

import com.darkcontinent.nenfoundation.enemy.ai.AwarenessTuning;
import com.darkcontinent.nenfoundation.enemy.base.BaseHxHMob;
import com.darkcontinent.nenfoundation.enemy.content.HunterExamProfiles;
import com.darkcontinent.nenfoundation.enemy.registry.EnemyEntityTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;

/** Primeiro vertical slice; goals, renderer e ataque serao ligados nas issues seguintes. */
public final class GreatStampEntity extends BaseHxHMob {
    public GreatStampEntity(EntityType<? extends GreatStampEntity> type, Level level) {
        super(type, level, HunterExamProfiles.greatStamp().metadata(), new AwarenessTuning(30, 160));
    }

    public static AttributeSupplier.Builder createAttributes() {
        var attrs = HunterExamProfiles.greatStamp().attributes();
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, attrs.maxHealth())
                .add(Attributes.MOVEMENT_SPEED, attrs.movementSpeed())
                .add(Attributes.ATTACK_DAMAGE, attrs.attackDamage())
                .add(Attributes.ARMOR, attrs.armor())
                .add(Attributes.FOLLOW_RANGE, attrs.followRange())
                .add(Attributes.KNOCKBACK_RESISTANCE, attrs.knockbackResistance());
    }

    public static EntityType<GreatStampEntity> registeredType() { return EnemyEntityTypes.GREAT_STAMP.get(); }
}
