package com.darkcontinent.nenfoundation.client.particle;

import com.darkcontinent.nenfoundation.registry.AuraSparkParticleOptions;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.world.entity.Entity;

/** Faisca curta de acabamento, vinculada ao jogador que a originou. */
public final class AuraSparkParticle extends TextureSheetParticle {

    private static final Map<Chave, Integer> ATIVAS_POR_DONO = new HashMap<>();
    private static ClientLevel ultimoNivel;

    private final int ownerId;
    private final float baseScale;
    private final float baseAlpha;
    private final Chave chave;
    private boolean contabilizada;

    private AuraSparkParticle(ClientLevel level, double x, double y, double z,
            double vx, double vy, double vz, AuraSparkParticleOptions options,
            SpriteSet sprites) {
        super(level, x, y, z, vx, vy, vz);
        this.ownerId = options.ownerId();
        this.rCol = ((options.argb() >> 16) & 0xFF) / 255.0F;
        this.gCol = ((options.argb() >> 8) & 0xFF) / 255.0F;
        this.bCol = (options.argb() & 0xFF) / 255.0F;
        this.baseAlpha = ((options.argb() >>> 24) & 0xFF) / 255.0F;
        this.alpha = this.baseAlpha;
        this.baseScale = options.scale();
        this.chave = new Chave(level, this.ownerId);
        this.xd = vx;
        this.yd = vy;
        this.zd = vz;
        this.lifetime = 8 + this.random.nextInt(5);
        this.gravity = 0.0F;
        this.hasPhysics = false;
        this.pickSprite(sprites);
        this.quadSize = this.baseScale * 0.45F;

        ATIVAS_POR_DONO.merge(this.chave, 1, Integer::sum);
        this.contabilizada = true;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Override
    public void tick() {
        super.tick();
        Entity owner = this.level.getEntity(this.ownerId);
        if (owner == null || owner.isRemoved() || !owner.isAlive()) {
            this.remove();
            return;
        }
        this.alpha = this.baseAlpha
                * (1.0F - ((float) this.age / (float) this.lifetime));
        this.quadSize = this.baseScale
                * (0.45F + 0.15F * ((float) this.age / this.lifetime));
    }

    @Override
    public void remove() {
        super.remove();
        if (this.contabilizada) {
            ATIVAS_POR_DONO.compute(this.chave,
                    (chave, quantidade) -> quantidade == null || quantidade <= 1
                            ? null : quantidade - 1);
            this.contabilizada = false;
        }
    }

    /** Limpa ids da sessao anterior caso o motor descarte particulas em lote. */
    public static void limparContagem() {
        ATIVAS_POR_DONO.clear();
        ultimoNivel = null;
    }

    /** Quantas faiscas ainda existem no mundo cliente, para o overlay de dev. */
    public static int totalAtivas() {
        int total = 0;
        for (int quantidade : ATIVAS_POR_DONO.values()) {
            total += quantidade;
        }
        return total;
    }

    public static final class Provider implements ParticleProvider<AuraSparkParticleOptions> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(AuraSparkParticleOptions tipo, ClientLevel nivel,
                double x, double y, double z, double dx, double dy, double dz) {
            if (ultimoNivel != nivel) {
                ATIVAS_POR_DONO.clear();
                ultimoNivel = nivel;
            }
            Chave chave = new Chave(nivel, tipo.ownerId());
            if (ATIVAS_POR_DONO.getOrDefault(chave, 0) >= tipo.maxAtivas()) {
                return null;
            }
            return new AuraSparkParticle(nivel, x, y, z, dx, dy, dz, tipo, this.sprites);
        }
    }

    private record Chave(ClientLevel nivel, int ownerId) {
    }
}
