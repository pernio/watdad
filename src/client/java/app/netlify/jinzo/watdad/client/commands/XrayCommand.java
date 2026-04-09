package app.netlify.jinzo.watdad.client.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.MinecraftClient;

import java.util.List;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

public final class XrayCommand {
    private static final List<String> ORES = List.of(
            "gold_ore",
            "deepslate_gold_ore",
            "diamond_ore",
            "deepslate_diamond_ore",
            "ancient_debris",
            "nether_quartz_ore"
    );

    public static LiteralArgumentBuilder<FabricClientCommandSource> register() {
        LiteralArgumentBuilder<FabricClientCommandSource> root = literal("xray")
                .executes(ctx -> executeCommand(ORES.getFirst()));

        for (String ore : ORES) {
            root.then(literal(ore).executes(ctx -> executeCommand(ore)));
        }

        return root;
    }

    private static int executeCommand(String oreType) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null && client.getNetworkHandler() != null) {
            client.player.networkHandler.sendChatCommand("co l a:-block i:" + oreType + " t:1h");
            return 1;
        } else {
            System.out.println("[Watdad] Player or network handler not available.");
            return 0;
        }
    }
}
