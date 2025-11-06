package jinzo.watdad.client;

import com.google.gson.*;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import jinzo.watdad.client.commands.*;
import me.shedaniel.autoconfig.AutoConfig;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.BufferedWriter;
import java.io.IOException;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;
import me.shedaniel.autoconfig.serializer.JanksonConfigSerializer;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class WatdadClient implements ClientModInitializer {
    public static boolean highlightBlock = false;
    public static BlockPos lastHighlightedBlock = null;
    public static final Map<Block, float[]> ORE_COLORS = new HashMap<>();
    private static final Path CONFIG_PATH = Path.of("config", "watdad_config.json");
    public static KeyBinding holdRevealKey;

    static {
        ORE_COLORS.put(Blocks.GOLD_ORE, new float[]{1f, 0.85f, 0f});
        ORE_COLORS.put(Blocks.DEEPSLATE_GOLD_ORE, new float[]{1f, 0.85f, 0f});
        ORE_COLORS.put(Blocks.DIAMOND_ORE, new float[]{0f, 0.7f, 1f});
        ORE_COLORS.put(Blocks.DEEPSLATE_DIAMOND_ORE, new float[]{0f, 0.7f, 1f});
        ORE_COLORS.put(Blocks.IRON_ORE, new float[]{0.8f, 0.5f, 0.2f});
        ORE_COLORS.put(Blocks.DEEPSLATE_IRON_ORE, new float[]{0.8f, 0.5f, 0.2f});
        ORE_COLORS.put(Blocks.EMERALD_ORE, new float[]{0f, 1f, 0.5f});
        ORE_COLORS.put(Blocks.DEEPSLATE_EMERALD_ORE, new float[]{0f, 1f, 0.5f});
        ORE_COLORS.put(Blocks.LAPIS_ORE, new float[]{0.3f, 0.3f, 1f});
        ORE_COLORS.put(Blocks.DEEPSLATE_LAPIS_ORE, new float[]{0.3f, 0.3f, 1f});
        ORE_COLORS.put(Blocks.REDSTONE_ORE, new float[]{1f, 0f, 0f});
        ORE_COLORS.put(Blocks.DEEPSLATE_REDSTONE_ORE, new float[]{1f, 0f, 0f});
        ORE_COLORS.put(Blocks.COAL_ORE, new float[]{0.2f, 0.2f, 0.2f});
        ORE_COLORS.put(Blocks.DEEPSLATE_COAL_ORE, new float[]{0.2f, 0.2f, 0.2f});
        ORE_COLORS.put(Blocks.COPPER_ORE, new float[]{0.8f, 0.5f, 0.2f});
        ORE_COLORS.put(Blocks.DEEPSLATE_COPPER_ORE, new float[]{0.8f, 0.5f, 0.2f});
        ORE_COLORS.put(Blocks.NETHER_GOLD_ORE, new float[]{1f, 0.85f, 0f});
        ORE_COLORS.put(Blocks.NETHER_QUARTZ_ORE, new float[]{1f, 1f, 1f});
        ORE_COLORS.put(Blocks.ANCIENT_DEBRIS, new float[]{0.5f, 0.3f, 0.1f});
    }

    @Override
    public void onInitializeClient() {
        AutoConfig.register(WatdadConfig.class, JanksonConfigSerializer::new);
        loadConfig();

        registerKeybindings();

        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            String msg = message.getString();
            if (msg.startsWith("CoreProtect - Inspector now enabled")) {
                highlightBlock = true;
                saveConfig();
            } else if (msg.startsWith("CoreProtect - Inspector now disabled")) {
                highlightBlock = false;
                saveConfig();
            }
        });

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            registerCommands(dispatcher);
            dispatcher.register(XrayCommand.register());
        });

        WorldRenderEvents.AFTER_TRANSLUCENT.register(HighlightCommand::renderBlockOutline);
    }

    private void registerCommands(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        // Root command
        var watdadRoot = literal("watdad");

        // Attach calculate under /watdad
        watdadRoot.then(ConfigCommand.register());

        // Register the watdad root
        dispatcher.register(watdadRoot);

        // Alias /wd -> /watdad
        dispatcher.register(
                literal("wd")
                        .redirect(dispatcher.getRoot().getChild("watdad"))
        );
    }

    public static void saveConfig() {
        AutoConfig.getConfigHolder(WatdadConfig.class).save();

        try {
            Files.createDirectories(CONFIG_PATH.getParent());

            Map<String, Object> config = new HashMap<>();
            config.put("highlightBlock", highlightBlock);

            Gson gson = new GsonBuilder().setPrettyPrinting().create();

            try (BufferedWriter writer = Files.newBufferedWriter(CONFIG_PATH)) {
                gson.toJson(config, writer);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadConfig() {
        if (Files.exists(CONFIG_PATH)) {
            try (BufferedReader reader = Files.newBufferedReader(CONFIG_PATH)) {
                Gson gson = new Gson();
                Map<?, ?> config = gson.fromJson(reader, Map.class);

                if (config != null) {
                    Object highlightBlockObj = config.get("highlightBlock");
                    if (highlightBlockObj instanceof Boolean) {
                        highlightBlock = (Boolean) highlightBlockObj;
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void registerKeybindings() {
        holdRevealKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.watdad.holdReveal",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_G,
                "key.categories.watdad"
        ));
    }

    public static WatdadConfig getConfig() {
        return AutoConfig.getConfigHolder(WatdadConfig.class).getConfig();
    }

    public static void sendSuccess(FabricClientCommandSource source, String msg) {
        source.sendFeedback(Text.literal("[Watdad] ").formatted(Formatting.LIGHT_PURPLE)
                .append(Text.literal(msg).formatted(Formatting.GREEN)));
    }

    public static void sendError(FabricClientCommandSource source, String msg) {
        source.sendFeedback(Text.literal("[Watdad] ").formatted(Formatting.LIGHT_PURPLE)
                .append(Text.literal(msg).formatted(Formatting.RED)));
    }

    public static void sendInfo(FabricClientCommandSource source, String msg) {
        source.sendFeedback(Text.literal("[Watdad] ").formatted(Formatting.LIGHT_PURPLE)
                .append(Text.literal(msg).formatted(Formatting.WHITE)));
    }
}
