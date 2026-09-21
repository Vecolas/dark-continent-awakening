package com.darkcontinent.nenfoundation.enemy.chimera;

import com.darkcontinent.nenfoundation.enemy.base.BaseChimeraAnt;
import com.darkcontinent.nenfoundation.enemy.registry.EnemyEntityTypes;
import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.levelgen.Heightmap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Materializa, no mundo, os nascimentos ja autorizados pela colonia. */
public final class ChimeraColonyMaterializer {

    private static final Logger LOG = LoggerFactory.getLogger(ChimeraColonyMaterializer.class);
    private static final int TENTATIVAS_POR_NASCIMENTO = 8;

    private ChimeraColonyMaterializer() { }

    /**
     * Tenta criar todas as entidades pendentes cujos ninhos estao carregados.
     *
     * <p>A autorizacao so e consumida depois de {@link EntityType#spawn} devolver
     * uma entidade. Falta de chunk ou de espaco deixa o nascimento persistido
     * para a proxima passagem; assim um restart nao perde crescimento e uma
     * tentativa repetida nao duplica o que ja entrou no mundo.</p>
     */
    public static int materializar(ServerLevel nivel, ChimeraColonySavedData dados) {
        Objects.requireNonNull(nivel, "nivel ausente");
        Objects.requireNonNull(dados, "dados ausentes");
        int criados = 0;
        for (ChimeraColony colonia : dados.colonias().values()) {
            int sequencia = colonia.tamanho();
            while (colonia.nascimentosPendentes() > 0) {
                Entity entidade = criarUm(nivel, colonia, sequencia);
                if (!(entidade instanceof BaseChimeraAnt formiga)) break;
                formiga.alistarEm(colonia.id());
                if (!colonia.consumirNascimento()) {
                    // Nao deveria acontecer: a entidade entrou no mundo antes
                    // da baixa. Remover evita uma formiga sem dono se o estado
                    // persistente tiver sido alterado por outro caminho.
                    entidade.discard();
                    throw new IllegalStateException("nascimento materializado sem autorizacao");
                }
                criados++;
                sequencia++;
            }
        }
        if (criados > 0) dados.sujar();
        return criados;
    }

    private static Entity criarUm(ServerLevel nivel, ChimeraColony colonia, int indice) {
        EntityType<? extends BaseChimeraAnt> tipo = tipoDoIndice(indice);
        BlockPos ancora = colonia.ninho();
        for (int tentativa = 0; tentativa < TENTATIVAS_POR_NASCIMENTO; tentativa++) {
            BlockPos posicao = posicaoAoRedor(nivel, ancora, indice, tentativa);
            if (!nivel.isLoaded(posicao)) continue;
            Entity entidade = tipo.spawn(nivel, posicao, MobSpawnType.EVENT);
            if (entidade != null) return entidade;
        }
        LOG.debug("Ninho {} ainda nao encontrou espaco carregado para nascimento {}.",
                colonia.id(), indice);
        return null;
    }

    private static EntityType<? extends BaseChimeraAnt> tipoDoIndice(int indice) {
        return switch (Math.floorMod(indice, 3)) {
            case 0 -> EnemyEntityTypes.CRAB_HEAVY.get();
            case 1 -> EnemyEntityTypes.BAT_SCOUT.get();
            default -> EnemyEntityTypes.WOLF_RUNNER.get();
        };
    }

    private static BlockPos posicaoAoRedor(ServerLevel nivel, BlockPos ancora,
            int indice, int tentativa) {
        double angulo = indice * 2.399963D + tentativa * 0.7D;
        double distancia = 1.5D + tentativa * 0.75D;
        int x = ancora.getX() + (int) Math.round(Math.cos(angulo) * distancia);
        int z = ancora.getZ() + (int) Math.round(Math.sin(angulo) * distancia);
        int y = nivel.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        return new BlockPos(x, y, z);
    }
}
