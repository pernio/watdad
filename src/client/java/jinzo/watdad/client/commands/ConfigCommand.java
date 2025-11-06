package jinzo.watdad.client.commands;

import com.mojang.brigadier.tree.LiteralCommandNode;
import jinzo.watdad.client.WatdadConfig;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.text.Text;

import static com.mojang.brigadier.arguments.IntegerArgumentType.getInteger;
import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static jinzo.watdad.client.WatdadClient.*;
import static jinzo.watdad.client.WatdadClient.sendSuccess;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

public class ConfigCommand {
    public static LiteralCommandNode<FabricClientCommandSource> register() {
        WatdadConfig config = getConfig();
        return literal("config")
                .then(literal("range")
                        .then(argument("value", integer(1, 100))
                                .executes(ctx -> {
                                    config.oreRenderRange = getInteger(ctx, "value");
                                    saveConfig();
                                    ctx.getSource().sendFeedback(Text.literal("Ore render range set to " + getConfig().oreRenderRange));
                                    return 1;
                                })
                        )
                ).then(literal("expose")
                        .executes(ctx -> {
                            config.exposeOres = !getConfig().exposeOres;
                            saveConfig();
                            sendSuccess(ctx.getSource(), "Exposed ore highlighting " + (getConfig().exposeOres ? "enabled" : "disabled"));
                            return 1;
                        })
                        .then(literal("on").executes(ctx -> {
                            config.exposeOres = true;
                            saveConfig();
                            sendSuccess(ctx.getSource(), "Exposed ore highlighting enabled");
                            return 1;
                        }))
                        .then(literal("off").executes(ctx -> {
                            config.exposeOres = false;
                            saveConfig();
                            sendSuccess(ctx.getSource(), "Exposed ore highlighting disabled");
                            return 1;
                        }))
                ).then(literal("ores")
                        .executes(ctx -> {
                            config.displayHighlightOres = !getConfig().displayHighlightOres;
                            saveConfig();
                            sendSuccess(ctx.getSource(), "Ore highlighting " + (getConfig().displayHighlightOres ? "enabled" : "disabled"));
                            return 1;
                        })
                        .then(literal("on").executes(ctx -> {
                            config.displayHighlightOres = true;
                            saveConfig();
                            sendSuccess(ctx.getSource(), "Ore highlighting enabled");
                            return 1;
                        }))
                        .then(literal("off").executes(ctx -> {
                            config.displayHighlightOres = false;
                            saveConfig();
                            sendSuccess(ctx.getSource(), "Ore highlighting disabled");
                            return 1;
                        }))
                ).then(literal("inspect")
                        .executes(ctx -> {
                            config.displayHighlightBlock = !getConfig().displayHighlightBlock;
                            saveConfig();
                            sendSuccess(ctx.getSource(), "Inspect highlighting " + (getConfig().displayHighlightBlock ? "enabled" : "disabled"));
                            return 1;
                        })
                        .then(literal("on").executes(ctx -> {
                            config.displayHighlightBlock = true;
                            saveConfig();
                            sendSuccess(ctx.getSource(), "Inspect highlighting enabled");
                            return 1;
                        }))
                        .then(literal("off").executes(ctx -> {
                            config.displayHighlightBlock = false;
                            saveConfig();
                            sendSuccess(ctx.getSource(), "Inspect highlighting disabled");
                            return 1;
                        }))
                ).build();
    }
}
