package app.netlify.jinzo.watdad.client.compat;

import app.netlify.jinzo.watdad.client.screen.WatdadConfigScreen;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

import net.minecraft.client.gui.screens.Screen;

public final class WatdadModMenu implements ModMenuApi {
    @Override
    public ConfigScreenFactory<Screen> getModConfigScreenFactory() {
        return WatdadConfigScreen::new;
    }
}
