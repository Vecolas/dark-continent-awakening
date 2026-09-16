package com.darkcontinent.nenfoundation.gametest;

import com.darkcontinent.nenfoundation.NenFoundation;
import com.darkcontinent.nenfoundation.enemy.chimera.ChimeraColony;
import com.darkcontinent.nenfoundation.enemy.chimera.ChimeraColonyMaterializer;
import com.darkcontinent.nenfoundation.enemy.chimera.ChimeraColonySavedData;
import com.darkcontinent.nenfoundation.enemy.base.BaseChimeraAnt;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Prova do caminho real de nascimento autorizado da colônia (EN9/EN16). */
@GameTestHolder(NenFoundation.MOD_ID)
public final class ChimeraColonyGameTest {

    private static final String ARENA = "arena";

    private ChimeraColonyGameTest() { }

    @GameTest(template = ARENA, timeoutTicks = 100)
    @PrefixGameTestTemplate(false)
    public static void nascimentoAutorizadoEntraNoMundo(GameTestHelper helper) {
        ServerLevel nivel = helper.getLevel();
        UUID id = UUID.randomUUID();
        ChimeraColonySavedData dados = ChimeraColonySavedData.de(nivel.getServer());
        ChimeraColony colonia = new ChimeraColony(id, helper.absolutePos(new BlockPos(5, 2, 5)));
        colonia.autorizarNascimentos(1);
        dados.registrar(colonia);

        int criados = ChimeraColonyMaterializer.materializar(nivel, dados);
        helper.assertTrue(criados == 1,
                "um nascimento autorizado nao entrou no mundo; criados=" + criados);
        helper.assertTrue(colonia.nascimentosPendentes() == 0,
                "o nascimento foi materializado mas continuou pendente: isso duplicaria"
                        + " a formiga na proxima passagem do scheduler");

        var formigas = nivel.getEntitiesOfClass(BaseChimeraAnt.class,
                new AABB(colonia.ninho()).inflate(8.0D), BaseChimeraAnt::isAlive);
        helper.assertTrue(formigas.size() == 1,
                "a materializacao criou " + formigas.size() + " formigas perto do ninho");
        helper.assertTrue(formigas.get(0).colonia().orElseThrow().equals(id),
                "a formiga nasceu sem a filiacao persistente da colonia");

        formigas.get(0).discard();
        dados.remover(id);
        helper.succeed();
    }
}
