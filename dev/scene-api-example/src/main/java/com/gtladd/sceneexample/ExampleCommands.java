package com.gtladd.sceneexample;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;

import com.gtladd.sceneexample.Placements.Placement;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;

import java.util.List;

/**
 * {@code /scene_example add orb|beacon <x> <y> <z>}, {@code clear}, {@code mode scene|direct} for the orbs,
 * {@code status}.
 */
final class ExampleCommands {

    private static final double ORB_RADIUS = 2.5;
    private static final double BEACON_RADIUS = 1.6;
    private static final String NEEDS_SCENE = "This needs GTL Additions";

    static void register(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("scene_example")
                .then(Commands.literal("add")
                        .then(Commands.literal("orb").then(position(context -> add(context, Placements.ORBS, ORB_RADIUS))))
                        .then(Commands.literal("beacon").then(position(context -> ExampleClient.SCENE ?
                                add(context, Placements.BEACONS, BEACON_RADIUS) : fail(context, NEEDS_SCENE)))))
                .then(Commands.literal("clear").executes(context -> {
                    Placements.clear();
                    return say(context, "Cleared");
                }))
                .then(Commands.literal("mode")
                        .then(Commands.literal("scene").executes(context -> {
                            if (!ExampleClient.SCENE) return fail(context, NEEDS_SCENE);
                            ExampleClient.sceneOrbs = true;
                            return say(context, "Orbs go through the scene scheduler");
                        }))
                        .then(Commands.literal("direct").executes(context -> {
                            ExampleClient.sceneOrbs = false;
                            return say(context, "Orbs are drawn directly");
                        })))
                .then(Commands.literal("status").executes(context -> say(context, status()))));
    }

    private static ArgumentBuilder<CommandSourceStack, ?> position(Command<CommandSourceStack> command) {
        return Commands.argument("x", DoubleArgumentType.doubleArg())
                .then(Commands.argument("y", DoubleArgumentType.doubleArg())
                        .then(Commands.argument("z", DoubleArgumentType.doubleArg()).executes(command)));
    }

    private static int add(CommandContext<CommandSourceStack> context, List<Placement> list, double radius) {
        Placements.add(list, DoubleArgumentType.getDouble(context, "x"), DoubleArgumentType.getDouble(context, "y"),
                DoubleArgumentType.getDouble(context, "z"), radius);
        return say(context, "Added; " + Placements.ORBS.size() + " orbs, " + Placements.BEACONS.size() + " beacons");
    }

    private static String status() {
        String orbs = Placements.ORBS.size() + " orbs (" + (ExampleClient.sceneOrbs ? "scene" : "direct") + "), " + Placements.BEACONS.size() + " beacons";
        return ExampleClient.SCENE ? orbs + "\n" + SceneBridge.status() : orbs + "; GTL Additions is not installed";
    }

    private static int say(CommandContext<CommandSourceStack> context, String text) {
        context.getSource().sendSuccess(() -> Component.literal(text), false);
        return Command.SINGLE_SUCCESS;
    }

    private static int fail(CommandContext<CommandSourceStack> context, String text) {
        context.getSource().sendFailure(Component.literal(text));
        return 0;
    }
}