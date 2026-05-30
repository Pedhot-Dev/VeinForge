package me.grish.veinforge.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import me.grish.veinforge.command.graph.GraphCommandRegistrar;
import me.grish.veinforge.config.ConfigActions;
import me.grish.veinforge.config.ConfigGuiManager;
import me.grish.veinforge.command.graph.GraphCommand;
import me.grish.veinforge.handler.RouteHandler;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.Minecraft;

import java.util.concurrent.CompletableFuture;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

public class CommandManager {

   private final DebugCommand debugCommand = new DebugCommand();
   private final GraphCommand graphCommand = new GraphCommand();
   private final GraphCommandRegistrar graphCommandRegistrar = new GraphCommandRegistrar(graphCommand);
   private final RouteBuilderCommand routeBuilderCommand =
           new RouteBuilderCommand();

   public void registerAll() {
      ClientCommandRegistrationCallback.EVENT.register(
              (dispatcher, registryAccess) -> {
                 Command<FabricClientCommandSource> openConfigAction = context -> {
                    Minecraft.getInstance()
                            .execute(() -> ConfigGuiManager.openConfigGui(null));
                    return 1;
                 };
                 registerDebugCommand(dispatcher);
                 graphCommandRegistrar.register(dispatcher);
                 registerRouteBuilderCommand(dispatcher);
                 // Primary mod command: /veinforge
                 registerVeinForgeCommand(dispatcher, "veinforge", openConfigAction);
                 registerVeinForgeCommand(dispatcher, "vf", openConfigAction);
              }
      );
   }

