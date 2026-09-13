package com.darkcontinent.nenfoundation.gametest;

import com.darkcontinent.nenfoundation.NenFoundation;
import com.darkcontinent.nenfoundation.structure.PostoAvancadoPlacementTransform;
import com.darkcontinent.nenfoundation.structure.PostoAvancadoWorldPlacer;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Prova no servidor que o planner termina em blocos reais, não só placements. */
@GameTestHolder(NenFoundation.MOD_ID)
public final class PostoAvancadoGameTest {
    private PostoAvancadoGameTest() { }

    @GameTest(template = "empty", timeoutTicks = 2)
    @PrefixGameTestTemplate(false)
    public static void colocaTodosOsBlocosDoPosto(GameTestHelper helper) {
        var resultado = PostoAvancadoWorldPlacer.colocar(helper.getLevel(), 0, 0,
                PostoAvancadoPlacementTransform.Rotacao.NONE);
        helper.assertFalse(resultado.rejeitado(),
                "placement rejeitado: " + resultado.motivo());
        helper.assertTrue(resultado.placements() > 500,
                "blockout colocou poucos placements: " + resultado.placements());
        helper.assertTrue(resultado.blocosUnicos() > 500,
                "blockout colocou poucos blocos unicos: " + resultado.blocosUnicos());

        BlockPos origem = resultado.origem();
        helper.assertTrue(!helper.getLevel().getBlockState(origem).isAir(),
                "a fundacao do posto nao foi escrita no nivel");
        helper.assertTrue(!helper.getLevel().getBlockState(origem.offset(0, 2, 0)).isAir(),
                "o posto nao possui bloco estrutural acima da fundacao");
        helper.succeed();
    }
}
