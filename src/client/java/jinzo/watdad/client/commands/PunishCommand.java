package jinzo.watdad.client.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import jinzo.watdad.client.PunishmentData;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.MinecraftClient;

import java.util.List;
import java.util.Map;

import static jinzo.watdad.client.WatdadClient.sendSuccess;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;
import static net.minecraft.command.CommandSource.suggestMatching;

public class PunishCommand {
    static Map<String, PunishmentData.PunishmentType> punishmentMap = PunishmentData.load();

    public static LiteralArgumentBuilder<FabricClientCommandSource> register() {
        return literal("punish")
                .then(argument("playername", StringArgumentType.word())
                        .suggests((ctx, builder) -> {
                            var client = MinecraftClient.getInstance();
                            if (client.getNetworkHandler() != null) {
                                List<String> names = client.getNetworkHandler().getPlayerList().stream()
                                        .map(entry -> entry.getProfile().getName())
                                        .toList();
                                return suggestMatching(names, builder);
                            }
                            return builder.buildFuture();
                        })
                        .then(literal("mute")
                                .then(argument("duration", StringArgumentType.word())
                                        .suggests((ctx, builder) -> {
                                            // Suggest common durations
                                            return suggestMatching(punishmentMap.get("mute").durations, builder);
                                        })
                                        .then(argument("reason", StringArgumentType.greedyString())
                                                .suggests((ctx, builder) -> {
                                                    // Suggest common reasons
                                                    return suggestMatching(punishmentMap.get("mute").reasons, builder);
                                                })
                                                .executes(ctx -> {
                                                    MinecraftClient client = MinecraftClient.getInstance();
                                                    if (client.player == null || client.getNetworkHandler() == null) return 1;

                                                    String playerName = StringArgumentType.getString(ctx, "playername");
                                                    String duration = StringArgumentType.getString(ctx, "duration");
                                                    String reason = StringArgumentType.getString(ctx, "reason");

                                                    String command = duration.equals("permanent") ? String.format("mute %s %s", playerName, reason) : String.format("tempmute %s %s %s", playerName, duration, reason);
                                                    client.execute(() -> client.getNetworkHandler().sendCommand(command));
                                                    MinecraftClient.getInstance().keyboard.setClipboard("/" + command);
                                                    sendSuccess(ctx.getSource(), "Copied command to clipboard: \n/" + command);

                                                    return 1;
                                                })
                                        )
                                )
                        )
                        .then(literal("kick")
                                .then(argument("reason", StringArgumentType.greedyString())
                                        .suggests((ctx, builder) -> {
                                            // Suggest common durations
                                            return suggestMatching(punishmentMap.get("kick").reasons, builder);
                                        })
                                        .executes(ctx -> {
                                            MinecraftClient client = MinecraftClient.getInstance();
                                            if (client.player == null || client.getNetworkHandler() == null) return 1;

                                            String playerName = StringArgumentType.getString(ctx, "playername");
                                            String reason = StringArgumentType.getString(ctx, "reason");

                                            String command = String.format("kick %s %s", playerName, reason);
                                            client.execute(() -> client.getNetworkHandler().sendCommand(command));

                                            return 1;
                                        })
                                )

                        )
                        .then(literal("warn")
                                .then(argument("reason", StringArgumentType.greedyString())
                                        .suggests((ctx, builder) -> {
                                            // Suggest common reasons
                                            return suggestMatching(punishmentMap.get("warn").reasons, builder);
                                        })
                                        .executes(ctx -> {
                                            MinecraftClient client = MinecraftClient.getInstance();
                                            if (client.player == null || client.getNetworkHandler() == null) return 1;

                                            String playerName = StringArgumentType.getString(ctx, "playername");
                                            String reason = StringArgumentType.getString(ctx, "reason");

                                            String command = String.format("warn %s %s", playerName, reason);
                                            client.execute(() -> client.getNetworkHandler().sendCommand(command));

                                            return 1;
                                        })
                                )
                        )
                        .then(literal("ban")
                                .then(argument("duration", StringArgumentType.word())
                                        .suggests((ctx, builder) -> {
                                            // Suggest common durations
                                            return suggestMatching(punishmentMap.get("ban").durations, builder);
                                        })
                                        .then(argument("reason", StringArgumentType.greedyString())
                                                .suggests((ctx, builder) -> {
                                                    // Suggest common reasons
                                                    return suggestMatching(punishmentMap.get("ban").reasons, builder);
                                                })
                                                .executes(ctx -> {
                                                    MinecraftClient client = MinecraftClient.getInstance();
                                                    if (client.player == null || client.getNetworkHandler() == null) return 1;

                                                    String playerName = StringArgumentType.getString(ctx, "playername");
                                                    String duration = StringArgumentType.getString(ctx, "duration");
                                                    String reason = StringArgumentType.getString(ctx, "reason");

                                                    String command = duration.equals("permanent") ? String.format("ban %s %s", playerName, reason) : String.format("tempban %s %s %s", playerName, duration, reason);
                                                    client.execute(() -> client.getNetworkHandler().sendCommand(command));

                                                    return 1;
                                                })
                                        )
                                )
                        )
                        .then(literal("ipban")
                                .then(argument("reason", StringArgumentType.greedyString())
                                        .executes(ctx -> {
                                            MinecraftClient client = MinecraftClient.getInstance();
                                            if (client.player == null || client.getNetworkHandler() == null) return 1;

                                            String playerName = StringArgumentType.getString(ctx, "playername");
                                            String reason = StringArgumentType.getString(ctx, "reason");

                                            String command = String.format("ipban %s %s", playerName, reason);
                                            client.execute(() -> client.getNetworkHandler().sendCommand(command));

                                            return 1;
                                        })
                                )
                        )
                        .then(literal("warnmute")
                                .then(argument("duration", StringArgumentType.word())
                                        .suggests((ctx, builder) -> {
                                            // Suggest common durations
                                            return suggestMatching(punishmentMap.get("warnmute").durations, builder);
                                        })
                                        .then(argument("reason", StringArgumentType.greedyString())
                                                .suggests((ctx, builder) -> {
                                                    // Suggest common reasons
                                                    return suggestMatching(punishmentMap.get("warnmute").reasons, builder);
                                                })
                                                .executes(ctx -> {
                                                    MinecraftClient client = MinecraftClient.getInstance();
                                                    if (client.player == null || client.getNetworkHandler() == null) return 1;

                                                    String playerName = StringArgumentType.getString(ctx, "playername");
                                                    String duration = StringArgumentType.getString(ctx, "duration");
                                                    String reason = StringArgumentType.getString(ctx, "reason");

                                                    String command = duration.equals("permanent") ? String.format("mute %s %s", playerName, reason) : String.format("tempmute %s %s %s", playerName, duration, reason);
                                                    client.execute(() -> {
                                                        client.getNetworkHandler().sendCommand(command); // tempmute
                                                        try {
                                                            Thread.sleep(50); // small delay (50ms) to prevent race condition
                                                        } catch (InterruptedException ignored) {}
                                                        client.getNetworkHandler().sendCommand("warn " + playerName + " " + reason);
                                                    });
                                                    MinecraftClient.getInstance().keyboard.setClipboard("/" + command);
                                                    sendSuccess(ctx.getSource(), "Copied command to clipboard: \n/" + command);

                                                    return 1;
                                                })
                                        )
                                )
                        )
                        .then(literal("warnkick")
                                .then(argument("reason", StringArgumentType.greedyString())
                                        .suggests((ctx, builder) -> {
                                            // Suggest common reasons
                                            return suggestMatching(punishmentMap.get("warnkick").reasons, builder);
                                        })
                                        .executes(ctx -> {
                                            MinecraftClient client = MinecraftClient.getInstance();
                                            if (client.player == null || client.getNetworkHandler() == null) return 1;

                                            String playerName = StringArgumentType.getString(ctx, "playername");
                                            String reason = StringArgumentType.getString(ctx, "reason");

                                            client.execute(() -> {
                                                client.getNetworkHandler().sendCommand("kick " + playerName + " " + reason); // Kick
                                                try {
                                                    Thread.sleep(50); // Small delay (50ms) to prevent race condition
                                                } catch (InterruptedException ignored) {}
                                                client.getNetworkHandler().sendCommand("warn " + playerName + " " + reason); // Warn
                                            });

                                            return 1;
                                        })
                                )
                        )
                );
    }

