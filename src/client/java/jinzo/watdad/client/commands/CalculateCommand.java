package jinzo.watdad.client.commands;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.minecraft.client.MinecraftClient;

import static jinzo.watdad.client.WatdadClient.*;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;


public class CalculateCommand {
    public static LiteralCommandNode<FabricClientCommandSource> register() {
        return literal("calculate")
                .then(literal("town")
                        .then(argument("name", StringArgumentType.greedyString())
                                .suggests(TOWN_NAME_SUGGESTIONS)
                                .executes(ctx -> {
                                    String townName = StringArgumentType.getString(ctx, "name");
                                    Town town = fetchTownByName(townName);

                                    if (town == null) {
                                        sendInfo(ctx.getSource(), "Town not found: " + townName);
                                        return 0;
                                    }

                                    int result = 64 + (town.size - 1) * 16;
                                    sendInfo(ctx.getSource(), "Town \"" + town.name + "\" (" + town.size + " chunks) costed " + result + "g to make");
                                    return result;
                                })
                        )
                )
                .then(literal("chunk")
                        .then(argument("x", IntegerArgumentType.integer(-100000, 100000))
                                .then(argument("z", IntegerArgumentType.integer(-100000, 100000))
                                        .executes(ctx -> {
                                            int x = IntegerArgumentType.getInteger(ctx, "x");
                                            int z = IntegerArgumentType.getInteger(ctx, "z");
                                            int coordsX = x * 16;
                                            int coordsZ = z * 16;

                                            String result = coordsX + " 100 " + coordsZ;
                                            sendInfo(ctx.getSource(), "Result: " + result);
                                            return 1;
                                        })
                                        .then(literal("copy")
                                                .executes(ctx -> {
                                                    int x = IntegerArgumentType.getInteger(ctx, "x");
                                                    int z = IntegerArgumentType.getInteger(ctx, "z");
                                                    int coordsX = x * 16;
                                                    int coordsZ = z * 16;

                                                    String result = coordsX + " 100 " + coordsZ;

                                                    MinecraftClient.getInstance().keyboard.setClipboard(result);
                                                    sendInfo(ctx.getSource(), "Copied to clipboard: " + result);
                                                    return 1;
                                                })
                                        )
                                )
                        )
                )
                .build();
    }
}