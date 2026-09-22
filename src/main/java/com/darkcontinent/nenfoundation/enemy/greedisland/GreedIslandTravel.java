package com.darkcontinent.nenfoundation.enemy.greedisland;

import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * A UNICA porta de entrada e de volta da ilha.
 *
 * <p><b>Por que um servico e nao um teleporte em cada chamador.</b> Hoje sao
 * dois -- o anel e o renascimento -- e amanha serao o card de saida e o
 * comando de debug. Quatro teleportes iguais divergem no dia em que a ilha
 * ganhar uma coordenada de chegada diferente de zero, e o que diverge nao
 * reclama: um dos caminhos simplesmente larga o jogador noutro lugar.</p>
 *
 * <p><b>A ALTURA E MEDIDA, e nunca constante.</b> Escrever um {@code y} fixo
 * aqui seria o mesmo defeito que este servico nasceu para consertar: a ilha
 * gerava a grama em {@code y=-60} porque as camadas do gerador plano empilham
 * a partir do {@code min_y} da dimensao, e quem chegava caia em {@code y=-59},
 * no fundo do mundo, com a nevoa de void do Overworld na tela. O terreno subiu,
 * e um numero cravado aqui teria de ser lembrado junto -- por isso ele e lido
 * do {@link Heightmap} a cada viagem.</p>
 */
public final class GreedIslandTravel {

    /**
     * Folga acima do chao medido.
     *
     * <p>Um bloco. O suficiente para nao materializar o jogador DENTRO da grama
     * -- o que o empurraria para o lado sem erro nenhum -- e pouco o bastante
     * para nao virar uma queda a cada chegada.</p>
     */
    private static final double FOLGA = 1.0D;

    private GreedIslandTravel() { }

    /**
     * Leva o jogador para a ilha, no ponto de chegada dela.
     *
     * @return {@code false} quando a dimensao nao existe neste servidor -- o
     *         datapack pode estar desligado, e isso nao pode derrubar quem usou
     *         o anel. O chamador avisa o jogador; recusa sem motivo produz o
     *         pior relato de bug que existe.
     */
    public static boolean enviar(ServerPlayer jogador) {
        Objects.requireNonNull(jogador, "jogador ausente");
        MinecraftServer servidor = jogador.getServer();
        if (servidor == null) {
            return false;
        }
        ServerLevel ilha = servidor.getLevel(GreedIslandRegion.DIMENSAO);
        if (ilha == null) {
            return false;
        }
        BlockPos chegada = chegadaEm(ilha);
        jogador.teleportTo(ilha, chegada.getX() + 0.5D, chegada.getY() + FOLGA,
                chegada.getZ() + 0.5D, jogador.getYRot(), jogador.getXRot());
        return true;
    }

    /**
     * O ponto de chegada, com o chao medido no lugar de adivinhado.
     *
     * <p>O X e o Z vem do spawn compartilhado da dimensao, e nao de zero fixo:
     * mover o spawn da ilha no datapack passa a mover a chegada junto, em vez
     * de deixar as duas discordando em silencio.</p>
     */
    public static BlockPos chegadaEm(ServerLevel ilha) {
        Objects.requireNonNull(ilha, "nivel ausente");
        BlockPos spawn = ilha.getSharedSpawnPos();
        int y = ilha.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                spawn.getX(), spawn.getZ());
        return new BlockPos(spawn.getX(), y, spawn.getZ());
    }

    /** O jogador esta na ilha AGORA? */
    public static boolean naIlha(ServerPlayer jogador) {
        Objects.requireNonNull(jogador, "jogador ausente");
        Level nivel = jogador.level();
        return GreedIslandRegion.dentro(nivel.dimension());
    }
}
