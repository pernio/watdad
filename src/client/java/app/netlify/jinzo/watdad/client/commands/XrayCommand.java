package app.netlify.jinzo.watdad.client.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.Minecraft;

import java.util.List;

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
        LiteralArgumentBuilder<FabricClientCommandSource> root = ClientCommands.literal("xray")
                .executes(ctx -> executeCommand(ORES.getFirst()));

        for (String ore : ORES) {
            root.then(ClientCommands.literal(ore).executes(ctx -> executeCommand(ore)));
        }

        return root;
    }

    private static int executeCommand(String oreType) {
        Minecraft client = Minecraft.getInstance();
        if (client.player != null && client.player.connection != null) {
            client.player.connection.sendCommand("co l a:-block i:" + oreType + " t:1h");
            return 1;
        }

        System.out.println("[Watdad] Player or network handler not available.");
        return 0;
    }
}
