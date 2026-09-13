package com.darkcontinent.nenfoundation.structure;

import com.darkcontinent.nenfoundation.NenFoundation;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;

/** Tag única de biomas onde o posto pode ser descoberto. */
public final class PostoAvancadoBiomes {
    public static final TagKey<Biome> HUNTER_OUTPOST_BIOMES = TagKey.create(
            Registries.BIOME, NenFoundation.id("hunter_outpost_biomes"));

    private PostoAvancadoBiomes() { }
}