    // Reusable method for /warnmute
    public static LiteralArgumentBuilder<FabricClientCommandSource> createWarnMuteCommand() {
        return literal("warnmute")
                .then(argument("playername", StringArgumentType.word())
                        .suggests((ctx, builder) -> {
                            var client = MinecraftClient.getInstance();
                            if (client.getNetworkHandler() != null) {
                                List<String> names = client.getNetworkHandler().getPlayerList().stream()
                                        .map(entry -> entry.getProfile().getName())
                                        .toList();
                                return suggestMatching(names, builder);
                            }
                            return builder.buildFuture();
                        })
                        .then(argument("duration", StringArgumentType.word())
                                .suggests((ctx, builder) -> suggestMatching(punishmentMap.get("warnmute").durations, builder))
                                .then(argument("reason", StringArgumentType.greedyString())
                                        .suggests((ctx, builder) -> suggestMatching(punishmentMap.get("warnmute").reasons, builder))
                                        .executes(ctx -> {
                                            MinecraftClient client = MinecraftClient.getInstance();
                                            if (client.player == null || client.getNetworkHandler() == null) return 1;

                                            String playerName = StringArgumentType.getString(ctx, "playername");
                                            String duration = StringArgumentType.getString(ctx, "duration");
                                            String reason = StringArgumentType.getString(ctx, "reason");

                                            String command = duration.equals("permanent")
                                                    ? String.format("mute %s %s", playerName, reason)
                                                    : String.format("tempmute %s %s %s", playerName, duration, reason);

                                            client.execute(() -> {
                                                client.getNetworkHandler().sendCommand(command);
                                                try {
                                                    Thread.sleep(50);
                                                } catch (InterruptedException ignored) {}
                                                client.getNetworkHandler().sendCommand("warn " + playerName + " " + reason);
                                            });

                                            client.keyboard.setClipboard("/" + command);
                                            sendSuccess(ctx.getSource(), "Copied command to clipboard: \n/" + command);
                                            return 1;
                                        })
                                )
                        )
                );
    }