   private void registerDebugCommand(
           CommandDispatcher<FabricClientCommandSource> dispatcher
   ) {
      dispatcher.register(
              literal("debug")
                      .executes(context -> {
                         debugCommand.main();
                         return 1;
                      })
                      .then(
                              literal("item").executes(context -> {
                                 debugCommand.item();
                                 return 1;
                              })
                      )
                      .then(
                              literal("nbt").executes(context -> {
                                 debugCommand.nbt();
                                 return 1;
                              })
                      )
                      .then(
                              literal("location").executes(context -> {
                                 debugCommand.location();
                                 return 1;
                              })
                      )
                      .then(
                              literal("sublocation").executes(context -> {
                                 debugCommand.sublocation();
                                 return 1;
                              })
                      )
                      .then(
                              literal("where").executes(context -> {
                                 debugCommand.where();
                                 return 1;
                              })
                      )
                      .then(
                              literal("scoreboard")
                                      .executes(context -> {
                                         debugCommand.scoreboard();
                                         return 1;
                                      })
                                      .then(
                                              literal("dump").executes(context -> {
                                                 debugCommand.scoreboardDump();
                                                 return 1;
                                              })
                                      )
                      )
                      .then(
                              literal("tablist").executes(context -> {
                                 debugCommand.tablist();
                                 return 1;
                              })
                      )
                      .then(
                              literal("commission").executes(context -> {
                                 debugCommand.commission();
                                 return 1;
                              })
                      )
                      .then(
                              literal("footer").executes(context -> {
                                 debugCommand.footer();
                                 return 1;
                              })
                      )
                      .then(
                              literal("state").executes(context -> {
                                 debugCommand.state();
                                 return 1;
                              })
                      )
                      .then(
                              literal("fishing-stage").executes(context -> {
                                 debugCommand.fishingStage();
                                 return 1;
                              })
                      )
                      .then(
                              literal("entities").executes(context -> {
                                 debugCommand.entities();
                                 return 1;
                              })
                      )
                      .then(
                              literal("minehere")
                                      .executes(context -> {
                                         debugCommand.mineHere(null);
                                         return 1;
                                      })
                                      .then(
                                              literal("stop").executes(context -> {
                                                 debugCommand.mineHereStop();
                                                 return 1;
                                              })
                                      )
                                      .then(
                                              argument("speed", IntegerArgumentType.integer(1)).executes(context -> {
                                                 debugCommand.mineHere(IntegerArgumentType.getInteger(context, "speed"));
                                                 return 1;
                                              })
                                      )
                      )
                      .then(
                              literal("path")
                                      .then(
                                              literal("stats").executes(context -> {
                                                 debugCommand.pathStats();
                                                 return 1;
                                              })
                                      )
                                      .then(
                                              literal("stop").executes(context -> {
                                                 debugCommand.pathStop();
                                                 return 1;
                                              })
                                      )
                                      .then(
                                              argument("x", IntegerArgumentType.integer())
                                                      .then(
                                                              argument("y", IntegerArgumentType.integer())
                                                                      .then(
                                                                              argument("z", IntegerArgumentType.integer())
                                                                                      .executes(context -> {
                                                                                         int x = IntegerArgumentType.getInteger(context, "x");
                                                                                         int y = IntegerArgumentType.getInteger(context, "y");
                                                                                         int z = IntegerArgumentType.getInteger(context, "z");
                                                                                         debugCommand.pathToBlock(x, y, z, false);
                                                                                         return 1;
                                                                                     })
                                                                                      .then(
                                                                                              argument("render_only", BoolArgumentType.bool())
                                                                                                      .executes(context -> {
                                                                                                         int x = IntegerArgumentType.getInteger(context, "x");
                                                                                                         int y = IntegerArgumentType.getInteger(context, "y");
                                                                                                         int z = IntegerArgumentType.getInteger(context, "z");
                                                                                                         boolean renderOnly = BoolArgumentType.getBool(context, "render_only");
                                                                                                         debugCommand.pathToBlock(x, y, z, renderOnly);
                                                                                                         return 1;
                                                                                                      })
                                                                                      )
                                                                      )
                                                      )
                                      )
                                      .then(
                                              literal("location")
                                                      .then(
                                                              argument("location", StringArgumentType.word())
                                                                      .suggests((context, builder) -> suggestDebugPathTargets(builder))
                                                                      .executes(context -> {
                                                                         debugCommand.pathDwarven(StringArgumentType.getString(context, "location"), false);
                                                                         return 1;
                                                                      })
                                                                      .then(
                                                                              argument("render_only", BoolArgumentType.bool())
                                                                                      .executes(context -> {
                                                                                         String location = StringArgumentType.getString(context, "location");
                                                                                         boolean renderOnly = BoolArgumentType.getBool(context, "render_only");
                                                                                         debugCommand.pathDwarven(location, renderOnly);
                                                                                         return 1;
                                                                                      })
                                                                      )
                                                      )
                                      )
                      )
                      .then(
                              literal("pathdm")
                                      .then(
                                              argument("location", StringArgumentType.word())
                                                      .suggests((context, builder) -> suggestDebugPathTargets(builder))
                                                      .executes(context -> {
                                                         debugCommand.pathDwarven(StringArgumentType.getString(context, "location"), false);
                                                         return 1;
                                                      })
                                                      .then(
                                                              argument("render_only", BoolArgumentType.bool())
                                                                      .executes(context -> {
                                                                         String location = StringArgumentType.getString(context, "location");
                                                                         boolean renderOnly = BoolArgumentType.getBool(context, "render_only");
                                                                         debugCommand.pathDwarven(location, renderOnly);
                                                         return 1;
                                                      })
                                                      )
                                      )
                      )

                       .then(
                               literal("getstats")
                                       .executes(context -> {
                                          debugCommand.getStatsStart();
                                          return 1;
                                      })
                                      .then(
                                              literal("start").executes(context -> {
                                                 debugCommand.getStatsStart();
                                                 return 1;
                                              })
                                      )
                                      .then(
                                               literal("status").executes(context -> {
                                                  debugCommand.getStatsStatus();
                                                  return 1;
                                               })
                                       )
                       )
                       .then(
                               literal("slayer")
                                       .executes(context -> {
                                          debugCommand.slayerStatus();
                                          return 1;
                                       })
                                       .then(
                                               literal("stop").executes(context -> {
                                                  debugCommand.slayerStop();
                                                  return 1;
                                               })
                                       )
                                       .then(
                                               literal("status").executes(context -> {
                                                  debugCommand.slayerStatus();
                                                  return 1;
                                               })
                                       )
                                       .then(
                                               literal("start")
                                                       .then(
                                                               argument("mob", StringArgumentType.word())
                                                                       .suggests((context, builder) -> {
                                                                          String remaining = builder.getRemainingLowerCase();
                                                                          for (String key : debugCommand.getSlayerDebugTargetKeys()) {
                                                                             if (key.startsWith(remaining)) {
                                                                                builder.suggest(key);
                                                                             }
                                                                          }
                                                                          return builder.buildFuture();
                                                                       })
                                                                       .executes(context -> {
                                                                          debugCommand.slayerStart(StringArgumentType.getString(context, "mob"));
                                                                          return 1;
                                                                       })
                                                       )
                                       )
                       )
      );
   }

   private CompletableFuture<Suggestions> suggestDebugPathTargets(SuggestionsBuilder builder) {
      String remaining = builder.getRemainingLowerCase();
      for (String key : debugCommand.getDwarvenPathTargetKeys()) {
         if (key.startsWith(remaining)) {
            builder.suggest(key);
         }
      }
      return builder.buildFuture();
   }

   private CompletableFuture<Suggestions> suggestRouteNames(SuggestionsBuilder builder) {
      String remaining = builder.getRemainingLowerCase();
      for (String name : RouteHandler.getInstance().getRoutes().keySet()) {
         if (name.toLowerCase().startsWith(remaining)) {
            builder.suggest(name);
         }
      }
      return builder.buildFuture();
   }

   private CompletableFuture<Suggestions> suggestRouteWaypointTypes(SuggestionsBuilder builder) {
      String remaining = builder.getRemainingLowerCase();
      String[] types = {"walk", "etherwarp", "mine"};
      for (String type : types) {
         if (type.startsWith(remaining)) {
            builder.suggest(type);
         }
      }
      return builder.buildFuture();
   }

