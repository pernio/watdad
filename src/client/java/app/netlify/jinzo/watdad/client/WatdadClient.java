package app.netlify.jinzo.watdad.client;

import app.netlify.jinzo.watdad.client.commands.XrayCommand;
import app.netlify.jinzo.watdad.client.config.OreOutlineConfigManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;

public class WatdadClient implements ClientModInitializer {
    private static final OreOutlineRenderer ORE_OUTLINE_RENDERER = new OreOutlineRenderer();
    private static OreOutlineConfigManager configManager;

    @Override
    public void onInitializeClient() {
        configManager = ORE_OUTLINE_RENDERER.getConfigManager();
        ORE_OUTLINE_RENDERER.initialize();
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
                dispatcher.register(XrayCommand.register())
        );
        LevelRenderEvents.BEFORE_GIZMOS.register(ORE_OUTLINE_RENDERER::render);
    }

    public static OreOutlineConfigManager getConfigManager() {
        return configManager;
    }
}
