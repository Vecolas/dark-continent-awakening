package com.darkcontinent.nenfoundation.client.vfx;

/**
 * Politica client-side de apresentacao. O servidor ainda deve decidir quais
 * informacoes de Nen o observador tem permissao para conhecer.
 */
public final class AuraVisibilityPolicy {
    private AuraVisibilityPolicy() {}

    public static boolean podeRenderizar(boolean observadorDesperto, boolean alvoEmZetsu,
            boolean alvoUsaIn, boolean observadorUsaGyo) {
        if (alvoEmZetsu) return false;
        if (alvoUsaIn && !observadorUsaGyo) return false;
        return observadorDesperto;
    }
}
