package com.darkcontinent.nenfoundation.client.particle;

import com.darkcontinent.nenfoundation.registry.AuraDebrisParticleOptions;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.world.entity.Entity;

/**
 * Um fragmento minusculo levantado pela pressao de Ren.
 *
 * <p><b>ELE NAO TOCA O MUNDO, E ISSO E REQUISITO.</b> Sem quebra de bloco, sem
 * {@code ItemEntity}, sem colisao, sem empurrao, sem fisica. VFX que altera o
 * mundo e VFX que precisa de autoridade de servidor, e a autoridade e do
 * servidor -- nao do renderer (ADR-001). O empurrao de Ren, se um dia existir, e
 * sistema de gameplay separado.
 *
 * <p>{@code hasPhysics = false} e o que garante isso no codigo: o detrito
 * atravessa geometria em vez de colidir, e nunca consulta {@code BlockState}
 * durante a vida. A APARENCIA foi amostrada uma vez, na emissao, e chega pronta
 * na cor -- ver {@code EmissorDeParticulasDeAura}.
 *
 * <p>ELE SOBE POUCO, E DE PROPOSITO: 0,05 a 0,25 bloco. Um fragmento que sobe
 * dois blocos nao e detrito, e destroco -- e destroco pede uma explicacao de
 * gameplay que nao existe.
 *
 * <p>ORBITA LEVE, e nao trajetoria reta. A deriva lateral vem de um angulo
 * sorteado uma vez e de uma velocidade angular pequena; ela e o que faz o
 * fragmento parecer arrastado por um campo em vez de cuspido.
 *
 * <p>QUEM LIGA, DESLIGA. A contagem por dono e decrementada em {@link #remove()},
 * que o motor chama tanto na morte natural quanto no descarte em lote -- e
 * {@link #limparContagem()} zera o resto no logout, pelo mesmo caminho da
 * faisca.
 */
public final class AuraDebrisParticle extends TextureSheetParticle {

    private static final Map<Chave, Integer> ATIVOS_POR_DONO = new HashMap<>();
    private static ClientLevel ultimoNivel;

    private final int ownerId;
    private final float baseScale;
    private final float baseAlpha;
    private final Chave chave;
    private final double raioDaOrbita;
    private final double velocidadeAngular;
    private final double centroX;
    private final double centroZ;
    private double angulo;
    private boolean contabilizado;