    // Reusable method for /warnkick
    public static LiteralArgumentBuilder<FabricClientCommandSource> createWarnKickCommand() {
        return literal("warnkick")
                .then(argument("playername", StringArgumentType.word())
                        .suggests((ctx, builder) -> {
                            var client = MinecraftClient.getInstance();
                            if (client.getNetworkHandler() != null) {
                                List<String> names = client.getNetworkHandler().getPlayerList().stream()
                                        .map(entry -> entry.getProfile().getName())
                                        .toList();
                                return suggestMatching(names, builder);
                            }
                            return builder.buildFuture();
                        })
                        .then(argument("reason", StringArgumentType.greedyString())
                                .suggests((ctx, builder) -> suggestMatching(punishmentMap.get("warnkick").reasons, builder))
                                .executes(ctx -> {
                                    MinecraftClient client = MinecraftClient.getInstance();
                                    if (client.player == null || client.getNetworkHandler() == null) return 1;

                                    String playerName = StringArgumentType.getString(ctx, "playername");
                                    String reason = StringArgumentType.getString(ctx, "reason");

                                    client.execute(() -> {
                                        client.getNetworkHandler().sendCommand("kick " + playerName + " " + reason);
                                        try {
                                            Thread.sleep(50);
                                        } catch (InterruptedException ignored) {}
                                        client.getNetworkHandler().sendCommand("warn " + playerName + " " + reason);
                                    });

                                    return 1;
                                })
                        )
                );
    }
}
