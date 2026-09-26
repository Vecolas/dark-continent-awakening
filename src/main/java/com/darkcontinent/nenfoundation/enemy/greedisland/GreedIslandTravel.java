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
        // A ORIGEM E GRAVADA ANTES DO TELEPORTE. Depois dele o jogador ja esta
        // na ilha, e a dimensao de onde ele veio se perdeu -- e o sintoma seria
        // o anel devolvendo todo mundo ao spawn do mundo, inclusive quem entrou
        // de uma base a dez mil blocos de distancia.
        gravarOrigem(jogador);
        BlockPos chegada = chegadaEm(ilha);
        jogador.teleportTo(ilha, chegada.getX() + 0.5D, chegada.getY() + FOLGA,
                chegada.getZ() + 0.5D, jogador.getYRot(), jogador.getXRot());
        return true;
    }

    /**
     * Devolve o jogador de onde ele veio.
     *
     * <p><b>O ANEL E A PORTA NOS DOIS SENTIDOS</b>, e ate 2026-09-26 ele so
     * abria para dentro: {@code enviar} existia e {@code retornar} nao. Quem
     * entrava na ilha so saia MORRENDO -- e nem isso, porque
     * {@code GreedIslandRespawnHooks} devolve o morto a propria ilha. A unica
     * saida era um comando de admin.
     *
     * <p>DIVERGE DO CANONE, e isso ja estava decidido: no material, sair de
     * Greed Island e um evento pesado. Aqui o anel e reutilizavel, pela mesma
     * razao que o jogador renasce na ilha em vez de perder o corpo -- decisao
     * do dono do projeto, registrada para nao ser lida como descuido.
     *
     * @return {@code false} quando nao ha para onde voltar -- o chamador avisa
     */
    public static boolean retornar(ServerPlayer jogador) {
        Objects.requireNonNull(jogador, "jogador ausente");
        MinecraftServer servidor = jogador.getServer();
        if (servidor == null) {
            return false;
        }
        Origem origem = lerOrigem(jogador).orElse(null);
        ServerLevel destino = origem == null ? null : servidor.getLevel(origem.dimensao());
        if (destino == null) {
            // SEM ORIGEM GRAVADA, O OVERWORLD. Acontece com quem entrou por
            // comando, com save anterior a esta versao, ou se a dimensao de
            // origem sumiu do servidor. Voltar ao spawn e pior que voltar para
            // casa, e infinitamente melhor que ficar preso.
            destino = servidor.overworld();
            BlockPos spawn = destino.getSharedSpawnPos();
            jogador.teleportTo(destino, spawn.getX() + 0.5D, spawn.getY() + FOLGA,
                    spawn.getZ() + 0.5D, jogador.getYRot(), jogador.getXRot());
            esquecerOrigem(jogador);
            return true;
        }
        jogador.teleportTo(destino, origem.x(), origem.y(), origem.z(),
                jogador.getYRot(), jogador.getXRot());
        // A MARCA SAI AO USAR. Deixada para tras, a proxima volta mandaria o
        // jogador para o lugar de onde ele entrou HA DUAS SESSOES.
        esquecerOrigem(jogador);
        return true;
    }

    /** De onde o jogador entrou na ilha. */
    public record Origem(net.minecraft.resources.ResourceKey<Level> dimensao,
            double x, double y, double z) {
    }

    /** Chave da marca. Namespaced: {@code getPersistentData()} e de todo mod. */
    static final String TAG_ORIGEM = com.darkcontinent.nenfoundation.NenFoundation.MOD_ID
            + ":origem_greed_island";

    /**
     * Grava a origem no jogador, e nao num mapa estatico.
     *
     * <p>Mesma razao de {@code GreedIslandRespawnHooks}: um mapa em memoria
     * perde o caso que mais acontece em servidor de verdade -- entrar na ilha,
     * fechar o jogo, e voltar dias depois querendo sair.
     */
    static void gravarOrigem(ServerPlayer jogador) {
        net.minecraft.nbt.CompoundTag marca = new net.minecraft.nbt.CompoundTag();
        marca.putString("dim", jogador.level().dimension().location().toString());
        marca.putDouble("x", jogador.getX());
        marca.putDouble("y", jogador.getY());
        marca.putDouble("z", jogador.getZ());
        jogador.getPersistentData().put(TAG_ORIGEM, marca);
    }

    /** A origem gravada, se houver e se ainda fizer sentido. */
    static java.util.Optional<Origem> lerOrigem(ServerPlayer jogador) {
        net.minecraft.nbt.CompoundTag dados = jogador.getPersistentData();
        if (!dados.contains(TAG_ORIGEM)) {
            return java.util.Optional.empty();
        }
        net.minecraft.nbt.CompoundTag marca = dados.getCompound(TAG_ORIGEM);
        net.minecraft.resources.ResourceLocation id =
                net.minecraft.resources.ResourceLocation.tryParse(marca.getString("dim"));
        if (id == null) {
            return java.util.Optional.empty();
        }
        var chave = net.minecraft.resources.ResourceKey.create(
                net.minecraft.core.registries.Registries.DIMENSION, id);
        // A ORIGEM NUNCA PODE SER A PROPRIA ILHA. Sem esta guarda, entrar duas
        // vezes seguidas -- por comando, ou por um caminho futuro -- gravaria a
        // ilha como origem e o anel devolveria o jogador para onde ele ja esta.
        if (GreedIslandRegion.dentro(chave)) {
            return java.util.Optional.empty();
        }
        return java.util.Optional.of(new Origem(chave,
                marca.getDouble("x"), marca.getDouble("y"), marca.getDouble("z")));
    }

    static void esquecerOrigem(ServerPlayer jogador) {
        jogador.getPersistentData().remove(TAG_ORIGEM);
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
        return chegadaEm(ilha, spawn.getX(), spawn.getZ());
    }

    /**
     * O ponto de pouso numa coluna, com o chao MEDIDO e conferido.
     *
     * <p><b>O DEFEITO QUE ISTO CONSERTA foi visto em jogo:</b> morrer na ilha
     * as vezes devolvia o jogador ENTERRADO, muitos blocos abaixo da
     * superficie. A versao anterior perguntava a altura e confiava na resposta.
     *
     * <p>SAO DUAS CAUSAS, e as duas sao silenciosas:
     *
     * <ol>
     *   <li><b>O CHUNK PODIA NAO ESTAR GERADO.</b> {@code getHeight} responde
     *       para uma coluna que ainda nao existe usando o que houver em
     *       memoria, e o que ha e o piso do mundo. O jogador ia para o fundo.
     *       Agora a coluna e CARREGADA antes de ser medida.
     *   <li><b>O HEIGHTMAP NAO PROMETE CEU.</b> Ele devolve o topo do que
     *       bloqueia movimento -- e num teto de caverna proximo a superficie
     *       isso e o teto, com pedra logo acima. Agora a coluna e conferida de
     *       baixo para cima ate achar dois blocos livres com chao solido.
     * </ol>
     *
     * <p>A BUSCA COMECA NO HEIGHTMAP e sobe: descer procuraria a primeira
     * caverna, que e exatamente o lugar errado.
     */
    public static BlockPos chegadaEm(ServerLevel ilha, int x, int z) {
        Objects.requireNonNull(ilha, "nivel ausente");
        // FORCA A GERACAO da coluna antes de medir. Sem isto, a medida e feita
        // sobre um chunk vazio e a resposta e o fundo do mundo.
        ilha.getChunk(net.minecraft.core.SectionPos.blockToSectionCoord(x),
                net.minecraft.core.SectionPos.blockToSectionCoord(z));

        int teto = ilha.getMaxBuildHeight() - 2;
        int inicio = Math.clamp(
                ilha.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z),
                ilha.getMinBuildHeight() + 1, teto);

        for (int y = inicio; y < teto; y++) {
            BlockPos pe = new BlockPos(x, y, z);
            if (ilha.getBlockState(pe).isAir()
                    && ilha.getBlockState(pe.above()).isAir()
                    && !ilha.getBlockState(pe.below()).isAir()) {
                return pe;
            }
        }
        // NENHUMA COLUNA LIVRE. Acontece sob agua funda e dentro de montanha.
        // O topo do mundo e melhor que o fundo: cair alguns blocos e um susto,
        // nascer dentro de pedra e sufocar sem entender por que.
        return new BlockPos(x, teto, z);
    }

    /** O jogador esta na ilha AGORA? */
    public static boolean naIlha(ServerPlayer jogador) {
        Objects.requireNonNull(jogador, "jogador ausente");
        Level nivel = jogador.level();
        return GreedIslandRegion.dentro(nivel.dimension());
    }
}
