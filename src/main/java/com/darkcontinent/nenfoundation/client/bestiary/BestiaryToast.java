package com.darkcontinent.nenfoundation.client.bestiary;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastComponent;
import net.minecraft.client.gui.components.toasts.TutorialToast;
import net.minecraft.network.chat.Component;

/** Aviso de descoberta com limite de cinco segundos, independente da preferência de notificações. */
public final class BestiaryToast extends TutorialToast {
    private static final long MAX_DISPLAY_TIME_MS = 5_000L;

    public BestiaryToast(Component title, Component message) {
        super(TutorialToast.Icons.RECIPE_BOOK, title, message, false);
    }

    @Override
    public Toast.Visibility render(GuiGraphics graphics, ToastComponent toasts, long timeSinceLastVisible) {
        Toast.Visibility visibility = super.render(graphics, toasts, timeSinceLastVisible);
        return timeSinceLastVisible >= MAX_DISPLAY_TIME_MS ? Toast.Visibility.HIDE : visibility;
    }
}
