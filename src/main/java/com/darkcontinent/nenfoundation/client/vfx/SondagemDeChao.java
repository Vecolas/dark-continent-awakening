package com.darkcontinent.nenfoundation.client.vfx;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Onde esta o chao sob uma entidade, e de que cor ele e -- com cache.
 *
 * <p><b>UMA FONTE, E NAO DUAS.</b> O anel de pressao precisa da ALTURA e os
 * detritos precisam da altura E da COR. Escritos separados, seriam duas
 * sondagens do mesmo lugar com dois ritmos de cache -- e o sintoma da divergencia
 * seria um anel pousado num degrau e detritos nascendo noutro, sem erro nenhum.
 * O raio do anel tambem limita de onde os detritos nascem, entao os dois ja
 * compartilham um numero; compartilhar tambem a sondagem e a consequencia.
 *
 * <p><b>CACHE OCASIONAL, NUNCA VARREDURA POR QUADRO.</b> Um {@code clip} por
 * jogador por quadro e uma travessia de voxel sessenta vezes por segundo para um
 * efeito cosmetico. O sintoma de ter feito isso nao e erro: e TPS e FPS caindo
 * devagar ao longo de uma sessao -- o mesmo sintoma do projetil orfao da lista
 * de erros previsiveis do {@code CLAUDE.md}.
 *
 * <p>A amostra e refeita quando o cache expira OU quando a entidade andou meio
 * bloco na horizontal, o que vier primeiro. Sem a segunda condicao, correr em
 * Ren deixaria o anel na altura de onde o jogador ESTAVA -- flutuando ao subir
 * uma escada, enterrado ao descer.
 *
 * <p>NADA AQUI ALTERA O MUNDO. Le {@code BlockState} para tirar uma cor, e so.
 */
public final class SondagemDeChao {

    /** De quantos em quantos ticks a sondagem e refeita. */
    public static final int TICKS_ENTRE_AMOSTRAS = 8;

    /** O quanto a entidade precisa andar para a amostra ser refeita antes da hora. */
    public static final double DISTANCIA_QUE_INVALIDA = 0.5D;

    /** Ate onde a sondagem procura chao, em blocos. Alem disso, a entidade esta no ar. */
    private static final double ALCANCE = 6.0D;

    /** Acima de quantas entidades o cache e podado. */
    private static final int LIMITE_DO_CACHE = 64;

    /**
     * Uma sondagem.
     *
     * @param y       a altura da superficie, em coordenada de mundo
     * @param cor     RGB do bloco sondado; branco quando nao ha bloco
     * @param achou   se ha chao dentro do alcance
     */
    public record Amostra(double y, int cor, double x, double z, long tick, boolean achou) { }

    private static final Amostra SEM_CHAO =
            new Amostra(0.0D, 0xFFFFFF, 0.0D, 0.0D, Long.MIN_VALUE, false);

    private final Int2ObjectOpenHashMap<Amostra> amostras = new Int2ObjectOpenHashMap<>();

    /**
     * A sondagem sob uma entidade, do cache ou refeita.
     *
     * @param parcial o tick parcial; zero serve para quem chama no tick
     */
    public Amostra sob(Level nivel, Entity entidade, float parcial) {
        if (nivel == null || entidade == null) {
            return SEM_CHAO;
        }
        long tick = nivel.getGameTime();
        double x = Mth.lerp(parcial, entidade.xo, entidade.getX());
        double y = Mth.lerp(parcial, entidade.yo, entidade.getY());
        double z = Mth.lerp(parcial, entidade.zo, entidade.getZ());

        Amostra guardada = this.amostras.get(entidade.getId());
        if (guardada != null && tick - guardada.tick() < TICKS_ENTRE_AMOSTRAS
                && Math.abs(guardada.x() - x) < DISTANCIA_QUE_INVALIDA
                && Math.abs(guardada.z() - z) < DISTANCIA_QUE_INVALIDA) {
            return guardada;
        }

        Amostra nova = sondar(nivel, entidade, x, y, z, tick);
        this.amostras.put(entidade.getId(), nova);
        // O CACHE NAO CRESCE SOZINHO. Sem esta poda, um servidor movimentado
        // deixaria uma entrada por jogador que passou por perto, para sempre --
        // e memoria subindo devagar e o sintoma de tudo o que se esquece de
        // limpar.
        if (this.amostras.size() > LIMITE_DO_CACHE) {
            this.amostras.values().removeIf(a -> tick - a.tick() > TICKS_ENTRE_AMOSTRAS * 8L);
        }
        return nova;
    }

    /** Quem liga, desliga: o cache do mundo anterior nao sobrevive ao logout. */
    public void limpar() {
        this.amostras.clear();
    }

    /** Quantas entidades o cache guarda. Regua do overlay de dev. */
    public int tamanho() {
        return this.amostras.size();
    }

    private static Amostra sondar(Level nivel, Entity entidade, double x, double y, double z,
            long tick) {
        double superficie;
        if (entidade.onGround()) {
            // NO CHAO, A ALTURA E DE GRACA. E o caso comum -- alguem em Ren
            // parado ou correndo --, e evitar a travessia de voxel nele e o que
            // torna o custo do cache quase irrelevante na pratica.
            superficie = y;
        } else {
            Vec3 de = new Vec3(x, y + 0.1D, z);
            Vec3 ate = new Vec3(x, y - ALCANCE, z);
            BlockHitResult acerto = nivel.clip(new ClipContext(de, ate,
                    ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, entidade));
            if (acerto.getType() == HitResult.Type.MISS) {
                return new Amostra(y, 0xFFFFFF, x, z, tick, false);
            }
            superficie = acerto.getLocation().y;
        }
        return new Amostra(superficie, corDoBlocoEm(nivel, x, superficie, z), x, z, tick, true);
    }

    /**
     * A cor representativa do bloco sob os pes.
     *
     * <p>{@code MapColor} E DE PROPOSITO, e nao uma aproximacao preguicosa. A
     * alternativa -- amostrar o atlas de textura -- exigiria o sprite montado, o
     * tint do bioma e a face certa, e tudo isso por detrito. O que o fragmento
     * precisa dizer e "esta materia veio DAQUI", e para isso a cor de mapa
     * acerta o suficiente: terra e marrom, pedra e cinza, areia e clara, grama e
     * verde.
     *
     * <p>MEIO BLOCO ABAIXO DA SUPERFICIE: exatamente na superficie, a posicao
     * cai no bloco de CIMA -- o ar -- e todo detrito sairia branco.
     */
    private static int corDoBlocoEm(Level nivel, double x, double superficie, double z) {
        BlockPos pos = BlockPos.containing(x, superficie - 0.5D, z);
        BlockState estado = nivel.getBlockState(pos);
        MapColor cor = estado.getMapColor(nivel, pos);
        if (cor == MapColor.NONE) {
            return 0xFFFFFF;
        }
        return cor.calculateRGBColor(MapColor.Brightness.NORMAL) & 0xFFFFFF;
    }
}