   private void registerRouteBuilderCommand(
           CommandDispatcher<FabricClientCommandSource> dispatcher
   ) {
      dispatcher.register(
              literal("rb")
                      .executes(context -> {
                         routeBuilderCommand.main();
                         return 1;
                      })
                      .then(
                              literal("list").executes(context -> {
                                 routeBuilderCommand.list();
                                 return 1;
                              })
                      )
                      .then(
                              literal("reload").executes(context -> {
                                 routeBuilderCommand.reload();
                                 return 1;
                              })
                      )
                      .then(
                              literal("keys").executes(context -> {
                                 routeBuilderCommand.keys();
                                 return 1;
                              })
                      )
                      .then(
                              literal("toggle").executes(context -> {
                                 routeBuilderCommand.toggle();
                                 return 1;
                              })
                      )
                      .then(
                              literal("new").then(
                                      argument(
                                              "name",
                                              StringArgumentType.greedyString()
                                      ).executes(context -> {
                                         routeBuilderCommand.create(
                                                 StringArgumentType.getString(context, "name")
                                         );
                                         return 1;
                                      })
                              )
                      )
                      .then(
                              literal("select").then(
                                      argument(
                                              "name",
                                              StringArgumentType.greedyString()
                                      ).suggests((context, builder) -> suggestRouteNames(builder))
                                              .executes(context -> {
                                                 routeBuilderCommand.select(
                                                         StringArgumentType.getString(context, "name")
                                                 );
                                                 return 1;
                                              })
                              )
                      )
                      .then(
                              literal("delete").then(
                                      argument(
                                              "name",
                                              StringArgumentType.greedyString()
                                      ).suggests((context, builder) -> suggestRouteNames(builder))
                                              .executes(context -> {
                                         routeBuilderCommand.delete(
                                                 StringArgumentType.getString(context, "name")
                                         );
                                         return 1;
                                      })
                              )
                      )
                      .then(
                              literal("add").then(
                                      argument("type", StringArgumentType.word())
                                              .suggests((context, builder) -> suggestRouteWaypointTypes(builder))
                                              .executes(
                                              context -> {
                                                 routeBuilderCommand.add(
                                                         StringArgumentType.getString(
                                                                 context,
                                                                 "type"
                                                         )
                                                 );
                                                 return 1;
                                              }
                                      )
                              )
                      )
                      .then(
                              literal("remove").then(
                                      argument(
                                              "index",
                                              IntegerArgumentType.integer()
                                      ).executes(context -> {
                                         routeBuilderCommand.remove(
                                                 IntegerArgumentType.getInteger(context, "index")
                                         );
                                         return 1;
                                      })
                              )
                      )
                      .then(
                              literal("replace").then(
                                      argument("index", IntegerArgumentType.integer()).then(
                                              argument(
                                                      "type",
                                                      StringArgumentType.word()
                                              ).suggests((context, builder) -> suggestRouteWaypointTypes(builder))
                                                      .executes(context -> {
                                                 routeBuilderCommand.replace(
                                                         IntegerArgumentType.getInteger(
                                                                 context,
                                                                 "index"
                                                         ),
                                                         StringArgumentType.getString(
                                                                 context,
                                                                 "type"
                                                         )
                                                 );
                                                 return 1;
                                              })
                                      )
                              )
                      )
      );
   }

   private void registerVeinForgeCommand(
           CommandDispatcher<FabricClientCommandSource> dispatcher,
           String root,
           Command<FabricClientCommandSource> openConfigAction
   ) {
      dispatcher.register(
              literal(root)
                      .executes(openConfigAction)
                      .then(buildConfigSetCommand())
      );
   }

   private LiteralArgumentBuilder<FabricClientCommandSource> buildConfigSetCommand() {
      return literal("set")
                     .then(
                             literal("mining-tool").executes(
                                     context -> executeClientAction(ConfigActions::setMiningToolCommand)
                             )
                     )
                     .then(
                             literal("alt-mining-tool").executes(
                                     context -> executeClientAction(ConfigActions::setAltMiningToolCommand)
                             )
                     )
                     .then(
                             literal("slayer-weapon").executes(
                                     context -> executeClientAction(ConfigActions::setSlayerWeaponCommand)
                             )
                     )
                     .then(
                             literal("fishing-rod").executes(
                                     context -> executeClientAction(ConfigActions::setFishingRodCommand)
                             )
                     )
                     .then(
                             literal("fishing-axe").executes(
                                     context -> executeClientAction(ConfigActions::setGalateaAxeCommand)
                             )
                     )
                     .then(
                             literal("fishing-weapon").executes(
                                     context -> executeClientAction(ConfigActions::setGalateaFishingWeaponCommand)
                             )
                     )
                     .then(
                             literal("fishing-secondary-weapon").executes(
                                     context -> executeClientAction(ConfigActions::setGalateaFishingWeaponCommand)
                             )
                     );
   }

   private int executeClientAction(Runnable action) {
      Minecraft client = Minecraft.getInstance();
      if (client == null) {
         return 0;
      }
      client.execute(action);
      return 1;
   }
}
