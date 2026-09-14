package com.darkcontinent.nenfoundation.command;

import com.darkcontinent.nenfoundation.config.NenConfig;
import com.darkcontinent.nenfoundation.enemy.api.HxHEnemy;
import com.darkcontinent.nenfoundation.enemy.debug.EnemyDebugArena;
import com.darkcontinent.nenfoundation.enemy.debug.EnemyDebugController;
import com.darkcontinent.nenfoundation.enemy.debug.EnemyDebugState;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;

/** Comandos transitórios do EN1; sempre sob operador e modo dev. */
final class EnemyDebugCommands {
    private static final SimpleCommandExceptionType NOT_HXH = new SimpleCommandExceptionType(
            Component.literal("A entidade selecionada nao e um inimigo HxH."));

    private EnemyDebugCommands() { }

    static void anexar(LiteralArgumentBuilder<CommandSourceStack> root) {
        root.then(Commands.literal("enemy")
                .requires(source -> NenConfig.devModeAtivo())
                .then(Commands.literal("spawn")
                        .then(Commands.argument("id", ResourceLocationArgument.id())
                                .executes(EnemyDebugCommands::spawn)))
                .then(Commands.literal("state")
                        .then(Commands.argument("inimigo", EntityArgument.entity())
                                .executes(EnemyDebugCommands::state)))
                .then(Commands.literal("ai")
                        .then(Commands.literal("freeze")
                                .then(Commands.argument("inimigo", EntityArgument.entity())
                                        .executes(EnemyDebugCommands::freeze))))
                .then(Commands.literal("hitboxes")
                        .then(Commands.argument("inimigo", EntityArgument.entity())
                                .executes(EnemyDebugCommands::hitboxes)))
                .then(Commands.literal("weakpoints")
                        .then(Commands.argument("inimigo", EntityArgument.entity())
                                .executes(EnemyDebugCommands::weakpoints)))
                .then(Commands.literal("animation")
                        .then(Commands.argument("inimigo", EntityArgument.entity())
                                .then(Commands.argument("nome", StringArgumentType.word())
                                        .executes(EnemyDebugCommands::animation))))
                .then(Commands.literal("target")
                        .then(Commands.argument("inimigo", EntityArgument.entity())
                                .then(Commands.argument("alvo", EntityArgument.entity())
                                        .executes(EnemyDebugCommands::target))))
                .then(Commands.literal("arena").executes(EnemyDebugCommands::arena)));
    }

    private static int spawn(CommandContext<CommandSourceStack> context)
            throws CommandSyntaxException {
        Player player = context.getSource().getPlayerOrException();
        ResourceLocation id = ResourceLocationArgument.getId(context, "id");
        var type = BuiltInRegistries.ENTITY_TYPE.get(id);
        Entity entity = type == null ? null : type.create(player.level());
        if (!(entity instanceof Mob mob) || !(entity instanceof HxHEnemy)) {
            context.getSource().sendFailure(Component.literal("Nao e um inimigo HxH registrado: " + id));
            return 0;
        }
        mob.moveTo(player.getX() + player.getLookAngle().x * 2.0D, player.getY(),
                player.getZ() + player.getLookAngle().z * 2.0D, player.getYRot(), 0.0F);
        player.level().addFreshEntity(mob);
        context.getSource().sendSuccess(() -> Component.literal("Inimigo criado: " + id), true);
        return 1;
    }

    private static int state(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Mob mob = enemy(context, "inimigo");
        EnemyDebugState debug = EnemyDebugController.state(mob.getUUID());
        HxHEnemy enemy = (HxHEnemy) mob;
        context.getSource().sendSuccess(() -> Component.literal("state=" + enemy.combatState()
                + " awareness=" + enemy.awarenessState() + " frozen=" + debug.aiFrozen()
                + " hitboxes=" + debug.showHitboxes() + " weakpoints=" + debug.showWeakPoints()
                + " animation=" + debug.animationOverride()), false);
        return 1;
    }

    private static int freeze(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Mob mob = enemy(context, "inimigo");
        boolean frozen = !EnemyDebugController.state(mob.getUUID()).aiFrozen();
        EnemyDebugState debug = EnemyDebugController.freeze(mob, frozen);
        context.getSource().sendSuccess(() -> Component.literal("IA congelada=" + debug.aiFrozen()), true);
        return 1;
    }

    private static int hitboxes(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        EnemyDebugState debug = EnemyDebugController.toggleHitboxes(enemy(context, "inimigo"));
        context.getSource().sendSuccess(() -> Component.literal("hitboxes=" + debug.showHitboxes()), true);
        return 1;
    }

    private static int weakpoints(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        EnemyDebugState debug = EnemyDebugController.toggleWeakPoints(enemy(context, "inimigo"));
        context.getSource().sendSuccess(() -> Component.literal("weakpoints=" + debug.showWeakPoints()), true);
        return 1;
    }

    private static int animation(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Mob mob = enemy(context, "inimigo");
        EnemyDebugState debug = EnemyDebugController.animation(mob, StringArgumentType.getString(context, "nome"));
        context.getSource().sendSuccess(() -> Component.literal("animation=" + debug.animationOverride()), true);
        return 1;
    }

    private static int target(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Mob mob = enemy(context, "inimigo");
        Entity target = EntityArgument.getEntity(context, "alvo");
        if (!(target instanceof LivingEntity living) || target.level() != mob.level()
                || mob.distanceToSqr(target) > 256.0D) {
            context.getSource().sendFailure(Component.literal(
                    "Alvo invalido: exige LivingEntity na mesma dimensao e ate 16 blocos."));
            return 0;
        }
        mob.setTarget(living);
        context.getSource().sendSuccess(() -> Component.literal(
                "Alvo server-side definido: " + target.getUUID()), true);
        return 1;
    }

    private static int arena(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Player player = context.getSource().getPlayerOrException();
        if (!(player.level() instanceof ServerLevel level)) return 0;
        int count = EnemyDebugArena.spawn(level, player.position());
        context.getSource().sendSuccess(() -> Component.literal(
                "Arena criada com " + count + " inimigos."), true);
        return count == 0 ? 0 : 1;
    }

    private static Mob enemy(CommandContext<CommandSourceStack> context, String name)
            throws CommandSyntaxException {
        Entity entity = EntityArgument.getEntity(context, name);
        if (!(entity instanceof Mob mob) || !(entity instanceof HxHEnemy)) throw NOT_HXH.create();
        return mob;
    }
}
