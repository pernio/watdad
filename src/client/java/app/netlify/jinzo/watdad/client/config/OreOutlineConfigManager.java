package app.netlify.jinzo.watdad.client.config;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

public final class OreOutlineConfigManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("watdad");
    private static final int DEFAULT_SCAN_RADIUS = 24;
    private static final long DEFAULT_SCAN_INTERVAL_TICKS = 20L;
    private static final String UNEXPOSED_BLOCKS_KEY = "UNEXPOSED_BLOCKS";

    private final Path configDirectory = FabricLoader.getInstance().getConfigDir().resolve("watdad");
    private final Path oreFile = configDirectory.resolve("blocks.yml");
    private final Path settingsFile = configDirectory.resolve("client.properties");
    private final Yaml yamlReader = new Yaml(new SafeConstructor(new LoaderOptions()));
    private final Yaml yamlWriter = createYamlWriter();

    private OreOutlineConfig config = OreOutlineConfig.defaults();
    private long oreFileModified;
    private long settingsFileModified;

    public void loadOrCreate() {
        try {
            Files.createDirectories(configDirectory);
            if (Files.notExists(oreFile)) {
                writeDefaultOreFile();
            }
            if (Files.notExists(settingsFile)) {
                writeSettings(new Settings(true, DEFAULT_SCAN_RADIUS, OreOutlineConfig.RenderMode.OUTLINE));
            }
            reload();
        } catch (IOException exception) {
            LOGGER.error("Failed to initialize Watdad config", exception);
            config = OreOutlineConfig.defaults();
        }
    }

    public void reloadIfChanged() {
        try {
            long currentOreModified = Files.exists(oreFile) ? Files.getLastModifiedTime(oreFile).toMillis() : -1L;
            long currentSettingsModified = Files.exists(settingsFile) ? Files.getLastModifiedTime(settingsFile).toMillis() : -1L;
            if (currentOreModified != oreFileModified || currentSettingsModified != settingsFileModified) {
                reload();
            }
        } catch (IOException exception) {
            LOGGER.error("Failed checking Watdad config timestamps", exception);
        }
    }

    public OreOutlineConfig getConfig() {
        return config;
    }

    public void saveMenuSettings(boolean exposedOnly, int scanRadius) {
        saveMenuSettings(exposedOnly, scanRadius, config.renderMode());
    }

    public void saveMenuSettings(boolean exposedOnly, int scanRadius, OreOutlineConfig.RenderMode renderMode) {
        Settings settings = new Settings(exposedOnly, Math.max(1, scanRadius), renderMode);
        try {
            writeSettings(settings);
            reload();
        } catch (IOException exception) {
            LOGGER.error("Failed to save Watdad menu settings", exception);
        }
    }

    public Path getOreFile() {
        return oreFile;
    }

    private void reload() {
        try {
            Settings settings = readSettings();
            ParsedBlocks parsedBlocks = readOres();
            config = new OreOutlineConfig(
                    true,
                    settings.exposedOnly(),
                    settings.scanRadius(),
                    DEFAULT_SCAN_INTERVAL_TICKS,
                    settings.renderMode(),
                    parsedBlocks.unexposedBlocksColor(),
                    Map.copyOf(parsedBlocks.blocks())
            );
            oreFileModified = Files.exists(oreFile) ? Files.getLastModifiedTime(oreFile).toMillis() : -1L;
            settingsFileModified = Files.exists(settingsFile) ? Files.getLastModifiedTime(settingsFile).toMillis() : -1L;
        } catch (IOException exception) {
            LOGGER.error("Failed to reload Watdad config", exception);
            config = OreOutlineConfig.defaults();
        }
    }

    private Settings readSettings() throws IOException {
        Properties properties = new Properties();
        if (Files.exists(settingsFile)) {
            try (Reader reader = Files.newBufferedReader(settingsFile)) {
                properties.load(reader);
            }
        }

        boolean exposedOnly = Boolean.parseBoolean(properties.getProperty("exposedOnly", "true"));
        int scanRadius = parsePositiveInt(properties.getProperty("scanRadius"), DEFAULT_SCAN_RADIUS);
        OreOutlineConfig.RenderMode renderMode = parseRenderMode(
                properties.getProperty("renderMode"),
                OreOutlineConfig.RenderMode.OUTLINE
        );
        return new Settings(exposedOnly, scanRadius, renderMode);
    }

    private ParsedBlocks readOres() throws IOException {
        Map<Block, OreOutlineConfig.OutlineColor> ores = new LinkedHashMap<>();
        OreOutlineConfig.OutlineColor unexposedBlocksColor = OreOutlineConfig.defaults().unexposedBlocksColor();
        try (Reader reader = Files.newBufferedReader(oreFile)) {
            Object loaded = yamlReader.load(reader);
            if (!(loaded instanceof Map<?, ?> rawMap)) {
                return new ParsedBlocks(ores, unexposedBlocksColor);
            }

            for (Map.Entry<?, ?> entry : rawMap.entrySet()) {
                if (!(entry.getKey() instanceof String key) || !(entry.getValue() instanceof String value)) {
                    continue;
                }

                if (UNEXPOSED_BLOCKS_KEY.equalsIgnoreCase(key.trim())) {
                    try {
                        unexposedBlocksColor = parseColor(value);
                    } catch (IllegalArgumentException exception) {
                        LOGGER.warn("Skipping invalid color '{}' for '{}'", value, key);
                    }
                    continue;
                }

                Identifier identifier = parseBlockIdentifier(key);
                if (identifier == null || !BuiltInRegistries.BLOCK.containsKey(identifier)) {
                    LOGGER.warn("Skipping unknown ore key '{}'", key);
                    continue;
                }

                try {
                    ores.put(BuiltInRegistries.BLOCK.getValue(identifier), parseColor(value));
                } catch (IllegalArgumentException exception) {
                    LOGGER.warn("Skipping invalid color '{}' for '{}'", value, key);
                }
            }
        }
        return new ParsedBlocks(ores, unexposedBlocksColor);
    }

    private void writeDefaultOreFile() throws IOException {
        Map<String, String> defaultOres = new LinkedHashMap<>();
        defaultOres.put(UNEXPOSED_BLOCKS_KEY, "#FF66CC");
        defaultOres.put("COAL_ORE", "#2B2B2B");
        defaultOres.put("DEEPSLATE_COAL_ORE", "#3D3D3D");
        defaultOres.put("IRON_ORE", "#D8AF93");
        defaultOres.put("DEEPSLATE_IRON_ORE", "#B08968");
        defaultOres.put("COPPER_ORE", "#C97542");
        defaultOres.put("DEEPSLATE_COPPER_ORE", "#A85E2F");
        defaultOres.put("GOLD_ORE", "#F2D64B");
        defaultOres.put("DEEPSLATE_GOLD_ORE", "#C9A227");
        defaultOres.put("REDSTONE_ORE", "#FF2E2E");
        defaultOres.put("DEEPSLATE_REDSTONE_ORE", "#C40000");
        defaultOres.put("EMERALD_ORE", "#2EE66B");
        defaultOres.put("DEEPSLATE_EMERALD_ORE", "#1FA34D");
        defaultOres.put("LAPIS_ORE", "#2D5BFF");
        defaultOres.put("DEEPSLATE_LAPIS_ORE", "#1D3FC2");
        defaultOres.put("DIAMOND_ORE", "#41F0FF");
        defaultOres.put("DEEPSLATE_DIAMOND_ORE", "#00B8D9");
        defaultOres.put("NETHER_GOLD_ORE", "#F0BE2D");
        defaultOres.put("ANCIENT_DEBRIS", "#7A4B36");

        try (Writer writer = Files.newBufferedWriter(oreFile)) {
            writer.write("# Vanilla block ids can be written as short names like COAL_ORE or full ids like minecraft:coal_ore.\n");
            writer.write("# UNEXPOSED_BLOCKS sets the fallback color used for tracked blocks without exposed faces.\n");
            writer.write("# Format: BLOCK_NAME: \"#RRGGBB\"\n");
            yamlWriter.dump(defaultOres, writer);
        }
    }

    private void writeSettings(Settings settings) throws IOException {
        Properties properties = new Properties();
        properties.setProperty("exposedOnly", Boolean.toString(settings.exposedOnly()));
        properties.setProperty("scanRadius", Integer.toString(settings.scanRadius()));
        properties.setProperty("renderMode", settings.renderMode().name());
        try (Writer writer = Files.newBufferedWriter(settingsFile)) {
            properties.store(writer, "Watdad menu settings");
        }
    }

    private Identifier parseBlockIdentifier(String rawKey) {
        String normalized = rawKey.trim();
        if (normalized.isEmpty()) {
            return null;
        }

        if (normalized.contains(":")) {
            return Identifier.tryParse(normalized.toLowerCase(Locale.ROOT));
        }

        return Identifier.tryParse("minecraft:" + normalized.toLowerCase(Locale.ROOT));
    }

    private OreOutlineConfig.OutlineColor parseColor(String hex) {
        String normalized = hex.startsWith("#") ? hex.substring(1) : hex;
        if (normalized.length() != 6) {
            throw new IllegalArgumentException("Expected 6 hex digits");
        }
        return OreOutlineConfig.OutlineColor.fromRgb(Integer.parseInt(normalized, 16));
    }

    private int parsePositiveInt(String value, int fallback) {
        if (value == null) {
            return fallback;
        }
        try {
            return Math.max(1, Integer.parseInt(value));
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    private OreOutlineConfig.RenderMode parseRenderMode(String value, OreOutlineConfig.RenderMode fallback) {
        if (value == null) {
            return fallback;
        }

        try {
            return OreOutlineConfig.RenderMode.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return fallback;
        }
    }

    private Yaml createYamlWriter() {
        DumperOptions dumperOptions = new DumperOptions();
        dumperOptions.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        dumperOptions.setPrettyFlow(true);
        return new Yaml(dumperOptions);
    }

    private record Settings(boolean exposedOnly, int scanRadius, OreOutlineConfig.RenderMode renderMode) {
    }

    private record ParsedBlocks(
            Map<Block, OreOutlineConfig.OutlineColor> blocks,
            OreOutlineConfig.OutlineColor unexposedBlocksColor
    ) {
    }
}
