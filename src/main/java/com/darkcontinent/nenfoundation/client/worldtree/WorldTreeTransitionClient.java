package com.darkcontinent.nenfoundation.client.worldtree;

import com.darkcontinent.nenfoundation.NenFoundation;
import com.darkcontinent.nenfoundation.worldtree.WorldTreeAltitudeService;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.sounds.SoundEvents;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/** Efeitos visuais locais; não decide teleporte nem desbloqueio. */
@EventBusSubscriber(modid = NenFoundation.MOD_ID, value = Dist.CLIENT)
public final class WorldTreeTransitionClient {
    static final int FADE_TICKS = 20;
    private static boolean lastWasWorldTree;
    private static int fadeTicks;

    private WorldTreeTransitionClient() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) {
            return;
        }
        boolean worldTree = WorldTreeAltitudeService.isWorldTree(minecraft.level.dimension());
        if (worldTree != lastWasWorldTree) {
            fadeTicks = FADE_TICKS;
            minecraft.player.playSound(SoundEvents.AMBIENT_CAVE.value(), 0.45F,
                    worldTree ? 0.72F : 0.9F);
            lastWasWorldTree = worldTree;
        }
        if (fadeTicks > 0) {
            fadeTicks--;
        }
    }

    @SubscribeEvent
    public static void onFog(ViewportEvent.RenderFog event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || !WorldTreeAltitudeService
                .isWorldTree(minecraft.level.dimension())) {
            return;
        }
        int y = minecraft.player == null ? 0 : minecraft.player.blockPosition().getY();
        if (y >= 320 && y < 520) {
            event.scaleFarPlaneDistance(0.38F);
            event.scaleNearPlaneDistance(0.75F);
        } else if (y >= 820 && y < 1120) {
            event.scaleFarPlaneDistance(0.72F);
        }
    }

    @SubscribeEvent
    public static void onFogColor(ViewportEvent.ComputeFogColor event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || !WorldTreeAltitudeService
                .isWorldTree(minecraft.level.dimension())) {
            return;
        }
        int y = minecraft.player == null ? 0 : minecraft.player.blockPosition().getY();
        if (y >= 320 && y < 520) {
            event.setRed(0.72F);
            event.setGreen(0.82F);
            event.setBlue(0.92F);
        }
    }

    @SubscribeEvent
    public static void onGui(RenderGuiEvent.Post event) {
        if (fadeTicks <= 0) {
            return;
        }
        float progress = 1.0F - (fadeTicks / (float) FADE_TICKS);
        float opacity = progress < 0.5F ? progress * 2.0F : (1.0F - progress) * 2.0F;
        int alpha = Math.min(190, Math.max(0, (int) (opacity * 190.0F)));
        GuiGraphics graphics = event.getGuiGraphics();
        Minecraft minecraft = Minecraft.getInstance();
        graphics.fill(0, 0, minecraft.getWindow().getGuiScaledWidth(),
                minecraft.getWindow().getGuiScaledHeight(), alpha << 24 | 0xE7EEF2);
    }
}
