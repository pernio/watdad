package jinzo.watdad.client.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;
import net.minecraft.client.MinecraftClient;

public class XrayCommand {
    public static LiteralArgumentBuilder<FabricClientCommandSource> register() {
        return literal("xray")
                .executes(ctx -> {
                    executeCommand("gold_ore");
                    return 1;
                })
                .then(literal("gold_ore")
                        .executes(ctx -> {
                            executeCommand("gold_ore");
                            return 1;
                        })
                )
                .then(literal("deepslate_gold_ore")
                        .executes(ctx -> {
                            executeCommand("deepslate_gold_ore");
                            return 1;
                        })
                )
                .then(literal("diamond_ore")
                        .executes(ctx -> {
                            executeCommand("diamond_ore");
                            return 1;
                        })
                )
                .then(literal("deepslate_diamond_ore")
                        .executes(ctx -> {
                            executeCommand("deepslate_diamond_ore");
                            return 1;
                        })
                );
    }

    public static void executeCommand(String oreType) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null && client.getNetworkHandler() != null) {
            client.getNetworkHandler().sendChatCommand("co l a:-block i:" + oreType + " t:1h");
        } else {
            System.out.println("[Watdad] Player or network handler not available.");
        }
    }
}
