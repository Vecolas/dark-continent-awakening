package com.darkcontinent.nenfoundation.structure;

import java.util.EnumMap;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;

/**
 * Mapeamento inicial, data-driven, do vocabulário visual V2 para blocos
 * vanilla. Blocos customizados podem substituir os ids sem alterar a
 * geometria nem o consumidor do blockout.
 */
public final class PostoAvancadoMaterialPalette {
    private PostoAvancadoMaterialPalette() { }

    public static Map<PostoAvancadoBlockout.Material, ResourceLocation> vanilla() {
        Map<PostoAvancadoBlockout.Material, ResourceLocation> palette = new EnumMap<>(
                PostoAvancadoBlockout.Material.class);
        palette.put(PostoAvancadoBlockout.Material.FOUNDATION,
                ResourceLocation.withDefaultNamespace("smooth_stone"));
        palette.put(PostoAvancadoBlockout.Material.CONCRETE,
                ResourceLocation.withDefaultNamespace("white_concrete"));
        palette.put(PostoAvancadoBlockout.Material.METAL,
                ResourceLocation.withDefaultNamespace("white_concrete"));
        palette.put(PostoAvancadoBlockout.Material.CANVAS,
                ResourceLocation.withDefaultNamespace("light_gray_wool"));
        palette.put(PostoAvancadoBlockout.Material.PATIO_GRAVEL,
                ResourceLocation.withDefaultNamespace("gravel"));
        palette.put(PostoAvancadoBlockout.Material.WINDOW,
                ResourceLocation.withDefaultNamespace("glass_pane"));
        palette.put(PostoAvancadoBlockout.Material.WOOD,
                ResourceLocation.withDefaultNamespace("spruce_planks"));
        palette.put(PostoAvancadoBlockout.Material.GRATING,
                ResourceLocation.withDefaultNamespace("white_concrete"));
        palette.put(PostoAvancadoBlockout.Material.HARDSTAND,
                ResourceLocation.withDefaultNamespace("polished_andesite"));
        palette.put(PostoAvancadoBlockout.Material.EQUIPMENT,
                ResourceLocation.withDefaultNamespace("blast_furnace"));
        palette.put(PostoAvancadoBlockout.Material.LIGHT,
                ResourceLocation.withDefaultNamespace("shroomlight"));
        palette.put(PostoAvancadoBlockout.Material.TRIM,
                ResourceLocation.withDefaultNamespace("smooth_stone"));
        palette.put(PostoAvancadoBlockout.Material.FRAME,
                ResourceLocation.withDefaultNamespace("iron_bars"));
        palette.put(PostoAvancadoBlockout.Material.CRATE,
                ResourceLocation.withDefaultNamespace("chest"));
        palette.put(PostoAvancadoBlockout.Material.SIGN,
                ResourceLocation.withDefaultNamespace("oak_wall_sign"));
        palette.put(PostoAvancadoBlockout.Material.GATE,
                ResourceLocation.withDefaultNamespace("iron_bars"));
        palette.put(PostoAvancadoBlockout.Material.CABLE,
                ResourceLocation.withDefaultNamespace("black_wool"));
        palette.put(PostoAvancadoBlockout.Material.ANTENNA,
                ResourceLocation.withDefaultNamespace("lightning_rod"));
        palette.put(PostoAvancadoBlockout.Material.SECRET_CACHE,
                ResourceLocation.withDefaultNamespace("chest"));
        palette.put(PostoAvancadoBlockout.Material.EMBLEM_RED,
                ResourceLocation.withDefaultNamespace("red_concrete"));
        return Map.copyOf(palette);
    }
}
