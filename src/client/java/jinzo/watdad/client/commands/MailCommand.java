package jinzo.watdad.client.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import jinzo.watdad.client.WatdadClient;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.MinecraftClient;

import static jinzo.watdad.client.WatdadClient.*;
import static jinzo.watdad.client.WatdadClient.fetchTownByName;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

public class MailCommand {
    public static LiteralCommandNode<FabricClientCommandSource> register() {
        return literal("mail")
                .then(literal("terraform")
                    .then(literal("contact")
                            .then(argument("town", StringArgumentType.greedyString())
                                    .suggests(TOWN_NAME_SUGGESTIONS)
                                    .executes(ctx -> {
                                        String message = ", is in violation of rule 3.5 terraform. You have 14 days to remove this terraform or we will remove it ourselves. For more info, /support";
                                        return sendMessage(ctx, message, "terraform contact");
                                    })
                            )
                    )
                    .then(literal("removed")
                            .then(argument("town", StringArgumentType.greedyString())
                                    .suggests(TOWN_NAME_SUGGESTIONS)
                                    .executes(ctx -> {
                                        String message = ", has exceeded the 14-day time limit by violating Rule 3.5. As a result, the terraform has been removed. For more info, /support";
                                        return sendMessage(ctx, message, "removed terraform");
                                    })
                            )
                    )
                )
                .then(literal("claim")
                        .then(literal("arm")
                            .then(argument("town", StringArgumentType.greedyString())
                                    .suggests(TOWN_NAME_SUGGESTIONS)
                                    .executes(ctx -> {
                                        String message = ", is in violation of rule 3.3 claim arm. This is a 14 day notice before we take action. For more info, /support";
                                        return sendMessage(ctx, message, "claim arm");
                                    })
                            )
                        )
                        .then(literal("hollow")
                                .then(argument("town", StringArgumentType.greedyString())
                                        .suggests(TOWN_NAME_SUGGESTIONS)
                                        .executes(ctx -> {
                                            String message = ", is in violation of rule 3.3 hollow claims. This is a 14 day notice before we take action. For more info, /support";
                                            return sendMessage(ctx, message, "hollow claims");
                                        })
                                )
                        )
                )
                .build();
    }

    private static int sendMessage(CommandContext<FabricClientCommandSource> ctx, String message, String type) {
        String townName = StringArgumentType.getString(ctx, "town");
        WatdadClient.Town town = fetchTownByName(townName);

        if (town == null) {
            sendInfo(ctx.getSource(), "Town not found: " + townName);
            return 0;
        }

        MinecraftClient.getInstance().keyboard.setClipboard("/mail send " + town.mayor + " Hello, this mail is on behalf of the EMC staff team. Your town, " + town.name + message);
        sendInfo(ctx.getSource(), "Copied " + type + " message to clipboard.");
        return 1;
    }
}
