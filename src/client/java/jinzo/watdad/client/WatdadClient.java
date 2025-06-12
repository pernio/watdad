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
import jinzo.watdad.client.commands.TeleportCommand;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

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
    public static boolean staffSessionActive = false;
    public static BlockPos lastHighlightedBlock = null;
    public static final Map<Block, float[]> ORE_COLORS = new HashMap<>();
    public static final List<String> cachedTownNames = new CopyOnWriteArrayList<>();
    public static final String apiUrl = "https://api.earthmc.net/v3/aurora/";
    private static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private static final Path CONFIG_PATH = Path.of("config", "watdad_config.json");
    public static KeyBinding holdRevealKey;

    static {
        // Start periodic updates of town names
        scheduler.scheduleAtFixedRate(WatdadClient::updateCachedTownNames, 0, 60, TimeUnit.SECONDS);

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
    }

    @Override
    public void onInitializeClient() {
        AutoConfig.register(WatdadConfig.class, JanksonConfigSerializer::new);
        loadConfig();

        registerKeybindings();

        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            handleChat(message.getString());

            String msg = message.getString();
            if (msg.startsWith("CoreProtect - Inspector now enabled")) {
                highlightBlock = true;
                saveConfig();
            } else if (msg.startsWith("CoreProtect - Inspector now disabled")) {
                highlightBlock = false;
                saveConfig();
            } else if (msg.startsWith("Staff session started")) {
                staffSessionActive = true;
                saveConfig();
            } else if (msg.startsWith("Staff session ended.")) {
                staffSessionActive = false;
                saveConfig();
            }
        });

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            registerCommands(dispatcher);

            // Independent commands (not nested under /watdad or /wd)
            dispatcher.register(TeleportCommand.register());
            dispatcher.register(PunishCommand.register());
        });

        HudRenderCallback.EVENT.register((drawContext, tickDelta) -> {
            if (getConfig().displayStaffSession && staffSessionActive) {
                MinecraftClient client = MinecraftClient.getInstance();
                TextRenderer textRenderer = client.textRenderer;
                String text = "Staff session is active";
                int textWidth = textRenderer.getWidth(text);
                int x = client.getWindow().getScaledWidth() - textWidth - 5; // X position from the right
                int y = client.getWindow().getScaledHeight() - 25; // Y position from the bottom
                int color = 0xFFFFFF; // White color

                drawContext.drawText(textRenderer, text, x, y, color, true);
            }
        });

        WorldRenderEvents.AFTER_TRANSLUCENT.register(HighlightCommand::renderBlockOutline);
    }

    private void handleChat(String msg) {
        // Match exactly messages like "[<] PlayerName [>]" with no colon
        if (msg.startsWith("[<]") && msg.endsWith("[>]") && !msg.contains(":")) {
            int start = msg.indexOf("[<]");
            int end = msg.indexOf("[>]");
            if (start != -1 && end != -1 && end > start) {
                String playername = msg.substring(start + 3, end).trim();
                TeleportCommand.teleportTo(playername);
            }
        }
    }

    private void registerCommands(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        // Root command
        var watdadRoot = literal("watdad");

        // Attach calculate under /watdad
        watdadRoot.then(CalculateCommand.register());
        watdadRoot.then(MailCommand.register());
        watdadRoot.then(ConfigCommand.register());

        // Register the watdad root
        dispatcher.register(watdadRoot);

        // Alias /wd -> /watdad
        dispatcher.register(
                literal("wd")
                        .redirect(dispatcher.getRoot().getChild("watdad"))
        );

        dispatcher.register(PunishCommand.createWarnMuteCommand());
        dispatcher.register(PunishCommand.createWarnKickCommand());
    }

    public static void saveConfig() {
        AutoConfig.getConfigHolder(WatdadConfig.class).save();

        try {
            Files.createDirectories(CONFIG_PATH.getParent());

            Map<String, Object> config = new HashMap<>();
            config.put("highlightBlock", highlightBlock);
            config.put("staffSessionActive", staffSessionActive);

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

                    Object staffSessionActiveObj = config.get("staffSessionActive");
                    if (staffSessionActiveObj instanceof Boolean) {
                        staffSessionActive = (Boolean) staffSessionActiveObj;
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static class Town {
        public String name;
        public int size;
        public String mayor;
    }

    public static void updateCachedTownNames() {
        try {
            URL url = new URL(apiUrl + "towns"); // Replace with actual URL
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.connect();

            List<String> updatedNames = new ArrayList<>();

            try (InputStreamReader reader = new InputStreamReader(connection.getInputStream())) {
                JsonArray array = JsonParser.parseReader(reader).getAsJsonArray();
                for (JsonElement element : array) {
                    JsonObject obj = element.getAsJsonObject();
                    if (obj.has("name")) {
                        updatedNames.add(obj.get("name").getAsString());
                    }
                }
            }

            cachedTownNames.clear();
            cachedTownNames.addAll(updatedNames);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static Town fetchTownByName(String name) {
        try {
            URL url = new URL(apiUrl + "towns");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoOutput(true);

            // Create JSON body
            JsonObject payload = new JsonObject();
            JsonArray array = new JsonArray();
            array.add(name);
            payload.add("query", array);
            String jsonInputString = new Gson().toJson(payload);

            // Send POST body
            try (var os = connection.getOutputStream()) {
                byte[] input = jsonInputString.getBytes("utf-8");
                os.write(input, 0, input.length);
            }

            // Parse response
            try (InputStreamReader reader = new InputStreamReader(connection.getInputStream())) {
                JsonArray response = JsonParser.parseReader(reader).getAsJsonArray();
                if (response.size() == 0) return null;

                JsonObject obj = response.get(0).getAsJsonObject();
                String townName = obj.get("name").getAsString();
                int townSize = obj.getAsJsonObject("stats").get("numTownBlocks").getAsInt();
                String townMayor = obj.getAsJsonObject("mayor").get("name").getAsString();

                Town town = new Town();
                town.name = townName;
                town.size = townSize;
                town.mayor = townMayor;
                return town;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static final SuggestionProvider<FabricClientCommandSource> TOWN_NAME_SUGGESTIONS = (context, builder) -> {
        String userInput = builder.getRemaining().toLowerCase();

        for (String name : cachedTownNames) {
            if (name.toLowerCase().startsWith(userInput)) {
                builder.suggest(name);
            }
        }

        return builder.buildFuture();
    };

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
