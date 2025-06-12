package jinzo.watdad.client.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

import java.util.concurrent.ThreadLocalRandom;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

public class TeleportCommand {
    public static LiteralArgumentBuilder<FabricClientCommandSource> register() {
        return literal("tpr")
                .executes(ctx -> executeTeleport(ctx.getSource()))
                .then(literal("tprandom").executes(ctx -> executeTeleport(ctx.getSource())));
    }

    private static int executeTeleport(FabricClientCommandSource source) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) {
            source.sendFeedback(Text.literal("§cCould not find player"));
            return 0;
        }

        int x = ThreadLocalRandom.current().nextInt(-33275, 33076);
        int z = ThreadLocalRandom.current().nextInt(-16635, 16501);
        int y = 100;

        String command = String.format("%d %d %d", x, y, z);
        client.player.networkHandler.sendCommand("tp " + command);
        source.sendFeedback(Text.literal("§aTeleporting to: " + x + " " + y + " " + z));
        return 1;
    }

    public static void teleportTo(String player) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null && client.getNetworkHandler() != null) {
            System.out.println("[Watdad] Sending teleport command to: " + player);
            client.player.networkHandler.sendCommand("tpo " + player);
            client.player.networkHandler.sendCommand("dupeip " + player);
        } else {
            System.out.println("[Watdad] Player or network handler not available.");
        }
    }
}