    private AuraDebrisParticle(ClientLevel level, double x, double y, double z,
            double vx, double vy, double vz, AuraDebrisParticleOptions options,
            SpriteSet sprites) {
        super(level, x, y, z, 0.0D, 0.0D, 0.0D);
        this.ownerId = options.ownerId();
        this.rCol = ((options.argb() >> 16) & 0xFF) / 255.0F;
        this.gCol = ((options.argb() >> 8) & 0xFF) / 255.0F;
        this.bCol = (options.argb() & 0xFF) / 255.0F;
        this.baseAlpha = ((options.argb() >>> 24) & 0xFF) / 255.0F;
        this.alpha = this.baseAlpha;
        this.baseScale = options.scale();
        this.chave = new Chave(level, this.ownerId);

        // A SUBIDA E LENTA E CURTA. `yd` vem de quem emitiu, ja dentro da faixa
        // de 0,05 a 0,25 bloco ao longo da vida -- o calculo mora la porque e la
        // que o perfil esta.
        this.yd = vy;
        this.xd = 0.0D;
        this.zd = 0.0D;
        this.gravity = 0.0F;
        // SEM FISICA: o detrito nao colide com nada. E a linha que garante, no
        // codigo, que ele nao empurra e nao e empurrado.
        this.hasPhysics = false;

        this.centroX = x;
        this.centroZ = z;
        this.raioDaOrbita = 0.02D + this.random.nextDouble() * 0.05D;
        this.angulo = this.random.nextDouble() * Math.PI * 2.0D;
        this.velocidadeAngular = (this.random.nextBoolean() ? 1.0D : -1.0D)
                * (0.04D + this.random.nextDouble() * 0.06D);

        this.lifetime = 14 + this.random.nextInt(12);
        this.pickSprite(sprites);
        this.quadSize = this.baseScale;

        ATIVOS_POR_DONO.merge(this.chave, 1, Integer::sum);
        this.contabilizado = true;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Override
    public void tick() {
        Entity dono = this.level.getEntity(this.ownerId);
        if (dono == null || dono.isRemoved() || !dono.isAlive()) {
            // QUEM LIGA, DESLIGA -- e aqui isso vale para a MORTE do dono
            // tambem. Sem esta guarda, um jogador que morre em Ren deixaria doze
            // fragmentos orbitando um ponto vazio ate o fim da vida deles.
            this.remove();
            return;
        }
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        if (this.age++ >= this.lifetime) {
            this.remove();
            return;
        }

        this.angulo += this.velocidadeAngular;
        float t = (float) this.age / (float) this.lifetime;
        // A ORBITA ABRE COM O TEMPO: o fragmento sai do lugar onde nasceu em vez
        // de girar em torno de si mesmo, que e a leitura de "preso num
        // redemoinho".
        double raio = this.raioDaOrbita * (0.4D + 0.6D * t);
        this.setPos(this.centroX + Math.cos(this.angulo) * raio,
                this.y + this.yd,
                this.centroZ + Math.sin(this.angulo) * raio);

        // O FADE E A SEGUNDA METADE DA VIDA, e nao a vida inteira: um fragmento
        // que ja nasce apagando nunca chega a ser lido como materia.
        this.alpha = this.baseAlpha * (1.0F - Math.max(0.0F, (t - 0.45F) / 0.55F));
        this.quadSize = this.baseScale * (1.0F - 0.25F * t);
    }

    @Override
    public void remove() {
        super.remove();
        if (this.contabilizado) {
            ATIVOS_POR_DONO.compute(this.chave,
                    (chave, quantidade) -> quantidade == null || quantidade <= 1
                            ? null : quantidade - 1);
            this.contabilizado = false;
        }
    }

    /** Limpa ids da sessao anterior caso o motor descarte particulas em lote. */
    public static void limparContagem() {
        ATIVOS_POR_DONO.clear();
        ultimoNivel = null;
    }

    /** Quantos detritos ainda existem no mundo cliente, para o overlay de dev. */
    public static int totalAtivos() {
        int total = 0;
        for (int quantidade : ATIVOS_POR_DONO.values()) {
            total += quantidade;
        }
        return total;
    }

    /** Quantos detritos deste dono existem agora. */
    public static int ativosDe(ClientLevel nivel, int ownerId) {
        return ATIVOS_POR_DONO.getOrDefault(new Chave(nivel, ownerId), 0);
    }

    public static final class Provider implements ParticleProvider<AuraDebrisParticleOptions> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(AuraDebrisParticleOptions tipo, ClientLevel nivel,
                double x, double y, double z, double dx, double dy, double dz) {
            if (ultimoNivel != nivel) {
                ATIVOS_POR_DONO.clear();
                ultimoNivel = nivel;
            }
            Chave chave = new Chave(nivel, tipo.ownerId());
            // O TETO E CONFERIDO AQUI, e nao so em quem emite. Duas guardas
            // parecem redundancia ate alguem acrescentar um segundo ponto de
            // emissao -- e ai a unica que sobrevive e a que fica no caminho por
            // onde TUDO passa.
            if (ATIVOS_POR_DONO.getOrDefault(chave, 0) >= tipo.maxAtivos()) {
                return null;
            }
            return new AuraDebrisParticle(nivel, x, y, z, dx, dy, dz, tipo, this.sprites);
        }
    }

    private record Chave(ClientLevel nivel, int ownerId) {
    }
}
