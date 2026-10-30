package invmod.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import invmod.block.NexusBlockEntity;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

/**
 * /invasion begin [wave] | end | range [radius] | status | continuous, acting on the nearest nexus within
 * 16 blocks. Operators only.
 */
public final class InvasionCommand {
    private static final int SEARCH_RANGE = 16;

    private InvasionCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("invasion")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("begin")
                        .executes(ctx -> begin(ctx, 1))
                        .then(Commands.argument("wave", IntegerArgumentType.integer(1, 1000))
                                .executes(ctx -> begin(ctx, IntegerArgumentType.getInteger(ctx, "wave")))))
                .then(Commands.literal("continuous").executes(InvasionCommand::continuous))
                .then(Commands.literal("end").executes(InvasionCommand::end))
                .then(Commands.literal("status").executes(InvasionCommand::status))
                .then(Commands.literal("range")
                        .executes(ctx -> reply(ctx, nexus -> "Nexus range: " + nexus.getSpawnRadius()))
                        .then(Commands.argument("radius", IntegerArgumentType.integer(16, 128))
                                .executes(ctx -> {
                                    int radius = IntegerArgumentType.getInteger(ctx, "radius");
                                    return reply(ctx, nexus -> nexus.setSpawnRadius(radius)
                                            ? "Nexus range set to " + radius
                                            : "Can't change the range while the nexus is active");
                                }))));
    }

    private static @Nullable NexusBlockEntity nearest(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        NexusBlockEntity nexus = NexusBlockEntity.findNearest(source.getLevel(), BlockPos.containing(source.getPosition()), SEARCH_RANGE);
        if (nexus == null) {
            source.sendFailure(Component.literal("No nexus within " + SEARCH_RANGE + " blocks"));
        }
        return nexus;
    }

    private interface NexusAction {
        String run(NexusBlockEntity nexus);
    }

    private static int reply(CommandContext<CommandSourceStack> ctx, NexusAction action) {
        NexusBlockEntity nexus = nearest(ctx);
        if (nexus == null) {
            return 0;
        }
        String message = action.run(nexus);
        ctx.getSource().sendSuccess(() -> Component.literal(message), true);
        return 1;
    }

    private static int begin(CommandContext<CommandSourceStack> ctx, int wave) {
        return reply(ctx, nexus -> {
            nexus.debugStartInvasion(wave);
            return "Invasion started at wave " + wave;
        });
    }

    private static int continuous(CommandContext<CommandSourceStack> ctx) {
        return reply(ctx, nexus -> {
            nexus.debugStartContinuous();
            return "Continuous mode started";
        });
    }

    private static int end(CommandContext<CommandSourceStack> ctx) {
        return reply(ctx, nexus -> {
            nexus.emergencyStop();
            return "Invasion stopped";
        });
    }

    private static int status(CommandContext<CommandSourceStack> ctx) {
        return reply(ctx, NexusBlockEntity::debugStatus);
    }
}
