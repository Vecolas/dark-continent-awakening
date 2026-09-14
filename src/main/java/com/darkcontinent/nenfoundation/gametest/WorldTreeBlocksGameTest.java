package com.darkcontinent.nenfoundation.gametest;

import com.darkcontinent.nenfoundation.NenFoundation;
import com.darkcontinent.nenfoundation.worldtree.WorldTreeBlocks;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/** Confirma que a fase de registros deixou os blocos e tags disponíveis no servidor. */
@GameTestHolder(NenFoundation.MOD_ID)
public final class WorldTreeBlocksGameTest {
    private static final TagKey<Block> STRUCTURAL = TagKey.create(
            Registries.BLOCK, ResourceLocation.fromNamespaceAndPath(NenFoundation.MOD_ID, "world_tree_structural"));

    private WorldTreeBlocksGameTest() {
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    @PrefixGameTestTemplate(false)
    public static void blocosWorldTreeEStagDisponiveis(GameTestHelper helper) {
        helper.assertTrue(WorldTreeBlocks.WORLD_TREE_BARK.get() != null,
                "world_tree_bark nao foi registrado");
        helper.assertTrue(WorldTreeBlocks.WORLD_TREE_CORE.get() != null,
                "world_tree_core nao foi registrado");
        helper.assertTrue(WorldTreeBlocks.WORLD_TREE_LEAVES.get() != null,
                "world_tree_leaves nao foi registrado");

        BlockPos local = new BlockPos(0, 0, 0);
        helper.getLevel().setBlock(helper.absolutePos(local),
                WorldTreeBlocks.WORLD_TREE_BARK.get().defaultBlockState(), 3);
        helper.assertTrue(helper.getBlockState(local).is(STRUCTURAL),
                "world_tree_bark nao entrou na tag estrutural");
        helper.succeed();
    }
}
