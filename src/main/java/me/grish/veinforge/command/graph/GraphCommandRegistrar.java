package me.grish.veinforge.command.graph;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import me.grish.veinforge.handler.GraphHandler;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;

import java.util.concurrent.CompletableFuture;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal;

public class GraphCommandRegistrar {

    private final GraphCommand graphCommand;

    public GraphCommandRegistrar(GraphCommand graphCommand) {
        this.graphCommand = graphCommand;
    }

    public void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        dispatcher.register(
                literal("graph")
                        .executes(context -> {
                            graphCommand.main();
                            return 1;
                        })
                        .then(
                                literal("list").executes(context -> {
                                    graphCommand.list();
                                    return 1;
                                })
                        )
                        .then(
                                literal("new")
                                        .then(
                                                argument(
                                                        "name",
                                                        StringArgumentType.greedyString()
                                                ).executes(context -> {
                                                    graphCommand.create(
                                                            StringArgumentType.getString(
                                                                    context,
                                                                    "name"
                                                            )
                                                    );
                                                    return 1;
                                                })
                                        )
                        )
                        .then(
                                literal("edit")
                                        .executes(context -> {
                                            graphCommand.edit();
                                            return 1;
                                        })
                                        .then(
                                                argument(
                                                        "name",
                                                        StringArgumentType.greedyString()
                                                ).suggests((context, builder) -> suggestGraphNames(builder))
                                                        .executes(context -> {
                                                            graphCommand.edit(
                                                                    StringArgumentType.getString(
                                                                            context,
                                                                            "name"
                                                                    )
                                                            );
                                                            return 1;
                                                        })
                                        )
                        )
                        .then(
                                literal("debug")
                                        .executes(context -> {
                                            graphCommand.debugMain();
                                            return 1;
                                        })
                                        .then(
                                                literal("list").executes(context -> {
                                                    graphCommand.debugList();
                                                    return 1;
                                                })
                                        )
                                        .then(
                                                literal("off").executes(context -> {
                                                    graphCommand.debugOff();
                                                    return 1;
                                                })
                                        )
                                        .then(
                                                literal("show")
                                                        .then(
                                                                argument(
                                                                        "name",
                                                                        StringArgumentType.greedyString()
                                                                ).suggests((context, builder) -> suggestGraphNames(builder))
                                                                        .executes(context -> {
                                                                            graphCommand.debug(
                                                                                    StringArgumentType.getString(
                                                                                            context,
                                                                                            "name"
                                                                                    )
                                                                            );
                                                                            return 1;
                                                                        })
                                                        )
                                        )
                        )
                        .then(
                                literal("save").executes(context -> {
                                    graphCommand.save();
                                    return 1;
                                })
                        )
                        .then(
                                literal("keys").executes(context -> {
                                    graphCommand.keys();
                                    return 1;
                                })
                        )
                        .then(
                                literal("tutorial").executes(context -> {
                                    graphCommand.tutorial();
                                    return 1;
                                })
                        )
        );
    }

    private CompletableFuture<Suggestions> suggestGraphNames(SuggestionsBuilder builder) {
        String remaining = builder.getRemainingLowerCase();
        for (String name : GraphHandler.instance.getKnownGraphNames()) {
            if (name.toLowerCase().startsWith(remaining)) {
                builder.suggest(name);
            }
        }
        return builder.buildFuture();
    }
}
